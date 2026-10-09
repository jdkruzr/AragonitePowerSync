# Native reading repeat and interrupted-application qualification

Run: 2026-10-06. Devices: Palma 2 Pro and Go 10.3 II Lumi. Private evidence:
`artifacts/gateway-lab/qualification/reader-workflow/`; Gateway trace:
`artifacts/gateway-lab/protocol-traces/reader_workflow/`.

Each device received a fresh cloud-only identity for the exact authored EPUB.
The original Downloads copy was escrowed, native signed download produced the
managed copy, and the enrolled READER_LIBRARY scope was restarted explicitly.
Note synchronization remained disabled. Initial native new-data prompts were
confirmed with local layout retained and Auto sync unchecked.

| Boundary | Palma | Lumi |
| --- | --- | --- |
| Exact new model IDs and timestamps in local CBL | Modes 1, 2, 7 matched | Modes 1, 2, 7 matched |
| First explicit native application | Success, one bookmark and highlight | RESULT_NONE, zero bookmark/highlight |
| Later application | Success | Retry succeeded, one bookmark and highlight |
| Repeat application without a new server revision | Rows exactly unchanged | Rows exactly unchanged |
| Ordinary UID newer local annotation comment, then unchanged cloud replay | Comment retained | Comment retained |
| Replication disconnected after exact fresh receipt, before application | Cached new comment applied | Cached new comment applied |
| Reconnect and repeat | Same new comment, one row of each kind | Same new comment, one row of each kind |

The outage stopped only the LAN replication proxy. Account facade, Sync Gateway,
Couchbase and local EPUB remained available; this is not a complete device/network
outage. A capture before proxy shutdown proved the new cloud comment was not yet
in the native provider. Both offline calls returned native success with new data;
reconnect calls returned success with no new data and no duplicate rows.

A callback is not an application receipt. Lumi's first RESULT_NONE happened after
exact userdata receipt and dismissal of the initial prompt. A later request worked;
root cause and a reliable readiness condition remain unresolved. Journal receipt
and native provider application separately, and retain bounded retry behavior.

The comment edit uses ordinary app ContentProvider access. Its guard requires the
lab cluster, enrolled book UUID, exact managed EPUB bytes, one native metadata row
and one active annotation. This is a direct provider write, not a native editor UI
edit or proof of publication back to the server. Unchanged-revision replay retained
the comment; concurrent divergent revisions and older-content/newer-envelope
conflicts remain unqualified. The later server revision had a newer timestamp and
applied through the disconnected cache test.

Terminology correction: mode 7 is a NoteDocumentModel reader-note configuration
record, not the reading-position record. Actual reading progress is mode-4 metadata
`progress` plus native options/location behavior. This run captured the initial
17/64 Palma and 8/32 Lumi positions but did not perform a new position-change
experiment. Earlier native bidirectional progress evidence remains separate.
Mode-7 receipt is proven here; its complete semantic application is not.

Provider application of bookmark/highlight is proven independently of callbacks.
Screenshots and final restoration receipts accompany the private capture; rendering
and position changes must not be inferred merely from userdata receipt.

The adapter implication is a durable state machine: exact incoming revision receipt,
pending native application, observed provider state, then acknowledgement. Cached
application can finish without replication, so a disconnect need not restart the
whole download. Do not count an unqualified RESULT_NONE as completion.

Final cold-open screenshots retain Palma 17/64 and Lumi 8/32. Lumi visibly shows
a filled bookmark indicator; Palma shows an outline indicator at its current
page. Neither screenshot clearly establishes highlight rendering. Provider
annotation presence therefore remains distinct from visual highlight proof for
these fresh identities. Do not promote this run to a full rendered reading gate.

Probe validation: JDK 17 Android build and lint passed; all 27 existing Python
tests passed. The updated probe was installed in place on both devices. The
new ordinary-UID comment operation was exercised successfully on each device.

Restoration completed and independently verified: all 28 original Palma and 14
original Lumi reader metadata rows were unchanged in full. Original authored
Downloads EPUBs were restored exactly, enrolled synthetic managed books/rows
were removed, notebook guards passed, cluster/accounts/global sync flags were
restored, and lab marker/host overrides were absent. Palma remains logged out
with note/reader sync false; Lumi has its original account with both flags true.
Server policy/grants and trace logging were restored, lab services stopped, and
persistent lab state retained. Private run evidence has 137 checksummed files;
the separate bounded trace has four.
