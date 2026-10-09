# PDF and reader-ink qualification checkpoint

Follow-up: [real stylus export and transport results](native-reader-ink-export.md).
The prepared checkpoint below remains its original pre-stroke observation.

2026-10-07. Private evidence: `artifacts/gateway-lab/qualification/reader-ink-static/`.
This is a prepared live fixture and a static dependency map, not a passed PDF/ink
replication gate. The checkpoint contains 83 independently verified evidence files.

`scripts/make-pdf-reader-fixture.py` authors a deterministic four-page PDF without
third-party content. Media/crop boxes and page rotations are recorded in its JSON
receipt: portrait 600x800, landscape 800x600, portrait rotated 90 degrees, and
600x800 media cropped to [50,100,550,700]. The PDF-point anchor rectangle is
[100,400,400,450], measured from the unrotated lower-left origin. Poppler pdfinfo
confirms every box and rotation, pdftotext extracts the authored labels, and host
rendering succeeds. These are host controls, not native annotation-coordinate proof.

Both devices open the exact 2270-byte PDF in NeoReader and show 1/4. An ordinary
app UID reads its hash-verified metadata, bookmarks and annotations. Each has one
native metadata row, matching content hashTag and different UUIDs. File and
user-data sync flags are both zero. No server scope or replication was enabled
for this PDF. First-open screenshots show the portrait anchor rectangle on both;
rotated/cropped-page application and round-trip geometry are still open.

The read-only `reader_pdf_capture` probe accepts only the fixed Downloads path
and exact SHA-256. Original reader guards exclude the new authored fixture paths;
before copying the PDF, the path was absent and the helper found no preexisting
PDF metadata row. Full original reader metadata remains exact (28 Palma, 14 Lumi),
notebook guards pass (1517 Palma, 1518 Lumi), and original note/reader flags are
restored. Accounts and cluster maps were not changed for this checkpoint. Probe
upgrades matched installed certificates and used in-place installation; fresh
Android assembly/lint passes.

## Static dependencies and a provider trap

Both pulled ksync builds have byte-identical ReaderModelType,
SyncReaderNoteDocumentModel and SyncReaderUserDataModel source files. Other
captured reader asset classes differ at the decompiled-byte level; that is not
proof of a wire contract change. The observed KReaderOssUtils difference is an
obfuscated ShapeDocumentArgs accessor name. Preserve the actual field contract
and compare behavior instead of interpreting renamed generated symbols as new
schema semantics.

ReaderModelType names mode 6 NOTE_SHAPE_MODE and mode 7 NOTE_DOCUMENT_MODE, but
isUserDataModelType admits only 1, 2 and 7. Do not infer that publishing a mode-6
record is the current delivery path. Mode 7 contains commitId/commitInfo,
readerNotePageNameMap, background, stroke parameters and extra attributes.
CompareReaderUserDocSyncFileAction separately compares shapes, points, resource
protobufs, resource data, book bytes and thumbnails under a user/document-scoped
OSS prefix. Consequently a mode-7 receipt does not establish ink dependency
closure or visible ink application.

The installed NeoReader manifests name ReaderNoteContentProviderImpl at the
exported com.onyx.kreader.note.provider authority. Its query method explicitly
throws UnsupportedOperationException("unimplemented method"). A generated
ReaderNoteContentProvider_Provider sibling has a query implementation but is not
the manifest-selected provider. Both live ordinary-app probes reproduce the
failure for document and shape endpoints. The capture reports explicit query
errors; it does not turn them into empty ink-row receipts. Exported authority is
therefore insufficient evidence of readable handwriting rows.

ReaderNoteDatabase is document-specific and the shape model contains page-origin
width/height, boundingRect, matrixValues, anchor and multiple page-position
representations. These fields make rotation/crop/viewport tests necessary. A raw
stroke-byte transfer alone cannot qualify placement on the receiving device.

Static MergePullDocDataRequest also identifies the receiver boundary: it selects
the newest mode-7 note-document configuration by updatedAt, remaps its reader
identity (and regenerates the note UUID when book identity changes), saves that
configuration, then imports shape protobufs and resource protobufs. Point/resource
data directories are copied when source and destination identities differ.
Subpage identity reconciliation may replace shape subpage IDs. This is native
application business logic after replication; the facade must preserve those
inputs and should not pre-flatten their identities. These are source-derived
contracts pending a real ink round trip.

ReaderExportService is an enabled exported service with an IReaderExportService
Binder. Its exportDocuments path accepts explicit path lists and produces a
native export through ExportDocumentsAction. This is a candidate for bounded
fixture-only observation when the handwriting provider cannot query; binding,
output contents and edit/save timing are not yet live-qualified here.

## Pending native control

Both devices retain the owned PDF with sync flags zero. The next native control
requires one small real-stylus stroke inside the page-1 rectangle on the 10.3 II.
A user request is pending because ADB finger input does not establish native
pressure-bearing pen behavior. Automated long presses did not create a captured
annotation; no highlight or handwriting success is claimed. After the pen control,
map the native export/scoped sync path, escrow exact assets, then qualify receipt,
native application and placement separately on the peer. User confirmation of a
stroke is a trigger for capture, not itself a success receipt.
