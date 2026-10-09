# Native reading offline conflict qualification

Qualification run: 2026-10-07 local time. Private evidence:
`artifacts/gateway-lab/qualification/reader-conflict/`; bounded Gateway trace:
`artifacts/gateway-lab/protocol-traces/reader_conflict/`.

Both devices use one fresh, shared cloud-only book UUID and exact native signed
EPUB downloads. Original Downloads EPUBs are in verified escrow. Initial prompts
were confirmed with local layout retained and Auto sync unchecked; note sync is
false. Original reader rows and notebook guards were captured before enrollment.

Each round uses a new annotation UUID, seeded as the same server revision on both
devices and applied before disconnection. The replication proxy is stopped;
ordinary app provider edits change the note/status/timestamp of only the enrolled
annotation. Native ISyncService transaction 3 saves each branch into that device's
CBL database. Read-only transaction 11 must show each exact local branch, while
the server revision remains unchanged. These are actual divergent local saves,
not two sequential server PUTs. The account facade and local book stay available.

After both branches are saved, both ksync processes are stopped. Proxy restoration
plus scoped native start/restart reconnects one device at a time, in the specified
order. Capture server revisions/raw revision metadata after each connection;
then capture both local CBL models and explicitly apply them through native
ReaderSyncService. Callback completion, provider state and visual rendering are
separate observations. No hard Couchbase delete is used: status 1 is a logical
annotation deletion.

Completed rounds: comments with Palma first; comments with Lumi first; Palma edit
versus later Lumi deletion; Palma deletion versus later Lumi edit. Publication
order and timestamp order are recorded separately. A fresh ID per round avoids
carrying a prior round's conflict/tombstone history into the next baseline.

Static candidate: BaseReplicator.handleConflictResolver compares updatedAt and
chooses remote on ties. Its mere presence does not prove that the installed
reader replicator registers it. Live branch histories and convergence must decide
which behavior this run actually exercises.

Limitations: provider edits and explicit native CBL saves qualify this adapter
path, not native editor dirty-state/save scheduling. No generic notebook conflict
policy, full offline device outage or arbitrary anchor translation is established.


## Results

In every round both local CBL branches were independently verified before the
proxy reopened, and the server revision remained unchanged while disconnected.
The wire trace shows two distinct generation-2 revision proposals with the same
generation-1 parent. These are sibling branches rather than sequential edits.

| Round | Local save order | Reconnect order | Final server, both CBL databases and native provider state |
| --- | --- | --- | --- |
| Competing comments A | Palma then Lumi | Palma then Lumi | Lumi comment, enabled |
| Competing comments B | Palma then Lumi | Lumi then Palma | Lumi comment, enabled |
| Edit versus deletion | Palma edit then Lumi deletion | Palma then Lumi | Older Palma edit, enabled; Lumi deletion lost |
| Deletion versus edit | Palma deletion then Lumi edit | Palma then Lumi | Older Palma deletion, removed; Lumi edit lost |

Both comment rounds selected the later-written body despite reversing reconnect
order. Both deletion rounds selected the older-written Palma body, once enabled
and once removed. Thus neither newest body updatedAt, latest reconnect, nor
always-delete-wins explains all four results. The enabled winner actually
replaced Lumi's newer local status-1 row in the native provider, demonstrating
logical resurrection in this adapter-driven conflict path.

Raw Gateway revision histories, proposed sibling IDs, local branch captures and
final native rows are preserved. All four winning bodies match the lexicographically
larger proposed sibling revision ID. Some losing bodies were not sent as full rev
payloads after their proposal; their mapping uses the explicitly staged reconnect
order and locally captured branch content. This agreement is consistent with the
CBL default revision-ID tie-break. It does not independently measure C4 revision
timestamps or prove the installed resolver call stack.

The pulled Java DefaultConflictResolver compares Document.compareAge, then revision
IDs on ties. Document.compareAge compares getTimestamp(), which delegates to the
native C4 document timestamp; it does not read the application updatedAt property.
ReaderLibraryReplicator's inspected constructor installs push/pull filters but no
custom conflict resolver. BaseReplicator has a separate updatedAt resolver helper;
its explicit registration was found for notebook shape replication, not this
reader library path. Do not generalize this reading result to notebook shapes.
No dedicated 409 error-response proof was extracted; branch proposals and final
histories establish the divergent revision path without assuming such a response.

## Application and presentation

Both providers reached the winning note/status/timestamp for all four annotations,
with one row per selected UUID. In the edit-versus-delete round Palma's explicit
application repeatedly timed out even after reader/process restarts; it already
held the winning older edit. Its observed provider state is not a successful
callback acknowledgement. Lumi's independent application returned success and
changed its local removed row back to the enabled winner. The fourth round's
fresh baseline and final application subsequently succeeded on both devices.
The cause of the earlier timeout remains unresolved.

A delayed new-data prompt appeared on Palma during the final cold-open/UI check.
It was confirmed with layout sync and Auto sync left unchecked. That later prompt
is not evidence that it caused the earlier callback timeouts.

Final native annotation-list screenshots show four enabled entries on each device:
the initial control, both independent comment-round winners, and the enabled
edit-versus-delete winner. The fourth round's tombstoned annotation is absent from
the enabled UI list and retained as status 1 in provider/CBL/server captures.
The two equal Lumi comment labels refer to two distinct round UUIDs, not duplicates
of one record. List presentation is qualified here; inline highlight painting and
anchor/page-label fidelity remain separate gates. The lists label these copied
annotations as page 1 despite the current reader positions 17/64 and 8/32.

## Facade implications

Couchbase convergence is not a domain policy for preserving user intent. Keep
logical status tombstones as native model data; do not assume the CBL hard-delete
rule protects them. Never implement a PostgreSQL projection with an independent
newest-updatedAt winner and assume it will agree with native replication.

The source adapter needs an explicit decision about accepting native CBL winners
versus enforcing an Alexandria domain policy. Strong deletion protection or
recoverable losing edits requires a durable operation/branch record before native
conflict resolution can discard an intent. A server changes-feed journal alone
must not be advertised as a complete history of all device edits: rejected or
locally discarded branches may never appear as accepted server revisions. This
run's local branch journal makes the losses observable; a production mechanism
for preserving native editor intents remains to be designed and qualified.

Validation: Android build and lint passed with JDK 17; 27 existing Python tests
passed. The updated probe was installed in place on both devices and its guarded
ordinary-UID edit/native-CBL-save operations were exercised successfully. Only
note/status/updatedAt were changed on the enrolled disposable annotations.


## Restoration and evidence closure

Final checks passed: all 28 original Palma and 14 original Lumi reader metadata
rows were unchanged in full, notebook guards passed, original fixture EPUBs were
restored with exact SHA-256, and only the enrolled synthetic managed book/rows
were removed. Original cluster/account/global flags were restored (Palma logged
out with note/reader false; Lumi original account with both true). Lab markers,
host overrides and transient conflict selection/edit/model files were removed.
Gateway policy/grants and console logging were restored; all lab listeners and
containers stopped, persistent state retained. The run has 225 checksummed private
files and its separate bounded trace has four. Driver scripts, probe Java/shell
source snapshots, installed APK bytes/hash, installation receipts and validation
logs are archived with the branch/server/provider/UI captures.


Policy chosen 2026-10-07: [native compatibility with optional server history](alexandria-native-boox-source.md#conflict-policy-decision-native-compatibility-with-optional-server-history).
Native winners govern live device state. Future server-side recovery/suggestions
may use retained observed alternatives; stronger deletion or conflict intervention
is not the v1 policy. Broader losing-intent capture remains optional future work.
