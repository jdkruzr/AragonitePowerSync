import importlib.util
import json
import pathlib
import struct
import tempfile
import threading
import unittest
import urllib.request
import urllib.error

ROOT = pathlib.Path(__file__).resolve().parents[1]

def module(name, file):
    spec = importlib.util.spec_from_file_location(name, ROOT / 'scripts' / file)
    value = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(value)
    return value

harness = module('cluster_harness', 'cluster-harness.py')
reader = module('cluster_reader', 'read-cluster-config.py')

class ClusterLabTests(unittest.TestCase):
    def test_binder_string_decode_and_exception(self):
        value = '{"curClusterId":"en","label":"Ω"}'
        encoded = value.encode('utf-16le')
        raw = struct.pack('<ii', 0, len(encoded)//2) + encoded + b'\0\0'
        raw += bytes((-len(raw)) % 4)
        words = struct.unpack('<' + 'I' * (len(raw)//4), raw)
        output = '\n'.join('  0x%08x: %s' % (i*4, ' '.join('%08x' % w for w in words[i:i+4]))
                           for i in range(0, len(words), 4))
        self.assertEqual(reader.decode_string_reply(output), value)
        with self.assertRaises(ValueError):
            reader.decode_string_reply('0x00000000: ffffffff 00000000')
        with self.assertRaises(ValueError):
            reader.decode_string_reply('0x00000000: 00000000 00002000')

    def test_all_custom_hosts_are_local(self):
        config = harness.cluster_definition(18765)
        self.assertEqual(config['serverId'], config['lang'])
        self.assertTrue(all('127.0.0.1:18765' in value for value in config['host'].values()))
        self.assertNotIn('boox.com', json.dumps(config))

    def test_unknown_auth_and_replication_fail_without_secret_logging(self):
        with tempfile.TemporaryDirectory() as tmp:
            output = pathlib.Path(tmp)/'requests.jsonl'
            server = harness.Harness(0, output)
            worker = threading.Thread(target=server.serve_forever, daemon=True)
            worker.start()
            base = f'http://127.0.0.1:{server.server_port}'
            try:
                request = urllib.request.Request(base+'/api/users/signin?token=private-query',
                         data=b'code=private-code', headers={'Authorization':'private-token'})
                with self.assertRaises(urllib.error.HTTPError) as error:
                    urllib.request.urlopen(request, timeout=3)
                self.assertEqual(error.exception.code, 401)
                error.exception.close()
                request = urllib.request.Request(base+'/neocloud/_blipsync',
                          headers={'Upgrade':'websocket', 'Sec-WebSocket-Protocol':'BLIP_3+CBMobile_3'})
                with self.assertRaises(urllib.error.HTTPError) as error:
                    urllib.request.urlopen(request, timeout=3)
                self.assertEqual(error.exception.code, 501)
                error.exception.close()
                log = output.read_text()
                for secret in ['private-query','private-code','private-token']:
                    self.assertNotIn(secret, log)
                self.assertIn('BLIP_3+CBMobile_3', log)
            finally:
                server.shutdown();server.server_close();worker.join()

if __name__ == '__main__':
    unittest.main()
