import importlib.util
import json
from pathlib import Path
import tempfile
import threading
import time
import unittest
from concurrent.futures import ThreadPoolExecutor
from unittest.mock import patch
from urllib.error import HTTPError
from urllib.request import Request, urlopen

ROOT = Path(__file__).resolve().parents[1]
spec = importlib.util.spec_from_file_location('account_shim', ROOT / 'scripts/onyx-account-shim.py')
shim = importlib.util.module_from_spec(spec)
spec.loader.exec_module(shim)

class AccountShimTests(unittest.TestCase):
    def test_reader_grants_survive_session_rotation_and_do_not_follow_requests(self):
        with tempfile.TemporaryDirectory() as temp:
            state=Path(temp);(state/'credentials.json').write_text(json.dumps({'username':'admin','password':'lab'}))
            book='a'*32; (state/'reader-asset-allowlist.json').write_text(json.dumps({'bookIds':[book]}))
            self.assertNotIn(shim.UID+'-READER_LIBRARY',shim.session_channels(state))
            (state/'reader-session-grants-enabled').touch()
            calls=[]
            def request(path,method='GET',body=None,auth=None):
                calls.append((path,method,body))
                if path.endswith('/_session'):return {'cookie_name':'Cookie','session_id':'session-'+str(len(calls))}
                return {}
            with patch.object(shim.gateway,'STATE',state),patch.object(shim.gateway,'request',request):
                server=shim.Shim(0,state/'shim',18766)
                try:
                    d=server.enroll('device');d['requestedChannels']=['*','another-account-READER_LIBRARY']
                    first=server.session(d);d['sessionExpiresAt']=0;second=server.session(d)
                    grants=[b['admin_channels'] for p,m,b in calls if m=='PUT' and '/_user/' in p]
                    self.assertNotEqual(first['session_id'],second['session_id']);self.assertEqual(len(grants),2);self.assertEqual(grants[0],grants[1])
                    for channel in [shim.UID+'-READER_LIBRARY',shim.UID+'-MESSAGE',shim.UID+'-'+book,shim.UID+'-ASSET-'+book]:self.assertIn(channel,grants[0])
                    self.assertNotIn('*',grants[0]);self.assertNotIn('another-account-READER_LIBRARY',grants[0])
                    d['sessionExpiresAt']=0;(state/'reader-asset-allowlist.json').write_text(json.dumps({'bookIds':['*']}))
                    previous=len(calls)
                    with self.assertRaises(ValueError):server.session(d)
                    self.assertEqual(len(calls),previous)
                finally:server.server_close()

    def test_account_fault_is_bounded_and_device_scoped(self):
        with tempfile.TemporaryDirectory() as temp:
            state=Path(temp);device={'deviceId':'device-a'}
            rule=dict(deviceId='device-a',mode='refresh_503',armedAt=100,expiresAt=160)
            path=state/'account-fault.json';path.write_text(json.dumps(rule))
            self.assertEqual(shim.account_fault(state,device,120),rule)
            self.assertIsNone(shim.account_fault(state,{'deviceId':'device-b'},120))
            self.assertIsNone(shim.account_fault(state,device,160))
            for changes in [dict(expiresAt=281),dict(armedAt=True),dict(mode='grant_all')]:
                path.write_text(json.dumps({**rule,**changes}))
                self.assertIsNone(shim.account_fault(state,device,120))

    def test_retirement_survives_control_removal_and_reenrollment(self):
        with tempfile.TemporaryDirectory() as temp:
            state=Path(temp);(state/'credentials.json').write_text(json.dumps({'username':'admin','password':'lab'}))
            calls=[]
            def request(path,method='GET',body=None,auth=None):
                calls.append((path,method));return {}
            with patch.object(shim.gateway,'STATE',state),patch.object(shim.gateway,'request',request):
                server=shim.Shim(0,state/'shim',18766)
                key=shim.identity({'deviceUniqueId':'hardware-a'});d=server.enroll(key);d['model']='Palma2_Pro_C';server.save();peer=server.enroll('peer');old=d['bearer'];peerBearer=peer['bearer'];principal=d['deviceId']
                d.update(oss={'AccessKeyId':'old-key'},sessionId='old-session',cookieName='Cookie',sessionExpiresAt=time.time()+3600)
                spec=importlib.util.spec_from_file_location('lifecycle',ROOT/'scripts/device-credential-lifecycle.py');lifecycle=importlib.util.module_from_spec(spec);spec.loader.exec_module(lifecycle)
                with self.assertRaises(ValueError):lifecycle.transition(server.state,'unknown','retire')
                lifecycle.transition(server.state,principal,'retire')
                control=server.state/'device-lifecycle.json'
                self.assertTrue(server.is_revoked(d));self.assertNotEqual(old,d['bearer']);self.assertNotIn('oss',d);self.assertNotIn('sessionId',d)
                self.assertIn(('/neocloud/_user/'+principal+'/_session','DELETE'),calls)
                control.unlink();server.server_close();server=shim.Shim(0,state/'shim',18766);d=server.registry[key]
                self.assertTrue(server.is_revoked(d))
                with self.assertRaises(shim.AccountRejected):server.signin(key)
                control.write_text(json.dumps({principal:{'generation':1,'state':'retired'}}))
                self.assertEqual(calls.count(('/neocloud/_user/'+principal+'/_session','DELETE')),1)
                lifecycle.transition(server.state,principal,'allow-reenroll')
                self.assertTrue(server.is_revoked(d))
                with self.assertRaises(shim.AccountRejected):server.authenticate({'deviceUniqueId':'hardware-a','Authorization':'Bearer '+old})
                fresh=server.signin(key);self.assertEqual(fresh['deviceId'],principal);self.assertEqual(fresh['credentialGeneration'],1)
                with self.assertRaises(ValueError):server.authenticate({'deviceUniqueId':'hardware-a','Authorization':'Bearer '+old})
                self.assertEqual(server.authenticate({'deviceUniqueId':'hardware-a','Authorization':'Bearer '+fresh['bearer']})['deviceId'],principal)
                self.assertEqual(server.registry['peer']['bearer'],peerBearer)
                fresh['credentialState']='invalid'
                with self.assertRaises(shim.AccountRejected):server.signin(key)
                fresh['credentialState']='active';server.save()
                server.server_close();server=shim.Shim(0,state/'shim',18766);self.assertFalse(server.is_revoked(server.registry[key]));server.server_close()

    def test_device_binding_sessions_and_logs(self):
        with tempfile.TemporaryDirectory() as temp:
            state = Path(temp)
            (state / 'credentials.json').write_text(json.dumps({'username': 'admin', 'password': 'secret-admin'}))
            calls = []
            revoked=set();session_number=0
            def gateway_request(path, method='GET', body=None, auth=None, **kwargs):
                nonlocal session_number
                calls.append((path, body))
                if '/_session/' in path and path.rsplit('/',1)[1] in revoked:
                    raise HTTPError('http://localhost/session',404,'revoked',{},None)
                if path.endswith('/_session'):
                    session_number+=1
                    return {'cookie_name': 'SyncGatewaySession', 'session_id': 'secret-session-' + body['name']+'-'+str(session_number)}
                return {}
            with patch.object(shim.gateway, 'STATE', state), patch.object(shim.gateway, 'request', gateway_request):
                server = shim.Shim(0, state / 'shim', 18766)
                thread = threading.Thread(target=server.serve_forever, daemon=True)
                thread.start()
                def request(path, identity='hardware-a', bearer=None, body=None, form=False):
                    headers = {'deviceUniqueId': identity}
                    if bearer:
                        headers['Authorization'] = 'Bearer ' + bearer
                    if form:
                        headers['Content-Type'] = 'application/x-www-form-urlencoded'
                    elif body is not None:
                        headers['Content-Type'] = 'application/json'
                        body = json.dumps(body).encode()
                    req = Request(f'http://127.0.0.1:{server.server_port}{path}', data=body, headers=headers)
                    try:
                        with urlopen(req, timeout=5) as response:
                            return response.status, json.load(response)
                    except HTTPError as error:
                        with error:
                            return error.code, json.load(error)
                try:
                    body = b'account=powersync-lab%40example.invalid&code=000000'
                    _, first = request('/api/v2/users/signin', body=body, form=True)
                    _, second = request('/api/v2/users/signin', identity='hardware-b', body=body, form=True)
                    token_a, token_b = first['data']['token'], second['data']['token']
                    self.assertNotEqual(token_a, token_b)
                    status, account = request('/api/1/users/me', bearer=token_a)
                    self.assertEqual(status, 200)
                    self.assertEqual(account['data']['parent'], '')
                    self.assertEqual(request('/api/1/users/me', identity='hardware-b', bearer=token_a)[0], 403)
                    self.assertEqual(request('/api/v2/users/signin', body=b'account=personal&code=000000', form=True)[0], 403)
                    self.assertEqual(request('/api/1/unimplemented-service', bearer=token_a)[0], 501)
                    status, sts = request('/api/1/config/stss', bearer=token_a)
                    self.assertEqual(status, 200)
                    self.assertEqual(set(sts['data']), {'AccessKeyId', 'AccessKeySecret', 'SecurityToken', 'Expiration'})
                    _, a = request('/api/1/users/syncToken', bearer=token_a)
                    _, b = request('/api/1/users/syncToken', identity='hardware-b', bearer=token_b)
                    self.assertNotEqual(a['data']['session_id'], b['data']['session_id'])
                    _, again = request('/api/1/users/syncToken', bearer=token_a)
                    self.assertEqual(a["data"], again["data"])
                    self.assertGreaterEqual(again["tokenExpiredAt"], a["tokenExpiredAt"])
                    self.assertEqual(len([p for p, _ in calls if p.endswith('/_session')]), 2)
                    control=server.state/'revoked-devices.json'
                    control.write_text(json.dumps([server.registry[shim.identity({'deviceUniqueId':'hardware-a'})]['deviceId']]))
                    self.assertEqual(request('/api/1/users/me',bearer=token_a)[0],401)
                    self.assertEqual(request('/api/1/users/syncToken',bearer=token_a)[0],401)
                    self.assertEqual(request('/api/1/config/stss',bearer=token_a)[0],401)
                    self.assertEqual(request('/api/v2/users/signin',body=body,form=True)[0],401)
                    self.assertEqual(request('/api/1/users/me',identity='hardware-b',bearer=token_b)[0],200)
                    control.unlink()
                    revoked.add(a['data']['session_id'])
                    _,renewed=request('/api/1/users/syncToken',bearer=token_a)
                    self.assertNotEqual(renewed['data']['session_id'],a['data']['session_id'])
                    _,unchanged=request('/api/1/users/syncToken',identity='hardware-b',bearer=token_b)
                    self.assertEqual(unchanged["data"],b["data"])
                    self.assertEqual(len([p for p,_ in calls if p.endswith('/_session')]),3)
                    status, _ = request('/api/1/users/device', bearer=token_a,
                                        body={'deviceUniqueId': 'hardware-b', 'model': 'wrong-device'})
                    self.assertEqual(status, 403)
                    request('/api/1/users/updateSyncChannels', bearer=token_a,
                            body={'channels': ['another-account-NOTE_TREE', '*']})
                    with ThreadPoolExecutor(max_workers=4) as pool:
                        results=list(pool.map(lambda i:request('/api/1/users/updateSyncChannels',
                                         identity='hardware-a' if i%2==0 else 'hardware-b',
                                         bearer=token_a if i%2==0 else token_b,
                                         body={'channels':['powersync-lab-test']}),range(20)))
                    self.assertTrue(all(status==200 for status,_ in results))
                    self.assertEqual(len(json.loads(server.registry_path.read_text())),2)
                    device=server.registry[shim.identity({'deviceUniqueId':'hardware-a'})]
                    fault=state/'shim/account-fault.json'
                    now=time.time()
                    rule=dict(deviceId=device['deviceId'],mode='expire_rotate',armedAt=now,expiresAt=now+60)
                    fault.write_text(json.dumps(rule))
                    self.assertEqual(request('/api/1/users/me',bearer=token_a)[0],401)
                    self.assertEqual(request('/api/1/users/me',identity='hardware-b',bearer=token_b)[0],200)
                    status,rotated=request('/api/1/token/refresh',bearer=token_a)
                    self.assertEqual(status,200)
                    new_token=rotated['data']['token'];self.assertNotEqual(new_token,token_a)
                    self.assertEqual(request('/api/1/users/me',bearer=token_a)[0],403)
                    self.assertEqual(request('/api/1/users/me',bearer=new_token)[0],200)
                    rule['mode']='refresh_503';fault.write_text(json.dumps(rule))
                    self.assertEqual(request('/api/1/token/refresh',bearer=new_token)[0],503)
                    self.assertEqual(request('/api/1/users/me',bearer=new_token)[0],200)
                    rule['expireBeforeRefresh']=True;rule['armedAt']=time.time();rule['expiresAt']=rule['armedAt']+60
                    fault.write_text(json.dumps(rule))
                    self.assertEqual(request('/api/1/users/me',bearer=new_token)[0],401)
                    self.assertEqual(request('/api/1/token/refresh',bearer=new_token)[0],503)
                    fault.unlink()
                    self.assertEqual(request('/api/1/token/refresh',bearer=new_token)[0],200)
                    self.assertEqual(request('/api/1/users/me',bearer=new_token)[0],200)
                    rule['mode']='revoke_401';fault.write_text(json.dumps(rule))
                    self.assertEqual(request('/api/1/token/refresh',bearer=new_token)[0],401)
                    self.assertEqual(request('/api/1/users/me',identity='hardware-b',bearer=token_b)[0],200)
                    rule['expiresAt']=now-1;fault.write_text(json.dumps(rule))
                    self.assertEqual(request('/api/1/users/me',bearer=new_token)[0],200)
                    fault.unlink()
                    self.assertTrue(all('*' not in (body or {}).get('admin_channels', []) for _, body in calls))
                    log = (state / 'shim/requests.jsonl').read_text()
                    for value in [token_a, token_b, new_token, a['data']['session_id'], *sts['data'].values(), 'secret-admin', 'hardware-a', 'hardware-b']:
                        self.assertNotIn(value, log)
                    self.assertEqual((state / 'shim/devices.json').stat().st_mode & 0o777, 0o600)
                finally:
                    server.shutdown(); server.server_close(); thread.join()

if __name__ == '__main__':
    unittest.main()
