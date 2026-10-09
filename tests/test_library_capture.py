import importlib.util
from pathlib import Path
import unittest

spec=importlib.util.spec_from_file_location('capture',Path(__file__).resolve().parents[1]/'scripts/capture-library-files.py');capture=importlib.util.module_from_spec(spec);spec.loader.exec_module(capture)

class LibraryCaptureTests(unittest.TestCase):
    def test_shared_paths_cannot_escape_or_cross_into_other_data(self):
        self.assertEqual(capture.safe_member('document/id/resource/data/file.html'),'document/id/resource/data/file.html')
        for name in ['/document/id/file','document/../accounts/file','reader/book','../point/id/file','']:
            with self.assertRaises(ValueError):capture.safe_member(name)
