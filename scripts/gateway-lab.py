#!/usr/bin/env python3
"""Start/stop an isolated loopback-only Couchbase reference stack with Podman.

Generated credentials and configuration live in ignored artifacts, never stdout.
Database writes initially fail closed; explicitly configure fixture IDs later.
"""
import argparse
import base64
import json
import os
from pathlib import Path
import secrets
import subprocess
import time
from urllib.request import Request, urlopen
from urllib.error import HTTPError, URLError

ROOT = Path(__file__).resolve().parents[1]
STATE = ROOT / 'artifacts/gateway-lab'
SERVER = 'docker.io/couchbase/server:community-7.6.2'
GATEWAY = 'docker.io/couchbase/sync-gateway:3.2.2-community'
NETWORK = 'powersync-couchbase-lab'

def run(*args):
    return subprocess.run(['podman', *args], check=True, stdout=subprocess.PIPE,
                          stderr=subprocess.PIPE, text=True).stdout.strip()

def request(path, method='GET', body=None, auth=None, port=18768):
    headers = {'Content-Type': 'application/json', 'Accept': 'application/json'}
    if auth:
        headers['Authorization'] = 'Basic ' + base64.b64encode(':'.join(auth).encode()).decode()
    data = None if body is None else json.dumps(body).encode()
    with urlopen(Request(f'http://127.0.0.1:{port}{path}', data=data,
                         headers=headers, method=method), timeout=15) as response:
        raw = response.read()
        return json.loads(raw) if raw else {}

def wait(port):
    deadline = time.monotonic() + 90
    while time.monotonic() < deadline:
        try:
            with urlopen(f'http://127.0.0.1:{port}/', timeout=5) as response:
                response.read(1024)
            return
        except HTTPError as error:
            if error.code == 401:
                return
        except (URLError, TimeoutError, ConnectionError):
            pass
        time.sleep(1)
    raise TimeoutError(f'Lab service on port {port} did not start')

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('action', choices=['start', 'stop', 'status'])
    args = parser.parse_args()
    STATE.mkdir(parents=True, exist_ok=True)
    STATE.chmod(0o700)
    if args.action == 'stop':
        for name in ('powersync-sg', 'powersync-cb'):
            subprocess.run(['podman', 'stop', '--time', '10', name],
                           stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
        print('Lab containers stopped; disposable state retained.')
        return
    if args.action == 'status':
        print(json.dumps(request('/'), sort_keys=True))
        return
    credentials_file = STATE / 'credentials.json'
    if not credentials_file.exists():
        credentials_file.write_text(json.dumps({'username': 'powersync_admin',
                                                'password': secrets.token_urlsafe(32)}))
        credentials_file.chmod(0o600)
    credentials = json.loads(credentials_file.read_text())
    auth = (credentials['username'], credentials['password'])
    if subprocess.run(['podman', 'network', 'exists', NETWORK]).returncode:
        run('network', 'create', '--internal', NETWORK)
    for image in (SERVER, GATEWAY):
        if subprocess.run(['podman', 'image', 'exists', image]).returncode:
            run('pull', image)
    if subprocess.run(['podman', 'container', 'exists', 'powersync-cb']).returncode:
        run('run', '-d', '--name', 'powersync-cb', '--network', NETWORK,
            '--memory', '3g', '--cpus', '2', '-p', '127.0.0.1:18769:8091',
            '-v', 'powersync-cb-data:/opt/couchbase/var', SERVER)
    else:
        run('start', 'powersync-cb')
    wait(18769)
    initialized = STATE / 'initialized'
    if not initialized.exists():
        # Secrets are passed to container CLI without printing command/output.
        run('exec', 'powersync-cb', '/opt/couchbase/bin/couchbase-cli', 'cluster-init',
            '-c', '127.0.0.1', '--cluster-username', auth[0], '--cluster-password', auth[1],
            '--services', 'data', '--cluster-ramsize', '512')
        run('exec', 'powersync-cb', '/opt/couchbase/bin/couchbase-cli', 'bucket-create',
            '-c', '127.0.0.1', '-u', auth[0], '-p', auth[1], '--bucket', 'neocloud',
            '--bucket-type', 'couchbase', '--bucket-ramsize', '256', '--bucket-replica', '0', '--wait')
        initialized.touch()
    config = {'bootstrap': {'server': 'couchbase://powersync-cb',
                            'username': auth[0], 'password': auth[1], 'use_tls_server': False},
              'api': {'public_interface': '0.0.0.0:4984', 'admin_interface': '0.0.0.0:4985',
                      'admin_interface_authentication': True},
              'logging': {'console': {'enabled': True, 'log_level': 'info',
                                      'log_keys': ['HTTP', 'Sync'], 'color_enabled': False},
                          'redaction_level': 'full'}}
    config_file = STATE / 'startup.json'
    config_file.write_text(json.dumps(config))
    # Container's sync_gateway UID needs read access; parent directory stays 0700.
    config_file.chmod(0o644)
    if subprocess.run(['podman', 'container', 'exists', 'powersync-sg']).returncode:
        run('run', '-d', '--name', 'powersync-sg', '--network', NETWORK,
            '--memory', '1g', '--cpus', '2', '-p', '127.0.0.1:18767:4984',
            '-p', '127.0.0.1:18768:4985', '-v', str(config_file) + ':/lab/startup.json:ro',
            GATEWAY, '/lab/startup.json')
    else:
        run('start', 'powersync-sg')
    wait(18768)
    if 'neocloud' not in request('/_all_dbs', auth=auth):
        request('/neocloud/', 'PUT', {'bucket': 'neocloud', 'use_views': True,
                'num_index_replicas': 0, 'enable_shared_bucket_access': True,
                'guest': {'disabled': True}, 'session_cookie_secure': False,
                'sync': 'function(doc, oldDoc) { throw({forbidden:"Fixture allowlist not configured"}); }'}, auth)
    images = {image: json.loads(run('image', 'inspect', image))[0]['Digest']
              for image in (SERVER, GATEWAY)}
    (STATE / 'images.json').write_text(json.dumps(images, indent=2))
    print(json.dumps({'ok': True, 'public': '127.0.0.1:18767',
                      'admin': '127.0.0.1:18768', 'writes': 'persisted fixture allowlist' if (STATE / 'fixture-allowlist.json').exists() else 'denied pending fixture allowlist',
                      'gateway': request('/', auth=auth)}))

if __name__ == '__main__':
    main()
