#!/usr/bin/env python3
"""Extract whitelisted protocol metadata; never copy bodies or credentials."""
import argparse
import json
import re
from pathlib import Path


def parse(raw):
    blocks = re.split(r'(?=^\d{4}-\d\d-\d\dT)', raw, flags=re.M)
    http, connections = {}, {}
    for block in blocks:
        request = re.search(r'c:#(\d+).*GET /neocloud/_blipsync.*?\b(device-[0-9a-f]{12})\b', block)
        if request:
            http[request[1]] = request[2]
        closed = re.search(r'c:\[([0-9a-f]+)\].*?#(\d+): --> BLIP\+WebSocket connection closed', block)
        if closed and closed[2] in http:
            connections[closed[1]] = http[closed[2]]
        feed = re.search(r'c:\[([0-9a-f]+)\].*?\(to (device-[0-9a-f]{12})\)', block)
        if feed:
            connections[feed[1]] = feed[2]
    events = []
    for block in blocks:
        connection = re.search(r'c:\[([0-9a-f]+)\]', block)
        stamp = re.match(r'\d{4}-\d\d-\d\dT\S+', block)
        if not stamp:
            continue
        base = {'at': stamp[0]}
        if connection:
            base.update(connection=connection[1], deviceId=connections.get(connection[1]))
        # Only parse Properties, never Body, even when the body spans lines.
        req = re.search(r'\b(Recv|Sent) Req MSG#(\d+)', block)
        profile = re.search(r'Properties:.*?\bProfile:(getCheckpoint|setCheckpoint|subChanges|proposeChanges|changes|rev)\b', block, re.S)
        if req and profile:
            events.append(dict(base, event='request', direction=req[1], message=int(req[2]), profile=profile[1]))
        # Outbound identity comes only from the final Properties envelope, never
        # from a JSON body (which can contain fake log/property strings).
        properties = block.rsplit(' Properties:', 1)[-1] if ' Properties:' in block else ''
        properties = properties.split('</ud>', 1)[0].split(' -- ', 1)[0]
        if req and req[1] == 'Sent' and re.search(r'\bProfile:rev\b', properties):
            doc = re.search(r'\bid:([a-zA-Z0-9_-]+)(?:\s|\])', properties)
            revision = re.search(r'\brev:([0-9]+-[a-zA-Z0-9]+)(?:\s|\])', properties)
            sequence = re.search(r'\bsequence:([0-9]+)(?:\s|\])', properties)
            if doc and revision:
                event = dict(base, event='sentRevision', documentId=doc[1], revision=revision[1], message=int(req[2]))
                if sequence: event['serverSequence'] = int(sequence[1])
                events.append(event)
        stored = re.search(r'Stored doc "<ud>([a-zA-Z0-9_-]+)</ud>" / "([0-9a-f-]+)" as #(\d+)', block)
        if stored:
            events.append(dict(base, event='stored', documentId=stored[1], revision=stored[2], serverSequence=int(stored[3])))
        rev = re.search(r'Type:rev Id:<ud>([a-zA-Z0-9_-]+)</ud> Rev:([0-9a-f-]+) Sequence:(\d+)', block)
        if rev:
            events.append(dict(base, event='receivedRevision', documentId=rev[1], revision=rev[2], clientSequence=int(rev[3])))
    return events


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('log', type=Path)
    parser.add_argument('output', type=Path)
    args = parser.parse_args()
    with args.output.open('w') as stream:
        for event in parse(args.log.read_text()):
            stream.write(json.dumps(event) + '\n')
