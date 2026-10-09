import importlib.util
import io
import json
from pathlib import Path
import sys
import tempfile
import unittest
import zipfile

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / 'scripts'))
from native_wire import encode, fields
spec = importlib.util.spec_from_file_location('catalog', Path(__file__).resolve().parents[1] / 'scripts/derive-image-catalog.py')
catalog = importlib.util.module_from_spec(spec)
spec.loader.exec_module(catalog)


class CatalogRecoveryTest(unittest.TestCase):
    def archive(self, folder, relative='image.png', existing=b''):
        resource = {'uniqueId': 'image-resource', 'documentId': 'document',
                    'relativePath': relative, 'type': 1, 'status': 0}
        shape = encode([(2, 0, 10), (3, 0, 20), (12, 0, 19),
                        (14, 2, json.dumps(resource).encode())])
        nested = io.BytesIO()
        with zipfile.ZipFile(nested, 'w') as z:
            z.writestr('shapes', encode([(1, 2, shape)]))
        path = folder / 'source.note'
        with zipfile.ZipFile(path, 'w') as z:
            z.writestr('document/shape/page.zip', nested.getvalue())
            z.writestr('document/resource/pb/catalog', existing)
            z.writestr('document/resource/data/image.png', b'exact image bytes')
            z.writestr('document/point/payload', b'exact point bytes')
        return path

    def test_catalog_roundtrip_and_immutable_payloads(self):
        with tempfile.TemporaryDirectory() as temp:
            folder = Path(temp); source = self.archive(folder); before = source.read_bytes()
            target = folder / 'derived.note'
            result = catalog.derive(source, target)
            self.assertEqual(result['resourceCount'], 1)
            self.assertTrue(result['otherEntriesByteIdentical'])
            self.assertEqual(source.read_bytes(), before)
            with zipfile.ZipFile(target) as z:
                record = fields(z.read('document/resource/pb/catalog'))[0]
                row = {n: v for n, w, v in fields(record[2])}
                self.assertEqual(row[1], b'image-resource')
                self.assertEqual(row[8], b'image.png')
            self.assertEqual(target.stat().st_mode & 0o777, 0o600)
            with self.assertRaises(ValueError): catalog.derive(source, target)

    def test_refuses_path_traversal_and_nonempty_catalog(self):
        for relative, existing in [('../image.png', b''), ('image.png', b'existing')]:
            with tempfile.TemporaryDirectory() as temp:
                folder = Path(temp); source = self.archive(folder, relative, existing)
                with self.assertRaises(ValueError): catalog.derive(source, folder / 'derived.note')
                self.assertFalse((folder / 'derived.note').exists())
