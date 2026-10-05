#!/usr/bin/env python3
"""Read only ONYX_CLUSTER_SYSTEM_KEY through Palma's captured system Binder ABI."""
import argparse
import json
import re
import struct
import subprocess
import shlex
from pathlib import Path

def decode_string_reply(output):
    raw = bytearray()
    for line in output.splitlines():
        match = re.search(r'0x[0-9a-fA-F]+:\s*((?:[0-9a-fA-F]{8}(?:\s+|$)){1,4})', line)
        if match:
            for word in match.group(1).split():
                raw.extend(struct.pack('<I', int(word, 16)))
    if len(raw) < 8:
        raise ValueError('No Binder string reply')
    exception, length = struct.unpack_from('<ii', raw)
    if exception != 0 or not 0 <= length <= 524288 or 8 + length * 2 > len(raw):
        raise ValueError('Invalid Binder string reply')
    return raw[8:8 + length * 2].decode('utf-16le')

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('serial')
    parser.add_argument('--transaction', type=int, required=True,
                        help='getSystemConfig transaction verified in this firmware')
    parser.add_argument('--output', type=Path, required=True)
    parser.add_argument('--restore-from', type=Path,
                        help='restore exactly a saved cluster configuration after native switch-back')
    args = parser.parse_args()
    if args.serial != 'bbb70bef':
        parser.error('this captured ABI is qualified only on Palma serial bbb70bef')
    if args.transaction != 3:
        parser.error('getSystemConfig transaction must be the verified read-only code 3')
    reply = subprocess.check_output(['adb', '-s', args.serial, 'shell', 'service', 'call',
                                     'onyx_device_service', str(args.transaction),
                                     's16', 'ONYX_CLUSTER_SYSTEM_KEY'], text=True)
    config = json.loads(decode_string_reply(reply))
    if not isinstance(config.get('clusterMap'), dict) or not config.get('curClusterId'):
        raise ValueError('Unexpected cluster schema')
    if args.restore_from:
        saved = json.loads(args.restore_from.read_text())
        if not isinstance(saved.get('clusterMap'), dict) or not saved.get('curClusterId'):
            raise ValueError('Unexpected saved cluster schema')
        if config['curClusterId'] != saved['curClusterId']:
            raise ValueError('Switch back through native Binder before restoring configuration')
        if 'powersync_lab' in saved['clusterMap']:
            raise ValueError('Saved baseline unexpectedly includes lab cluster')
        # setSystemConfig=4 was verified beside getSystemConfig=3 in the captured
        # IOnyxDeviceService. This writes only the key read above, never files or
        # account state. Shell quoting protects the structured string argument.
        command = 'service call onyx_device_service 4 s16 ONYX_CLUSTER_SYSTEM_KEY s16 ' + shlex.quote(json.dumps(saved))
        subprocess.check_output(['adb', '-s', args.serial, 'shell', command], text=True)
        reply = subprocess.check_output(['adb', '-s', args.serial, 'shell', 'service', 'call',
                                         'onyx_device_service', str(args.transaction),
                                         's16', 'ONYX_CLUSTER_SYSTEM_KEY'], text=True)
        config = json.loads(decode_string_reply(reply))
        if config != saved:
            raise ValueError('Restored configuration verification failed')
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(config, indent=2) + '\n')
    # Host observation is staged only in our own application's private directory.
    subprocess.run(['adb', '-s', args.serial, 'exec-in', 'run-as', 'dev.aragonite.powersync',
                    'sh', '-c', 'cat > files/cluster-config-observed.json'],
                   input=json.dumps(config).encode(), check=True, stdout=subprocess.DEVNULL)
    print(json.dumps({'clusterId': config['curClusterId'],
                      'availableClusterIds': list(config['clusterMap'])}))

if __name__ == '__main__':
    main()
