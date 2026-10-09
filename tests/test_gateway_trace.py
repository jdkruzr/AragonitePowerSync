import importlib.util
from pathlib import Path
import unittest

spec = importlib.util.spec_from_file_location('trace_parser', Path(__file__).resolve().parents[1] / 'scripts/parse-gateway-trace.py')
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)


class TraceTests(unittest.TestCase):
    def test_interleaved_identity_multiline_and_sequence_domains(self):
        raw = '''2026-10-06T03:00:00Z HTTP: c:#1 GET /neocloud/_blipsync (as <ud>device-111111111111</ud>)
2026-10-06T03:00:01Z HTTP: c:#2 GET /neocloud/_blipsync (as <ud>device-222222222222</ud>)
2026-10-06T03:00:02Z SyncMsg: c:[abc] Recv Req MSG#5: Body:'SECRET
PAYLOAD' Properties:map[Profile:rev Authorization:SECRET]
2026-10-06T03:00:03Z SyncMsg: c:[abc] Type:rev Id:<ud>fixture</ud> Rev:6-abcd Sequence:24
2026-10-06T03:00:04Z CRUD: c:[abc] Stored doc "<ud>fixture</ud>" / "6-abcd" as #87
2026-10-06T03:00:05Z HTTP: c:[abc] #1: --> BLIP+WebSocket connection closed
'''
        events = module.parse(raw)
        self.assertEqual(events[0]['deviceId'], 'device-111111111111')
        self.assertEqual(events[0]['profile'], 'rev')
        self.assertEqual(events[1]['clientSequence'], 24)
        self.assertEqual(events[2]['serverSequence'], 87)
        self.assertNotIn('SECRET', str(events))
        self.assertNotIn('PAYLOAD', str(events))

    def test_sent_revision_uses_properties_and_keeps_sequence_domain(self):
        raw = """2026-10-07T10:00:00Z Sync: c:[abc] feed (to device-111111111111)
2026-10-07T10:00:01Z SyncMsg: c:[abc] Sent Req MSG#7~: Body: '<ud>{"text":"Properties: Profile:rev id:fake rev:9-bad SECRET"}</ud>' Properties: <ud>map[Profile:rev id:fixture rev:4-abcd sequence:123 Authorization:SECRET]</ud> -- debug
"""
        events = module.parse(raw)
        sent = [event for event in events if event['event'] == 'sentRevision']
        self.assertEqual(sent, [{'at':'2026-10-07T10:00:01Z','connection':'abc','deviceId':'device-111111111111','event':'sentRevision','documentId':'fixture','revision':'4-abcd','message':7,'serverSequence':123}])
        self.assertNotIn('SECRET',str(events))
        self.assertNotIn('fake',str(events))
        self.assertNotIn('clientSequence',sent[0])
