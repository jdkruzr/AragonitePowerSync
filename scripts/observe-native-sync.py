#!/usr/bin/env python3
"""Bounded, read-only BOOX sync observations. Reports aggregates, never provider rows.

Native task counts are per-document/batch, not a global completion percentage.
Log windows can overlap; window event counts must not be summed across samples.
"""
import argparse
from collections import Counter
import datetime
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
import time


def command(serial, *args):
    result = subprocess.run(['adb', '-s', serial, *args], capture_output=True, text=True, timeout=30)
    if result.returncode:
        raise RuntimeError('ADB read failed')
    return result.stdout


def catalog(serial, uri, projection, fields):
    raw = command(serial, 'shell', 'content', 'query', '--uri', uri, '--projection', projection)
    if 'Exception' in raw or not (raw.startswith('Row:') or 'No result found' in raw):
        raise RuntimeError('Unrecognized catalog reply')
    rows = [re.sub(r'^Row: \d+ ', '', line) for line in raw.splitlines() if line.startswith('Row:')]
    return {'rows': len(rows), 'digest': hashlib.sha256('\n'.join(sorted(rows)).encode()).hexdigest(),
            'states': {field: dict(Counter(re.findall(r'(?:^|, )' + field + r'=([^,\n]+)', '\n'.join(rows), re.M)))
                       for field in fields}}


def logs(serial):
    pids = command(serial, 'shell', 'pidof', 'com.onyx.android.ksync').split()
    if not pids or not all(pid.isdigit() for pid in pids):
        raise RuntimeError('Native sync process unavailable')
    raw = command(serial, 'logcat', '-d', '--pid=' + pids[0], '-t', '4000', '-v', 'epoch')
    lines = raw.splitlines()
    times = [float(m.group(1)) for line in lines if (m := re.match(r'^\s*(\d+\.\d+)\s', line))]
    event_counts = Counter()
    documents = Counter()
    successful = incomplete = batches = 0
    last_task = None
    for line in lines:
        for event in ('StreamResetException', 'REFUSED_STREAM', 'IOException', 'noticeDownloadBookSuccess',
                      'noticeUploadBookSuccess', 'noticeDownloadBookError', 'noticeUploadBookError'):
            if event in line:
                event_counts[event] += 1
        for tag in ('NoteTreeReplicator', 'ReaderLibraryReplicator', 'MessageReplicator'):
            if tag + ':' in line:
                for direction, count in re.findall(r'(pull|push) doc count:\s*(\d+)', line):
                    documents[tag + '_' + direction] += int(count)
        if 'TaskManager:' in line and 'record:' in line:
            batches += 1
            parts = re.findall(r'record:\s*(\d+)(?:，s:(\d+))?(?:，f:(\d+))?', line)
            successful += sum(int(s or 0) for _, s, _ in parts)
            incomplete += sum(int(f or 0) for _, _, f in parts)
            match = re.match(r'^\s*(\d+\.\d+)', line)
            last_task = float(match.group(1)) if match else last_task
    return {'pid': int(pids[0]), 'firstLogEpoch': min(times) if times else None,
            'lastLogEpoch': max(times) if times else None, 'eventsInWindow': dict(event_counts),
            'documentsReportedInWindow': dict(documents), 'taskReportsInWindow': batches,
            'successfulTasksReportedInWindow': successful, 'otherTasksReportedInWindow': incomplete,
            'lastTaskReportEpoch': last_task}


def observe(serial):
    row = {'at': datetime.datetime.now(datetime.timezone.utc).isoformat()}
    readers = ('content://com.onyx.content.database.ContentProvider/Metadata',
               'uuid:fileSyncStatus:userDataSyncStatus', ['fileSyncStatus', 'userDataSyncStatus'])
    notes = ('content://com.onyx.android.sdk.note.ContentProvider/NoteModel',
             'uniqueId:type:status:noteSyncStatus', ['type', 'status', 'noteSyncStatus'])
    for name, args in [('reader', readers), ('notes', notes)]:
        try:
            row[name] = catalog(serial, *args)
        except Exception as error:
            row[name + 'Error'] = type(error).__name__
    try:
        row['nativeLog'] = logs(serial)
    except Exception as error:
        row['nativeLogError'] = type(error).__name__
    return row


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--serial', required=True)
    parser.add_argument('--state', type=Path, required=True)
    parser.add_argument('--minutes', type=int, default=30)
    parser.add_argument('--interval', type=int, default=30)
    args = parser.parse_args()
    if not 1 <= args.minutes <= 120 or not 15 <= args.interval <= 300:
        parser.error('minutes must be 1..120; interval must be 15..300 seconds')
    os.umask(0o077)
    args.state.mkdir(parents=True, exist_ok=True, mode=0o700)
    end = time.monotonic() + args.minutes * 60
    summary = {'startedAt': datetime.datetime.now(datetime.timezone.utc).isoformat(), 'state': 'running', 'minutes': args.minutes, 'intervalSeconds': args.interval, 'samples': 0}
    while time.monotonic() < end:
        start = time.monotonic()
        row = observe(args.serial)
        with (args.state / 'samples.jsonl').open('a') as stream:
            stream.write(json.dumps(row) + '\n')
        summary.update(samples=summary['samples'] + 1, lastSample=row)
        tmp = args.state / 'status.tmp'
        tmp.write_text(json.dumps(summary, indent=2) + '\n')
        tmp.replace(args.state / 'status.json')
        print(json.dumps(row), flush=True)
        time.sleep(max(0, min(args.interval - (time.monotonic() - start), end - time.monotonic())))
    summary['state'] = 'completed'
    (args.state / 'status.json').write_text(json.dumps(summary, indent=2) + '\n')


if __name__ == '__main__':
    main()
