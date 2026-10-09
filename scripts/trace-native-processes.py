#!/usr/bin/env python3
"""Capture stock-process logcat privately during an isolated disposable-account run.

Logs can contain credentials. Never commit or print them; inspect locally with
explicit redaction. Existing processes only; restart capture after PID changes.
"""
import os
from pathlib import Path
import subprocess
import time
import json

ROOT = Path(__file__).resolve().parents[1]
folder = ROOT / 'artifacts/gateway-lab/native-trace'
folder.mkdir(parents=True, exist_ok=True, mode=0o700)
folder.chmod(0o700)
processes = []
streams = []
manifest = []
try:
    for serial in ['bbb70bef', 'dfef8c1']:
        for package in ['com.onyx.android.note', 'com.onyx.android.ksync']:
            result = subprocess.run(['adb', '-s', serial, 'shell', 'pidof', package], capture_output=True, text=True)
            for pid in result.stdout.split():
                if not pid.isdigit():
                    raise ValueError('Unexpected PID')
                path = folder / (serial + '-' + package.rsplit('.', 1)[-1] + '-' + pid + '.log')
                fd = os.open(path, os.O_WRONLY | os.O_CREAT | os.O_APPEND, 0o600)
                os.chmod(path, 0o600)
                stream = os.fdopen(fd, 'wb')
                streams.append(stream)
                proc = subprocess.Popen(['adb', '-s', serial, 'logcat', '--pid=' + pid, '-T', '1', '-v', 'epoch'], stdout=stream, stderr=subprocess.DEVNULL)
                processes.append(proc)
                manifest.append({'serial': serial, 'package': package, 'pid': int(pid), 'log': path.name, 'startedAt': time.time()})
    (folder / 'processes.json').write_text(json.dumps(manifest, indent=2))
    (folder / 'processes.json').chmod(0o600)
    print('Private stock-process traces started; no log contents printed.', flush=True)
    deadline = time.monotonic() + 900
    while time.monotonic() < deadline:
        if any(proc.poll() is not None for proc in processes):
            raise RuntimeError('A logcat process exited; restart trace after checking device connection')
        if sum(path.stat().st_size for path in folder.glob('*.log')) > 64 * 1024 * 1024:
            raise RuntimeError('Trace size limit reached')
        time.sleep(1)
finally:
    for proc in processes:
        proc.terminate()
    for proc in processes:
        proc.wait(timeout=5)
    for stream in streams:
        stream.close()
