# Native EPUB highlight and deletion qualification

Completed and restored, 2026-10-06. Only the hash-verified authored EPUB and its two enrolled
UUID aliases are writable. Private evidence is beneath
`artifacts/gateway-lab/qualification/reader-annotations/`.

Static native `DeleteAnnotationRequest` sets Annotation.status to 1 and updates
through the content data provider. `SyncAnnotationModel` carries status together
with native anchors, quote, styling and timestamps. `MergeAnnotationHandler`
converts incoming records and saves removed records as well as enabled records;
when fromDocId differs from toDocId it assigns new UUIDs and destination parents.
These source branches require validation on installed builds.

Live gates distinguish native UI creation, ksync publication, real Couchbase
record, receiver Couchbase Lite delivery, receiver SQLite application and rendered
reader state. Deletion must retain a causally traceable status-1 record, remove the
visible highlight, survive a repeated publication and reopen, and preserve all
original-library guards. Receiver reader sync is enabled before publication.

Initial request saved the annotation into source native CBL but did not upload
until a scoped replication start followed the save. Server and receiver CBL then
contained the enabled model with matching anchors/style. An initial receiver
prompt still referenced older metadata and did not import the annotation; the
next cold opening detected newly published native metadata. This intermediate
failure is retained rather than counted as successful application.

## Highlight application and native deletion

Palma's native long-press Mark action created one enabled annotation on the word
“bookmark” in paragraph 061. Its source record is positionVersion 6, modeType 1.
After source save, scoped start, fresh metadata publication and receiver
confirmation, Lumi stored the same annotation and rendered it on page 8/32;
Palma's source location was page 17/64. All captured fields matched except the
SQLite row ID and a locally derived legacy position field. UUID, parent,
versioned anchors, quote, timestamps, style and captured geometry were preserved.

Deletion used Palma's native Contents → Annotations → row trash → OK action.
The row remained with the same UUID and parent, status 1, and a newer updatedAt.
The native server revision and receiver CBL retained status 1; this is a logical
model tombstone, not evidence of a Couchbase `_deleted` revision. Lumi applied the removed record and no longer rendered the highlight. Repeating
publication and a cold reopen/confirmation retained exactly one removed record
and did not resurrect or duplicate the highlight.

## Adapter implications

Preserve logical status-1 records and their native revisions; do not convert them
to Couchbase hard deletions or discard them from a server snapshot. This qualified
path carries negative information through the ordinary annotation model. Anchors
are opaque native position-version 6 strings: native cross-layout rendering is
qualified for this EPUB, while conversion to Alexandria's anchor format remains
unqualified. Fresh user-data metadata and receiver confirmation remain separate
from replication. Scoping must preserve parents and each device's native aliases.

Annotation publication follows the pulled SyncAnnotationModel field map, including
JSON rectangle-array conversion from the SQLite string representation. This is
an unprivileged provider/Binder adapter; it does not fabricate annotation anchors
or edit original books. Requests still report requested, not completed; native
server and receiver captures supply completion evidence.

## Bookmark deletion

Palma's native corner toggle removed the bookmark created on Lumi during the
previous run. It retained the same UUID/parent with status 1. Native publication
(modeType 2), server replication, receiver CBL and receiver SQLite all retained
that logical tombstone. Every portable bookmark field matched; Lumi's page-9
bookmark indicator became empty. The two other fixture bookmarks remained
enabled. Repeat/cold-open non-resurrection was tested for the highlight; bookmark
repeat, stale-device reintroduction and concurrent edit/delete remain separate
gates. No general hard-delete, alias-delete or library-delete behavior is claimed.

## Evidence

Private captures include original-library baselines, native source records,
server revisions, receiver CBL contents, receiver application snapshots, rendered
screenshots, intermediate failures, scoped replay scripts and restoration checks.
Highlight visual removal was checked before and after repeat publication/cold
reopen. Immutable earlier runs remain separate. Original-library guards cover
identity/status/flags rather than a full original-byte audit.

## Restoration and validation

Both devices returned to exact original cluster maps, account states and sync
flags; lab markers/host overrides are absent. Original reader guards pass for
28 Palma and 14 Lumi rows; original notebook guards pass. Both fixture aliases
have file/user-data sync disabled. Each native store retains one removed annotation
and three bookmarks (two enabled, one removed). Gateway sync policy and original
principal grants were verified restored before shutting down lab services, with
persistent volumes retained. These checks are not a full original-library backup.

Android assemble and lint pass under the repository's compatible Java 17 toolchain;
all 25 Python tests pass. The first build invocation used the system Java 25 and
failed before compilation; switching to Java 17 resolved that tooling failure.
Private evidence includes build/test output, replay helpers and a SHA-256 manifest.

## Tombstone lifetime follow-up

2026-10-06 static follow-up: no age-based purge/expiry was found in the pulled
Lumi ksync reader package. `SaveReaderUserDataRequest` saves annotation/bookmark
models as ordinary CBL documents, without converting status 1 to database deletion.
Native merge handlers replace/deduplicate SQLite rows but save removed models
again. This supports continued retention, not a proven permanent-retention policy.
Live checks cover replication, repeat publication and reopen, not long-term aging.

A separate physical book purge path exists: `MergePullLibraryTreeRequest` handles
explicit deleteIdJson book IDs and invokes `ReaderDataProviderUtils.purgeDocFullData`,
which removes metadata, thumbnails, collections, doc data and recycle-bin files.
That is a static whole-document cleanup candidate, not an observed timer for
individual annotation/bookmark tombstones. Onyx server retention is still unknown.

Couchbase Sync Gateway 3.2 documents actual deletion tombstones as `_deleted: true`
revisions. Their purge policy must not be assumed to apply to live JSON documents
whose application field is status 1. See
https://docs.couchbase.com/sync-gateway/3.2/managing-tombstones.html . The adapter
should retain logical tombstones until a separately qualified retirement/resync
policy prevents stale-device resurrection; no cleanup interval is qualified yet.
