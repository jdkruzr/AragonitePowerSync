#!/usr/bin/env python3
"""Author a deterministic PDF with explicit geometry for isolated reader tests."""
import argparse
import hashlib
import json
from pathlib import Path


def make(path):
    path = Path(path)
    pages = [
        {"name": "portrait", "mediaBox": [0, 0, 600, 800], "cropBox": [0, 0, 600, 800], "rotate": 0},
        {"name": "landscape", "mediaBox": [0, 0, 800, 600], "cropBox": [0, 0, 800, 600], "rotate": 0},
        {"name": "rotated", "mediaBox": [0, 0, 600, 800], "cropBox": [0, 0, 600, 800], "rotate": 90},
        {"name": "cropped", "mediaBox": [0, 0, 600, 800], "cropBox": [50, 100, 550, 700], "rotate": 0},
    ]
    objects = [b"<< /Type /Catalog /Pages 2 0 R >>",
               b"<< /Type /Pages /Kids [4 0 R 6 0 R 8 0 R 10 0 R] /Count 4 >>",
               b"<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>"]
    for i, page in enumerate(pages, 1):
        # PDF coordinates are points, origin at the lower left before rotation.
        anchor = [100, 400, 400, 450]
        page["anchorRectangle"] = anchor
        text = f"PowerSync PDF {i}: {page['name']} - highlight this line"
        stream = (f"q 0.8 G 100 400 300 50 re S Q\n"
                  f"BT /F1 12 Tf 110 425 Td ({text}) Tj ET\n"
                  f"BT /F1 14 Tf 100 500 Td (Native ink goes inside the rectangle.) Tj ET\n"
                  f"0 G 100 100 m 130 100 l S 100 100 m 100 130 l S\n").encode("ascii")
        page_id = len(objects) + 1
        box = lambda values: "[" + " ".join(map(str, values)) + "]"
        objects.append((f"<< /Type /Page /Parent 2 0 R /MediaBox {box(page['mediaBox'])} "
                        f"/CropBox {box(page['cropBox'])} /Rotate {page['rotate']} "
                        f"/Resources << /Font << /F1 3 0 R >> >> /Contents {page_id + 1} 0 R >>").encode("ascii"))
        objects.append(f"<< /Length {len(stream)} >>\nstream\n".encode() + stream + b"endstream")
    data = bytearray(b"%PDF-1.4\n%\xe2\xe3\xcf\xd3\n")
    offsets = [0]
    for i, body in enumerate(objects, 1):
        offsets.append(len(data))
        data.extend(f"{i} 0 obj\n".encode() + body + b"\nendobj\n")
    xref = len(data)
    data.extend(f"xref\n0 {len(offsets)}\n0000000000 65535 f \n".encode())
    for offset in offsets[1:]:
        data.extend(f"{offset:010d} 00000 n \n".encode())
    data.extend(f"trailer\n<< /Size {len(offsets)} /Root 1 0 R >>\nstartxref\n{xref}\n%%EOF\n".encode())
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("xb") as output:
        output.write(data)
    receipt = {"fixture": "urn:powersync:pdf-reader-fixture:v1", "sha256": hashlib.sha256(data).hexdigest(),
               "pages": pages, "coordinateSystem": "PDF points, lower-left origin before page rotation",
               "nativeGeometryQualified": False, "nativeInkQualified": False}
    with path.with_suffix(".json").open("x") as output:
        json.dump(receipt, output, indent=2)
        output.write("\n")
    return receipt


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("output")
    print(json.dumps(make(parser.parse_args().output), indent=2))
