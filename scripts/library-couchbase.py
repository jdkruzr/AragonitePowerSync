#!/usr/bin/env python3
"""Private, resumable preservation packs; distinct from native replication."""
import base64
import argparse
import hashlib
import importlib.util
import io
import json
import os
from pathlib import Path
import subprocess
import time
import zipfile
from urllib.error import HTTPError

ROOT=Path(__file__).resolve().parents[1]
STATE=ROOT/'artifacts/full-library'
spec=importlib.util.spec_from_file_location('gateway',ROOT/'scripts/gateway-lab.py')
g=importlib.util.module_from_spec(spec);spec.loader.exec_module(g)
credentials=json.loads((g.STATE/'credentials.json').read_text())
AUTH=(credentials['username'],credentials['password'])
DB='powersync_library'

def request(path,method='GET',body=None):
    for attempt in range(5):
        try:return g.request('/'+DB+'/'+path,method,body,AUTH)
        except HTTPError as error:
            if method!='GET' or error.code not in (429,502,503,504) or attempt==4:raise
            error.close();time.sleep(min(2**attempt,8))

def initialize():
    if DB in g.request('/_all_dbs',auth=AUTH):return
    try:
        g.run('exec','powersync-cb','/opt/couchbase/bin/couchbase-cli','bucket-create',
              '-c','127.0.0.1','-u',AUTH[0],'-p',AUTH[1],'--bucket',DB,
              '--bucket-type','couchbase','--bucket-ramsize','128','--bucket-replica','0','--wait')
    except subprocess.CalledProcessError:
        # Interrupted initialization may have left the bucket intact.
        buckets=json.loads(g.run('exec','powersync-cb','/opt/couchbase/bin/couchbase-cli',
              'bucket-list','-c','127.0.0.1','-u',AUTH[0],'-p',AUTH[1],'--output','json'))
        if not any(b['name']==DB for b in buckets):raise RuntimeError('Backup bucket creation failed') from None
    request('','PUT',{'bucket':DB,'use_views':True,'num_index_replicas':0,
                     'guest':{'disabled':True},'sync':'function(doc, oldDoc) { channel("library-backup"); }'})

def publish(kind,entries):
    buffer=io.BytesIO()
    with zipfile.ZipFile(buffer,'w',zipfile.ZIP_DEFLATED,compresslevel=1) as archive:
        for name,path in entries:archive.write(path,name)
    content=buffer.getvalue();digest=hashlib.sha256(content).hexdigest();key=kind+'-'+digest
    try:old=request(key)
    except HTTPError as error:
        if error.code!=404:raise
        old=None
    if old is None:
        request(key,'PUT',{'type':'preservationPack','kind':kind,'sha256':digest,
                'bytes':len(content),'entries':len(entries),
                '_attachments':{'content':{'content_type':'application/zip',
                                          'data':base64.b64encode(content).decode()}}})
    stored=request(key+'?attachments=true')
    recovered=base64.b64decode(stored['_attachments']['content']['data'],validate=True)
    if hashlib.sha256(recovered).hexdigest()!=digest:raise ValueError('Server pack hash mismatch')
    with zipfile.ZipFile(io.BytesIO(recovered)) as archive:
        if archive.testzip() is not None:raise ValueError('Server pack CRC mismatch')
        for name,path in entries:
            if hashlib.sha256(archive.read(name)).digest()!=hashlib.sha256(path.read_bytes()).digest():
                raise ValueError('Server entry hash mismatch')
    return dict(id=key,sha256=digest,bytes=len(content),entries=len(entries),serverReadVerified=True)

def upload():
    os.umask(0o077);initialize()
    journal=STATE/'server-packs.jsonl'
    files=sorted(p for p in (STATE/'objects').glob('*/*') if len(p.name)==64)
    batch=[];size=0;count=0
    with journal.open('a') as log:
        def flush(kind):
            nonlocal batch,size,count
            if not batch:return
            result=publish(kind,batch);log.write(json.dumps(result)+'\n');log.flush();os.fsync(log.fileno())
            count+=len(batch);print(json.dumps({'serverVerifiedFiles':count,'packBytes':result['bytes']}),flush=True)
            batch=[];size=0
        for path in files:
            if batch and (size+path.stat().st_size>8*1024**2 or len(batch)>=256):flush('objects')
            batch.append((path.name,path));size+=path.stat().st_size
        flush('objects')
        for serial in ['bbb70bef','dfef8c1']:
            paths=[STATE/(serial+'-shared-files.jsonl'),STATE/(serial+'-capture.json'),
                   ROOT/'artifacts/gateway-lab/qualification'/(serial+'-library-inventory-private.json')]
            batch=[(serial+'/'+p.name,p) for p in paths if p.exists()];flush('inventory-'+serial)
    print(json.dumps({'uniqueObjects':len(files),'serverReadVerified':True,'nativeRecoveryQualified':False}),flush=True)

def recover():
    """Reconstruct from server attachments, without opening the source CAS."""
    os.umask(0o077);destination=STATE/'server-recovery';destination.mkdir(mode=0o700,exist_ok=True)
    keys=sorted({json.loads(line)['id'] for line in (STATE/'server-packs.jsonl').read_text().splitlines()})
    objects=0;bytes_restored=0
    for key in keys:
        doc=request(key+'?attachments=true');raw=base64.b64decode(doc['_attachments']['content']['data'],validate=True)
        if hashlib.sha256(raw).hexdigest()!=doc['sha256']:raise ValueError('Recovery pack integrity failure')
        with zipfile.ZipFile(io.BytesIO(raw)) as archive:
            if archive.testzip() is not None:raise ValueError('Recovery ZIP CRC failure')
            for entry in archive.infolist():
                name=entry.filename
                if entry.is_dir() or name.startswith('/') or any(p in ('..','.') for p in name.split('/')):
                    raise ValueError('Unsafe recovery entry')
                if doc['kind']=='objects':
                    if len(name)!=64 or any(c not in '0123456789abcdef' for c in name):raise ValueError('Invalid object digest')
                    target=destination/'objects'/name[:2]/name
                else:target=destination/'inventories'/name
                target.parent.mkdir(parents=True,exist_ok=True,mode=0o700)
                content=archive.read(entry)
                if doc['kind']=='objects' and hashlib.sha256(content).hexdigest()!=name:raise ValueError('Recovery object integrity failure')
                temp=target.with_suffix('.part')
                with temp.open('wb') as output:output.write(content);output.flush();os.fsync(output.fileno())
                os.replace(temp,target);bytes_restored+=len(content)
                if doc['kind']=='objects':objects+=1
        print(json.dumps({'serverOnlyRecoveredObjects':objects}),flush=True)
    devices=[]
    for serial in ['bbb70bef','dfef8c1']:
        manifest=destination/'inventories'/serial/(serial+'-shared-files.jsonl')
        count=0;missing=0;wrong_size=0
        for line in manifest.read_text().splitlines():
            item=json.loads(line);digest=item['sha256'];target=destination/'objects'/digest[:2]/digest
            count+=1
            if not target.exists():missing+=1
            elif target.stat().st_size!=item['bytes']:wrong_size+=1
        devices.append({'device':serial,'referencedFiles':count,'missing':missing,'wrongSize':wrong_size})
    report={'source':'server-attachments-only','objects':objects,'bytes':bytes_restored,
            'devices':devices,'allSharedReferencesResolved':all(not d['missing'] and not d['wrongSize'] for d in devices),
            'nativeRecoveryQualified':False}
    (destination/'recovery.json').write_text(json.dumps(report,indent=2)+'\n');print(json.dumps(report),flush=True)

def native(recovery=False):
    """Preserve native archives with receipts; recovery never reads source archives."""
    os.umask(0o077)
    journal=STATE/'native-packs.jsonl'
    if not recovery:
        initialize()
        entries=[]
        for serial in ['bbb70bef','dfef8c1']:
            folder=STATE/serial/'native-exports'
            for receipt in sorted(folder.glob('*.json')):
                item=json.loads(receipt.read_text())
                if item.get('status')!='archive-captured':continue
                archive=folder/item['archiveFile']
                if hashlib.sha256(archive.read_bytes()).hexdigest()!=item['sha256']:
                    raise ValueError('Native source archive changed')
                entries.extend([(serial+'/'+archive.name,archive),(serial+'/'+receipt.name,receipt)])
        batch=[];size=0;count=0
        with journal.open('a') as log:
            def flush():
                nonlocal batch,size,count
                if not batch:return
                result=publish('native-archives',batch)
                log.write(json.dumps(result)+'\n');log.flush();os.fsync(log.fileno())
                count+=len(batch)
                print(json.dumps({'nativePackEntriesVerified':count}),flush=True)
                batch=[];size=0
            for name,path in entries:
                if batch and (size+path.stat().st_size>8*1024**2 or len(batch)>=128):flush()
                batch.append((name,path));size+=path.stat().st_size
            flush()
        return
    destination=STATE/'server-native-recovery'
    destination.mkdir(mode=0o700,exist_ok=True)
    keys=sorted({json.loads(line)['id'] for line in journal.read_text().splitlines()})
    for key in keys:
        doc=request(key+'?attachments=true')
        raw=base64.b64decode(doc['_attachments']['content']['data'],validate=True)
        if hashlib.sha256(raw).hexdigest()!=doc['sha256']:raise ValueError('Native pack digest mismatch')
        with zipfile.ZipFile(io.BytesIO(raw)) as pack:
            if pack.testzip() is not None:raise ValueError('Native pack CRC mismatch')
            for entry in pack.infolist():
                parts=entry.filename.split('/')
                if len(parts)!=2 or parts[0] not in ['bbb70bef','dfef8c1'] or parts[1] in ['','..','.']:
                    raise ValueError('Unsafe native recovery path')
                target=destination/entry.filename
                target.parent.mkdir(mode=0o700,exist_ok=True)
                content=pack.read(entry)
                if target.exists() and target.read_bytes()!=content:
                    raise ValueError('Conflicting native preservation entry')
                temp=target.with_suffix(target.suffix+'.part')
                with temp.open('wb') as output:output.write(content);output.flush();os.fsync(output.fileno())
                os.replace(temp,target)
    devices=[]
    for serial in ['bbb70bef','dfef8c1']:
        count=0
        for receipt in (destination/serial).glob('*.json'):
            item=json.loads(receipt.read_text());archive=destination/serial/item['archiveFile']
            if hashlib.sha256(archive.read_bytes()).hexdigest()!=item['sha256']:
                raise ValueError('Recovered native archive digest mismatch')
            with zipfile.ZipFile(archive) as note:
                if note.testzip() is not None:raise ValueError('Recovered native archive CRC mismatch')
            count+=1
        devices.append({'device':serial,'verifiedNativeArchives':count})
    report={'source':'server-attachments-only','devices':devices,'completeNotebookBackup':False}
    (STATE/'native-full-server-recovery.json').write_text(json.dumps(report,indent=2)+'\n')
    print(json.dumps(report),flush=True)

if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('--recover',action='store_true');parser.add_argument('--native',action='store_true');args=parser.parse_args()
    if args.native:native(args.recover)
    elif args.recover:recover()
    else:upload()
