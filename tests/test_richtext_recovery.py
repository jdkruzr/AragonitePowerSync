import importlib.util,io,json,pathlib,struct,sys,tempfile,unittest,uuid,zipfile
sys.path.insert(0,str(pathlib.Path(__file__).resolve().parents[1]/'scripts'))
from native_wire import fields,encode
spec=importlib.util.spec_from_file_location('recovery',pathlib.Path(__file__).resolve().parents[1]/'scripts/make-richtext-recovery.py');r=importlib.util.module_from_spec(spec);spec.loader.exec_module(r)
class RecoveryTest(unittest.TestCase):
 def fixture(self,d):
  page=str(uuid.uuid4());pointRev=str(uuid.uuid4());resourceId=str(uuid.uuid4());resourceRev=str(uuid.uuid4());shapeRev=str(uuid.uuid4())
  body=b'{"docVersion":2,"content":{"type":"doc","content":[{"type":"paragraph","content":[{"type":"text","text":"old"}]}]}}'
  ref=dict(documentId=r.DOC,uniqueId=resourceId,relativePath='/'+resourceId+'.html',revisionId=resourceRev,type=3,status=0)
  shape=encode([(1,2,r.SHAPE.encode()),(2,0,10),(3,0,20),(12,0,29),(14,2,json.dumps(ref).encode()),(16,2,pointRev.encode()),(18,2,shapeRev.encode()),(99,2,b'preserve-unknown')]);nested=io.BytesIO()
  with zipfile.ZipFile(nested,'w') as z:z.writestr('shape',encode([(1,2,shape)]))
  points=struct.pack('>hh',0,1)+page.encode().ljust(36,b' ')+pointRev.encode().ljust(36,b' ')+struct.pack('>i8f',2,0,0,0,0,100,200,0,0)+r.SHAPE.encode().ljust(36,b' ')+struct.pack('>iii',76,36,112)
  resource=encode([(1,2,resourceId.encode()),(2,2,r.DOC.encode()),(3,0,10),(4,0,20),(7,0,3),(8,2,ref['relativePath'].encode()),(11,2,resourceRev.encode()),(77,2,b'unknown-resource')]);src=d/'source.note'
  with zipfile.ZipFile(src,'w') as z:
   z.writestr(f'{r.DOC}/shape/{page}#{shapeRev}#20.zip',nested.getvalue());z.writestr(f'{r.DOC}/point/{page}/{page}#{pointRev}#points',points);z.writestr(f'{r.DOC}/resource/pb/{resourceRev}#20',encode([(1,2,resource)]));z.writestr(f'{r.DOC}/resource/data/'+resourceId+'.html',body)
  retained=d/'retained.html';retained.write_bytes(body.replace(b'old',b'recovered'));return src,retained,points
 def test_immutable_same_shape_recovery_keeps_exact_body_and_unknown_fields(self):
  with tempfile.TemporaryDirectory() as t:
   d=pathlib.Path(t);src,retained,points=self.fixture(d);original=src.read_bytes();out=d/'payload.zip';receipt=r.make(src,retained,out)
   self.assertEqual(src.read_bytes(),original);self.assertEqual(receipt['shapeId'],r.SHAPE);self.assertNotEqual(receipt['newResourceId'],receipt['expectedResourceId'])
   with zipfile.ZipFile(out) as z:
    self.assertEqual(len(z.namelist()),4);html=next(n for n in z.namelist() if n.endswith('.html'));self.assertEqual(z.read(html),retained.read_bytes());point=z.read(next(n for n in z.namelist() if n.startswith('point/')));self.assertEqual(point[:40],points[:40]);self.assertEqual(point[76:],points[76:]);nested=zipfile.ZipFile(io.BytesIO(z.read(next(n for n in z.namelist() if '/shape/' in n))));shape={n:v for n,t,v in fields(fields(nested.read(nested.namelist()[0]))[0][2])};self.assertEqual(shape[1],r.SHAPE.encode());self.assertEqual(shape[99],b'preserve-unknown');res={n:v for n,t,v in fields(fields(z.read(next(n for n in z.namelist() if '/resource/pb/' in n)))[0][2])};self.assertEqual(res[77],b'unknown-resource');self.assertEqual(json.loads(shape[14])['uniqueId'].encode(),res[1])
   with self.assertRaises(ValueError):r.make(src,retained,out)
 def test_rejects_attachment_graph_and_ambiguous_shape(self):
  with tempfile.TemporaryDirectory() as t:
   d=pathlib.Path(t);src,retained,_=self.fixture(d);retained.write_text('{"docVersion":2,"content":{"type":"doc","content":[{"type":"image","attrs":{"src":"foreign"}}]}}')
   with self.assertRaises(ValueError):r.make(src,retained,d/'bad.zip')
   with zipfile.ZipFile(src,'a') as z:z.writestr(r.DOC+'/shape/extra.zip',z.read(next(n for n in z.namelist() if '/shape/' in n)))
   with self.assertRaises(ValueError):r.inspect(src)
 def test_missing_reference_refuses_before_output(self):
  with tempfile.TemporaryDirectory() as t:
   d=pathlib.Path(t);src,retained,_=self.fixture(d)
   with zipfile.ZipFile(src) as z:entries={n:z.read(n) for n in z.namelist()}
   name=next(n for n in entries if '/shape/' in n);nested=zipfile.ZipFile(io.BytesIO(entries[name]));row=fields(fields(nested.read(nested.namelist()[0]))[0][2]);blob=io.BytesIO()
   with zipfile.ZipFile(blob,'w') as z:z.writestr('shape',encode([(1,2,encode([f for f in row if f[0]!=14]))]))
   entries[name]=blob.getvalue()
   with zipfile.ZipFile(src,'w') as z:
    for n,b in entries.items():z.writestr(n,b)
   with self.assertRaisesRegex(ValueError,'reference is incomplete'):r.make(src,retained,d/'refused.zip')
   self.assertFalse((d/'refused.zip').exists())
if __name__=='__main__':unittest.main()
