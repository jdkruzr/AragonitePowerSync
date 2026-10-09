import base64
from email.message import Message
from email.utils import formatdate
import hashlib
import hmac
import importlib.util
from pathlib import Path
import time
import json
import tempfile
import threading
from types import SimpleNamespace
import unittest

spec = importlib.util.spec_from_file_location('oss', Path(__file__).resolve().parents[1] / 'scripts/oss-couchbase-lab.py')
oss = importlib.util.module_from_spec(spec)
spec.loader.exec_module(oss)

class OssLabTests(unittest.TestCase):
    def test_reader_upload_fault_is_owned_expiring_put_only(self):
        with tempfile.TemporaryDirectory() as folder:
            state = Path(folder); device = {'deviceId': 'device-test'}
            book = 'owned-book'; key = 'powersync-lab/reader/owned-book/shape/revision.zip'
            rule = {'mode': 'put_503', 'bookId': book, 'deviceId': 'device-test', 'armedAt': 100, 'expiresAt': 160}
            file = state / 'reader-upload-fault.json'; file.write_text(json.dumps(rule))
            self.assertTrue(oss.reader_upload_fault(state, 'PUT', key, book, device, 120))
            for method, candidate, fixture, dev, now in [
                    ('GET', key, book, device, 120), ('PUT', key, 'other', device, 120),
                    ('PUT', key, book, {'deviceId': 'other'}, 120), ('PUT', key, book, device, 160),
                    ('PUT', 'powersync-lab/reader/owned-book/book/owned-book.pdf', book, device, 120)]:
                self.assertFalse(oss.reader_upload_fault(state, method, candidate, fixture, dev, now))
            rule['expiresAt'] = 281; file.write_text(json.dumps(rule))
            self.assertFalse(oss.reader_upload_fault(state, 'PUT', key, book, device, 120))

    def test_publication_hold_is_exact_expiring_put_only(self):
        with tempfile.TemporaryDirectory() as folder:
            state=Path(folder);device={'deviceId':'device-test'}
            rule={'mode':'hold_put_ack','key':'powersync-lab/resource/test.html',
                  'deviceId':'device-test','armedAt':100,'expiresAt':150}
            file=state/'asset-publication-hold.json';file.write_text(json.dumps(rule))
            self.assertEqual(oss.publication_hold(state,'PUT',rule['key'],device,120),150)
            for method,key,dev,now in [('GET',rule['key'],device,120),('PUT','other',device,120),
                    ('PUT',rule['key'],{'deviceId':'other'},120),('PUT',rule['key'],device,150)]:
                self.assertIsNone(oss.publication_hold(state,method,key,dev,now))
            rule['expiresAt']=161;file.write_text(json.dumps(rule))
            self.assertIsNone(oss.publication_hold(state,'PUT',rule['key'],device,120))

    def test_next_html_hold_stays_in_one_notebook(self):
        with tempfile.TemporaryDirectory() as folder:
            state=Path(folder);device={'deviceId':'device-test'}
            fixture='f7f635344f624650aa300f84d114a1d3'
            rule=dict(mode='hold_put_ack',nextNotebookHtml=fixture,deviceId='device-test',armedAt=100,expiresAt=150)
            (state/'asset-publication-hold.json').write_text(json.dumps(rule))
            key=f'powersync-lab/note/{fixture}/resource/data/12345678-1234-1234-1234-123456789abc.html'
            self.assertEqual(oss.publication_hold(state,'PUT',key,device,120),150)
            for other in [key.replace(fixture,'0'*32),key.replace('.html','.png'),key.replace('resource/data','resource/../data')]:
                self.assertIsNone(oss.publication_hold(state,'PUT',other,device,120))

    def test_reader_assets_require_exact_enrolled_keys(self):
        book='12345678-1234-1234-1234-123456789012'
        for key in oss.reader_keys(book):
            path=f'/powersync-lab/{key}'
            with self.assertRaises(ValueError):oss.scope(path,[])
            self.assertEqual(oss.scope(path,[],reader_ids=[book])[2],book)
        for suffix in ['book/other.epub','point/points.bin','thumbnail/other.png','book/../secret.epub']:
            with self.assertRaises(ValueError):
                oss.scope(f'/powersync-lab/powersync-lab/reader/{book}/{suffix}',[],reader_ids=[book])

    def test_exact_reader_point_asset_does_not_admit_other_keys(self):
        book='12345678-1234-1234-1234-123456789012'
        key=f'powersync-lab/reader/{book}/point/page#revision#points'
        path='/powersync-lab/'+key.replace('#','%23')
        self.assertEqual(oss.scope(path,[],reader_ids=[book],native_assets={key:book})[2],book)
        for other in [path+'-other',path.replace(book,'f'*32),path.replace('/point/','/../')]:
            with self.assertRaises(ValueError):oss.scope(other,[],reader_ids=[book],native_assets={key:book})

    def test_dynamic_native_reader_paths_are_explicit_and_bounded(self):
        book='12345678-1234-1234-1234-123456789012'
        for suffix in ['point/-12#revision#points','shape/-12#revision#123.zip']:
            path=f'/powersync-lab/powersync-lab/reader/{book}/{suffix}'.replace('#','%23')
            with self.assertRaises(ValueError):oss.scope(path,[],reader_ids=[book])
            self.assertEqual(oss.scope(path,[],reader_ids=[book],dynamic_native_books=[book])[2],book)
        for suffix in ['point/../secret#points','point/file.bin','shape/fileXzip',
                       'shape/file.zip/extra','resource/data/file.html','shape/..zip']:
            with self.assertRaises(ValueError):
                oss.scope(f'/powersync-lab/powersync-lab/reader/{book}/{suffix}',[],
                          reader_ids=[book],dynamic_native_books=[book])
        with self.assertRaises(ValueError):
            oss.scope('/powersync-lab/anything',[],reader_ids=[book],dynamic_native_books=['f'*32])

    def test_signed_listing_includes_dynamic_ink_only_when_enrolled(self):
        from urllib.parse import urlencode
        book='12345678-1234-1234-1234-123456789012'
        prefix=f'powersync-lab/reader/{book}/'
        names=['book/'+book+'.pdf','point/-12#fresh#points','shape/-12#fresh#123.zip',
               'resource/data/arbitrary.html','shape/file.zip/extra']
        docs=[dict(type='labAsset',ossKey=prefix+n,md5='0'*32,bytes=12,updatedAt=1000) for n in names]
        device=dict(deviceId='device-test',ossExpiresAt=time.time()+3600,
                    oss=dict(AccessKeyId='key',AccessKeySecret='secret',SecurityToken='token'))
        server=SimpleNamespace(lock=threading.RLock(),registry={'test':device},auth=('admin','password'))
        with tempfile.TemporaryDirectory() as temp:
            state=Path(temp);(state/'fixture-allowlist.json').write_text('[]')
            (state/'reader-asset-allowlist.json').write_text(json.dumps({'bookIds':[book]}))
            gateway=SimpleNamespace(STATE=state,request=lambda *a,**kw:dict(rows=[dict(doc=d) for d in docs]))
            def listing():
                headers=Message();headers['Date']=formatdate(usegmt=True)
                headers['x-oss-security-token']='token'
                headers['Authorization']='OSS key:'+base64.b64encode(hmac.new(b'secret',
                    oss.canonical('GET','/powersync-lab/',headers).encode(),hashlib.sha1).digest()).decode()
                return oss.operate(server,'GET','/powersync-lab?'+urlencode({'prefix':prefix}),headers,b'',gateway)[3].decode()
            before=listing();self.assertIn(prefix+names[0],before);self.assertNotIn('fresh',before)
            (state/'reader-native-dynamic-book-allowlist.json').write_text(json.dumps([book]))
            after=listing()
            for n in names[:3]:self.assertIn(prefix+n,after)
            for n in names[3:]:self.assertNotIn(prefix+n,after)

    def test_bulk_delete_is_signed_scoped_and_idempotent(self):
        fixture='f7f635344f624650aa300f84d114a1d3';key=f'powersync-lab/note/{fixture}/resource/data/delete-test.html'
        docid='labasset-'+hashlib.sha256(key.encode()).hexdigest();stored={docid:dict(_rev='1-test',type='labAsset',user='powersync-lab',ossKey=key,documentUniqueId=fixture)};mutations=[]
        device=dict(deviceId='device-test',ossExpiresAt=time.time()+3600,oss=dict(AccessKeyId='key',AccessKeySecret='secret',SecurityToken='token'));server=SimpleNamespace(lock=threading.RLock(),registry={'test':device},auth=('admin','password'))
        def request(endpoint,method='GET',body=None,auth=None):
            ident=endpoint.rsplit('/',1)[1]
            if method=='PUT':mutations.append(ident);stored.pop(ident,None);return {'rev':'2-deleted'}
            return stored.get(ident)
        with tempfile.TemporaryDirectory() as temp:
            state=Path(temp);(state/'fixture-allowlist.json').write_text(json.dumps([fixture]));gateway=SimpleNamespace(STATE=state,request=request)
            def operate(keys,token='token'):
                body=('<Delete><Quiet>true</Quiet>'+''.join('<Object><Key>'+k+'</Key></Object>' for k in keys)+'</Delete>').encode();headers=Message();headers['Date']=formatdate(usegmt=True);headers['Content-Type']='application/xml';headers['x-oss-security-token']=token;headers['Content-MD5']=base64.b64encode(hashlib.md5(body).digest()).decode();headers['Authorization']='OSS key:'+base64.b64encode(hmac.new(b'secret',oss.canonical('POST','/powersync-lab/?delete',headers).encode(),hashlib.sha1).digest()).decode();return oss.operate(server,'POST','/powersync-lab/?delete',headers,body,gateway,'delete-test')
            with self.assertRaises(ValueError):operate([key,key.replace(fixture,'another-notebook')])
            self.assertEqual(mutations,[])
            with self.assertRaises(ValueError):operate([key],token='bad')
            self.assertEqual(mutations,[])
            rule=dict(deviceId='device-test',key=key,armedAt=time.time()-1,expiresAt=time.time()+60)
            (state/'bulk-delete-fault.json').write_text(json.dumps(rule))
            self.assertEqual(operate([key])[1],503);self.assertEqual(mutations,[])
            (state/'bulk-delete-fault.json').unlink()
            self.assertEqual(operate([key])[1],200);self.assertEqual(len(mutations),1)
            self.assertEqual(operate([key])[1],200);self.assertEqual(len(mutations),1)
            events=[json.loads(l) for l in (state/'bulk-delete.jsonl').read_text().splitlines()];self.assertTrue(events[0]['deleted']);self.assertEqual(events[0]['backendRev'],'2-deleted');self.assertTrue(events[1]['alreadyAbsent'])

    def test_revision_provenance_and_download_integrity(self):
        fixture = 'f7f635344f624650aa300f84d114a1d3'
        path = f'/powersync-lab/powersync-lab/note/{fixture}/resource/data/test.html'
        creds = {'AccessKeyId': 'key', 'AccessKeySecret': 'secret', 'SecurityToken': 'token'}
        device = {'deviceId': 'device-test', 'oss': creds, 'ossExpiresAt': time.time()+3600}
        server = SimpleNamespace(lock=threading.RLock(), registry={'test': device}, auth=('admin', 'password'))
        stored = {}
        def request(endpoint, method='GET', body=None, auth=None):
            if method == 'PUT':
                stored.update(body);stored['_rev'] = '1-test'
                return {'rev': '1-test'}
            return stored or None
        with tempfile.TemporaryDirectory() as temp:
            state = Path(temp);(state/'fixture-allowlist.json').write_text(json.dumps([fixture]))
            gateway = SimpleNamespace(STATE=state, request=request)
            def operate(method, body=b''):
                headers = Message();headers['Date']=formatdate(usegmt=True)
                headers['x-oss-security-token']='token';headers['Content-Type']='text/plain'
                signature=base64.b64encode(hmac.new(b'secret', oss.canonical(method,path,headers).encode(), hashlib.sha1).digest()).decode()
                headers['Authorization']='OSS key:'+signature
                return oss.operate(server,method,path,headers,body,gateway,'operation-test')
            _, status, headers, _, _ = operate('PUT',b'fixture bytes')
            self.assertEqual(status,200)
            self.assertEqual(stored['sourceDeviceId'],'device-test')
            self.assertEqual(stored['sourceOperationId'],'operation-test')
            self.assertEqual(headers['X-PowerSync-Backend-Rev'],'1-test')
            server.is_revoked=lambda d:True
            for method in ['GET','HEAD','PUT','DELETE']:
                with self.assertRaisesRegex(ValueError,'Revoked lab device'):operate(method,b'blocked')
            self.assertEqual(stored['_rev'],'1-test')
            server.is_revoked=lambda d:False

            _, status, headers, body, length = operate('GET')
            self.assertEqual((status,body,length),(200,b'fixture bytes',13))
            self.assertEqual(headers['X-PowerSync-Backend-Rev'],'1-test')
            self.assertEqual(headers['Content-MD5'],base64.b64encode(hashlib.md5(body).digest()).decode())
            server_state_before = json.loads(json.dumps(stored))
            for fault in (404,503):
                rule = dict(fixture=fixture, deviceId='device-test', status=fault,
                            armedAt=time.time()-1, expiresAt=time.time()+60, key=path.split('/',2)[2])
                (state/'asset-fault.json').write_text(json.dumps(rule))
                _, status, headers, body, _ = operate('GET')
                self.assertEqual(status,fault)
                self.assertEqual(headers['X-PowerSync-Injected-Fault'],str(fault))
                self.assertEqual(stored,server_state_before)
                self.assertEqual(operate('HEAD')[1],200)
                rule['deviceId']='other-device'
                (state/'asset-fault.json').write_text(json.dumps(rule))
                self.assertEqual(operate('GET')[1],200)
            (state/'asset-fault.json').unlink()
            for mode in ['corrupt','truncate','slow']:
                rule=dict(key=path.split('/',2)[2],fixture=fixture,deviceId='device-test',mode=mode,armedAt=time.time()-1,expiresAt=time.time()+60)
                (state/'asset-transfer-fault.json').write_text(json.dumps(rule))
                _,status,headers,response,length=operate('GET')
                self.assertEqual(status,200);self.assertEqual(length,13)
                self.assertEqual(headers['X-PowerSync-Transfer-Fault'],mode)
                self.assertEqual(headers['Content-MD5'],base64.b64encode(hashlib.md5(b'fixture bytes').digest()).decode())
                self.assertEqual(stored,server_state_before)
                if mode=='corrupt':self.assertNotEqual(response,b'fixture bytes');self.assertEqual(len(response),13)
                elif mode=='truncate':self.assertEqual(response,b'fixture bytes'[:6])
                else:self.assertEqual(response,b'fixture bytes')
                self.assertIsNone(oss.transfer_fault(state,'PUT',rule['key'],fixture,device))
                self.assertIsNone(oss.transfer_fault(state,'GET',rule['key'],fixture,dict(device,deviceId='another-device')))
                self.assertIsNone(oss.transfer_fault(state,'GET',rule['key'],fixture,device,rule['expiresAt']+1))
            (state/'asset-transfer-fault.json').unlink()
            stored['sha256']='wrong'
            with self.assertRaises(ValueError):operate('GET')

    def test_fault_bounds_and_scope(self):
        fixture='fixture';key='powersync-lab/note/fixture/resource/data/new.html'
        with tempfile.TemporaryDirectory() as temp:
            state=Path(temp)
            rule=dict(fixture=fixture,deviceId='receiver',status=404,armedAt=100,expiresAt=200)
            def probe(**changes):
                (state/'asset-fault.json').write_text(json.dumps(dict(rule,**changes)))
                return oss.injected_fault(state,'GET',key,fixture,{'deviceId':'receiver'},150)
            self.assertEqual(probe(),404)
            for change in [dict(expiresAt=140),dict(expiresAt=401),dict(armedAt=151),
                           dict(status=500),dict(fixture='other'),dict(key=key+'-other'),
                           dict(excludeKeys=[key])]:
                self.assertIsNone(probe(**change))
            probe()
            for method in ['PUT','HEAD','DELETE']:
                self.assertIsNone(oss.injected_fault(state,method,key,fixture,{'deviceId':'receiver'},150))
            for target in [key.replace('resource/data','point'),key.replace('new.html','new-history.html')]:
                self.assertIsNone(oss.injected_fault(state,'GET',target,fixture,{'deviceId':'receiver'},150))

    def test_fixture_scope_rejects_escape_and_other_accounts(self):
        fixture = '9e6004bb-7bc3-4a7e-87d9-79b9f9ba6206'
        path = f'/powersync-lab/powersync-lab/note/{fixture}/point/file'
        self.assertEqual(oss.scope(path, [fixture])[2], fixture)
        for target in [path.replace(fixture, 'another-document'), path.replace('/point/', '/%2e%2e/'),
                       path.replace('/note/', '/userData/'), path.replace('/point/', '/%5c/')]:
            with self.assertRaises(ValueError):
                oss.scope(target, [fixture])
        with self.assertRaises(NotImplementedError):
            oss.scope(path + '?uploads', [fixture])
        key='powersync-lab/resource/powersync-v2-test.html';target='/powersync-lab/'+key
        with self.assertRaises(ValueError):oss.scope(target,[fixture])
        self.assertEqual(oss.scope(target,[fixture],{key:fixture})[2],fixture)
        with self.assertRaises(ValueError):oss.scope(target,[fixture],{key:'other'})
        with self.assertRaises(ValueError):oss.scope(target+'-other',[fixture],{key:fixture})

    def test_signature_token_expiry_and_method_are_bound(self):
        creds = {'AccessKeyId': 'lab-key', 'AccessKeySecret': 'lab-secret', 'SecurityToken': 'lab-token'}
        device = {'oss': creds, 'ossExpiresAt': time.time() + 3600}
        headers = Message()
        headers['Date'] = formatdate(usegmt=True)
        headers['x-oss-security-token'] = creds['SecurityToken']
        path = '/powersync-lab/powersync-lab/note/fixture/file'
        signed = oss.canonical('PUT', path, headers)
        signature = base64.b64encode(hmac.new(creds['AccessKeySecret'].encode(), signed.encode(), hashlib.sha1).digest()).decode()
        headers['Authorization'] = 'OSS lab-key:' + signature
        self.assertIs(oss.authenticate('PUT', path, headers, {'device': device}), device)
        for method, target in [('GET', path), ('PUT', path + '-changed')]:
            with self.assertRaises(ValueError):
                oss.authenticate(method, target, headers, {'device': device})
        headers.replace_header('x-oss-security-token', 'wrong-token')
        with self.assertRaises(ValueError):
            oss.authenticate('PUT', path, headers, {'device': device})
        headers.replace_header('x-oss-security-token', creds['SecurityToken'])
        device['ossExpiresAt'] = 0
        with self.assertRaises(ValueError):
            oss.authenticate('PUT', path, headers, {'device': device})
