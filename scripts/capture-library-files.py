#!/usr/bin/env python3
"""Read shared native note trees into a private, content-addressed preservation store.

No native metadata writes, extraction into device paths, or deletion. This is
file preservation, not proof that every referenced note asset is present.
"""
import argparse
import hashlib
import json
import os
from pathlib import Path, PurePosixPath
import shutil
import subprocess
import tarfile
import tempfile
import time
from concurrent.futures import ThreadPoolExecutor

ROOT=Path(__file__).resolve().parents[1]
DEFAULT=ROOT/'artifacts/full-library'

def safe_member(name):
    path=PurePosixPath(name)
    if path.is_absolute() or '..' in path.parts or '\\' in name or '\x00' in name or '\n' in name or not path.parts or path.parts[0] not in ('document','point'):
        raise ValueError('Unsafe shared note path')
    return str(path)

def resume(serial,state):
    """Complete a retained partial stream using bounded, independently timed reads."""
    os.umask(0o077);journal=state/(serial+'-shared-files.jsonl');objects=state/'objects'
    prior={r['path']:r for r in (json.loads(l) for l in journal.read_text().splitlines())}
    raw=subprocess.check_output(['adb','-s',serial,'shell','find','/sdcard/.ksync/document','/sdcard/.ksync/point','-type','f'],timeout=60,text=True)
    expected={safe_member(p.removeprefix('/sdcard/.ksync/')) for p in raw.splitlines()}
    missing=sorted(expected-prior.keys());added=0;errors=[];start=time.time()
    def read(name):
        path='/sdcard/.ksync/'+name
        info=subprocess.check_output(['adb','-s',serial,'exec-out','stat','-c','%s %Y',path],timeout=15,text=True).split();size,mtime=map(int,info)
        if size>536870912 or shutil.disk_usage(state).free<2*1024**3+size:raise OSError('Capture file/reserve bound reached')
        body=subprocess.check_output(['adb','-s',serial,'exec-out','cat',path],timeout=30)
        if len(body)!=size:raise ValueError('Changed or truncated shared file')
        sha=hashlib.sha256(body).hexdigest();dest=objects/sha[:2]/sha;dest.parent.mkdir(exist_ok=True,mode=0o700)
        new=0
        if not dest.exists():
            fd,tmp=tempfile.mkstemp(prefix='.resume-',dir=dest.parent)
            try:
                with os.fdopen(fd,'wb') as stream:stream.write(body);stream.flush();os.fsync(stream.fileno())
                os.replace(tmp,dest);new=size
            finally:
                if os.path.exists(tmp):os.unlink(tmp)
        return dict(path=name,bytes=size,sha256=sha,mtime=mtime,capturedBy='bounded-resume'),new
    with journal.open('a') as out,ThreadPoolExecutor(2) as pool:
        # Keep only a small wave in flight, so failures do not leave a large queue.
        for offset in range(0,len(missing),32):
            wave=[(name,pool.submit(read,name)) for name in missing[offset:offset+32]]
            for name,future in wave:
                try:row,new=future.result();out.write(json.dumps(row)+'\n');prior[name]=row;added+=new
                except Exception as error:errors.append(dict(path=name,errorClass=type(error).__name__))
            out.flush();os.fsync(out.fileno());print(json.dumps(dict(device=serial,resumed=min(offset+32,len(missing)),remainingTotal=len(missing),errors=len(errors))),flush=True)
    report=dict(device=serial,startedAt=start,completedAt=time.time(),files=len(prior),bytes=sum(r['bytes'] for r in prior.values()),newBytes=added,resumed=True,expectedFiles=len(expected),errors=errors,sharedTreeCaptureComplete=not errors and expected<=prior.keys(),completeNotebookBackup=False)
    (state/(serial+'-capture.json')).write_text(json.dumps(report,indent=2)+'\n');print(json.dumps({k:v for k,v in report.items() if k!='errors'}),flush=True)

def capture(serial,state):
    os.umask(0o077);state.mkdir(parents=True,exist_ok=True,mode=0o700)
    objects=state/'objects';objects.mkdir(exist_ok=True,mode=0o700)
    journal=state/(serial+'-shared-files.jsonl')
    if journal.exists():raise ValueError('Existing snapshot journal; use a fresh capture directory')
    inventory=ROOT/'artifacts/gateway-lab/qualification'/(serial+'-library-inventory-private.json')
    inventory_hash=hashlib.sha256(inventory.read_bytes()).hexdigest()
    started=time.time();count=total=added=0
    with (state/(serial+'-tar-private.log')).open('wb') as err,journal.open('w') as out:
        proc=subprocess.Popen(['adb','-s',serial,'exec-out','tar','-C','/sdcard/.ksync','-cf','-','document','point'],stdout=subprocess.PIPE,stderr=err)
        try:
            with tarfile.open(fileobj=proc.stdout,mode='r|') as archive:
                for entry in archive:
                    name=safe_member(entry.name)
                    if entry.isdir():continue
                    if not entry.isfile() or entry.size>536870912:raise ValueError('Unsupported member type/size')
                    if shutil.disk_usage(state).free<2*1024**3+entry.size:raise OSError('Preservation disk reserve reached')
                    digest=hashlib.sha256();size=0
                    fd,tmp=tempfile.mkstemp(prefix='.capture-',dir=objects)
                    try:
                        with os.fdopen(fd,'wb') as target,archive.extractfile(entry) as source:
                            while chunk:=source.read(1024*1024):digest.update(chunk);target.write(chunk);size+=len(chunk)
                            target.flush();os.fsync(target.fileno())
                        if size!=entry.size:raise ValueError('Truncated shared file')
                        sha=digest.hexdigest();dest=objects/sha[:2]/sha;dest.parent.mkdir(exist_ok=True,mode=0o700)
                        if dest.exists():
                            if dest.stat().st_size!=size:raise ValueError('CAS size mismatch')
                            os.unlink(tmp)
                        else:os.replace(tmp,dest);added+=size
                    finally:
                        if os.path.exists(tmp):os.unlink(tmp)
                    out.write(json.dumps(dict(path=name,bytes=size,sha256=sha,mtime=entry.mtime))+'\n');out.flush()
                    count+=1;total+=size
                    if count%500==0:os.fsync(out.fileno());print(json.dumps(dict(device=serial,files=count,bytes=total,newBytes=added)),flush=True)
            code=proc.wait(timeout=30)
            if code:raise RuntimeError('Device tar capture failed; retain partial evidence')
            os.fsync(out.fileno())
        finally:
            if proc.poll() is None:proc.kill();proc.wait()
            proc.stdout.close()
    report=dict(device=serial,startedAt=started,completedAt=time.time(),files=count,bytes=total,newBytes=added,inventorySha256=inventory_hash,sharedTreeCaptureComplete=True,completeNotebookBackup=False)
    (state/(serial+'-capture.json')).write_text(json.dumps(report,indent=2)+'\n');print(json.dumps(report),flush=True)

if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('serial',choices=['bbb70bef','dfef8c1']);parser.add_argument('--state',type=Path,default=DEFAULT);parser.add_argument('--resume',action='store_true');args=parser.parse_args();(resume if args.resume else capture)(args.serial,args.state)
