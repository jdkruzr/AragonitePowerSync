import importlib.util, json, pathlib, subprocess, tempfile, unittest
ROOT = pathlib.Path(__file__).resolve().parents[1]
spec = importlib.util.spec_from_file_location('ink', ROOT/'scripts/make-ink-fixture.py')
ink = importlib.util.module_from_spec(spec)
spec.loader.exec_module(ink)

class NativeInkTests(unittest.TestCase):
    def test_revision_and_integrity(self):
        report = {'note': {'uniqueId': 'a'*32, 'title': 'PowerSync ink test', 'activeScene': 0},
                  'pages': {'documentId': 'a'*32, 'pageIds': ['b'*32]}}
        with tempfile.TemporaryDirectory() as temp:
            path = pathlib.Path(temp)
            first = ink.make(report, path/'v1.zip')
            second = ink.make(report, path/'v2.zip', first)
            self.assertEqual(first['shapeId'], second['shapeId'])
            self.assertEqual(first['createdAt'], second['createdAt'])
            self.assertGreater(second['updatedAt'], first['updatedAt'])
            self.assertNotEqual(first['pointRevision'], second['pointRevision'])
            self.assertNotEqual(first['shapeRevision'], second['shapeRevision'])
            removed = ink.make(report, path/'removed.zip', second, removed=True)
            restored = ink.make(report, path/'restored.zip', removed)
            self.assertTrue(removed['removed'])
            self.assertFalse(restored['removed'])
            self.assertEqual(first['shapeId'], restored['shapeId'])
            foreign = dict(first, documentId='c'*32)
            with self.assertRaises(ValueError):
                ink.make(report, path/'bad.zip', foreign)
            subprocess.run(['javac', '-d', temp,
                            str(ROOT/'app/src/main/java/dev/aragonite/powersync/NativeInk.java'),
                            str(ROOT/'tests/NativeInkTest.java')], check=True)
            subprocess.run(['java', '-cp', temp, 'dev.aragonite.powersync.NativeInkTest',
                            str(path/'v1.zip'), str(path/'v2.zip'),
                            str(path/'removed.zip'), str(path/'restored.zip')], check=True)

if __name__ == '__main__':
    unittest.main()
