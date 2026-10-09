import importlib.util
import json
from pathlib import Path
import subprocess
import unittest

spec = importlib.util.spec_from_file_location('fixtures', Path(__file__).resolve().parents[1] / 'scripts/gateway-fixtures.py')
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)


class DoorbellPolicyTests(unittest.TestCase):
    def test_only_lab_note_doorbell_is_admitted(self):
        program = '''const sync = %s;
let channels=[];
function requireAccess(s) { if(s!=="powersync-lab-MESSAGE") throw Error("access"); }
function channel(s) { channels.push(s); }
const good={_id:"notification",user:"powersync-lab",dbId:"powersync-lab-MESSAGE",msgType:6,replicatorName:"NOTE_TREE",syncAt:123};
sync(good,null);
if(channels.length!==1) throw Error("missing channel");
for(const patch of [{user:"personal"},{msgType:4},{replicatorName:"CONFIG"},{_deleted:true},{syncAt:"123"},{dbId:"other-MESSAGE"}]) {
 let denied=false;try { sync({...good,...patch},null); } catch(e) {denied=true;}
 if(!denied) throw Error("escaped policy "+JSON.stringify(patch));
}
''' % module.sync_function(['fixture'], True)
        subprocess.run(['node', '-e', program], check=True, capture_output=True)
