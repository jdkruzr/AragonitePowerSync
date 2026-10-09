#!/usr/bin/env python3
"""Read-only two-device fixture snapshot; no account/cluster changes or payload export."""
import argparse
from concurrent.futures import ThreadPoolExecutor
from datetime import datetime, timezone
import hashlib
import json
from pathlib import Path
import re
import subprocess

ROOT = Path(__file__).resolve().parents[1]
DEVICES = {'bbb70bef': 'palma2pro', 'dfef8c1': 'lumi'}

def adb(serial, *args):
    return subprocess.check_output(['adb', '-s', serial, *args], timeout=30).decode(errors='replace')

def capture(serial, fixture, phase):
    target = ROOT / 'artifacts' / DEVICES[serial] / 'gateway' / 'trace' / phase / fixture
    target.mkdir(parents=True, exist_ok=True, mode=0o700)
    target.chmod(0o700)
    files = []
    for folder in ['document', 'point']:
        base = '/sdcard/.ksync/' + folder + '/' + fixture
        result = subprocess.run(['adb', '-s', serial, 'shell', 'find', base, '-type', 'f'],
                                capture_output=True, text=True, timeout=30)
        if result.returncode:
            files.append({'root': folder, 'available': False})
            continue
        paths = result.stdout.splitlines()
        if len(paths) > 2000:
            raise ValueError('Fixture file count exceeds trace bound')
        for path in paths:
            if not path.startswith(base + '/') or any(c in path for c in '\x00\n\r'):
                raise ValueError('Unexpected fixture path')
            # exec-out passes an Android shell command; quote every file path.
            import shlex
            command = 'sha256sum ' + shlex.quote(path)
            checksum = adb(serial, 'exec-out', command).split()[0]
            if not re.fullmatch('[0-9a-f]{64}', checksum):
                raise ValueError('Unexpected digest response')
            files.append({'root': folder, 'path': path[len(base)+1:], 'sha256': checksum})
    # Retain only lines explicitly naming this fixture. Missing lines are not proof
    # that an action did not run; generic failures require a separate PID trace.
    log = adb(serial, 'logcat', '-d', '-t', '10000', '-v', 'epoch')
    lines = [line for line in log.splitlines() if fixture in line]
    report = {'serial': serial, 'device': DEVICES[serial], 'fixture': fixture,
              'phase': phase, 'at': datetime.now(timezone.utc).isoformat(),
              'files': files, 'fixtureLogLines': len(lines),
              'payloadsExported': False, 'deviceStateChanged': False}
    for name, content in [('snapshot.json', json.dumps(report, indent=2)),
                          ('fixture-logcat.txt', '\n'.join(lines) + '\n')]:
        path = target / name
        path.write_text(content)
        path.chmod(0o600)
    return {'device': DEVICES[serial], 'fileEntries': len(files), 'fixtureLogLines': len(lines)}

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('phase')
    parser.add_argument('--fixture', help='Select one of the explicitly allowlisted notebooks')
    args = parser.parse_args()
    if not re.fullmatch('[a-zA-Z0-9_-]{1,64}', args.phase):
        parser.error('Use a simple bounded phase name')
    ids = json.loads((ROOT / 'artifacts/gateway-lab/fixture-allowlist.json').read_text())
    fixture = args.fixture or (ids[0] if len(ids) == 1 else None)
    if fixture not in ids or not re.fullmatch('[0-9a-f-]{32,36}', fixture):
        raise ValueError('Select exactly one previously allowlisted lab fixture')
    with ThreadPoolExecutor(max_workers=2) as pool:
        futures = [pool.submit(capture, serial, fixture, args.phase) for serial in DEVICES]
        for future in futures:
            print(json.dumps(future.result()))

if __name__ == '__main__':
    main()
