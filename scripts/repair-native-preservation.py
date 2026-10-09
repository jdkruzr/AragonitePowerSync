#!/usr/bin/env python3
"""Derive recovery archives using only server-reconstructed archives and objects.

Original exports remain immutable. Receipts identify every inserted image and its
server-preserved content digest; this is not native recovery qualification.
"""
import hashlib
import json
import os
from pathlib import Path
import zipfile

ROOT=Path(__file__).resolve().parents[1]/'artifacts/full-library'

def repair():
    os.umask(0o077)
    total=0
    for serial in ['bbb70bef','dfef8c1']:
        groups={}
        manifest=ROOT/(serial+'-missing-image-reconciliation-private.json')
        for item in json.loads(manifest.read_text()):
            if not item['uniqueServerPreservedMatch']:
                raise ValueError('Missing image does not have one preserved candidate')
            groups.setdefault(item['archive'],[]).append(item)
        folder=ROOT/'server-native-repaired'/serial
        folder.mkdir(mode=0o700,parents=True,exist_ok=True)
        for name,items in groups.items():
            source=ROOT/'server-native-recovery'/serial/name
            if not source.is_file():raise ValueError('Required native archive not recovered from server')
            target=folder/name;temp=target.with_suffix('.part');insertions=[]
            with zipfile.ZipFile(source) as native,zipfile.ZipFile(temp,'w',zipfile.ZIP_DEFLATED) as derived:
                if native.testzip() is not None:raise ValueError('Source native CRC failure')
                names=set(native.namelist())
                for entry in native.infolist():derived.writestr(entry,native.read(entry))
                for item in items:
                    path=item['reference'];parts=path.split('/')
                    if path in names or path.startswith('/') or '..' in parts or '\\' in path:
                        raise ValueError('Unsafe or conflicting recovery insertion')
                    record=item['capturedMatches'][0];digest=record['sha256']
                    obj=ROOT/'server-recovery'/'objects'/digest[:2]/digest
                    body=obj.read_bytes()
                    if len(body)!=record['bytes'] or hashlib.sha256(body).hexdigest()!=digest:
                        raise ValueError('Server-reconstructed object mismatch')
                    derived.writestr(path,body);names.add(path)
                    insertions.append({'path':path,'sha256':digest,'bytes':len(body),'capturedPath':record['path']})
            with zipfile.ZipFile(temp) as derived:
                if derived.testzip() is not None:raise ValueError('Derived recovery CRC failure')
            os.replace(temp,target)
            receipt={'source':'server-reconstructed-native-and-CAS-only','originalArchiveSha256':hashlib.sha256(source.read_bytes()).hexdigest(),
                     'derivedSha256':hashlib.sha256(target.read_bytes()).hexdigest(),'insertions':insertions,
                     'nativeRecoveryQualified':False}
            target.with_suffix('.json').write_text(json.dumps(receipt,indent=2)+'\n')
            total+=1
    print(json.dumps({'derivedRecoveryArchives':total,'nativeRecoveryQualified':False}))

if __name__=='__main__':repair()
