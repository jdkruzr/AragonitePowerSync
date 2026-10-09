# Native publication trace: Gamma

Captured 2026-10-05 (Chicago), using the stock Palma 2 Pro and Go 10.3 II
Lumi, independently hosted Couchbase Server / Sync Gateway, and the lab OSS
facade. Only the two disposable notebook fixtures were allowlisted.

The existing native UI notebook received `GAMMA traced on Palma` in Palma's
native editor. Lumi subsequently displayed that text in its native editor,
and its native `.note` export contained Gamma in the active resource data.
No vendor APK modification or device root was involved.

## Server timeline

Times below are UTC on 2026-10-06, corresponding to the previous evening in
Chicago. Persistence times come from Gateway `Stored doc` messages; the
download interval comes from the instrumented file facade.

| Time | Observed event | Server sequence |
| --- | --- | --- |
| 03:43:42.074 | Palma notebook metadata revision 6 stored | 87 |
| 03:43:42.824 | Gamma HTML asset stored, 1,614 bytes | 90 |
| Before 03:43:45.407 | Other related assets accepted; last journaled asset is thumbnail | 88–94, with gaps |
| 03:43:45.407 | Native commit record revision 12 stored, commitStatus=1 | 95 |
| 03:44:52.823–03:44:52.840 | Lumi GET returned the same Gamma asset revision, 1,614 bytes | asset revision from seq90 |
| After download | Lumi native editor and native export confirmed Gamma | UI/export evidence |

Metadata was published before the files, and the commit record followed the
file uploads. This is one observed staged update, not proof of atomicity or of
all error recovery behavior. The notebook remains the same ID on both devices.
The Gamma upload and download share exact backend revision
`1-a538a57e189e8f8e5bf89fbec1f6b319`; stored SHA-256 is
`cdf4cbf057100575e2ea50788860498ad958d080afbd33887b8f6d6bde9da1e8`.

## Protocol evidence

The capture includes 32 `getCheckpoint`, 32 `subChanges`, 49 `proposeChanges`,
36 `changes`, 13 `rev`, and 14 `setCheckpoint` request messages. These are
protocol counts across the controlled window, including rejoin, empty batches,
retries and multiple logical replicators; they are not counts of user edits.
All 176 parsed request messages were attributed: 108 Palma, 68 Lumi.

Connection attribution uses Gateway's explicit principal-bearing feed lines
and HTTP request / BLIP connection-close linkage, rather than neighboring
timestamps. Raw messages include direction and message number. Palma's native
metadata `rev` carried client sequence 24; Gateway persisted it as server
sequence 87. The commit carried client sequence 28 and became server sequence
95. Client and server cursors must remain separate.

The long-poll changes journal records announced revision IDs and fetches their
exact revision metadata. It is not a complete revision history: rapid updates
can coalesce, and sequence gaps are not interpreted as missing user actions.
CRUD and protocol logging supplement that journal. Out-of-allowlist native
account bookkeeping is still denied; such 403s are not Gamma asset failures.

## Reproducing the capture

With the existing lab stack running and disposable allowlist configured:

```sh
python3 scripts/trace-gateway-server.py fresh-run --seconds 900
```

Make a scoped synthetic edit and collect receiver UI/export evidence, then
interrupt the capture orderly. It restores console log settings on orderly
exit and records that restoration. Raw logs are private, ignored artifacts;
they can contain bodies and device identifiers. Parse whitelisted metadata:

```sh
python3 scripts/parse-gateway-trace.py \
  artifacts/gateway-lab/protocol-traces/fresh-run/gateway-private.log \
  artifacts/gateway-lab/protocol-traces/fresh-run/protocol.jsonl
```

File requests now carry operation IDs, upload device provenance, exact backend
revision, timestamps and response byte counts in the host journal. Internal
provenance headers are removed before the response reaches stock clients.
`publicationAcknowledged` now applies only to successful revision-backed writes;
the first Gamma capture predates that correction, so its GET flag must not be
interpreted as a publication.

Private evidence is under `artifacts/gateway-lab/protocol-traces/gamma-run/`:
raw protocol, parsed protocol, durable changes/cursor/control journals,
source/receiver exports and Lumi screenshot. Request correlation is in the
private shim request journal.

## Restoration and validation

Both original cluster maps and original sync flags were restored; Lumi's
original account restoration succeeded, and Palma returned to its original
logged-out state. Test-server markers and both ADB tunnels were removed.
Capture stopped with loggingRestored=true; lab listeners and containers were
stopped, retaining disposable server state.

Outside the two allowlisted fixtures, identity/status/ownership fingerprints
remain unchanged: Palma 1,517 rows, Lumi 1,516 rows. This is preservation of
those tracked fields, not a byte-for-byte audit of every personal note file.
The Python suite passes all 13 tests, including provenance/integrity and
interleaved protocol attribution with multiline body redaction.

The next meaningful qualification is failure between file upload and commit,
followed by retry, concurrent edits and larger handwriting/image notebooks.
Do not infer general conflict safety from this successful text update.
