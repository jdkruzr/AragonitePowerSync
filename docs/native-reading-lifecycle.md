# Native reading delivery and subscription lifecycle

Qualification run, 2026-10-06 (wire trace crosses midnight UTC). Private evidence:
`artifacts/gateway-lab/qualification/reader-lifecycle/`; bounded Gateway trace:
`artifacts/gateway-lab/protocol-traces/reader_lifecycle/`.

Only Palma joined the LAN lab in this run. Note sync remained disabled. Lumi
remained on its original account/endpoint. A new authored cloud-only EPUB UUID
was enrolled, downloaded exactly, and opened at 17/64 with its native identity.
Original reader/notebook guards and fixture escrow govern restoration.

## Subscription and revision evidence

Login creates a continuous MESSAGE subscription even with note sync disabled.
The server sent both controlled MESSAGE revisions as BLIP rev requests on that
connection. Native handler execution is not established merely by a sent rev.
Reader subscriptions request `powersync-lab-READER_LIBRARY`; no subscription
requested either old book channel or the fresh UUID channel in the captured
phases. Static KSyncBundle.updateSyncGateway invokes startSync, which starts
MESSAGE. A process restart also recreated MESSAGE; absence of MESSAGE is not
a sufficient explanation of this run.

Transaction 17 scopes the library replication filter. It does not explicitly
start a per-book data replicator. The probe method/report was corrected to
state READER_LIBRARY and bookDataReplicatorStartRequested=false. Transaction 16
is the native library restart control. Both remain bounded by enrolled UUIDs
and the exact managed authored EPUB in the probe; server policy admits only
synthetic fixtures. A restart itself does not transmit a new scope; this run
established scoped transaction 17 in the same ksync process beforehand.

## Controls and semantics

Distinct title and bookmark updates did not apply in background/HOME phases
(approximately 33 seconds each). A subsequent process restart produced a
native metadata push replacing the server title marker with the local title.
The durable revision journal records initial metadata, BG marker, FG marker,
then the newer native rewrite. A missing title marker alone is therefore not
proof of missing receipt. Local publication and server delivery must be
recorded separately, including revision identities.

A single-book ReaderSyncService request timed out while the native new-data
prompt remained pending. After ordinary UI confirmation with local layout
retained and Auto sync enabled only for the disposable book, the same request
completed. This associates the pending prompt with the timeout; it does not
prove a universal service blocking rule.

Two native user-data copies in book scope yielded no local query models and no
sent revision lines. Moving those exact two documents to READER_LIBRARY, with
new revisions and MESSAGE, still did not deliver them during the short control.
Explicit transaction-16 library restart then returned two local query models
(including one bookmark); the native single-book pull reported new annotation
data and inserted one native bookmark. This qualifies library-scope delivery
and native application with explicit orchestration on Palma. It does not
by itself resolve the earlier Lumi scope attribution. The subsequent matched
[scope comparison](native-reading-scope-comparison.md) corrects that earlier
claim: the Lumi capture contained old library-scoped models, not new book-scope
revisions.

A final distinct bookmark quote revision separates MESSAGE-only wake from
explicit library restart and observes provider application without another
ReaderSyncService application request. Results are captured separately in
`unattended-result.json`.

Final update result: the MESSAGE-only provider check remained unchanged.
Transaction-16 restart delivered the distinct bookmark quote into local CBL;
the native provider remained unchanged in background, HOME foreground, and
normal cold-open observations (approximately eight seconds for each restart
phase and five seconds for reopen). A subsequent explicit single-book pull
reported new annotation data and applied the exact quote marker. The resulting
bookmark UUID and nine portable fields match the current published model.
This establishes a receipt-versus-application distinction with a new revision,
not merely pre-existing row presence. Short observation windows do not prove
that a longer unattended interval could never apply the revision.

## Alexandria adapter implication

A native integration should retain separate delivery and application journal
states. The demonstrated fallback is scoped library setup, native library
restart, read-only receipt verification, and native single-book application.
MESSAGE remains a notification mechanism to qualify; do not use it as an
application acknowledgement. Gate application completion on callback and
provider identity/content evidence. Record native local metadata rewrites
as independent publication/conflict events, rather than treating server
metadata as authoritative by default. Per-book Auto sync UI is not itself
proof of unattended cloud application.

Cleanup caught re-import of two previously deleted disposable cloud metadata
rows from the earlier Lumi/Palma fresh-cloud runs. All 28 original Palma rows
matched their saved full JSON contents exactly; only the two archived synthetic
UUIDs at their exact authored managed paths were extra. Those rows and any
parent-scoped synthetic bookmark/annotation rows were journaled and removed
with original sync stopped, before account/endpoint restoration. Any re-downloaded managed files were captured, verified as the exact 3527-byte
authored EPUB by SHA-256, and removed. Local metadata deletion does not clear retained CBL or
server library state; subsequent reconciliation can re-import such records.
This observation concerns disposable local row removal, not tombstone purging
or resurrection of a native logical tombstone.

## Restoration and validation

Original accounts, exact endpoint/cluster maps and global sync flags restored
on both devices. Reader guards pass (28 Palma / 14 Lumi); notebook preservation
and authored Downloads EPUB hash pass. Fresh/previously replayed disposable
managed rows/files removed; retained original test aliases disabled. Server
policy/grants restored. Build and lint pass using the repository's JDK 17;
27 Python tests pass. The native restart and application controls were run on
the installed updated APK. Final restoration and service shutdown receipts
and evidence checksums are stored beside the private captures.
