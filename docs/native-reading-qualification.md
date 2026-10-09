# Native reading qualification

2026-10-06: native reading metadata reached our real Couchbase stack from both
BOOX devices. A Palma bookmark reached the Lumi's native database and rendered
bookmark indicator. Reading progress applied in both directions, through
NeoReader's own new-data confirmation, with each device's layout preserved.
These are scoped synthetic EPUB results, not complete reading-source qualification.

## Interfaces and server contract

The ordinary app hash-verifies `PowerSync-reader-fixture.epub` before any reader
operation. It publishes one `SyncMetadataModel` using native
`ISyncService.pushReaderLibrary` (transaction 1), `SYNC_DOCUMENT` and empty
library/container lists. The payload must be JSON supplied through a read-only
`ParcelFileDescriptor`; the publication methods do not consume the content
string. A regular-file descriptor worked with native MemoryFileUtils. Bookmarks
use `pushReaderUserData` (transaction 3) with a UserDataBean payload through the
same descriptor mechanism. Scoped start (transaction 17) instead consumes a
content-string JSON containing `startAllDoc:false` and the two admitted book UUIDs.
A successful one-way transaction is recorded as requested, not completed.

Native reading Couchbase IDs are `user#uniqueId`. REST queries must encode the
`#`; the initial bare-UUID policy rejected native metadata. Correcting that exact
namespace rule allowed both native publications. On these builds,
SaveReaderUserDataRequest stamps bookmark/annotation/reader-note records with
`powersync-lab-READER_LIBRARY`, rather than a per-book database. The older
SaveBookmarkRequest is a separate per-book path and must not be conflated with it.

The independent Sync Gateway policy admits only mode-4 metadata for the two
synthetic UUIDs, mode-1/2/7 records whose documentId names one of those UUIDs,
correlated commits, and type-6 READER_LIBRARY MESSAGE records. Library-scoped
records must use the native user-prefixed document ID. Existing note restrictions
remain in place; unrelated container/library modes remain denied. Each channel
also requires an independent principal grant. Four executed policy tests cover
namespace, unrelated books, containers, wrong users, missing grants, cross-book
scope, and tombstones. A correctly namespaced unrelated book was rejected by the
real Gateway with HTTP 403.

## Observed native application

| Gate | Observed result | Boundary |
| --- | --- | --- |
| Metadata publication | Native ksync on both devices published mode-4 reading records | Exact authored book only; no reading OSS assets admitted |
| Bookmark publication | Ordinary Palma app submitted the native UI-created bookmark; ksync published mode 2 into READER_LIBRARY | One source bookmark, position version 4 |
| Bookmark application | After a READER_LIBRARY notification and accepting NeoReader's new-data prompt, Lumi had the bookmark and showed a filled indicator | UUID, book UUID, quote and all portable native fields equal; only SQLite `id` differs; passive DB application was not observed before opening/confirmation |
| Forward progress | Palma 10/64 reached Couchbase; Lumi prompted for page 5 and applied 5/32 | Layout synchronization unchecked; native pagination/location handling, not an Alexandria anchor conversion |
| Return progress | Lumi native UI published 8/32 without an app push; Palma prompted for page 15 and applied 15/64 | Reader notification supplied; receiver confirmation retained; page boundaries differ |

The source at Palma page 10 includes chapter-one paragraphs 34–38, while the Lumi
page 5 includes 32–39. The return positions similarly overlap, rather than having
identical first text offsets. This establishes native progress application and
layout retention, not exact cross-renderer location conversion. Do not scale a
page number by the page count and call it a portable anchor.

## Identity reconciliation

Identical EPUB bytes and paths initially had matching idString/hashTag but
independent native UUIDs. Incoming metadata created two native Metadata rows at
the same synthetic path on each device. The native confirmation/merge then used
the Palma book UUID on the Lumi; the imported bookmark retained that identity.
Both aliases remained in bounded diagnostic captures. This qualifies one native
merge path, not general automatic UUID deduplication or catalog reconciliation.

The harness now captures both fixture aliases, queries bookmarks/annotations
through their known identity variants, and selects the immutable original local
UUID baseline explicitly for publications. It refuses a missing original UUID,
unadmitted scope or more than two path aliases. It does not choose a first row
silently or rewrite existing book UUIDs.

## Preservation and remaining gates

Existing books were excluded from the server allowlist and fixture mutations.
An independent guard verified identity/status/file and user-data flags for 28
original reader metadata rows on Palma and 14 on Lumi. This is a scoped metadata
guard, not a byte-complete audit of all reader content. Original notebook guards
are checked independently during teardown. Both synthetic aliases have their
sync flags restored to zero; aliases and test bookmarks are retained as evidence.

Reading OSS/book download, highlights, bookmark deletion, reverse bookmark
creation/publication, PDF geometry, reader handwriting and general identity
reconciliation remain unqualified. Lumi bookmark-corner tap attempts did not
produce an additional native bookmark row; reverse progress succeeded independently.
The native automatic-sync checkbox stayed unchecked. This run does not establish
unattended reader application, its conflict policy or exact anchor translation.

Private evidence lives beneath `artifacts/gateway-lab/qualification/reading/`:
per-device baseline and alias captures, native server metadata/bookmark records,
bookmark/progress transfer receipts, negative authorization receipts, screenshots,
restoration receipts, build/tests, replay helpers and a SHA-256 evidence manifest.
Raw accounts/sessions and device/book IDs stay private. See the
[results index](experiment-results.md) and [static source map](native-reading-source-map.md).

Closing checkpoint: original endpoint maps and account states, global sync flags,
marker/host-override removal and both original-library guards pass after settling.
Gateway sync policy and principal grants were verified restored. Lab services
are stopped with database volumes retained. The signed debug APK was upgraded
in place on both devices; build/lint and all 25 Python tests pass.

Follow-up correction: [identity and reverse bookmark qualification](native-reading-identity.md)
shows that selecting the Palma UUID did not prove a UUID rewrite: both original
rows survive and creation-time ordering switches the opened alias. Reverse native
bookmark creation/publication/application now passes; the earlier failed corner
taps occurred with finger touch disabled.
