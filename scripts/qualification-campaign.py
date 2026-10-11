#!/usr/bin/env python3
"""Bounded read-only device/production observations and explicit qualification receipts."""
import argparse
import datetime
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
import time
import urllib.request

DEVICES = {'palma': 'bbb70bef', 'lumi': 'dfef8c1'}
CASES = ('fresh_restore', 'retry_recovery', 'disaster_restore', 'book_lifecycle', 'conflicts', 'bounded_load', 'failure_reporting')

def now(): return datetime.datetime.now(datetime.timezone.utc).isoformat()
def command(args):
    r = subprocess.run(args, capture_output=True, text=True, timeout=30)
    if r.returncode: raise RuntimeError('command failed')
    return r.stdout

def observe(root):
    row = {'at': now(), 'devices': {}}
    for name, serial in DEVICES.items():
        d = {}
        try:
            battery = command(['adb', '-s', serial, 'shell', 'dumpsys', 'battery'])
            for field in ('level', 'temperature'):
                match = re.search(r'^\s*' + field + r': (\d+)', battery, re.M)
                d[field] = int(match.group(1)) if match else None
            power = command(['adb', '-s', serial, 'shell', 'dumpsys', 'power'])
            wake = re.search(r'mWakefulness=(\w+)', power)
            d['wakefulness'] = wake.group(1) if wake else 'unknown'
            raw = command(['adb', '-s', serial, 'shell', 'content', 'query', '--uri', 'content://com.onyx.android.sdk.note.ContentProvider/NoteModel', '--projection', 'uniqueId:userId:type:status:noteSyncStatus'])
            if 'Exception' in raw or not (raw.startswith('Row:') or 'No result found' in raw):
                raise RuntimeError('unrecognized provider response')
            rows = [re.sub(r'^Row: \d+ ', '', s) for s in raw.splitlines() if s.startswith('Row:')]
            d['catalog_rows'] = len(rows)
            d['catalog_sha256'] = hashlib.sha256('\n'.join(sorted(rows)).encode()).hexdigest()
        except Exception as error:
            d['observation_error'] = type(error).__name__
        row['devices'][name] = d
    start = time.monotonic()
    try:
        with urllib.request.urlopen('https://alexandria.federation.engineering/readyz', timeout=10) as r:
            row['ready_status'] = r.status
    except Exception as error:
        row['ready_error'] = type(error).__name__
    row['ready_seconds'] = round(time.monotonic() - start, 3)
    with (root / 'observations.jsonl').open('a') as f: f.write(json.dumps(row) + '\n')
    print(json.dumps(row), flush=True)

def main():
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('--state',type=Path,required=True)
    sub=p.add_subparsers(dest='action',required=True)
    sub.add_parser('snapshot')
    m=sub.add_parser('monitor');m.add_argument('--minutes',type=int,default=120);m.add_argument('--interval-seconds',type=int,default=60)
    r=sub.add_parser('receipt');r.add_argument('case',choices=CASES);r.add_argument('status',choices=('passed','failed','blocked','deferred','in_progress'));r.add_argument('--evidence',required=True);r.add_argument('--summary',required=True)
    args=p.parse_args();os.umask(0o077);args.state.mkdir(parents=True,exist_ok=True,mode=0o700)
    if args.action=='receipt':
        dest=args.state/'receipts.jsonl'
        with dest.open('a') as f:f.write(json.dumps({'at':now(),'case':args.case,'status':args.status,'evidence':args.evidence,'summary':args.summary})+'\n')
    elif args.action=='snapshot':observe(args.state)
    else:
        if not 1<=args.minutes<=2880:p.error('monitor duration must be 1..2880 minutes')
        if not 30<=args.interval_seconds<=900:p.error('monitor interval must be 30..900 seconds')
        summary={'startedAt':now(),'minutes':args.minutes,'intervalSeconds':args.interval_seconds,'state':'running','samples':0}
        status=args.state/'monitor-status.json'
        status.write_text(json.dumps(summary))
        end=time.monotonic()+args.minutes*60
        while time.monotonic()<end:
            start=time.monotonic();observe(args.state)
            summary['samples']+=1;summary['lastSampleAt']=now();status.write_text(json.dumps(summary))
            time.sleep(max(0,min(args.interval_seconds-(time.monotonic()-start),end-time.monotonic())))
        summary['state']='completed';summary['finishedAt']=now();status.write_text(json.dumps(summary))
if __name__=='__main__':main()
