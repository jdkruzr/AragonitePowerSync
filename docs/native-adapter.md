# Native adapter contracts and evidence

2026-10-05. Implementation is a fixture-only lab, not a general library importer.

## Tested interfaces

| Capability | Interface | Lumi result |
| --- | --- | --- |
| Read notebook metadata | Notes `ContentProvider/NoteModel` | Pass, ordinary app UID |
| Enumerate virtual pages | Notes Binder `loadVirtualPageIdList` | Pass |
| Create text notebook | `CREATE_NOTE_FROM_TEXT`, direct `OPEN_NOTE_BEAN` | Pass; native importer may append numeric title suffix |
| Rename notebook | `RENAME_NOTE`, `note_service_bean.noteBean` | Pass; native ID preserved |
| Open notebook | Exported `ScribbleActivity`, JSON `OPEN_NOTE_BEAN` | Pass |
| Create handwriting notebook | `ScribbleActivity`, `create=true`, `SCRIBBLE_NOTE` | Native creation screen; fixture title prefilled; Create completes it |
| Export native bundle | `EXPORT_NOTE`, native format 5, `remoteExport=true` | Pass for text and synthetic ink |
| Disable fixture cloud sync | `UPDATE_SYNC_STATUS`, documentId, noteSyncStatus=0 | Persisted native flag observed |
| Notify changed local payload | Notes Binder bundle `KSYNC_NOTE_ACTION=KSYNC_NOTE_DATA_UPDATE_ACTION`, `KEY_DOCUMENT_ID` | New and revised native strokes consumed on reopen |
| Edit text resource | Shared `.ksync/document/{id}/resource/data/*.html` JSON | Marker visible, editable, retained by native export |
| Import shape revision | Shared `document/{id}/shape/{page}#{revision}#{timestamp}.zip` | Synthetic oil-pen stroke rendered |
| Import points | Shared `point/{id}/{page}/{page}#{pointRevision}#points` | All 101 point records preserved byte-for-byte through export |
| Revise existing shape | Same shape ID, newer updatedAt, new PB and point revisions | One moved stroke, same notebook and shape IDs |
| Remove and restore shape | Newer revisions with status 1, then status 0, same IDs | Stroke disappears and returns; native document remained open |
| Recover interrupted staging | Durable points first, shape published last, immutable receipt and journal | Injected interruption left only points; retry completed |
| Reject malformed payload | Shape/point reference, page, spans, CRC and coordinate validation | Bad point header rejected before native files written |
| Bind ksync | Exported `KSyncService` | Descriptor verified; no stock replication control invoked |
| Replace native image | Asset, resource PB, point and shape revisions | Same image ID and geometry; exported PNG equals incoming bytes; existing stroke retained |

The `.html` resources actually contain JSON editor documents. Native edits rotate
resource IDs and retain history. Never assume a file path remains the current
resource after an edit. The lab text writer refuses a changed or ambiguous target.

## Shape wire layout

Shape ZIP contains `NoteShapeDocProto.ShapeInfoProtoList`: repeated field 1 is a
shape message. The synthetic generator uses shape fields 1 (unique ID), 2/3
(created/updated epoch milliseconds), 4 (color), 5 (float thickness), 7 (JSON bounds),
11 (JSON create arguments), 12 (type, 3 oil pen), 15 (status), 16 (point revision),
18 (info revision). Unknown fields matter for a full adapter; do not discard them
in a generic capture/import conversion. The lab validator allows exactly one
shape and rejects duplicates; it is deliberately not a general protobuf codec.

Point format v1 uses big-endian data: 76-byte header (two shorts, padded 36-byte
page ID, padded 36-byte point revision); per-shape two shorts and 16-byte records
(float x/y, short size/pressure, int time); 44-byte index entries (padded shape ID,
int offset/length); trailing int index offset. Export regenerates container
revisions and point filenames while retaining the actual record bytes. Whole-ZIP
or filenames are not stable content identities. The app now reports an optional
semantic identity for single oil-pen shape records: preserve all protobuf field
values and wire types except container revision IDs 16/18, and include actual
point record bytes. Two native exports without edits produced different archive
manifests but identical ink identities. This is a narrow supported scope, not a
complete notebook hash; text/images/multi-shape envelopes are not covered yet.

## Application lifecycle

Immutable receipt ZIPs are stored in app-private `ink-inbox/`; one JSON journal
per receipt records prepared, points-staged, staged and notified phases. External
native files are written atomically, points before shape metadata. Before any
write, all existing target files must match the received bytes. Retry skips
identical files. Binder delivery and `notified` are not native application ACKs:
the report keeps `nativeEditorVerified=false` until separate runtime verification.

Image bundles stage the asset and resource PB before points and shape metadata.
The image generator preserves unknown protobuf fields and native geometry, and
extracts the image's point span even when native export groups several shapes
in one point file. Resource identifiers and relative paths are checked together.

This lab does not yet scan pending journals automatically at startup, integrate
Rhizome receipt cursors, detect native concurrent edits, or manage asset GC.
Account-free behavior, arbitrary images/templates, multi-page/layer edits and
physical-pen qualification remain open. Synthetic ink is a real native stroke
object, but does not replace physical handwriting coverage.

Broad stock RESTORE_NOTE is unsuitable: inspected restore code can affect the
whole library and regenerate notebook identities. No restore action is used.

## Stock ksync endpoint substitution

Selected classes recovered from Go6 ksync 32198 confirm the older 32174 behavior:
`UpdateClusterInfosAction` parses a JSON list of ServerInfo from
`/sdcard/onyx_test_server`, adds regions absent from the fetched official list,
and converts host/OSS/Couchbase configuration into ClusterInfo. It runs after a
successful nonempty index-server response. Changing current cluster invokes
Onyx-account logout. `/sdcard/onyx_cb_test_server` is a separate test flag, not a
file containing an arbitrary URL.

Endpoint substitution cannot be ruled out. This is source evidence, not a live
custom-server test. A Couchbase-compatible replicator alone may be insufficient:
account/token, metadata APIs, message replication and object-store services also
need inspection. No marker file or cluster setting was changed on the Lumi.
The agreed independent adapter architecture remains in place.

The later live Palma spike demonstrated custom endpoint selection and an
anonymous credential-validated CBMobile_3/BLIP connection. This still does not
qualify note-account replication. See [ksync-retargeting-spike.md](ksync-retargeting-spike.md)
for exact evidence, dependency scope and restored device state.
