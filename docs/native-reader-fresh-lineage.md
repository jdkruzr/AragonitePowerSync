# Fresh reader lineage and configuration conflict audit

Follow-up: [real pen trial and cache timing](native-reader-fresh-ink-cache-timing.md)
qualifies native delivery and controlled full-data recovery; automatic application
misses the cache and remains unqualified.

2026-10-07. Evidence: `artifacts/gateway-lab/qualification/reader-fresh-lineage/`.
The eight-shape case remains sealed and unchanged. This is a new empty-book baseline;
real native pen synchronization is pending a user stroke.

## Conflict evidence and limits

The old configuration's admin `_raw` metadata and `_revisions` were captured read-only.
The accepted server revision tree contains seven revisions in one linear chain, with
one leaf. It has no accepted competing leaf containing the rejected fresh edit.
The winning revision is the prior explicit fixture-only configuration-copy control.
This independently confirms why server changes/history alone cannot recover the
rejected incoming branch. It does not reveal that branch's revision ID/parent chain
or explain why the client did not reconcile it. Existing CBL 10409 logs remain the
live rejection evidence; do not label this a solved default-resolver defect.

Installed `ReaderLibraryReplicator` registers push/pull filters but no custom
conflict resolver. `BaseReplicator` defaults to PUSH_AND_PULL, and `CouchHolder`
passes its resolver to `ReplicatorConfiguration`; the inspected reading constructor
leaves it null. This is distinct from the separate `updatedAt` resolver helper.

Pinned public LiteCore 3.2.2 `Replicator/Pusher.cc` has three relevant paths:
`handleProposedChangeResponse` (around line 346) handles 409; the ancestor retry
path (around line 403) requires a usable server ancestor or waits for remote-ancestor
change; `_docRemoteAncestorChanged` (around line 450) either retries against an
ancestor or reports `conflicts with server document`. `_connectionClosed` (around
line 504) also reports that exact message for queued unresolved conflicts. Thus the
installed error text alone does not prove which path ran, whether the resolver was
invoked, or whether restarting replication could repair the lineage. The public
source is release-family evidence, not the exact embedded Enterprise native build;
see [source provenance](static-sync-semantics.md#versions-and-source-provenance).

## New fixture baseline

`/storage/emulated/0/Download/PowerSync-reader-clean-v2.pdf` is a separately authored
2340-byte PDF, SHA-256
`37962785464fb87d6ae1fedf7850aca2ea01dc4245af43ce49bb3e4c99b58086`.
The source retains its native Metadata UUID `ea6ad9be04304a399583fe557a21f63e`.
Palma has metadata ID 143; Lumi has ID 141. Lumi's one newly authored row was enrolled
under the source UUID before publication; its former UUID and full row were journaled.
No managed-file alias was inserted. Both use the one authored Download path.

Native publication created fresh mode-4 book and mode-7 configuration documents at
server revision 1; no host-authored document/commit/configuration/MESSAGE write was
used to establish this book lineage. The mode-7 ID is
`powersync-lab#e61b286c49594b1686b4bca987a8274b`, timestamp 1791410286497.
Both native userdata queries agree on configuration identity, parent and timestamp.
Both native ink exports contain zero shapes. Initial source sync callback reported
result 0 with no data; native logs and server receipts independently establish the
accepted metadata/configuration publication. Receiver baseline application reports
success, `newData=false`. Do not silently upgrade the source callback to success.

The old eight-shape book's per-device user-data eligibility was disabled for this
trial and journaled through preceding settings/provider evidence; its ink stays intact.
Gateway and OSS policies admit the exact additional book, its strict dynamic native
shape/point keys and per-device channels. Previous policy/allowlist/principal contents
are saved for restoration. Global note sync stays disabled; reader sync is enabled.
All pre-existing reader guard rows and notebook guards pass. Added fixture rows are
not original library rows and do not redefine the original baseline.

## Application setting and instrumentation

The inspected `ShowPullTipDialogAction` (around line 190) gates a pull confirmation
dialog on `MetadataExtraInfo.isAutoSync()`. When true it logs `auto sync without tip
dialog`; it does not independently guarantee background application. An ordinary-UID
helper sets that property true only on the one SHA-verified fixture row, preserving
all other extraInfo properties and journaling their old value. Both readers are open
on the blank first page to give the native reader lifecycle an application opportunity.
No one-shot full pull was used. `pullFullUserDataFromSync` remains false.

A separate hash-bound `NativeFreshReaderPdfLab` and six reader actions plus one
fixture auto-sync action avoid repurposing the old helper's guarded UUID/file or
private receipt names. Android assemble/lint pass; both receive certificate-matched
in-place upgrades. Runtime capture/export/query/push/pull and ordinary-UID auto-sync
configuration are exercised. The helper remains experimental lab code, not an
Alexandria production adapter.

`user-edit-baseline.json` and `server-before-user-edit-private.json` bound the next
passive trial. After the stroke, capture source/receiver logs, server changes, OSS
PUT/GET receipts, configuration revisions, commit, MESSAGE and native ink exports
before any explicit source retry, receiver pull/start/reset, or host server refresh.
Receipt, configuration acceptance and reader application remain separate milestones.

## Handoff

Both devices are open on the new blank PDF; the user was asked for one real Palma
pen squiggle and Back to save, leaving Lumi untouched. Lab endpoints and reader-only
flags are intentionally staged; original endpoint restoration remains pending.
Bounded trace `reader_fresh_lineage` is live for at most 900 seconds; if it expires
before the edit, restart a new bounded trace and check sessions without redefining
an already edited baseline. The initial evidence files are checksummed separately
from the live trace; preserve the initial manifest while adding later results.
