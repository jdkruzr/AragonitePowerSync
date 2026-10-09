#!/usr/bin/env python3
"""Configure reference Gateway access for explicitly journaled lab notebooks only."""
import argparse
import importlib.util
import json
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]
spec = importlib.util.spec_from_file_location('gateway', ROOT / 'scripts/gateway-lab.py')
gateway = importlib.util.module_from_spec(spec)
spec.loader.exec_module(gateway)
UID = 'powersync-lab'

def sync_function(ids, note_doorbell=False):
    return '''function(doc, oldDoc) {
 var ids = %s;
 var tree = %s;
 var scope = doc.dbId || (oldDoc && oldDoc.dbId);
 var user = doc.user || (oldDoc && oldDoc.user);
 var parent = doc.documentUniqueId || doc.documentId || (oldDoc && (oldDoc.documentUniqueId || oldDoc.documentId));
 var ownDoc = ids.indexOf(doc._id) >= 0 || ids.indexOf(parent) >= 0;
 var ownScope = false;
 %s
 for (var i = 0; i < ids.length; i++) {
   if (scope === %s + "-" + ids[i]) ownScope = true;
   if (scope === %s + "-ASSET-" + ids[i] && (doc.type || (oldDoc && oldDoc.type)) === "labAsset" && parent === ids[i]) ownScope = true;
 }
 if (user !== %s || !((scope === tree && ownDoc) || ownScope)) {
   throw({forbidden: "Outside disposable fixture allowlist"});
 }
 requireAccess(scope);
 channel(scope);
}''' % (json.dumps(ids), json.dumps(UID + '-NOTE_TREE'),
        ('if (user === "powersync-lab" && scope === "powersync-lab-MESSAGE" && '
         'doc.msgType === 6 && doc.replicatorName === "NOTE_TREE" && '
         '!doc._deleted && typeof doc.syncAt === "number") { '
         'requireAccess(scope); channel(scope); return; }') if note_doorbell else '',
        json.dumps(UID), json.dumps(UID), json.dumps(UID))

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--note-doorbell', action='store_true',
                        help='also admit synthetic-account type-6 NOTE_TREE notifications')
    parser.add_argument('reports', nargs='+', type=Path, help='native gateway-create/capture reports')
    args = parser.parse_args()
    ids = []
    for path in args.reports:
        report = json.loads(path.read_text())
        note = report['note']
        if note.get('userId') != UID or not note.get('title', '').startswith('PowerSync fixture '):
            raise ValueError('Not a disposable lab-account fixture')
        if not report['preservation']['otherRowsPreserved']:
            raise ValueError('Native library preservation check failed')
        if not re.fullmatch(r'[0-9a-f-]{32,36}', note['uniqueId']):
            raise ValueError('Unexpected native document ID')
        ids.append(note['uniqueId'])
    ids = sorted(set(ids))
    credentials = json.loads((gateway.STATE / 'credentials.json').read_text())
    auth = credentials['username'], credentials['password']
    config = gateway.request('/neocloud/_config', auth=auth)
    config['sync'] = sync_function(ids, args.note_doorbell)
    gateway.request('/neocloud/_config', 'PUT', config, auth)
    doorbell_marker = gateway.STATE / 'note-doorbell-enabled'
    if args.note_doorbell:
        doorbell_marker.touch()
    else:
        doorbell_marker.unlink(missing_ok=True)
    (gateway.STATE / 'fixture-allowlist.json').write_text(json.dumps(ids, indent=2))
    registry = gateway.STATE / 'shim/devices.json'
    if registry.exists():
        for device in json.loads(registry.read_text()).values():
            if not device.get('sessionId'):
                continue  # STS-only test clients have no Gateway principal.
            channels = [UID + '-NOTE_TREE'] + [UID + '-' + item for item in ids]
            if args.note_doorbell:
                channels.append(UID + '-MESSAGE')
            gateway.request('/neocloud/_user/' + device['deviceId'], 'PUT',
                            {'name': device['deviceId'], 'admin_channels': channels}, auth)
    print(json.dumps({'allowedFixtureIds': ids, 'otherNotebooks': 'refused'}))

if __name__ == '__main__':
    main()
