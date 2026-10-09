# Native reader identity and reverse bookmark qualification

Completed and restored, 2026-10-06. Only the authored, hash-verified EPUB is writable.
Previous [reading qualification](native-reading-qualification.md) retained two
same-path metadata aliases per device. Independent original-reader and notebook
guards are required before and after this run. Evidence is private beneath
`artifacts/gateway-lab/qualification/reader-identity/`.

## Static identity decision rules

Pulled Lumi NeoReader `MergeMetadataRequest` first considers opened cloud metadata
with the matching path, ordered by createdAt. Its candidate branches are:

| Condition | Native candidate behavior |
| --- | --- |
| Local createdAt is earlier than the first cloud match | Retain local UUID/create time; merge cloud progress/data into it |
| Earliest matching cloud UUID equals local UUID | Retain UUID; merge according to saved pull time and user-data update time |
| Earliest cloud UUID differs and is absent from native DB | Adopt the cloud UUID |
| Earliest cloud UUID differs but already exists in native DB | Generate a fresh UUID; merge/remap into it |
| No preceding path conflict, same UUID but different path | Generate a fresh UUID for the local record |

`MergePullDocDataRequest` filters models by fromDocId, then merges into toDocId.
`MergeBookmarkHandler` assigns a fresh bookmark UUID and the destination idString
when those book IDs differ. The adapter must journal both mappings. These source
rules are candidates until observed on the installed builds; matching content
hashes do not by themselves imply native UUID equality.

## Live results

Lumi created two additional bookmarks through the native touch control. Its
finger-touch setting initially prevented corner taps; it was temporarily enabled
and then restored. Native publication delivered both bookmarks to the real
Couchbase server. After the final metadata/user-data publication and receiver
confirmation, Palma held all three bookmarks. UUIDs, parent IDs, anchors, text,
timestamps and all other portable fields matched Lumi exactly; SQLite row IDs
were excluded from comparison. Reverse bookmark creation/publication/application
is qualified for this EPUB and these installed builds.

One intermediate publication happened while Palma reader sync was disabled.
Enabling it and sending a MESSAGE did not deliver that bookmark into Palma's
native Couchbase Lite store. A full-user-data Binder query distinguished this
transport absence from NeoReader application failure. Updating only that
synthetic bookmark's updatedAt and natively publishing a new revision made it
arrive. A subsequent clean publication with the receiver enabled, fresh metadata
and MESSAGE then applied both new bookmarks. Filter/checkpoint interaction is a
working explanation, not a universal Couchbase Lite replay guarantee. The adapter
must verify receiver state and revision delivery independently from native reader
application, and must not treat a MESSAGE as a replay of a rejected revision.

Two same-path metadata rows remained, with distinct SQLite IDs, UUIDs, creation
times and BaseOptions backend.doc_id values. With reader sync disabled and ksync
stopped, moving only Lumi's original alias creation time ahead of the Palma alias
made a cold native opening select page 1/32 instead of page 9/32. Restoring both
exact original creation times and cold opening again selected page 9/32. Thus the
observed identity transition is alias selection; no UUID rewrite or deduplication
was demonstrated. RemoteDataProvider's earliest-entry grouping agrees with this
observation. LocalDataProvider's unordered path query is a separate code path.

The integration needs a canonical content identity plus all native aliases and
device provenance. Keep metadata/bookmark parent IDs and native revisions intact.
The static fresh-UUID/remap branches above, alias deletion, highlights, reading
assets and cross-format anchor translation remain unqualified.

## Evidence and harness

Private evidence: `artifacts/gateway-lab/qualification/reader-identity/` contains
failed intermediate captures as well as successful portable-field comparison,
native CBL query snapshots, Couchbase revisions, A/B/A screenshots, timestamps,
original-library guards and restoration receipts. Synthetic timestamp mutations
were journaled before writing and restored through the ordinary app/provider.

New app actions support explicit enrolled-alias selection, read-only native full
user-data queries, fixture-only bookmark revision refresh and reversible fixture
creation-time ordering. Every mutation requires the custom lab endpoint, exact
EPUB hash and enrolled UUID scope. Creation-time changes require reader sync off.

## Restoration and validation

Both devices returned to their exact original cluster maps, sync flags and account
states (Palma logged out, Lumi original account). Lab markers and host overrides
are absent. Original reader identity/status/flag guards pass for 28 Palma and 14
Lumi rows, and original notebook guards pass. Both fixture aliases have file and
user-data sync disabled. The identity test restored exact creation timestamps
and retained original SQLite IDs. Gateway policy and original principal grants
were checked before stopping lab services; persistent volumes remain intact.

All 25 Python tests pass. Android assemble and lint pass. Private scripts, build
output and a SHA-256 evidence manifest are retained with the run. These guards
are not a full byte audit or complete backup of the original libraries.
