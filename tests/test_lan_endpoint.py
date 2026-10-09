import importlib.util
from pathlib import Path
import unittest

path=Path(__file__).resolve().parents[1]/'scripts/cluster-harness.py'
spec=importlib.util.spec_from_file_location('cluster_lan',path)
harness=importlib.util.module_from_spec(spec);spec.loader.exec_module(harness)

class LanEndpointTests(unittest.TestCase):
 def test_native_host_and_asset_routes_share_advertised_interface(self):
  definition=harness.cluster_definition(18765,18766,'192.168.8.95')
  self.assertEqual(definition['host']['onyxHostBaseUrl'],'http://192.168.8.95:18765')
  self.assertEqual(definition['host']['onyxCouchbaseWSSHost'],'ws://192.168.8.95:18766/neocloud')
  self.assertEqual(definition['ossConfig']['ossNoteEndPoint'],'http://192.168.8.95:18765')
 def test_listener_scope_excludes_wildcards_and_public_addresses(self):
  for host in ['0.0.0.0','8.8.8.8','224.0.0.1','example.com']:
   with self.assertRaises(ValueError):harness.cluster_definition(18765,18766,host)
