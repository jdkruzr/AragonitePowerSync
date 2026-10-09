#!/usr/bin/env python3
"""Two-device lab toggle: modify only native note-sync flag, verify system value.

Enable requires lab cluster plus a provisioned Gateway fixture allowlist.
Disable works during teardown. Uses the already recovered system service ABI.
"""
import argparse
import importlib.util
import json
from pathlib import Path
import shlex
import subprocess

ROOT = Path(__file__).resolve().parents[1]
spec = importlib.util.spec_from_file_location('reader', ROOT / 'scripts/read-cluster-config.py')
reader = importlib.util.module_from_spec(spec)
spec.loader.exec_module(reader)

def read(serial, key):
    reply = subprocess.check_output(['adb', '-s', serial, 'shell', 'service', 'call',
                                     'onyx_device_service', '3', 's16', key], text=True)
    return json.loads(reader.decode_string_reply(reply))

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('serial', choices=['bbb70bef', 'dfef8c1'])
    parser.add_argument('value', choices=['true', 'false'])
    args = parser.parse_args()
    if args.value == 'true':
        cluster = read(args.serial, 'ONYX_CLUSTER_SYSTEM_KEY')
        if cluster['curClusterId'] != 'powersync_lab':
            raise ValueError('Native note enable requires lab cluster')
        allowlist = json.loads((ROOT / 'artifacts/gateway-lab/fixture-allowlist.json').read_text())
        if not allowlist:
            raise ValueError('Configure disposable fixture allowlist first')
    config = read(args.serial, 'ONYX_SYSTEM_CONFIG_KEY')
    changed = json.loads(json.dumps(config))
    changed['configMap']['key_enable_sync_note_data'] = args.value
    command = 'service call onyx_device_service 4 s16 ONYX_SYSTEM_CONFIG_KEY s16 ' + shlex.quote(json.dumps(changed))
    subprocess.check_output(['adb', '-s', args.serial, 'shell', command], text=True)
    if read(args.serial, 'ONYX_SYSTEM_CONFIG_KEY') != changed:
        raise ValueError('Native system flag verification failed')
    # Native code emits this same broadcast after its own persisted config write.
    subprocess.run(['adb', '-s', args.serial, 'shell', 'am', 'broadcast',
                    '-a', 'com.onyx.action.system.config.changed'], check=True, stdout=subprocess.DEVNULL)
    print(json.dumps({'nativeNoteSync': args.value, 'otherConfiguration': 'unchanged'}))

if __name__ == '__main__':
    main()
