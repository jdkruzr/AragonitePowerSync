#!/usr/bin/env python3
"""Resumable native exports of inventoried live notebooks; never mutates ownership."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess
import time
import zipfile

ROOT=Path(__file__).resolve().parents[1]
STATE=ROOT/'artifacts/full-library'
PACKAGE='dev.aragonite.powersync'

def adb(serial,*args,**kwargs):return subprocess.run(['adb','-s',serial,*args],check=True,stdout=subprocess.PIPE,stderr=subprocess.PIPE,**kwargs).stdout

def export(serial,limit=None,warm=False,retry_failed=False):
    os.umask(0o077);folder=STATE/serial/'native-exports';folder.mkdir(parents=True,exist_ok=True,mode=0o700)
    inventory=json.loads((ROOT/'artifacts/gateway-lab/qualification'/(serial+'-library-inventory-private.json')).read_text())
    # Native NoteModel.isRemoved() uses 0 or 2. Status 1 is the live population.
    rows=[r for r in inventory['rows'] if r['type']==1 and r['status']==1]
    rows.sort(key=lambda r:(r.get('cloudNoteSize') or 0,r['uniqueId']))
    outcomes=[];processed=0
    for row in rows:
        doc=row['uniqueId'];receipt=folder/(doc+'.json')
        if receipt.exists():
            previous=json.loads(receipt.read_text())
            if not retry_failed or previous.get('status')=='archive-captured':outcomes.append(previous);continue
            attempts=folder/'failed-attempts';attempts.mkdir(mode=0o700,exist_ok=True)
            shutil.copy2(receipt,attempts/(doc+'-'+str(time.time_ns())+'.json'))
        if limit is not None and processed>=limit:break
        if shutil.disk_usage(STATE).free<2*1024**3:raise OSError('Preservation disk reserve reached')
        start=time.time();result=dict(documentId=doc,device=serial,startedAt=start,completeNativeSnapshot=False)
        try:
            adb(serial,'exec-in','run-as',PACKAGE,'sh','-c','cat > files/library-export-request.json',input=json.dumps({'documentId':doc}).encode())
            adb(serial,'shell','run-as',PACKAGE,'rm','-f','files/library-export-one.json')
            if warm:
                adb(serial,'shell','am','start','-f','0x20000000','-n',PACKAGE+'/.MainActivity','--es','lab_action','library_export_one')
            else:
                subprocess.run(['scripts/run-lab.sh',serial,'library_export_one'],cwd=ROOT,check=True,stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL)
            deadline=time.monotonic()+120;report=None
            while time.monotonic()<deadline:
                raw=adb(serial,'exec-out','run-as',PACKAGE,'cat','files/library-export-one.json')
                try:report=json.loads(raw)
                except json.JSONDecodeError:time.sleep(.3);continue
                break
            if report is None:raise TimeoutError('Native export callback not observed')
            if report.get('error'):
                result.update(status='native-export-failed',privateError=report['error'])
            else:
                path=report['archivePath'];name=Path(path).name
                if not path.startswith('/storage/emulated/0/') and not path.startswith('/sdcard/'):raise ValueError('Unexpected native export root')
                if not name.startswith('PowerSync-export-') or not name.endswith('.note'):raise ValueError('Unexpected native export name')
                temp=folder/(doc+'.note.part')
                with temp.open('wb') as stream:
                    child=subprocess.run(['adb','-s',serial,'exec-out','cat',path],stdout=stream,stderr=subprocess.PIPE)
                    if child.returncode:raise RuntimeError('Export archive transfer failed')
                    stream.flush();os.fsync(stream.fileno())
                with zipfile.ZipFile(temp) as archive:
                    if archive.testzip() is not None:raise ValueError('Native archive CRC failure')
                    if not archive.namelist():raise ValueError('Empty export')
                digest=hashlib.sha256()
                with temp.open('rb') as stream:
                    while chunk:=stream.read(1024*1024):digest.update(chunk)
                final=folder/(doc+'.note');os.replace(temp,final)
                result.update(status='archive-captured',archiveFile=final.name,bytes=final.stat().st_size,sha256=digest.hexdigest(),nativeReport=report)
                # Remove only this uniquely named generated export after verified host capture.
                adb(serial,'shell','rm','-f',path)
        except Exception as error:
            result.update(status='capture-failed',errorClass=type(error).__name__)
        result['completedAt']=time.time();receipt.write_text(json.dumps(result,indent=2)+'\n');outcomes.append(result);processed+=1
        if processed%10==0:print(json.dumps(dict(device=serial,processed=len(outcomes),total=len(rows),captured=sum(r['status']=='archive-captured' for r in outcomes),failures=sum(r['status']!='archive-captured' for r in outcomes))),flush=True)
    summary=dict(device=serial,expectedLiveNotebooks=len(rows),processed=len(outcomes),archivesCaptured=sum(r['status']=='archive-captured' for r in outcomes),exceptions=sum(r['status']!='archive-captured' for r in outcomes),completeNotebookBackup=False)
    (STATE/(serial+'-exports.json')).write_text(json.dumps(summary,indent=2)+'\n');print(json.dumps(summary),flush=True)

if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('serial',choices=['bbb70bef','dfef8c1']);parser.add_argument('--limit',type=int);parser.add_argument('--warm',action='store_true',help='Reuse the app activity and serial worker instead of restarting per notebook');parser.add_argument('--retry-failed',action='store_true',help='Retain failed receipts and retry only uncaptured notebooks');args=parser.parse_args();export(args.serial,args.limit,args.warm,args.retry_failed)
