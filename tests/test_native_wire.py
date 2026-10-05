import importlib.util, io, json, pathlib, struct, sys, tempfile, unittest, uuid, zipfile, zlib
ROOT=pathlib.Path(__file__).resolve().parents[1]
sys.path.insert(0,str(ROOT/'scripts'))
from native_wire import fields, encode, replace
spec=importlib.util.spec_from_file_location('image_update',ROOT/'scripts/make-image-update.py')
image_update=importlib.util.module_from_spec(spec);spec.loader.exec_module(image_update)

class NativeWireTests(unittest.TestCase):
    def test_unknown_fields_and_repeated_envelope_preserved(self):
        original=[(1,2,b'first'),(1,2,b'second'),(100,5,b'1234'),(101,0,(1<<64)-1)]
        self.assertEqual(fields(encode(original)),original)
        scalars=[(1,2,b'identity'),(100,5,b'1234')]
        self.assertEqual(replace(scalars,{1:(2,b'new')}),[(1,2,b'new'),(100,5,b'1234')])
        with self.assertRaises(ValueError):replace(original,{1:(2,b'new')})

    def test_truncation_and_varint_bounds(self):
        for data in [b'\x0a\x10abc',b'\x08'+b'\xff'*10,b'\x00\x01',b'\x0f']:
            with self.subTest(data=data),self.assertRaises(ValueError):fields(data)

    def test_png_crc(self):
        image=image_update.png();self.assertEqual(image[:8],b'\x89PNG\r\n\x1a\n')
        pos=8;types=[]
        while pos<len(image):
            length=struct.unpack_from('>I',image,pos)[0];kind=image[pos+4:pos+8];body=image[pos+8:pos+8+length]
            self.assertEqual(struct.unpack_from('>I',image,pos+8+length)[0],zlib.crc32(kind+body)&0xffffffff)
            types.append(kind);pos+=12+length
        self.assertEqual(types,[b'IHDR',b'IDAT',b'IEND'])

    def test_image_update_extracts_own_points_from_shared_export(self):
        doc,page,shape_id,other_id,revision,resource_id=[str(uuid.uuid4()) for _ in range(6)]
        meta={'note':{'title':'PowerSync ink synthetic','activeScene':0,'uniqueId':doc},'pages':{'documentId':doc,'pageIds':[page]}}
        reference={'documentId':doc,'uniqueId':resource_id,'relativePath':'/old.png','revisionId':str(uuid.uuid4()),'type':0,'status':0}
        shape=[(1,2,shape_id.encode()),(3,0,1),(7,2,b'{"left":100}'),(8,2,b'{"values":[1]}'),(12,0,19),
               (14,2,json.dumps(reference).encode()),(16,2,revision.encode()),(100,2,b'unknown-shape-data')]
        other_body=struct.pack('>hhffhhi',0,0,1,2,3,4,5)
        image_body=struct.pack('>hh',0,0)+struct.pack('>ffhhi',100,200,0,0,1)+struct.pack('>ffhhi',300,400,0,0,2)
        index=76+len(other_body)+len(image_body)
        points=struct.pack('>hh',0,1)+page.encode()+revision.encode()+other_body+image_body
        points+=other_id.encode()+struct.pack('>ii',76,len(other_body))
        points+=shape_id.encode()+struct.pack('>ii',76+len(other_body),len(image_body))+struct.pack('>i',index)
        resource=[(1,2,resource_id.encode()),(2,2,doc.encode()),(7,0,0),(8,2,b'/old.png'),(100,2,b'unknown-resource-data')]
        with tempfile.TemporaryDirectory() as temporary:
            folder=pathlib.Path(temporary);source=folder/'source.note';output=folder/'incoming.zip'
            nested=io.BytesIO()
            with zipfile.ZipFile(nested,'w') as z:z.writestr('pb',encode([(1,2,encode(shape))]))
            with zipfile.ZipFile(source,'w') as z:
                z.writestr(f'{doc}/shape/{page}#{uuid.uuid4()}#1.zip',nested.getvalue())
                z.writestr(f'{doc}/point/{page}/{page}#{revision}#points',points)
                z.writestr(f'{doc}/resource/pb/{uuid.uuid4()}#1',encode([(1,2,encode(resource))]))
            state=image_update.make(meta,source,output)
            self.assertEqual(state['shapeId'],shape_id)
            with zipfile.ZipFile(output) as z:
                self.assertEqual(len(z.namelist()),4)
                point_name=next(n for n in z.namelist() if n.startswith('point/'))
                updated=z.read(point_name);new_index=struct.unpack_from('>i',updated,len(updated)-4)[0]
                self.assertEqual(updated[76:new_index],image_body)
                self.assertEqual(len(updated)-new_index,48)
                resource_name=next(n for n in z.namelist() if '/resource/pb/' in n)
                new_record=fields(fields(z.read(resource_name))[0][2])
                self.assertIn((100,2,b'unknown-resource-data'),new_record)
                shape_name=next(n for n in z.namelist() if '/shape/' in n)
                with zipfile.ZipFile(io.BytesIO(z.read(shape_name))) as shapes:
                    new_shape=fields(fields(shapes.read(shapes.namelist()[0]))[0][2])
                self.assertIn((100,2,b'unknown-shape-data'),new_shape)
                self.assertIn((8,2,b'{"values":[1]}'),new_shape)

if __name__=='__main__':unittest.main()
