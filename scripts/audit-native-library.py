#!/usr/bin/env python3
import io,json,zipfile,collections,os,argparse
from pathlib import Path
os.umask(0o077)
root=Path(__file__).resolve().parents[1]/'artifacts/full-library'
parser=argparse.ArgumentParser(description='Audit nested shape CRCs and live point references without printing note identities')
parser.add_argument('--server',action='store_true',help='Inspect server-only reconstructed archives');args=parser.parse_args()
def fields(raw):
 i=0;out=[]
 def varint():
  nonlocal i
  n=0;s=0
  while True:
   if i>=len(raw) or s>=70:raise ValueError('Invalid protobuf varint')
   b=raw[i];i+=1;n|=(b&127)<<s
   if b<128:return n
   s+=7
 while i<len(raw):
  tag=varint();wire=tag&7;field=tag>>3
  if wire==0:value=varint()
  elif wire in (1,5):
   size=8 if wire==1 else 4;value=raw[i:i+size];i+=size
  elif wire==2:
   size=varint();value=raw[i:i+size];i+=size
  else:raise ValueError('Unsupported protobuf wire')
  if i>len(raw):raise ValueError('Truncated protobuf')
  out.append((field,value))
 return out
reports=[]
for serial in ['bbb70bef','dfef8c1']:
 counts=collections.Counter();issues=[]
 folder=root/'server-native-recovery'/serial if args.server else root/serial/'native-exports'
 for archive in sorted(folder.glob('*.note')):
  counts['archives']+=1
  with zipfile.ZipFile(archive) as note:
   names=set(note.namelist());roots={n.split('/')[0] for n in names if n}
   if len(roots)!=1:
    counts['invalidArchiveRoots']+=1;issues.append({'archive':archive.name,'error':'Expected one archive namespace'});continue
   doc=next(iter(roots))
   if doc+'/note/pb/note_info' not in names:counts['missingNoteMetadata']+=1
   for name in names:
    if not name.startswith(doc+'/shape/') or not name.endswith('.zip'):continue
    counts['shapeRecords']+=1
    try:
     with zipfile.ZipFile(io.BytesIO(note.read(name))) as shapezip:
      if shapezip.testzip() is not None:raise ValueError('Nested CRC')
      for entry in shapezip.infolist():
       for field,shape in fields(shapezip.read(entry)):
        if field!=1 or not isinstance(shape,bytes):continue
        values=dict(fields(shape));counts['shapes']+=1
        if values.get(12)==19 and values.get(15,0)==0:
         counts['liveImageShapes']+=1
         try:
          reference=json.loads(values[14].decode());relative=reference['relativePath']
          asset=doc+'/resource/data'+('/' if not relative.startswith('/') else '')+relative
          if asset not in names:
           counts['missingImageAssets']+=1;issues.append({'archive':archive.name,'shapeRecord':name,'missing':asset})
         except (ValueError,KeyError,UnicodeError):
          counts['unparsedImageReferences']+=1;issues.append({'archive':archive.name,'shapeRecord':name,'error':'Unparsed image reference'})
        revision=values.get(16)
        if not revision or values.get(15,0)!=0:continue
        page=name.rsplit('/',1)[1].split('#')[0]
        point=doc+'/point/'+page+'/'+page+'#'+revision.decode()+'#points'
        counts['pointReferences']+=1
        if point not in names:
         counts['missingPointReferences']+=1;issues.append({'archive':archive.name,'shapeRecord':name,'missing':point})
    except Exception as error:
     counts['unparsedShapeRecords']+=1;issues.append({'archive':archive.name,'shapeRecord':name,'errorClass':type(error).__name__})
 report={'device':serial,'counts':dict(counts),'issues':issues,'scope':'live point/image references and nested CRC; not full page/resource/template closure'}
 (root/(serial+('-server' if args.server else '')+'-reference-audit-private.json')).write_text(json.dumps(report,indent=2))
 reports.append({'device':serial,'counts':dict(counts),'scope':report['scope']})
print(json.dumps(reports))
