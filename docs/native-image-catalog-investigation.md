# Native image catalog investigation

2026-10-06, static source inspection plus server-reconstructed archive inspection.
The representative catalog repair now passes native fresh-copy import/re-export
on Palma. This is not a general original-library repair or migration qualification.

The representative derived archive contains eleven image files and eleven live
image shape resource JSON descriptions, but its resource protobuf file is empty.
The fresh imported copy's native export contains no image files and an empty
resource protobuf. File insertion alone does not produce native resource closure.

In the Go 10.3 II Notes dump, `ResourceModelData.exportToNoteFile` gathers resource
IDs from selected page backgrounds and live shape rows. It loads matching resource
models, then `ResourceModel.filterExportPBResourceModel` retains enabled models
whose resolved files exist, plus removed models. It copies those models' files
and writes their protobuf catalog. It does not enumerate loose resource files.
`NewShapeModel.fromShapeInfoProto` extracts the resource ID from embedded shape
JSON; that ID alone does not establish a matching resource model.

The archive's empty catalog is therefore a concrete missing dependency, consistent
with export omission. Live shape loading, model import, resolved paths and status
still need verification before assigning the complete cause. The opened copy's
export also lacks live image resource JSON, so repairing the catalog may require
reimporting the complete original shape records into a fresh disposable copy.

Completed experiment: derive a new archive from the immutable server reconstruction,
using embedded resource descriptions to construct the missing resource catalog.
Require unambiguous resource IDs and paths and hash-verified files. Retain original
shape/point bytes and record every changed entry. Import only as a fresh lab-owned
copy; compare page rendering, point spans, and a subsequent native export.
Never rewrite the original notebook or its preservation receipts.

`scripts/derive-image-catalog.py` reconstructs only the narrow case of one empty
catalog with unambiguous live image resource descriptions and exact files. It
rejects nonempty catalogs, conflicting descriptions, unsafe paths and existing
outputs. The derivation changed only the catalog entry. Native fresh-copy import
and re-export retained all eleven exact image hashes, eleven catalog records,
eleven live image shapes, four point files, and all 549 exact point payload spans.
The native editor displayed an inserted image on page two. Rendering of every
image on every page remains unchecked; background/template/full resource closure
and the empty JSON reference in another archive remain unresolved.

Private evidence: `artifacts/gateway-lab/qualification/lan/image-catalog-audit.json`.
The report contains counts and field names rather than personal notebook identity.
