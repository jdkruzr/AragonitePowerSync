# Native cloud-book identity qualification

Completed receiver qualification, 2026-10-06. Private evidence:
`artifacts/gateway-lab/qualification/reader-cloud-open/`.

The previous byte-transfer run used existing reader aliases; direct VIEW of a
managed EPUB created a new identity. This run adds a fresh cloud-only synthetic
UUID, using the same exact authored EPUB, so no pre-existing local row owns that
UUID. Original metadata aliases and all original books remain untouched. A
bounded signed native download precedes any file open. A new ordinary-app probe
calls the reader's exported `ReaderSyncService`, not the ksync transfer service.

Static ABI: `IReaderSyncService` transaction 3 is `syncDocument`; its
`ReaderSyncIntentArgs` request type 10002 invokes `SyncDocumentFromLibraryAction`
and `SyncDocumentByPathAction`. This queries cloud metadata, merges the selected
book and pulls document data. Transaction 1/2 sync the entire library; the probe
does not invoke them. The fixture-only probe requires lab cluster, enrolled UUID,
SHA-verified private escrow and exact managed EPUB path/bytes. It requests pull
and disables requested push. Binder acceptance is not completion evidence.

The fresh cloud metadata and asset are server-created copies of the verified
synthetic models/bytes, journaled separately from native source publication.
This experiment qualifies receiver behavior, not native UUID creation on upload.
Fresh identity reading data must be separately re-parented and qualified before
claiming a complete reading-state restore. Account/endpoint/library preservation
and exact synthetic-file restoration apply throughout.

Single-book request observations: omitting its callback Binder triggers a native
NullPointerException in the installed reader service; Binder acceptance did not
mean the request ran. The corrected callback probe reports native RESULT_SUCCESS
(1), pull requested, push not requested. However, the managed file retains its
VIEW-generated identity. An empty metadata placeholder also appeared with only
UUID, row ID and migration extraInfo populated. A private exact signature
comparison proved that removing that one novel row restores the original
14-row guard digest; its full row was journaled before exact deletion. No
original reader row was changed in that comparison.

The broader follow-up explicitly invokes exported transaction 2 (native library
delta pull/merge/push), guarded by the enrolled managed fixture and exact bytes.
Unlike the single-book request, this operation is library-wide. Before invoking
it, the exact original guard passed and a full private metadata snapshot was
taken. Gateway policy continues to admit only the enrolled synthetic reading
UUIDs; original-book publications are refused. The initial delta returned
SYNC_NO_DATA and created no fresh cloud UUID row. The receiver's scoped native
replicator is being restarted with the expanded three-UUID scope before a fresh
server metadata revision and another import attempt.

Clean native cloud import now passes its identity/path gate. After restarting
Lumi's ksync with the expanded three-UUID scope and publishing a fresh metadata
revision, native reader library delta import created the fresh cloud UUID at
`.ksync/powersync-lab/reader/<freshUUID>/book/<freshUUID>.epub`. The earlier
SYNC_NO_DATA attempt was made before that receiver replicator restart; it does
not demonstrate a failure of the converter. The known two local aliases were
retained. Import happened before the final file open; no provider-written
UUID/path mapping was used to obtain it.

Reading-state follow-up stages one copied native mode-7 user-data model and one
active native mode-2 bookmark under the fresh cloud book identity, with new
record IDs, through the server. This is journaled server-originated synthetic
publication, not a new native user edit. A metadata revision/MESSAGE and the
single-book library sync precede native open; receiver rows and screen are the
application evidence.

Receiver application passes on Lumi. Opening the natively imported managed EPUB
keeps the fresh cloud UUID (one metadata row) and renders page 8/32 from the
source's 17/64 metadata/options, rather than starting at page 1. This is native
layout-relative restoration, not Alexandria anchor translation.

The fresh bookmark/user-data copies initially placed in READER_LIBRARY were
not visible to the first completed pull (zero models). A later pull, after the
server records were moved into book scope, returned two models—but review of
that capture shows READER_LIBRARY scope and the original publication timestamps.
It did not prove delivery of the book-scoped revisions. The matched
[scope comparison](native-reading-scope-comparison.md) reproduces this cached
library-data effect on both devices and supersedes the earlier scope inference.
After publishing metadata/options with
`user_doc_data_update_time` and requesting single-book library sync, the callback
reported RESULT_SUCCESS, newAnnotationDataPulled=true, and no identity change.
The ordinary app read one native bookmark associated with the cloud UUID;
its UUID, portable fields and versioned position fields match the published
model. Cold native reopen retains the cloud UUID and visibly renders the filled
bookmark indicator on page 8/32. Both an open-page text screenshot and private
native model capture establish application.

Native application above remains qualified, but its data-scope attribution was
incorrect: retained library-scoped models were applied. Exact revision marker,
scope and timestamp must accompany row-count/application evidence. Both tested
firmwares receive fresh READER_LIBRARY revisions through library restart;
book-scoped writes in the matched sequence do not replace the cached models.

A full clean factory/account library join, Palma as the fresh-import receiver,
thumbnail transfer, and automatic foreground/background trigger behavior remain
unqualified. The library import was explicitly requested through an ordinary-app
Binder probe; that is not evidence of a purely unattended stock-launcher flow.

Restoration passes: exact original cluster/account/global note and reader flags
on both devices, original reader guards (28 Palma / 14 Lumi), original notebook
guards, disabled old fixture aliases, absent marker/host override and restored
Gateway policy/principal grants. The original receiver Downloads EPUB is restored
with exact SHA; the newly imported managed file, metadata and native bookmark
are removed after private capture. Original aliases remain. Lab-only cloud
models and assets remain in retained server state as evidence, not production
data.

Android debug build/lint and 27 Python tests pass; NativeInk reports 41 integrity
checks. The suite initially hit a clock-boundary failure in the cached-session
test: identical session data had a one-second difference in calculated expiry.
The test now verifies stable session payload rather than equal moving expiry
timestamps; no session implementation change was needed. New reader APIs were
verified on device, including callback completion and scoped native captures.

Lab services are stopped and all five listener ports are closed; persistent
Couchbase state is retained. The SHA-256 evidence manifest covers 93 private
files. Captured helpers are run journals requiring fresh baselines and scope
setup, not unattended idempotent replay scripts.
