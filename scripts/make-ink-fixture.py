#!/usr/bin/env python3
"""Generate a synthetic native stroke for a recorded PowerSync ink fixture.

Wire fields reconstructed from local BOOX APKs. No vendor implementation is
included. Lab-only: input must be the app's own ink-metadata report.
"""
import argparse, io, json, math, pathlib, re, struct, time, uuid, zipfile

def varint(value):
    if not 0 <= value < 1 << 64:
        raise ValueError('uint64 range')
    out = bytearray()
    while value >= 128:
        out.append((value & 127) | 128)
        value >>= 7
    return bytes(out) + bytes([value])

def number(field, value):
    return varint(field << 3) + varint(value)

def blob(field, value):
    if isinstance(value, str):
        value = value.encode()
    return varint((field << 3) | 2) + varint(len(value)) + value

def floating(field, value):
    return varint((field << 3) | 5) + struct.pack('<f', value)

def make(report, output, previous=None, removed=False):
    note = report['note']
    if not note['title'].startswith('PowerSync ink ') or note['activeScene'] != 0:
        raise ValueError('Not a PowerSync ink fixture')
    doc = note['uniqueId']
    page = report['pages']['pageIds'][0]
    for identity in (doc, page):
        if not re.fullmatch(r'[0-9a-fA-F-]{32,36}', identity):
            raise ValueError('Invalid native identity')
    if report['pages']['documentId'] != doc:
        raise ValueError('Page report belongs to another document')
    if previous and (previous['documentId'] != doc or previous['pageId'] != page):
        raise ValueError('Previous revision belongs to another fixture')
    shape = previous['shapeId'] if previous else str(uuid.uuid4())
    revision, points_revision = str(uuid.uuid4()), str(uuid.uuid4())
    stamp = max(int(time.time() * 1000), previous['updatedAt']+1 if previous else 0)
    created = previous['createdAt'] if previous else stamp
    # Revision two moves the same stroke down; it does not create a second shape.
    y_offset = previous.get("yOffset", 250) if previous and removed else previous.get("yOffset", 250)+250 if previous else 0
    points = [(400 + i * 8, 650 + y_offset + 100 * math.sin(i / 10), 2048, 2048, i * 10) for i in range(101)]
    bounds = dict(left=390, top=540+y_offset, right=1210, bottom=760+y_offset, empty=False, stability=0)
    args = dict(alphaFactor=1.0, displayScale=1.0, dpi=320.0, maxPressure=4096.0,
                newBrushRatio=0.0, pressure=2048.0, pressureSensitivity=1.0,
                smoothLevel=0.2, source=1, tiltX=0, tiltY=0)
    shape_pb = b''.join((blob(1, shape), number(2, created), number(3, stamp),
                        number(4, 0xffffffffff000000), floating(5, 8.0),
                        blob(7, json.dumps(bounds)), blob(11, json.dumps(args)),
                        number(12, 3), number(15, int(removed)), blob(16, points_revision), blob(18, revision)))
    filename = f'{page}#{revision}#{stamp}'
    nested = io.BytesIO()
    with zipfile.ZipFile(nested, 'w', zipfile.ZIP_DEFLATED) as z:
        z.writestr(filename, blob(1, shape_pb))
    header = struct.pack('>hh', 0, 1) + page.encode().ljust(36, b' ') + points_revision.encode().ljust(36, b' ')
    payload = struct.pack('>hh', 0, 0) + b''.join(struct.pack('>ffhhi', *point) for point in points)
    xref = len(header) + len(payload)
    point_file = header + payload + shape.encode().ljust(36, b' ') + struct.pack('>iii', 76, len(payload), xref)
    with zipfile.ZipFile(output, 'w', zipfile.ZIP_DEFLATED) as z:
        z.writestr(f'point/{doc}/{page}/{page}#{points_revision}#points', point_file)
        z.writestr(f'document/{doc}/shape/{filename}.zip', nested.getvalue())
    state = dict(documentId=doc, pageId=page, shapeId=shape, createdAt=created, updatedAt=stamp,
                 shapeRevision=revision, pointRevision=points_revision, synthetic=True, yOffset=y_offset, removed=removed)
    pathlib.Path(str(output)+'.json').write_text(json.dumps(state, indent=2)+'\n')
    return state

if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('metadata', type=pathlib.Path)
    parser.add_argument('output', type=pathlib.Path)
    parser.add_argument('--previous', type=pathlib.Path)
    parser.add_argument('--remove', action='store_true')
    a = parser.parse_args()
    print(json.dumps(make(json.loads(a.metadata.read_text()), a.output,
                          json.loads(a.previous.read_text()) if a.previous else None, a.remove), indent=2))
