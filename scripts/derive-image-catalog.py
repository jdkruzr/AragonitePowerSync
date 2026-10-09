#!/usr/bin/env python3
"""Lab-only catalog reconstruction; immutable input, fresh-copy import required."""
import argparse
import hashlib
import io
import json
import os
from pathlib import Path
import zipfile

from native_wire import encode, fields


def derive(source, target):
    source, target = Path(source), Path(target)
    receipt_path = target.with_suffix('.receipt.json')
    if source.resolve() == target.resolve() or target.exists() or receipt_path.exists():
        raise ValueError('Require a fresh output distinct from input')
    resources = {}
    with zipfile.ZipFile(source) as archive:
        names = archive.namelist()
        if len(names) != len(set(names)) or archive.testzip() is not None:
            raise ValueError('Duplicate entries or bad CRC')
        roots = {n.split('/')[0] for n in names}
        if len(roots) != 1:
            raise ValueError('Expected one archive namespace')
        root = roots.pop()
        catalogs = [n for n in names if '/resource/' in n and '/data/' not in n and not n.endswith('/')]
        if len(catalogs) != 1 or archive.read(catalogs[0]):
            raise ValueError('This experiment requires exactly one empty catalog')
        for name in names:
            if not name.startswith(root + '/shape/') or not name.endswith('.zip'):
                continue
            with zipfile.ZipFile(io.BytesIO(archive.read(name))) as shapes:
                if shapes.testzip() is not None:
                    raise ValueError('Bad nested CRC')
                for entry in shapes.namelist():
                    if entry.endswith('/'):
                        continue
                    for number, wire, body in fields(shapes.read(entry)):
                        if number != 1 or wire != 2:
                            continue
                        row = {n: v for n, w, v in fields(body)}
                        if row.get(12) != 19 or row.get(15, 0) != 0:
                            continue
                        resource = json.loads(row[14])
                        uid, relative = resource['uniqueId'], resource['relativePath']
                        if not uid or not relative or '\\' in relative or '..' in relative.split('/'):
                            raise ValueError('Unsafe or empty resource identity/path')
                        asset = root + '/resource/data/' + relative.lstrip('/')
                        if asset not in names or resource.get('status', 0) != 0:
                            raise ValueError('Resource must be live and have its exact file')
                        if resource['documentId'] != root:
                            raise ValueError('Resource namespace mismatch')
                        strings = {1: uid, 2: root, 5: resource.get('title', ''),
                                   8: relative, 9: resource.get('ossUrl', ''),
                                   11: resource.get('revisionId', ''), 12: resource.get('localPath', '')}
                        integers = {3: row.get(2, 0), 4: row.get(3, 0),
                                    6: resource.get('audioDuration', 0), 7: resource['type'], 13: 0}
                        encoded = encode(sorted([(n, 2, v.encode()) for n, v in strings.items()] +
                                                [(n, 0, v) for n, v in integers.items()]))
                        if uid in resources and resources[uid][0] != encoded:
                            raise ValueError('Ambiguous resource descriptions')
                        resources[uid] = (encoded, asset)
        if not resources:
            raise ValueError('No recoverable live image resources')
        catalog = encode([(1, 2, value[0]) for key, value in sorted(resources.items())])
        target.parent.mkdir(parents=True, exist_ok=True)
        with target.open('xb') as stream, zipfile.ZipFile(stream, 'w', zipfile.ZIP_DEFLATED) as output:
            os.fchmod(stream.fileno(), 0o600)
            for entry in archive.infolist():
                output.writestr(entry, catalog if entry.filename == catalogs[0] else archive.read(entry))
    with zipfile.ZipFile(source) as original, zipfile.ZipFile(target) as repaired:
        if repaired.testzip() is not None:
            raise ValueError('Derived CRC failure')
        unchanged = all(original.read(n) == repaired.read(n) for n in names if n != catalogs[0])
        if not unchanged:
            raise ValueError('Unexpected payload mutation')
        receipt = {'sourceSha256': hashlib.sha256(source.read_bytes()).hexdigest(),
                   'derivedSha256': hashlib.sha256(target.read_bytes()).hexdigest(),
                   'changedEntry': catalogs[0], 'resourceCount': len(resources),
                   'otherEntriesByteIdentical': unchanged, 'nativeRecoveryQualified': False,
                   'assetHashes': {asset: hashlib.sha256(repaired.read(asset)).hexdigest()
                                   for _, asset in resources.values()}}
    with receipt_path.open('x') as stream:
        os.fchmod(stream.fileno(), 0o600)
        stream.write(json.dumps(receipt, indent=2) + '\n')
    return {'resourceCount': len(resources), 'otherEntriesByteIdentical': unchanged,
            'nativeRecoveryQualified': False}


if __name__ == '__main__':
    os.umask(0o077)
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('source'); parser.add_argument('target')
    args = parser.parse_args()
    print(json.dumps(derive(args.source, args.target)))
