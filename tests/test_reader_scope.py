import importlib.util
import json
from pathlib import Path
import subprocess
import unittest

root = Path(__file__).resolve().parents[1]
spec = importlib.util.spec_from_file_location('reader_scope', root / 'scripts/reader-fixtures.py')
module = importlib.util.module_from_spec(spec); spec.loader.exec_module(module)
book = '12345678-1234-1234-1234-123456789012'


class ReaderScopeTests(unittest.TestCase):
    def test_reader_asset_policy_rejects_cross_book_and_other_file_types(self):
        scope='powersync-lab-ASSET-'+book
        doc={'_id':'labasset-test','user':'powersync-lab','type':'labAsset',
             'dbId':scope,'documentUniqueId':book,
             'ossKey':f'powersync-lab/reader/{book}/book/{book}.epub'}
        self.assertTrue(self.run_policy(doc,[scope])['accepted'])
        pdf={**doc,'ossKey':f'powersync-lab/reader/{book}/book/{book}.pdf'}
        self.assertTrue(self.run_policy(pdf,[scope])['accepted'])
        self.assertFalse(self.run_policy(pdf,[])['accepted'])
        self.assertFalse(self.run_policy(doc,[])['accepted'])
        for change in [{'documentUniqueId':'ffffffff-ffff-ffff-ffff-ffffffffffff'},
                       {'ossKey':f'powersync-lab/reader/{book}/point/anything.bin'},
                       {'user':'another-account'},{'type':'anotherType'}]:
            self.assertFalse(self.run_policy({**doc,**change},[scope])['accepted'])

    def test_native_point_asset_requires_exact_key_and_parent(self):
        key=f'powersync-lab/reader/{book}/point/page#revision#points'
        scope='powersync-lab-ASSET-'+book
        doc={'_id':'labasset-point','user':'powersync-lab','type':'labAsset',
             'dbId':scope,'documentUniqueId':book,'ossKey':key}
        assets={key:book}
        self.assertTrue(self.run_policy(doc,[scope],native_assets=assets)['accepted'])
        self.assertFalse(self.run_policy(doc,[],native_assets=assets)['accepted'])
        self.assertFalse(self.run_policy({**doc,'ossKey':key+'-other'},[scope],native_assets=assets)['accepted'])
        self.assertFalse(self.run_policy({**doc,'documentUniqueId':'f'*32},[scope],native_assets=assets)['accepted'])
        for bad in [{key:'f'*32},{key.replace('/point/','/../'):book}]:
            with self.assertRaises(ValueError):module.extend_sync('function(doc,oldDoc) {}',[book],bad)

    def run_policy(self, doc, grants, old=None, native_assets=None, dynamic_native_books=()):
        source = module.extend_sync('function(doc, oldDoc) { throw({forbidden:"outside"}); }', [book],native_assets,dynamic_native_books)
        script = '''const channels=[];const grants=INPUT.grants;
function requireAccess(scope){if(!grants.includes(scope))throw Error("no grant");}
function channel(scope){channels.push(scope);}
let accepted=true;try{(POLICY)(INPUT.doc,INPUT.old);}catch(e){accepted=false;}
process.stdout.write(JSON.stringify({accepted,channels}));'''
        script = script.replace('POLICY', source).replace('INPUT', json.dumps({'doc':doc,'old':old,'grants':grants}))
        return json.loads(subprocess.check_output(['node','-e',script], text=True))

    def test_dynamic_native_keys_require_opt_in_grant_and_exact_book(self):
        scope='powersync-lab-ASSET-'+book
        doc=dict(_id='labasset-new', user='powersync-lab', type='labAsset',
                 dbId=scope, documentUniqueId=book)
        for suffix in ['point/-12#new-revision#points','shape/-12#new-revision#123.zip']:
            value={**doc,'ossKey':f'powersync-lab/reader/{book}/{suffix}'}
            self.assertFalse(self.run_policy(value,[scope])['accepted'])
            self.assertTrue(self.run_policy(value,[scope],dynamic_native_books=[book])['accepted'])
            self.assertFalse(self.run_policy(value,[],dynamic_native_books=[book])['accepted'])
        for suffix in ['point/../secret#points','point/file.bin','shape/file.zip/extra',
                       'resource/data/file.html','shape/fileXzip','shape/..zip']:
            value={**doc,'ossKey':f'powersync-lab/reader/{book}/{suffix}'}
            self.assertFalse(self.run_policy(value,[scope],dynamic_native_books=[book])['accepted'])
        with self.assertRaises(ValueError):
            module.extend_sync('function(doc,oldDoc) {}',[book],dynamic_native_books=['f'*32])

    def test_reader_commit_point_requires_native_type_identity_and_grant(self):
        scope='powersync-lab-READER_LIBRARY'
        doc={'_id':'powersync-lab#reader-commit','uniqueId':'reader-commit',
             'user':'powersync-lab','dbId':scope,'documentUniqueId':book,
             'commitType':4,'commitStatus':1}
        self.assertTrue(self.run_policy(doc,[scope])['accepted'])
        self.assertFalse(self.run_policy(doc,[])['accepted'])
        native={**doc,'_id':doc['uniqueId']}
        self.assertTrue(self.run_policy(native,[scope])['accepted'])
        self.assertFalse(self.run_policy(native,[])['accepted'])
        for change in [{'commitType':1},{'commitStatus':0},{'documentUniqueId':'f'*32},
                       {'_id':'unrelated'},{'user':'other-library'}]:
            self.assertFalse(self.run_policy({**doc,**change},[scope])['accepted'])

    def test_exact_book_and_independent_grant(self):
        scope='powersync-lab-READER_LIBRARY'
        doc={'_id':'powersync-lab#'+book,'uniqueId':book,'user':'powersync-lab','dbId':scope,'modeType':4}
        self.assertTrue(self.run_policy(doc,[scope])['accepted'])
        self.assertFalse(self.run_policy(doc,[])['accepted'])
        self.assertFalse(self.run_policy({**doc,'_id':'unrelated'},[scope])['accepted'])
        self.assertFalse(self.run_policy({**doc,'modeType':3},[scope])['accepted'])
        self.assertFalse(self.run_policy({**doc,'user':'other-library'},[scope])['accepted'])

    def test_bookmark_cannot_cross_book_scope(self):
        scope='powersync-lab-'+book
        doc={'_id':'bookmark','user':'powersync-lab','dbId':scope,'documentId':book,'modeType':2}
        self.assertTrue(self.run_policy(doc,[scope])['accepted'])
        self.assertFalse(self.run_policy({**doc,'documentId':'another-book'},[scope])['accepted'])
        self.assertFalse(self.run_policy({**doc,'dbId':'powersync-lab-READER_LIBRARY'},[scope])['accepted'])

    def test_metadata_tombstone_uses_old_identity(self):
        scope='powersync-lab-READER_LIBRARY'
        old={'_id':'powersync-lab#'+book,'uniqueId':book,'user':'powersync-lab','dbId':scope,'modeType':4}
        self.assertTrue(self.run_policy({'_id':'powersync-lab#'+book,'_deleted':True},[scope],old)['accepted'])

    def test_library_scoped_bookmark_is_still_book_restricted(self):
        scope='powersync-lab-READER_LIBRARY'
        doc={'_id':'powersync-lab#bookmark','uniqueId':'bookmark','user':'powersync-lab','dbId':scope,'documentId':book,'modeType':2}
        self.assertTrue(self.run_policy(doc,[scope])['accepted'])
        self.assertFalse(self.run_policy({**doc,'documentId':'unrelated'},[scope])['accepted'])
