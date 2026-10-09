#!/usr/bin/env python3
"""Create a deterministic, locally authored EPUB for isolated BOOX reader tests."""
import argparse
import hashlib
import json
from pathlib import Path
import zipfile


def make(path):
    path = Path(path)
    if path.exists():
        raise ValueError('Fixture output already exists')
    path.parent.mkdir(parents=True, exist_ok=True)
    entries = {
        'mimetype': 'application/epub+zip',
        'META-INF/container.xml': '''<?xml version="1.0"?>
<container version="1.0" xmlns="urn:oasis:names:tc:opendocument:xmlns:container">
<rootfiles><rootfile full-path="OEBPS/content.opf" media-type="application/oebps-package+xml"/></rootfiles></container>''',
        'OEBPS/content.opf': '''<?xml version="1.0"?>
<package xmlns="http://www.idpf.org/2007/opf" version="2.0" unique-identifier="bookid">
<metadata xmlns:dc="http://purl.org/dc/elements/1.1/">
<dc:identifier id="bookid">urn:powersync:reader-fixture:v1</dc:identifier>
<dc:title>PowerSync reader fixture</dc:title><dc:creator>PowerSync lab</dc:creator><dc:language>en</dc:language></metadata>
<manifest><item id="ncx" href="toc.ncx" media-type="application/x-dtbncx+xml"/>
<item id="one" href="one.xhtml" media-type="application/xhtml+xml"/>
<item id="two" href="two.xhtml" media-type="application/xhtml+xml"/></manifest>
<spine toc="ncx"><itemref idref="one"/><itemref idref="two"/></spine></package>''',
        'OEBPS/toc.ncx': '''<?xml version="1.0"?>
<ncx xmlns="http://www.daisy.org/z3986/2005/ncx/" version="2005-1">
<head><meta name="dtb:uid" content="urn:powersync:reader-fixture:v1"/><meta name="dtb:depth" content="1"/>
<meta name="dtb:totalPageCount" content="0"/><meta name="dtb:maxPageNumber" content="0"/></head>
<docTitle><text>PowerSync reader fixture</text></docTitle><navMap>
<navPoint id="one" playOrder="1"><navLabel><text>Chapter one</text></navLabel><content src="one.xhtml"/></navPoint>
<navPoint id="two" playOrder="2"><navLabel><text>Chapter two</text></navLabel><content src="two.xhtml"/></navPoint>
</navMap></ncx>'''
    }
    for chapter, name in enumerate(['one', 'two'], 1):
        paragraphs = '\n'.join(
            f'<p id="c{chapter}p{i:03}">PowerSync chapter {chapter}, paragraph {i:03}. '
            'The synchronization goblin keeps a bookmark, a highlight, and a receipt. '
            'This text was written for the lab. Stable paragraph identities let us compare '
            'reader locations across fonts, devices, and reopened sessions.</p>'
            for i in range(1, 121))
        entries[f'OEBPS/{name}.xhtml'] = f'''<?xml version="1.0"?>
<html xmlns="http://www.w3.org/1999/xhtml"><head><title>Chapter {chapter}</title></head>
<body><h1>Chapter {chapter}</h1>{paragraphs}</body></html>'''
    with path.open('xb') as stream, zipfile.ZipFile(stream, 'w') as archive:
        for name, body in entries.items():
            entry = zipfile.ZipInfo(name, date_time=(2026, 1, 1, 0, 0, 0))
            entry.compress_type = zipfile.ZIP_STORED if name == 'mimetype' else zipfile.ZIP_DEFLATED
            archive.writestr(entry, body.encode())
    digest = hashlib.sha256(path.read_bytes()).hexdigest()
    receipt = {'fixture': 'urn:powersync:reader-fixture:v1', 'sha256': digest,
               'chapters': 2, 'paragraphs': 240,
               'bookmarkAnchor': 'OEBPS/one.xhtml#c1p030',
               'highlightAnchor': 'OEBPS/two.xhtml#c2p060',
               'nativeBookIdentityQualified': False, 'readerSyncQualified': False}
    path.with_suffix('.json').write_text(json.dumps(receipt, indent=2) + '\n')
    return receipt


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('output')
    print(json.dumps(make(parser.parse_args().output)))
