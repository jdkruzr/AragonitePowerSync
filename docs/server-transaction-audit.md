# Server transaction audit: native UI text-note experiment

Audit performed against retained lab server state after device restoration.
No device was rejoined for this audit. Run `scripts/gateway-lab.py start`, then
`scripts/audit-gateway-lab.py`; the audit only reads Gateway and existing journals.
Raw output is ignored `artifacts/gateway-lab/server-audit/report.json`.

## Concrete content timeline

Times below are America/Chicago on 2026-10-05. Upload attribution comes from
validated per-device OSS credentials and the uploaded body SHA-256. Download
attribution comes from validated credentials and the matching object filename.
Stored attachment bytes were independently checked against their SHA-256.

| Time | Server operation | Device | Content |
| --- | --- | --- | --- |
| 21:05:02 | Signed object PUT, 200 | Palma | ALPHA text, 879 bytes |
| 21:05:57 | Signed object GET, 200 | Lumi | Same ALPHA object |
| 21:06:59 | Signed object PUT, 200 | Lumi | ALPHA plus BETA edit, 899 bytes |
| 21:07:25 | Signed object GET, 200 | Palma | Same edited object |
| 21:08:06 | Signed object PUT, 200 | Palma | Another resource revision containing both markers, 1,117 bytes |

The third resource retains the same text but differs in editor state/metadata;
it is not a third distinct user text edit. The first and second resources remain
stored under different resource UUIDs. Native UI screenshots and exports establish
application/rendering separately; a server GET success alone cannot establish it.
These timings include deliberate sync/reconnect/open interventions and are not
benchmarks of unattended sync latency.

## Traffic during the UI-note run

Window: 21:00:55–21:08:12 CDT. These counts include both allowlisted fixtures and
bookkeeping/repeated operations, not just new content from the UI fixture.

| Observed request | Palma | Lumi |
| --- | ---: | ---: |
| Account/session/quota API requests attributed to the device | 19 | 31 |
| Successful file PUT | 20 | 10 |
| Successful file GET | 14 | 18 |
| Successful file HEAD | 20 | 0 |
| Uploaded request-body bytes, including repeats | 44,445 | 35,278 |
| Successful BLIP WebSocket upgrades | 23 | 31 |

The connection totals include multiple native logical replicators and our manual
reconnects/configuration reloads. They are not separate devices or user edits.
Bucket listing requests count as GET in the table. GET response byte counts were
not captured in the old request journal. Anonymous failed API requests are not
included in per-device totals because authentication did not establish identity.

## What the database actually holds

For UI notebook `f7f635344f624650aa300f84d114a1d3`:

- One native notebook metadata document in `powersync-lab-NOTE_TREE`, current
  revision generation 4.
- One native commit-point record referencing that notebook, current revision
  generation 8. Its current numeric fields are commitType=1, recordType=1,
  commitStatus=1. Do not translate these into user-edit counts.
- 24 adapter asset documents with attachments, totaling 34,339 current content
  bytes. Categories: 8 resource files, 7 shape files, 3 page-model files, 2 virtual
  document files, 2 template files, one points file and one PNG.

Asset documents and ASSET channels are our adapter's storage representation;
the native devices issue OSS operations and do not replicate these internal asset
documents directly. Native metadata/commit documents arrive through BLIP; the
adapter's attachment writes arrive through Gateway Admin REST. The lab uses one
physical bucket/database and native `dbId` values as channel scopes.

Across the entire retained lab database, `_changes` currently returns 35 rows and
last sequence `81`. This is **not 81 user edits or a complete 81-event audit**.
Changes feed rows report the latest change per document and sequence numbers are
replication cursors. Document revision generations describe revision ancestry.
See [Gateway changes API](https://docs.couchbase.com/sync-gateway/3.2/rest_api_admin.html)
and [replication protocol](https://github.com/couchbase/couchbase-lite-core/wiki/Replication-Protocol).

## Visibility limits and next instrumentation

The account/file request journal records time, validated device principal,
operation, sanitized path, status, request-body length and SHA-256. The proxy
records device principal, connection time, upgrade status and negotiated protocol.
Normal Gateway logs record incoming authenticated BLIP connections and change-feed
start/caught-up messages. They do not expose every BLIP message at current verbosity.

Therefore the file transfer chronology above is directly reconstructable, but the
old server logs do not establish a complete per-device chronology of every native
metadata/commit write, checkpoint update, revision proposal or conflict resolution.
Current documents are a snapshot, not that missing audit. Client traces separately
show an old-fixture revision conflict and denied MESSAGE writes outside our narrow
allowlist; those must not be confused with failure of the successful UI-note transfer.

For the next controlled run, enable bounded Gateway Sync/BLIP diagnostics and
correlate connection IDs with device principals. Stream `_changes` into a durable
metadata-only journal while traffic is happening; record revision IDs and channel
scope, not note bodies. Extend asset events with response byte counts, backend
revision IDs and operation IDs. Checkpoint/proposal/revision profile details must
come from protocol diagnostics, not fabricated from connection counts.

A notebook update spans independent metadata/commit revisions and file requests.
Our current logs do not prove an atomic notebook-wide publication boundary. Mapping
when references become visible relative to their assets is the next useful question
for interruption/recovery tests.
