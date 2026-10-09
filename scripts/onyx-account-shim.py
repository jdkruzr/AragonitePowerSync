#!/usr/bin/env python3
"""Disposable Onyx-shaped account shim; private-network lab only, not production auth.

Only the fixed lab account/code can sign in. Hardware identity is hashed locally;
each enrolled device gets an independent bearer and Sync Gateway principal/session.
Unknown routes, uploads and anonymous replication remain refused.
"""
import argparse
import hashlib
import importlib.util
import json
import os
from pathlib import Path
import secrets
import threading
import time
from datetime import datetime, timezone
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import parse_qs, urlsplit
from urllib.error import HTTPError

ROOT = Path(__file__).resolve().parents[1]
spec = importlib.util.spec_from_file_location('gateway_lab', ROOT / 'scripts/gateway-lab.py')
gateway = importlib.util.module_from_spec(spec)
spec.loader.exec_module(gateway)
spec = importlib.util.spec_from_file_location('cluster_harness', ROOT / 'scripts/cluster-harness.py')
harness = importlib.util.module_from_spec(spec)
spec.loader.exec_module(harness)
spec = importlib.util.spec_from_file_location('oss_lab', ROOT / 'scripts/oss-couchbase-lab.py')
oss = importlib.util.module_from_spec(spec)
spec.loader.exec_module(oss)
ACCOUNT = 'powersync-lab@example.invalid'
CODE = '000000'
UID = 'powersync-lab'

class AccountRejected(Exception):
    def __init__(self, status):
        self.status = status

def account_fault(state, device, now=None):
    """Bounded, exact-principal account faults; never grants authentication."""
    now = time.time() if now is None else now
    try:
        rule = json.loads((state / 'account-fault.json').read_text())
    except (FileNotFoundError, json.JSONDecodeError):
        return None
    start, end = rule.get('armedAt'), rule.get('expiresAt')
    if (rule.get('deviceId') != device['deviceId']
            or rule.get('mode') not in ('expire_rotate', 'refresh_503', 'revoke_401')
            or type(start) not in (int, float) or type(end) not in (int, float)
            or not 0 < end - start <= 180 or not start <= now < end):
        return None
    return rule

def session_channels(state):
    """Server-owned disposable scopes, independent of credential lifetime."""
    allowlist = state / 'fixture-allowlist.json'
    ids = json.loads(allowlist.read_text()) if allowlist.exists() else []
    channels = [UID + '-NOTE_TREE'] + [UID + '-' + item for item in ids]
    if (state / 'note-doorbell-enabled').exists():
        channels.append(UID + '-MESSAGE')
    if (state / 'reader-session-grants-enabled').exists():
        path = state / 'reader-asset-allowlist.json'
        if not path.is_file() or path.stat().st_size > 65536:
            raise ValueError('Missing bounded reader session allowlist')
        books = json.loads(path.read_text()).get('bookIds')
        if (not isinstance(books, list) or not books or len(books) > 128
                or any(not isinstance(book, str) or len(book) != 32
                       or any(c not in '0123456789abcdef' for c in book) for book in books)):
            raise ValueError('Invalid reader session allowlist')
        channels.extend([UID + '-READER_LIBRARY', UID + '-MESSAGE'])
        for book in books:
            channels.extend([UID + '-' + book, UID + '-ASSET-' + book])
    return sorted(set(channels))

def identity(headers):
    value = headers.get('deviceUniqueId') or headers.get('mac')
    if not value or len(value) > 256 or value in ('02:00:00:00:00:00', 'unknown'):
        raise ValueError('Missing usable native device identity')
    return hashlib.sha256(value.strip().lower().encode()).hexdigest()

class Shim(ThreadingHTTPServer):
    def __init__(self, port, state, gateway_port, bind_host='127.0.0.1'):
        super().__init__((harness.lab_host(bind_host), port), Handler)
        self.state = state
        self.state.mkdir(parents=True, exist_ok=True)
        self.state.chmod(0o700)
        self.registry_path = state / 'devices.json'
        self.registry = json.loads(self.registry_path.read_text()) if self.registry_path.exists() else {}
        self.lock = threading.RLock()
        credentials = json.loads((gateway.STATE / 'credentials.json').read_text())
        self.auth = credentials['username'], credentials['password']
        self.gateway_port = gateway_port
        for device in list(self.registry.values()):
            self.reconcile_credentials(device)

    def is_revoked(self, device):
        self.reconcile_credentials(device)
        return device.get('credentialState', 'active') != 'active' or device_revoked(self.state, device)

    def reconcile_credentials(self, device):
        with self.lock:
            control = self.state / 'device-lifecycle.json'
            rules = json.loads(control.read_text()) if control.exists() else {}
            if not isinstance(rules, dict):
                raise ValueError('Invalid credential lifecycle control')
            rule = rules.get(device['deviceId'])
            if rule is not None:
                generation = rule.get('generation')
                if type(generation) is not int or generation < 1 or rule.get('state') not in ('retired', 'awaiting_enrollment'):
                    raise ValueError('Invalid credential lifecycle rule')
                current = device.get('credentialGeneration', 0)
                if generation < current:
                    raise ValueError('Stale credential lifecycle rule')
                if generation > current:
                    device.update(credentialGeneration=generation, credentialState='retired',
                                  bearer=secrets.token_urlsafe(32), bearerExpiresAt=0,
                                  sessionExpiresAt=0, ossExpiresAt=0)
                    for key in ('sessionId', 'cookieName', 'oss'):
                        device.pop(key, None)
                    self.save()
            generation = device.get('credentialGeneration', 0)
            if device.get('credentialState') == 'retired' and device.get('gatewayRetiredGeneration', 0) < generation:
                principal = device['deviceId']
                gateway.request('/neocloud/_user/' + principal, 'PUT',
                                {'name': principal, 'disabled': True, 'admin_channels': []}, self.auth)
                gateway.request('/neocloud/_user/' + principal + '/_session', 'DELETE', auth=self.auth)
                device['gatewayRetiredGeneration'] = generation
                self.save()
            if rule and rule['state'] == 'awaiting_enrollment' and device.get('credentialState') == 'retired':
                device['credentialState'] = 'awaiting_enrollment'
                self.save()

    def signin(self, fingerprint):
        with self.lock:
            device = self.enroll(fingerprint)
            self.reconcile_credentials(device)
            if device.get('credentialState', 'active') not in ('active', 'awaiting_enrollment') or device_revoked(self.state, device):
                raise AccountRejected(401)
            device.update(credentialState='active', bearerExpiresAt=time.time()+3600)
            self.save()
            return device

    def save(self):
        # Channel and registration requests run on separate HTTP threads.
        # Serialize the shared temporary-file write as well as its rename.
        with self.lock:
            temp = self.registry_path.with_suffix('.tmp')
            with open(temp, 'w') as stream:
                temp.chmod(0o600)
                json.dump(self.registry, stream, indent=2)
                stream.flush()
                os.fsync(stream.fileno())
            temp.replace(self.registry_path)
            fd = os.open(self.state, os.O_RDONLY)
            try: os.fsync(fd)
            finally: os.close(fd)

    def enroll(self, fingerprint):
        with self.lock:
            if fingerprint not in self.registry:
                self.registry[fingerprint] = {'deviceId': 'device-' + secrets.token_hex(6),
                                               'bearer': secrets.token_urlsafe(32)}
                self.save()
            return self.registry[fingerprint]

    def authenticate(self, headers):
        fingerprint = identity(headers)
        with self.lock:
            device = self.registry.get(fingerprint)
            if device:
                self.reconcile_credentials(device)
                if device.get('credentialState', 'active') != 'active':
                    raise AccountRejected(401)
            token = headers.get('Authorization', '').removeprefix('Bearer ')
            if not device or not secrets.compare_digest(token, device['bearer']):
                raise ValueError('Invalid device-bound lab bearer')
            if device_revoked(self.state, device):
                raise AccountRejected(401)
            return device

    def session(self, device):
        with self.lock:
            if self.is_revoked(device):
                raise AccountRejected(401)
            if device.get('sessionExpiresAt', 0) > time.time() + 60:
                try:
                    gateway.request('/neocloud/_session/'+device['sessionId'],auth=self.auth)
                    return {'cookie_name': device['cookieName'], 'session_id': device['sessionId']}
                except HTTPError as error:
                    with error:
                        if error.code!=404:raise
                    # Server-side expiry/revocation overrides our cached TTL.
            principal = device['deviceId']
            # Shared notebook account, independent device principals. Requested
            # vendor channels are never treated as authorization grants.
            channels = session_channels(gateway.STATE)
            gateway.request('/neocloud/_user/' + principal, 'PUT',
                            {'name': principal, 'disabled': False, 'password': secrets.token_urlsafe(32),
                             'admin_channels': channels}, self.auth)
            session = gateway.request('/neocloud/_session', 'POST',
                                      {'name': principal, 'ttl': 3600}, self.auth)
            device.update(sessionId=session['session_id'], cookieName=session['cookie_name'],
                          sessionExpiresAt=time.time() + 3600)
            self.save()
            return {'cookie_name': device['cookieName'], 'session_id': device['sessionId']}

def device_revoked(state, device):
    path = state / 'revoked-devices.json'
    if not path.exists():
        return False
    if path.stat().st_size > 65536:
        raise ValueError('Invalid revocation registry')
    ids = json.loads(path.read_text())
    if not isinstance(ids, list) or any(not isinstance(x, str) or not x or len(x)>64 for x in ids):
        raise ValueError('Invalid revocation registry')
    return device['deviceId'] in ids

class Handler(BaseHTTPRequestHandler):
    protocol_version = 'HTTP/1.1'
    def log_message(self, *args):
        pass
    def respond(self, status, value):
        raw = json.dumps(value).encode()
        self.send_response(status)
        self.send_header('Content-Type', 'application/json')
        self.send_header('Content-Length', str(len(raw)))
        self.send_header('Connection', 'close')
        self.end_headers()
        self.wfile.write(raw)
        self.close_connection = True

    def handle_request(self):
        body = b''
        device = None
        path = urlsplit(self.path).path
        try:
            size = int(self.headers.get('Content-Length', '0'))
            if 'Transfer-Encoding' in self.headers or not 0 <= size <= 1048576:
                self.respond(413, {'error': 'Unsupported body framing'}); return
            self.connection.settimeout(10)
            body = self.rfile.read(size)
            if len(body) != size:
                raise ValueError('Incomplete request')
            if path == '/powersync-lab' or path.startswith('/powersync-lab/'):
                self.handle_object(body)
                return
            form = parse_qs(body.decode()) if self.headers.get('Content-Type', '').startswith('application/x-www-form-urlencoded') else {}
            status, response = 401, {'code': -1, 'error': 'Unsupported lab route'}
            data = None
            if self.command == 'GET' and path == '/health':
                status, response = 200, {'ok': True, 'purpose': 'disposable native login lab'}
            elif self.command == 'POST' and path.endswith('/devices/logout'):
                status, response = 200, {'code': 0}
            elif self.command == 'POST' and path.endswith('/users/checkRegistrationStatus'):
                if form.get('account') != [ACCOUNT]:
                    raise ValueError('Only disposable account allowed')
                identity(self.headers)
                data = {'isRegistered': True}
            elif self.command == 'POST' and path.endswith('/users/signin'):
                if form.get('account') != [ACCOUNT] or form.get('code') != [CODE]:
                    raise ValueError('Only disposable credentials allowed')
                device = self.server.signin(identity(self.headers))
                data = {'token': device['bearer'], 'tokenExpiredAt': int(time.time()) + 3600}
            else:
                device = self.server.authenticate(self.headers)
                fault = account_fault(self.server.state, device)
                refresh = path.endswith('/token/refresh') and self.command == 'GET'
                if fault:
                    if fault['mode'] == 'revoke_401':
                        raise AccountRejected(401)
                    if ((fault['mode'] == 'expire_rotate' or
                         (fault['mode'] == 'refresh_503' and fault.get('expireBeforeRefresh') is True))
                            and device.get('expirationInjectedAt') != fault['armedAt']):
                        device.update(bearerExpiresAt=time.time()-1,
                                      expirationInjectedAt=fault['armedAt'])
                        self.server.save()
                    if fault['mode'] == 'refresh_503' and refresh:
                        raise AccountRejected(503)
                if device.get('bearerExpiresAt', float('inf')) <= time.time() and not refresh:
                    raise AccountRejected(401)
                # A valid session requesting an unimplemented service is not an
                # authentication failure. Native ksync logs out on HTTP 401.
                status, response = 501, {'code': -1, 'error': 'Lab service not implemented'}
                if self.command == 'GET' and path.endswith('/users/me'):
                    data = {'id': 2000000001, 'uid': UID, 'name': 'PowerSync lab',
                            'nickname': 'PowerSync lab', 'email': ACCOUNT,
                            'parent': '',
                            'storage_limit': 1073741824, 'storage_used': 0}
                elif self.command == 'POST' and path.endswith('/users/device'):
                    registration = json.loads(body)
                    body_identity = registration.get('deviceUniqueId') or registration.get('macAddress')
                    if not body_identity or hashlib.sha256(body_identity.strip().lower().encode()).hexdigest() != identity(self.headers):
                        raise ValueError('Device registration identity mismatch')
                    device['model'] = str(registration.get('model', ''))[:100]
                    self.server.save()
                    data = {}
                elif self.command == 'POST' and path.endswith('/users/updateSyncChannels'):
                    channels = json.loads(body).get('channels', [])
                    device['requestedChannels'] = [c for c in channels if isinstance(c, str) and c.startswith(UID + '-')]
                    self.server.save()
                    data = {}
                elif self.command == 'GET' and path.endswith('/users/syncToken'):
                    data = self.server.session(device)
                elif self.command == 'GET' and path.endswith('/statistics/user/left'):
                    data = {'storage_left': 1073741824, 'storage_limit': 1073741824,
                            'storage_used': 0, 'storage_red': 1048576}
                elif self.command == 'GET' and path.endswith('/token/refresh'):
                    if fault and fault['mode'] == 'expire_rotate':
                        device['bearer'] = secrets.token_urlsafe(32)
                    device['bearerExpiresAt'] = time.time()+3600
                    self.server.save()
                    data = {'token': device['bearer'], 'tokenExpiredAt': int(time.time()) + 3600}
                elif self.command == 'GET' and path.endswith('/config/stss'):
                    with self.server.lock:
                        if device.get('ossExpiresAt', 0) < time.time() + 60:
                            device['ossExpiresAt'] = time.time() + 3600
                            device['oss'] = {'AccessKeyId': 'LAB' + secrets.token_hex(12),
                                             'AccessKeySecret': secrets.token_urlsafe(32),
                                             'SecurityToken': secrets.token_urlsafe(32),
                                             'Expiration': datetime.fromtimestamp(device['ossExpiresAt'], timezone.utc).strftime('%Y-%m-%dT%H:%M:%SZ')}
                            self.server.save()
                        data = device['oss']
            if data is not None:
                status, response = 200, {'code': 0, 'result_code': 0, 'data': data,
                                         'tokenExpiredAt': int(time.time()) + 3600}
        except AccountRejected as error:
            status, response = error.status, {'code': -1, 'result_code': -1,
                                               'error': 'Scoped lab account fault'}
        except (ValueError, UnicodeError, json.JSONDecodeError):
            status, response = 403, {'code': -1, 'error': 'Lab credentials or device identity rejected'}
        except Exception:
            # Never expose server errors containing session/credential values.
            status, response = 502, {'code': -1, 'error': 'Lab upstream failed'}
        event = harness.safe_event(self.command, self.path, self.headers, body)
        event.update(responseStatus=status, deviceId=device['deviceId'] if device else None,
                     at=time.time(),
                     deviceIdentityHeaderPresent=bool(self.headers.get('deviceUniqueId')),
                     macHeaderPresent=bool(self.headers.get('mac')))
        with self.server.lock:
            with (self.server.state / 'requests.jsonl').open('a') as stream:
                stream.write(json.dumps(event) + '\n')
        self.respond(status, response)
    def handle_object(self, body):
        device = None
        failure = None
        operation_id = secrets.token_hex(16)
        started = time.time()
        try:
            device, status, headers, response, size = oss.operate(self.server, self.command, self.path, self.headers, body, gateway, operation_id)
        except NotImplementedError:
            status, headers, response = 501, {'Content-Type': 'application/xml'}, b'<Error><Code>NotImplemented</Code></Error>'
            size = len(response)
        except ValueError as error:
            failure = str(error) if str(error) in ('Invalid OSS signature', 'Unknown OSS access key', 'Invalid lab security token', 'Expired lab OSS credentials', 'Object outside disposable notebook allowlist', 'Listing outside disposable notebook', 'Invalid object path', 'Stale signed date') else 'validation_failed'
            status, headers, response = 403, {'Content-Type': 'application/xml'}, b'<Error><Code>AccessDenied</Code></Error>'
            size = len(response)
        except Exception:
            status, headers, response = 502, {'Content-Type': 'application/xml'}, b'<Error><Code>LabUpstreamFailed</Code></Error>'
            size = len(response)
        held_until = None
        if device and status == 200 and 'X-PowerSync-Backend-Rev' in headers:
            # operate() already checked the signature and explicit synthetic scope.
            key = urlsplit(self.path).path.split('/', 2)[-1]
            held_until = oss.publication_hold(gateway.STATE, self.command, key, device)
        if held_until:
            held = dict(event='asset_stored_response_held', operationId=operation_id,
                        deviceId=device['deviceId'], at=time.time(), expiresAt=held_until,
                        backendDocId=headers['X-PowerSync-Backend-Id'],
                        backendRev=headers['X-PowerSync-Backend-Rev'],
                        objectSha256=headers['X-PowerSync-Object-SHA256'])
            with self.server.lock:
                with (self.server.state / 'publication-holds.jsonl').open('a') as stream:
                    stream.write(json.dumps(held) + '\n'); stream.flush(); os.fsync(stream.fileno())
            while (time.time() < held_until
                   and (gateway.STATE / 'asset-publication-hold.json').exists()):
                time.sleep(.1)
        event = harness.safe_event(self.command, self.path, self.headers, body)
        event.update(responseStatus=status, deviceId=device['deviceId'] if device else None, service='fixture-asset')
        if held_until:
            event['publicationResponseHeld'] = True

        event.update(operationId=operation_id, startedAt=started, completedAt=time.time(),
                     responseBodyBytes=0 if self.command == 'HEAD' else len(response),
                     responseContentLength=size, publicationAcknowledged=(
                         self.command in ('PUT', 'DELETE') and status in (200, 204)
                         and 'X-PowerSync-Backend-Rev' in headers))
        for header, key in [('X-PowerSync-Backend-Id', 'backendDocId'),
                            ('X-PowerSync-Backend-Rev', 'backendRev'),
                            ('X-PowerSync-Object-SHA256', 'objectSha256'),
                            ('X-PowerSync-Injected-Fault', 'injectedFault')]:
            if header in headers:
                event[key] = headers.pop(header)
        if 'X-PowerSync-Deleted-Count' in headers:
            event['deletedObjectCount']=int(headers.pop('X-PowerSync-Deleted-Count'))
        transfer_mode=headers.pop('X-PowerSync-Transfer-Fault',None)
        if transfer_mode:event['transferFault']=transfer_mode
        if failure:
            event['failure'] = failure
        with self.server.lock:
            with (self.server.state / 'requests.jsonl').open('a') as stream:
                stream.write(json.dumps(event) + '\n')
                stream.flush()
                os.fsync(stream.fileno())
        self.send_response(status)
        for name, value in headers.items():
            self.send_header(name, value)
        self.send_header('Content-Length', str(size))
        self.send_header('Connection', 'close')
        self.end_headers()
        if self.command != 'HEAD':
            if transfer_mode=='slow':
                for offset in range(0,len(response),2048):
                    self.wfile.write(response[offset:offset+2048]);self.wfile.flush();time.sleep(.1)
            else:self.wfile.write(response)
        self.close_connection = True
    do_GET = do_POST = do_PUT = do_DELETE = do_PATCH = do_HEAD = handle_request

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--port', type=int, default=18765)
    parser.add_argument('--bind-host', default='127.0.0.1', help='Concrete private IPv4 listener; no wildcard')
    parser.add_argument('--gateway-port', type=int, default=18766)
    parser.add_argument('--state', type=Path, default=gateway.STATE / 'shim')
    args = parser.parse_args()
    for port in (args.port, args.gateway_port):
        if not 1024 <= port <= 65535:
            parser.error('Ports must be 1024..65535')
    server = Shim(args.port, args.state, args.gateway_port, args.bind_host)
    (args.state / 'onyx_test_server.json').write_text(json.dumps([harness.cluster_definition(args.port, args.gateway_port, args.bind_host)], indent=2))
    print('Disposable account shim listening on selected private interface; unknown routes refused.', flush=True)
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        pass
    finally:
        server.server_close()

if __name__ == '__main__':
    main()
