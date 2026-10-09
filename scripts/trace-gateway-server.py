#!/usr/bin/env python3
"""Bounded lab Gateway protocol capture and durable metadata-only changes journal.

Raw protocol logs are private and may contain synthetic payloads or credentials.
No device mutation. Console verbosity is restored on orderly exit.
"""
import argparse
from datetime import datetime, timezone
import importlib.util
import json
import os
from pathlib import Path
import re
import signal
import subprocess
import threading
import time
from urllib.error import HTTPError
from urllib.parse import quote

ROOT = Path(__file__).resolve().parents[1]
spec = importlib.util.spec_from_file_location('gateway', ROOT / 'scripts/gateway-lab.py')
gateway = importlib.util.module_from_spec(spec)
spec.loader.exec_module(gateway)

FIELDS = ['dbId', 'documentUniqueId', 'documentId', 'sourceDeviceId', 'sourceOperationId',
          'bytes', 'sha256', 'type', 'updatedAt', 'commitType', 'recordType', 'commitStatus',
          'msgType', 'replicatorName', 'syncAt', 'msgStatus']

def durable_append(path, value):
    fd = os.open(path, os.O_WRONLY | os.O_CREAT | os.O_APPEND, 0o600)
    with os.fdopen(fd, 'a') as stream:
        stream.write(json.dumps(value) + '\n')
        stream.flush()
        os.fsync(stream.fileno())

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('run')
    parser.add_argument('--seconds', type=int, default=900)
    args = parser.parse_args()
    if not re.fullmatch('[a-zA-Z0-9_-]{1,64}', args.run) or not 1 <= args.seconds <= 1800:
        parser.error('Use a bounded run name and 1–1800 seconds')
    folder = gateway.STATE / 'protocol-traces' / args.run
    if folder.exists():
        raise ValueError('Run already exists; preserve its journal and select a new name')
    folder.mkdir(parents=True, mode=0o700)
    folder.chmod(0o700)
    creds = json.loads((gateway.STATE / 'credentials.json').read_text())
    auth = creds['username'], creds['password']
    old_keys = gateway.request('/_logging', auth=auth)
    old_level = gateway.request('/_config?include_runtime=true', auth=auth)['logging']['console']['log_level']
    baseline = gateway.request('/neocloud/_changes?limit=1000', auth=auth)
    cursor = baseline['last_seq']
    started = datetime.now(timezone.utc).isoformat()
    durable_append(folder / 'control.jsonl', {'event': 'baseline', 'at': time.time(), 'since': cursor,
                                             'originalLogKeys': old_keys, 'originalLogLevel': old_level})
    stop = threading.Event()
    for sig in [signal.SIGINT, signal.SIGTERM]:
        signal.signal(sig, lambda *_: stop.set())
    log_process = None
    stream = None
    try:
        keys = {k: True for k in ['HTTP', 'Sync', 'SyncMsg', 'CRUD', 'Changes', 'WS']}
        gateway.request('/_logging?logLevel=trace', 'PUT', keys, auth)
        log_path = folder / 'gateway-private.log'
        stream = os.fdopen(os.open(log_path, os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600), 'wb')
        log_process = subprocess.Popen(['podman', 'logs', '--follow', '--since', started, 'powersync-sg'],
                                       stdout=stream, stderr=stream)
        durable_append(folder / 'control.jsonl', {'event': 'ready', 'at': time.time(), 'since': cursor})
        print('Gateway protocol capture and durable revision journal ready.', flush=True)
        deadline = time.monotonic() + args.seconds
        while not stop.is_set() and time.monotonic() < deadline:
            if log_process.poll() is not None or log_path.stat().st_size > 64 * 1024 * 1024:
                raise RuntimeError('Protocol capture stopped or exceeded size bound')
            response = gateway.request('/neocloud/_changes?feed=longpoll&timeout=1000&limit=1000&since=' + quote(str(cursor), safe=''), auth=auth)
            for change in response['results']:
                event = {'at': time.time(), 'seq': change['seq'], 'id': change['id'],
                         'changes': change['changes'], 'deleted': change.get('deleted', False)}
                # Fetch the announced revision, not whatever happens to be current later.
                rev = change['changes'][0]['rev']
                try:
                    doc = gateway.request('/neocloud/' + quote(change['id'], safe='') + '?rev=' + quote(rev, safe=''), auth=auth)
                    event['metadata'] = {key: doc[key] for key in FIELDS if key in doc}
                except HTTPError as error:
                    with error:
                        event['revisionMetadataStatus'] = error.code
                durable_append(folder / 'changes.jsonl', event)
            cursor = response['last_seq']
            durable_append(folder / 'cursor.jsonl', {'at': time.time(), 'since': cursor})
    finally:
        if log_process is not None:
            log_process.terminate()
            log_process.wait(timeout=10)
        if stream is not None:
            stream.close()
        gateway.request('/_logging?logLevel=' + old_level, 'PUT', old_keys, auth)
        durable_append(folder / 'control.jsonl', {'event': 'stopped', 'at': time.time(), 'lastSeq': cursor,
                                                 'loggingRestored': True})
        print('Capture stopped; Gateway console logging restored.', flush=True)

if __name__ == '__main__':
    main()
