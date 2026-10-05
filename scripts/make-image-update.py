#!/usr/bin/env python3
"""Replace the synthetic image in an exported PowerSync ink fixture.

Retains the native image shape ID, geometry and unknown wire fields. Packages
new immutable image, resource metadata, point and shape revisions for the lab.
"""
import argparse, io, json, pathlib, struct, time, uuid, zipfile, zlib
from native_wire import fields, encode, replace

def png():
    def chunk(kind, body):
        return struct.pack('>I',len(body))+kind+body+struct.pack('>I',zlib.crc32(kind+body)&0xffffffff)
    rows = bytearray()
    for y in range(128):
        rows.append(0)
        for x in range(256):
            black = x<6 or x>=250 or y<6 or y>=122 or 122<=x<134 or 58<=y<70
            rows.extend(bytes([0 if black else 255])*3)
    return b'\x89PNG\r\n\x1a\n'+chunk(b'IHDR',struct.pack('>IIBBBBB',256,128,8,2,0,0,0))+chunk(b'IDAT',zlib.compress(bytes(rows)))+chunk(b'IEND',b'')

def make(metadata, source, output):
    note=metadata['note'];doc=note['uniqueId']
    if not note['title'].startswith('PowerSync ink ') or note['activeScene']!=0:
        raise ValueError('Not a PowerSync ink fixture')
    matches=[]
    with zipfile.ZipFile(source) as archive:
        for name in archive.namelist():
            if name.startswith(doc+'/shape/') and name.endswith('.zip'):
                with zipfile.ZipFile(io.BytesIO(archive.read(name))) as nested:
                    for number,wire,body in fields(nested.read(nested.namelist()[0])):
                        if number!=1 or wire!=2: raise ValueError('Unexpected envelope')
                        record=fields(body);values={n:v for n,t,v in record}
                        if values.get(12)==19: matches.append((name,record,values))
        if len(matches)!=1: raise ValueError('Expected exactly one synthetic fixture image')
        name,shape,values=matches[0]
        page=name.rsplit('/',1)[1].split('#')[0]
        if page not in metadata['pages']['pageIds']: raise ValueError('Unknown fixture page')
        old_resource=json.loads(values[14]);old_revision=values[16].decode()
        if old_resource['documentId']!=doc: raise ValueError('Foreign image resource')
        point_name=f'{doc}/point/{page}/{page}#{old_revision}#points'
        points=bytearray(archive.read(point_name))
        index=struct.unpack_from('>i',points,len(points)-4)[0]
        if index<76 or (len(points)-index-4)%44: raise ValueError('Invalid point index')
        spans=[]
        for pos in range(index,len(points)-4,44):
            if bytes(points[pos:pos+36]).decode().strip()==values[1].decode():
                offset,length=struct.unpack_from('>ii',points,pos+36)
                if offset<76 or length<4 or offset+length>index: raise ValueError('Invalid image point span')
                spans.append(bytes(points[offset:offset+length]))
        if len(spans)!=1: raise ValueError('Missing or ambiguous image points')
        body=spans[0]
        points=bytearray(points[:76]+body+values[1].ljust(36,b' ')+struct.pack('>iii',76,len(body),76+len(body)))
        candidates=[]
        for resource_name in archive.namelist():
            if resource_name.startswith(doc+'/resource/pb/') and not resource_name.endswith('/'):
                for n,t,body in fields(archive.read(resource_name)):
                    resource=fields(body);rv={n:v for n,t,v in resource}
                    if rv.get(1,b'').decode()==old_resource['uniqueId']: candidates.append(resource)
        if len(candidates)!=1: raise ValueError('Expected one matching resource record')
    stamp=max(int(time.time()*1000),values[3]+1)
    shape_revision,point_revision,resource_revision=str(uuid.uuid4()),str(uuid.uuid4()),str(uuid.uuid4())
    resource_id=uuid.uuid4().hex;relative='/'+resource_id+'.png'
    replacement=dict(old_resource,uniqueId=resource_id,relativePath=relative,revisionId=resource_revision,status=0)
    replacement.pop('localPath',None)
    new_shape=replace(shape,{3:(0,stamp),14:(2,json.dumps(replacement,separators=(',',':')).encode()),
                             16:(2,point_revision.encode()),18:(2,shape_revision.encode())})
    new_resource=replace(candidates[0],{1:(2,resource_id.encode()),2:(2,doc.encode()),3:(0,stamp),4:(0,stamp),
                                       8:(2,relative.encode()),11:(2,resource_revision.encode()),12:(2,b''),13:(0,0)})
    points[40:76]=point_revision.encode().ljust(36,b' ')
    base=f'{page}#{shape_revision}#{stamp}'
    nested=io.BytesIO()
    with zipfile.ZipFile(nested,'w',zipfile.ZIP_DEFLATED) as z: z.writestr(base,encode([(1,2,encode(new_shape))]))
    with zipfile.ZipFile(output,'w',zipfile.ZIP_DEFLATED) as z:
        z.writestr(f'document/{doc}/resource/data/{resource_id}.png',png())
        z.writestr(f'document/{doc}/resource/pb/{resource_revision}#{stamp}',encode([(1,2,encode(new_resource))]))
        z.writestr(f'point/{doc}/{page}/{page}#{point_revision}#points',points)
        z.writestr(f'document/{doc}/shape/{base}.zip',nested.getvalue())
    result=dict(documentId=doc,pageId=page,shapeId=values[1].decode(),resourceId=resource_id,updatedAt=stamp,synthetic=True)
    pathlib.Path(str(output)+'.json').write_text(json.dumps(result,indent=2)+'\n')
    return result

if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('metadata',type=pathlib.Path);parser.add_argument('native_note',type=pathlib.Path);parser.add_argument('output',type=pathlib.Path)
    a=parser.parse_args();print(json.dumps(make(json.loads(a.metadata.read_text()),a.native_note,a.output),indent=2))
