#!/usr/bin/env python3
"""Explicit lab recovery: same rich-text shape, fresh immutable resource/revisions.

The caller supplies a current native export and exact retained JSON bytes. Never
select history by timestamp or substring, and never replace a historical asset.
"""
import argparse, hashlib, io, json, os, pathlib, struct, time, uuid, zipfile
from native_wire import fields, encode, replace
DOC = 'f7f635344f624650aa300f84d114a1d3'
SHAPE = 'c57e650a-6226-4ab5-a0a9-5e6830f40c84'

def inspect(source):
    if pathlib.Path(source).stat().st_size>268435456: raise ValueError('Source exceeds lab bound')
    with zipfile.ZipFile(source) as archive:
        if len(archive.infolist())>10000 or sum(e.file_size for e in archive.infolist())>536870912: raise ValueError('Expanded source exceeds lab bound')
        if archive.testzip() or len(archive.namelist()) != len(set(archive.namelist())):
            raise ValueError('Invalid or duplicate archive entries')
        matches=[]
        for name in archive.namelist():
            if not name.startswith(DOC+'/shape/') or not name.endswith('.zip'): continue
            with zipfile.ZipFile(io.BytesIO(archive.read(name))) as nested:
                if len(nested.namelist()) != 1: raise ValueError('Ambiguous shape envelope')
                for n,t,b in fields(nested.read(nested.namelist()[0])):
                    if n!=1 or t!=2: raise ValueError('Unexpected shape envelope')
                    record=fields(b);row={n:v for n,t,v in record}
                    if row.get(12)==29: matches.append((name,record,row))
        if len(matches)!=1: raise ValueError('Require one rich-text shape')
        name,record,row=matches[0]
        if not all(k in row for k in [1,3,14,16,18]):raise ValueError('Current rich-text reference is incomplete')
        ref=json.loads(row[14]);page=name.rsplit('/',1)[1].split('#')[0]
        if str(uuid.UUID(page))!=page:raise ValueError('Invalid text page identity')
        if row[1].decode()!=SHAPE or ref.get('documentId')!=DOC or ref.get('type')!=3 or ref.get('status',0)!=0:
            raise ValueError('Not the owned live text fixture')
        relative=ref['relativePath'];uid=ref['uniqueId']
        if relative!='/'+uid+'.html' or str(uuid.UUID(uid))!=uid: raise ValueError('Unsafe resource path')
        body=archive.read(DOC+'/resource/data'+relative)
        candidates=[]
        for rn in archive.namelist():
            if not rn.startswith(DOC+'/resource/pb/') or rn.endswith('/'): continue
            for n,t,b in fields(archive.read(rn)):
                if n!=1 or t!=2: raise ValueError('Unexpected resource envelope')
                resource=fields(b);rv={n:v for n,t,v in resource}
                if rv.get(1,b'').decode()==uid:candidates.append(resource)
        if len(candidates)!=1: raise ValueError('Missing or ambiguous resource metadata')
        point=archive.read(f'{DOC}/point/{page}/{page}#{row[16].decode()}#points')
        index=struct.unpack_from('>i',point,len(point)-4)[0]
        if len(point)<128 or index!=len(point)-48 or point[index:index+36].decode().strip()!=SHAPE:
            raise ValueError('Invalid single-shape points')
        offset,length=struct.unpack_from('>ii',point,index+36)
        if offset!=76 or length!=index-76: raise ValueError('Invalid point span')
        return dict(pageId=page,shapeId=SHAPE,resourceId=uid,activeSha256=hashlib.sha256(body).hexdigest(),
                    shape=record,resource=candidates[0],points=point,ref=ref,activeBody=body)

def make(source, retained, output):
    source,retained,output=map(pathlib.Path,(source,retained,output))
    if output.exists() or pathlib.Path(str(output)+'.json').exists(): raise ValueError('Fresh output required')
    if retained.stat().st_size>1048576:raise ValueError('Retained text exceeds lab bound')
    raw=retained.read_bytes();value=json.loads(raw)
    if len(raw)>1048576 or value.get('docVersion')!=2 or value.get('content',{}).get('type')!='doc':
        raise ValueError('Require bounded native V2 rich-text document')
    # Attachments require a separately closed asset graph; this path is text-only.
    def check(node):
        if isinstance(node,dict):
            if node.get('type') in ('image','audio','video','file','attachment'):raise ValueError('Unsupported rich-text attachment')
            for v in node.values():check(v)
        elif isinstance(node,list):
            for v in node:check(v)
    check(value['content'])
    base=inspect(source);shape=base['shape'];sv={n:v for n,t,v in shape};resource=base['resource'];rv={n:v for n,t,v in resource}
    stamp=max(int(time.time()*1000),sv.get(3,0)+1,rv.get(4,0)+1)
    uid,resourceRev,shapeRev,pointRev=[str(uuid.uuid4()) for _ in range(4)]
    relative='/'+uid+'.html';ref=dict(base['ref'],uniqueId=uid,relativePath=relative,revisionId=resourceRev,status=0,localPath='')
    ref['ossUrl']='/note/'+DOC+'/resource/data/'+uid+'.html'
    newResource=replace(resource,{1:(2,uid.encode()),2:(2,DOC.encode()),3:(0,stamp),4:(0,stamp),8:(2,relative.encode()),9:(2,ref['ossUrl'].encode()),11:(2,resourceRev.encode()),12:(2,b''),13:(0,0)})
    newShape=replace(shape,{3:(0,stamp),14:(2,json.dumps(ref,separators=(',',':')).encode()),16:(2,pointRev.encode()),18:(2,shapeRev.encode())})
    point=bytearray(base['points']);point[40:76]=pointRev.encode().ljust(36,b' ')
    filename=f"{base['pageId']}#{shapeRev}#{stamp}";nested=io.BytesIO()
    with zipfile.ZipFile(nested,'w',zipfile.ZIP_DEFLATED) as z:z.writestr(filename,encode([(1,2,encode(newShape))]))
    output.parent.mkdir(parents=True,exist_ok=True)
    with output.open('xb') as stream,zipfile.ZipFile(stream,'w',zipfile.ZIP_DEFLATED) as z:
        os.fchmod(stream.fileno(),0o600)
        z.writestr(f'document/{DOC}/resource/data/{uid}.html',raw)
        z.writestr(f'document/{DOC}/resource/pb/{resourceRev}#{stamp}',encode([(1,2,encode(newResource))]))
        z.writestr(f'point/{DOC}/{base["pageId"]}/{base["pageId"]}#{pointRev}#points',point)
        z.writestr(f'document/{DOC}/shape/{filename}.zip',nested.getvalue())
    receipt={k:base[k] for k in ['pageId','shapeId']};receipt.update(documentId=DOC,expectedResourceId=base['resourceId'],expectedActiveSha256=base['activeSha256'],retainedSha256=hashlib.sha256(raw).hexdigest(),payloadSha256=hashlib.sha256(output.read_bytes()).hexdigest(),newResourceId=uid,updatedAt=stamp,sourceArchiveSha256=hashlib.sha256(source.read_bytes()).hexdigest(),explicitRecovery=True,textOnly=True)
    sidecar=pathlib.Path(str(output)+'.json')
    with sidecar.open('x') as stream:
        os.fchmod(stream.fileno(),0o600);stream.write(json.dumps(receipt,indent=2)+'\n')
    return receipt
if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('source',type=pathlib.Path);p.add_argument('retained',type=pathlib.Path);p.add_argument('output',type=pathlib.Path);a=p.parse_args();print(json.dumps(make(a.source,a.retained,a.output)))
