#!/usr/bin/env python3
"""Audit decoded native reader asset revisions; never modify devices or server.

Input is new-assets-private.json from a qualification capture. Status disagreements
are candidates for review, not proof of concurrency: sequential Undo has the same
pattern. No timestamp or Couchbase configuration winner is chosen here.
"""
import argparse
import json
from pathlib import Path


def audit(assets):
    shapes = {}
    for asset in assets:
        for record in asset.get('shapeRecords', []):
            shape_id = record.get('UNIQUEID')
            revision = record.get('INFOREVISIONID')
            if not shape_id or not revision:
                raise ValueError('Shape record lacks shape or information revision identity')
            status = record.get('STATUS', 0)
            if status not in (0, 1):
                raise ValueError(f'Unsupported native status: {status}')
            operation = {
                'informationRevision': revision,
                'status': status,
                'updatedAt': record.get('UPDATEDAT'),
                'pressureRevision': record.get('REVISIONID'),
                'assetId': asset['id'],
                'assetSha256': asset['sha256'],
            }
            shapes.setdefault(shape_id, []).append(operation)
    candidates = []
    for shape_id, operations in sorted(shapes.items()):
        by_revision = {}
        for operation in operations:
            previous = by_revision.get(operation['informationRevision'])
            if previous and (previous['status'], previous['pressureRevision']) != (
                    operation['status'], operation['pressureRevision']):
                raise ValueError('Inconsistent payloads for one information revision')
            by_revision[operation['informationRevision']] = operation
        if len({x['status'] for x in operations}) > 1:
            candidates.append({'shapeId': shape_id, 'operations': operations,
                               'pressureDependencyUnchanged': len({x['pressureRevision'] for x in operations}) == 1})
    return {'schemaVersion': 1, 'classification': 'status-disagreement-candidates',
            'requiresCausalityOrDeviceEvidence': True, 'winnerSelected': False,
            'candidates': candidates}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('assets', type=Path)
    parser.add_argument('--output', type=Path)
    args = parser.parse_args()
    result = json.dumps(audit(json.loads(args.assets.read_text())), indent=2) + '\n'
    if args.output:
        args.output.write_text(result)
    else:
        print(result, end='')


if __name__ == '__main__':
    main()
