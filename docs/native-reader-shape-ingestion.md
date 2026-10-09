# Native reader shape ingestion

Follow-up: [the real “Hi.” write-back control](native-reader-ink-writeback.md).
The staged state described below is historical; see the follow-up restoration receipts.

2026-10-07. Private evidence: `artifacts/gateway-lab/qualification/reader-shape-import/`.
Follow-up to [PDF export and transport](native-reader-ink-export.md).

## Qualified result

Palma's native importer now imports one shape and reports success/newData=true.
Its subsequent ordinary-app native .booxc export contains one shape, the original
shape/document/point revision IDs, identical bounds and point save type, and the
source 11152-byte point file byte-for-byte (all 689 pressure points). Shape protobuf
size changes from source 609 to peer 638 bytes; differing fields are OBJECTID,
INFOREVISIONID and UPDATEDAT. This is native shape ingestion, not only visible
PDF appearance. A repeat native application returns success/newData=false.
Native peer editing and bidirectional ink write-back remain pending a real edit.

## Cause and controlled repair

The prior importer log says `no pageFileList`; the exact expected cache directory
`.ksync/document/<owned book>/shape` was empty. The source's ZIP is valid and has
one 608-byte shape payload. Import rejection and transform failure were not the
initial cause.

Static AddReaderDocCommitPointRequest constructs reader commitType=4,
commitStatus=1 in READER_LIBRARY. Our fixture admission rule incorrectly admitted
commitType=1. SavePullReaderLibraryRecordRequest collects document IDs from
received commit points, and StartPullReaderReplicatorRequest schedules enabled
book asset replication. The corrected policy admits native type 4 only with the
fixture parent, exact library/user/CBL identity and channel grant. Regression
controls deny type 1, wrong status, missing grant, wrong ID/library and cross-book
parents. Seven reader-policy tests and nine OSS tests pass; fresh Android build
and lint pass.

A server-authored type-4 commit control, followed by the proper scoped library
start, caused native ksync to fetch the 678-byte shape ZIP. Downloaded bytes match
the previously stored server ZIP exactly; no ADB file injection was used. The
native task log lists a shape download. ReaderSyncService then imports the ZIP:
`update sync shape count: 1`. This qualifies the type-4 delivery trigger, not
native source commit publication: the control commit was explicitly server-authored.
Source-side type-4 emission and a full automatic pen-edit round trip remain gates.

Binder transaction 16 restartLibraryReplicator ignores SyncIntentArgs. Transaction
17 startLibraryReplicator parses its arguments; the helper uses startAllDoc=false
and one fixture ID. Correct earlier wording that transaction-16 restart itself
was scoped: server admission restricted stored writes, but native arguments did
not constrain that restart. Do not treat native request receipts as completion.

## Identity and preservation

Only Palma is rejoined for this case; Lumi remains on its original cluster/account
and flags. Current Palma note sync is false, reader sync true, and the lab server
admits the one authored PDF plus its exact assets. Full pre-run original reader
metadata, including the retained disabled blank row, and notebook guards were
captured. Native rejoin recreated a cloud fixture metadata alias at the owned
Downloads PDF path. The stage helper initially refused an existing identity before
writing; after inspection it relocates only that exact cloud fixture alias to its
fixed managed PDF path using an ordinary app UID. The preexisting local Downloads
fixture and personal library rows are not the relocation target.

The Palma is intentionally left at the open fixture while a real edit is requested.
Do not run cleanup or restore its account mid-edit. On completion restore the
pre-run server policy/grants/asset allowlists, remove only the owned managed alias,
restore the original endpoint/account/flags, and compare every pre-run reader row.
The previously unexplained blank disabled row must be retained, not rebaselined
away. Raw exports/configuration/logs remain private. Earlier 212-file evidence
stays sealed and is not rewritten.

## Alexandria consequences

Reader metadata (mode 4), ink configuration (mode 7), OSS dependencies and reader
commit point type 4 are separate contracts. A server-driven update must publish
its commit trigger only after durable dependencies exist; stock Sync Gateway does
not invent this vendor trigger. Accept native source triggers under the proper
policy instead of mistaking type-1 notebook commits for reader commits. Persist
receipt, native import and user-edit confirmation separately. Preserve pressure
files exactly; native local object bookkeeping may legitimately change.
