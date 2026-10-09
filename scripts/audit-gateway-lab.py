#!/usr/bin/env python3
"""Read-only audit of retained disposable Gateway data and server request journals."""
import base64
from collections import Counter
from datetime import datetime, timezone
import hashlib
import importlib.util
import json
from pathlib import Path
from urllib.parse import unquote

ROOT = Path(__file__).resolve().parents[1]
spec = importlib.util.spec_from_file_location('gateway', ROOT / 'scripts/gateway-lab.py')
gateway = importlib.util.module_from_spec(spec)
spec.loader.exec_module(gateway)

def main():
    state = gateway.STATE
    credentials = json.loads((state / 'credentials.json').read_text())
    auth = credentials['username'], credentials['password']
    fixture = json.loads((state / 'ui-note-request.json').read_text())['documentId']
    allowlist = json.loads((state / 'fixture-allowlist.json').read_text())
    if fixture not in allowlist:
        raise ValueError('UI fixture not allowlisted')
    requests = [json.loads(line) for line in (state / 'shim/requests.jsonl').read_text().splitlines()]
    # Second experiment began with disposable account sign-ins at this recorded UTC boundary.
    requests = [event for event in requests if event['at'] >= 1791252000]
    rows = gateway.request('/neocloud/_all_docs?include_docs=true', auth=auth)['rows']
    inventory = []
    marker_objects = []
    for row in rows:
        doc = row.get('doc', {})
        parent = doc.get('documentUniqueId') or doc.get('documentId')
        if row['id'] != fixture and parent != fixture:
            continue
        if doc.get('user') != 'powersync-lab':
            raise ValueError('Unexpected tenant')
        kind = 'asset' if doc.get('type') == 'labAsset' else 'notebook' if row['id'] == fixture else 'commit_record'
        item = {'id': row['id'], 'rev': doc['_rev'], 'kind': kind}
        for key in ['dbId', 'bytes', 'ossKey', 'sha256', 'updatedAt', 'commitType', 'recordType', 'commitStatus']:
            if key in doc:
                item[key] = doc[key]
        inventory.append(item)
        if kind == 'asset' and '/resource/data/' in doc.get('ossKey', ''):
            full = gateway.request('/neocloud/' + row['id'] + '?attachments=true', auth=auth)
            content = base64.b64decode(full['_attachments']['content']['data'])
            if hashlib.sha256(content).hexdigest() != doc['sha256']:
                raise ValueError('Attachment integrity failure')
            markers = [marker for marker in ['ALPHA', 'BETA edited on Lumi'] if marker.encode() in content]
            put_events = [event for event in requests if event.get('service') == 'fixture-asset'
                          and event['method'] == 'PUT' and event['responseStatus'] == 200
                          and event['bodySha256'] == doc['sha256']]
            filename = doc['ossKey'].rsplit('/', 1)[-1]
            get_events = [event for event in requests if event.get('service') == 'fixture-asset'
                          and event['method'] == 'GET' and event['responseStatus'] == 200
                          and unquote(event['path']).endswith('/' + filename)]
            marker_objects.append({'key': doc['ossKey'], 'markers': markers, 'sha256': doc['sha256'],
                                   'uploads': [{'at': e['at'], 'deviceId': e['deviceId'], 'bytes': e['bodyBytes']} for e in put_events],
                                   'downloads': [{'at': e['at'], 'deviceId': e['deviceId']} for e in get_events]})
    device_stats = {}
    for principal, label in [('device-7eea12e7cc57', 'Palma'), ('device-c5f80bd5a2a0', 'Lumi')]:
        events = [e for e in requests if e.get('deviceId') == principal]
        files = [e for e in events if e.get('service') == 'fixture-asset']
        methods = Counter((e['method'], e['responseStatus']) for e in files)
        device_stats[label] = {'deviceId': principal, 'accountApiRequests': len(events) - len(files),
                               'fileRequests': {method + ' ' + str(status): count for (method, status), count in methods.items()},
                               'uploadedBytesIncludingRepeats': sum(e['bodyBytes'] for e in files if e['method'] == 'PUT' and e['responseStatus'] == 200)}
    changes = gateway.request('/neocloud/_changes?limit=1000', auth=auth)
    proxy = [json.loads(line) for line in (state / 'proxy-events.jsonl').read_text().splitlines()]
    proxy = [e for e in proxy if datetime.fromisoformat(e['at'].replace('Z', '+00:00')).timestamp() >= 1791252000]
    report = {'fixture': fixture, 'requestWindowUTC': [datetime.fromtimestamp(min(e['at'] for e in requests), timezone.utc).isoformat(),
                                                     datetime.fromtimestamp(max(e['at'] for e in requests), timezone.utc).isoformat()],
              'trafficScope': 'All allowlisted fixture traffic during UI-note experiment, including original fixture',
              'devices': device_stats, 'storedUIFixtureDocuments': dict(Counter(d['kind'] for d in inventory)),
              'storedUIFixtureAssetBytes': sum(d.get('bytes', 0) for d in inventory),
              'inventory': inventory, 'markerObjects': marker_objects,
              'blipUpgrades': dict(Counter(e.get('deviceId') for e in proxy if e.get('event') == 'response' and e.get('status') == 101)),
              'gatewayLatestChangesRows': len(changes['results']), 'gatewayLastSequence': changes['last_seq'],
              'limitations': ['Normal Gateway logs omit per-message BLIP details.',
                              'Changes feed is latest change per document, not full event history.',
                              'Gateway sequence and revision generation are not counts of user edits.',
                              'Upload sizes include retries and repeated object writes; GET byte counts were not logged.',
                              'Native BLIP writes have no durable per-write device attribution in current documents.']}
    output = state / 'server-audit'
    output.mkdir(exist_ok=True, mode=0o700)
    (output / 'report.json').write_text(json.dumps(report, indent=2))
    (output / 'report.json').chmod(0o600)
    print(json.dumps({key: report[key] for key in ['requestWindowUTC', 'devices', 'storedUIFixtureDocuments', 'storedUIFixtureAssetBytes', 'blipUpgrades', 'markerObjects', 'gatewayLatestChangesRows', 'gatewayLastSequence']}, indent=2))

if __name__ == '__main__':
    main()
