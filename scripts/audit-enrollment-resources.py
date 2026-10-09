#!/usr/bin/env python3
"""Compare native server resource inventory to an independent BOOX backup.

Read-only. Output contains aggregate counts only. A manifest hash match verifies
recorded ingestion hashes; independent object-store reads remain a separate gate.
Historical stash files are counted separately, not silently treated as live sync.
"""
import argparse
import collections
import hashlib
import json
import zipfile
from pathlib import Path


def audit(backup, manifest):
    objects=json.loads(Path(manifest).read_text())
    stats=collections.Counter()
    by_family=collections.defaultdict(collections.Counter)
    with zipfile.ZipFile(backup) as archive:
        entries={i.filename.split('/',1)[1]:i for i in archive.infolist()
                 if not i.is_dir() and '/' in i.filename}
        stats['backupFiles']=len(entries)
        stats['backupHistoricalStashFiles']=sum('/stash/' in n for n in entries)
        matched=set()
        for obj in objects:
            parts=obj['native_key'].split('/',2)
            if len(parts)!=3 or parts[1]!='note':
                stats['otherDomainObjects']+=1
                continue
            relative=parts[2]
            bits=relative.split('/')
            family=bits[1] if len(bits)>2 else 'other'
            if family=='point' and len(bits)==3:
                # Native OSS flattens the per-page directory used in .note backups.
                relative='/'.join([bits[0],bits[1],bits[2].split('#',1)[0],bits[2]])
            by_family[family]['serverObjects']+=1
            entry=entries.get(relative)
            if entry is None:
                by_family[family]['noBackupPathMatch']+=1
                continue
            matched.add(relative)
            raw=archive.read(entry)
            exact=len(raw)==obj['bytes'] and hashlib.sha256(raw).hexdigest()==obj['sha256']
            by_family[family]['exactBackupBytes' if exact else 'differentBackupBytes']+=1
        absent=collections.Counter()
        for name in entries.keys()-matched:
            bits=name.split('/')
            family=bits[1] if len(bits)>2 else 'other'
            absent[family]+=1
    return {'counts':dict(stats),'serverNoteResources':{k:dict(v) for k,v in sorted(by_family.items())},
            'backupPathsNotYetMatched':dict(sorted(absent.items())),
            'scope':'Backup-path and recorded-ingestion-hash comparison; not independent S3 verification, complete reference closure or native restore proof.'}

if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('backup',type=Path)
    parser.add_argument('manifest',type=Path)
    parser.add_argument('--output',type=Path)
    args=parser.parse_args()
    result=json.dumps(audit(args.backup,args.manifest),indent=2)+'\n'
    if args.output:args.output.write_text(result)
    else:print(result,end='')
