# Native reading source map

Static first pass, 2026-10-06. Source paths are beneath the pulled Go 10.3 II
`com.onyx.android.ksync/sources/com/onyx/android/` tree. Decompiled code is a
candidate contract; installed-version and live behavior qualification remain.
No reading sync flag was enabled for the initial static investigation. The later
[scoped live reading run](native-reading-qualification.md) qualifies metadata,
one bookmark transfer and progress in both directions.

| Surface | Static evidence | Adapter implication |
| --- | --- | --- |
| Library/catalog replication | `ksync/reader/couch/ReaderLibraryReplicator` uses user-scoped `READER_LIBRARY`, global reader flag and per-book eligibility | Treat catalog/progress separately from per-book annotations and assets |
| Reading progress and identity | `sdk/sync/ksync/reader/model/SyncMetadataModel`, mode 4, carries progress, readingStatus, uuid, idString, hashTag, path, cloud/storage identity and file/user-data sync states | Preserve native identities and opaque progress until location semantics are mapped; filenames alone are insufficient |
| Library containers | `SyncLibraryModel`, mode 3, carries hierarchy, query and status | Map native containers explicitly; do not confuse a book with a library |
| Bookmarks | `SyncBookmarkModel`, mode 2, carries documentId, quote, XPath, page number, position type/version and multiple position representations | Retain native anchor variants and status rather than flattening to a page number |
| Highlights/annotations | `SyncAnnotationModel` carries quote, note/linkNote, start/end positions and XPath, color and rectangles | Separate text anchor mapping from fixed-layout geometry |
| Reader handwritten notes | `SyncReaderNoteDocumentModel`, mode 7, carries commit, reader page-name map, stroke/background state | Handwriting requires its own dependency closure and native acknowledgement |
| Per-book data | `BaseReaderDocRequest.getDbId` derives a user-scoped book database; `SaveBookmarkRequest` stamps user/db and persists native models | Authorization must admit only the synthetic book and its own record scope |
| Book assets | `ksync/reader/file/BookSyncFile` compares native book records with OSS keys and creates file work; separate resource/shape/point file classes exist | Book bytes and annotation payloads require independent durable asset receipts |
| Wakeups | `StartReplicatorByMsgRecordAction` dispatches `READER_LIBRARY` to `KReaderSyncManager.sync` | A reading notification is not a notebook notification |
| Binder entry points | `ISyncService` exposes library/user-data push/pull, book upload/download, metadata query, library restart and scoped start methods | Probe read-only methods before qualifying narrow writes |

`StartLibraryReplicatorArgs.startSpecifyDoc` exists, but is not sufficient isolation
by itself: the inspected library filter admits mode-3 and mode-5 records before
checking the specified book IDs. Server authorization must independently deny
unrelated catalog/container records. A device request is not an access grant.

Next live gate: a locally authored EPUB with known chapters, stable text anchors
and a content hash, copied to both devices. Capture its native identities and
baseline progress/bookmark/annotation rows read-only. Establish a dedicated
server allowlist for this book, metadata/container dependencies and assets before
enabling reader sync. Qualify progress, bookmark, highlight and deletion round
trips separately; then add a synthetic PDF for geometry and handwriting. Reading
support remains unqualified, including cross-device book identity reconciliation.

Prepared fixture: `scripts/make-reader-fixture.py` creates an authored two-chapter,
240-paragraph EPUB with stable bookmark/highlight anchors and a content digest.
The host fixture passed ZIP CRC and XML parsing checks. It is now opened on both
devices; no reader replication channel has been granted.

## Live ordinary-app identity probe

The installed `reader_fixture_capture` action verifies the fixture's exact SHA-256
before querying only its known path through the exported reader content provider.
Both ordinary app UIDs read one metadata row and saved progress. The same bytes
and path produce matching `idString` and `hashTag`, but different native `uuid`
values. Those UUIDs stayed stable across repeated captures and account restoration.
Native pagination is 64 pages on Palma and 32 on Lumi with their current settings.
The adapter needs content identity plus per-device native identity aliases, and
must preserve opaque progress/anchor representations rather than normalize by
page count. This observation does not establish automatic native UUID reconciliation.

A bookmark created through Palma's native UI is readable by the ordinary app;
its `idString` refers to the metadata's native UUID, not the metadata `idString`.
The observed bookmark uses position version 4. The first probe's idString-only
query therefore missed it; the bounded probe now admits both known identity
variants. Both fixture metadata rows retain file/user-data sync status 0.
Reader sync was disabled during book creation; this is local access evidence,
not Couchbase reading replication. No highlights or handwritten reading
annotations have yet been qualified.

Private evidence: `qualification/lan/*-reader-opened-private.json`,
`*-reader-anchor-private.json`, `reader-identity-result.json` and reader screenshots.

## Scoped native replication follow-up

The live run confirms user-prefixed `user#uniqueId` Couchbase IDs and PFD JSON
publication through transactions 1 and 3. Current SaveReaderUserDataRequest uses
READER_LIBRARY for bookmark/annotation/reader-note records; the older per-book
SaveBookmarkRequest is a distinct interface. Scope authorization must check
documentId even when the record lives in the shared reader-library channel.

Incoming metadata creates two same-path aliases. NeoReader's native confirmation
then merges progress and attaches the imported bookmark using the Palma UUID on
Lumi. A native UI progress return also succeeds. The reader confirmation and
layout/automatic-sync choices are separate from CBL transport success; general
identity reconciliation and unattended application remain unqualified. Earlier
local-only evidence above is retained as its original checkpoint.
