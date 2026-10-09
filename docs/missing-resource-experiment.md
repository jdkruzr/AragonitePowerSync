# Missing-resource experiment: 404 and 503

Run 2026-10-06 on the existing disposable native UI text notebook, with Palma
as writer and Go 10.3 II as receiver. Stock ksync used the isolated Couchbase
Server / Sync Gateway lab and narrowly admitted NOTE_TREE messages.

The facade accepted and retained each fresh HTML upload, but returned signed
OSS error responses to Lumi's GET: `NoSuchKey` / 404 or `ServiceUnavailable` /
503. Authentication and the notebook allowlist still ran first. Fault rules
excluded baseline keys, affected only fresh non-history resource HTML for the
receiver, and expired within five minutes. Uploads, other objects, and HEAD
were unaffected. This simulates an unavailable destination download, not loss
of the server's underlying object.

## Results

| Observation | 404: ETA | 503: IOTA |
| --- | --- | --- |
| Rejected HTTP GET attempts | 3 | 9, in three groups of three |
| Destination asset present before fault removal | No | No |
| Requests for the asset in 60 seconds after removal | 0 | 0 |
| Recovery intervention | Fresh THETA source edit/commit | Fresh KAPPA source edit/commit |

For 404, a scoped receiver pull was also requested after the quiet window. It
did not fetch the target in the observed interval. The subsequent source edit
and fresh native commit caused a successful GET of the previously blocked
object, with its original backend revision and SHA-256. Lumi's native editor
then displayed ETA and THETA. After closing that editor, its native export
contained both markers in one active resource HTML file.

The 503 case also recovered the originally blocked object at its original
revision/hash after the fresh KAPPA edit. Lumi's editor displayed IOTA and
KAPPA; its export after editor close contained both in one active HTML file.
The 404 object was 1,699 bytes (server asset sequence 158), and the 503 object
was 1,731 bytes (sequence 209). These are asset-document sequences, not client
checkpoint positions or native application acknowledgements.

An export before opening/closing the recovered receiver editor had an empty
resource/data directory despite the successful download. Opening displayed the
correct recovered text; closing and exporting materialized the active HTML.
This is another reason that a successful download or export request alone is
not a complete native-application acknowledgement.

The OSS SDK's `OSSRetryHandler` explains the additional 503 attempts: status
codes >=500 retry internally, with exponential intervals; ordinary 404 does
not. The observed inner intervals were approximately 0.4 and 0.8 seconds.
Onyx's outer task logic separately limits failures. Once that observed work
had exhausted its attempts, simply restoring server availability did not
restart it within the measured window. This does not rule out a later periodic
retry or a different scheduling trigger.

## Scope of the conclusion

These text downloads exercised a file-discovery path that reported pull
failures, rather than establishing the special V2 missing-file-success branch.
`ResourceDataSyncFile` creates generic `SyncDataBean` work; the static V2 branch
requires a nonnull LocalRecordModel. We did not inspect the daemon's private
pending-record database or instrument the in-memory bean identity. Therefore
the static prediction that a non-cover V2 404 clears pending work remains a
separate live test, not a result of this experiment.

The experiment did not independently gate metadata, commit delivery and
MESSAGE delivery. A fresh source edit changed those together. Recovery is
observed after that intervention; we do not isolate which individual changed
record is sufficient, or prove MESSAGE is its only trigger. No broad restore,
production notebook write, or server object deletion was used.

## Evidence

Private `artifacts/gateway-lab/protocol-traces/fault-run/` contains rules,
intervention timestamps, target revisions/hashes, receiver file-presence
checks, native screenshots and exports. `fault-protocol/` contains durable
revision journals and the private protocol capture. The shim's durable request
journal records injected faults with exact backend object provenance; the
internal provenance headers are removed before the native response.

The ordinary-app endpoint access investigation is recorded separately in
[the UID probe report](unprivileged-endpoint-access.md).

## Restoration and validation

Both exact original cluster maps, original account states and note/reader
flags were restored. Palma is logged out with both flags false; Lumi's original
account is restored with both flags true. Lab markers/tunnels removed. A late
Lumi cache write reintroduced the lab entry after the initial restore; stopping
Notes and ksync, rewriting the original map and checking again resolved it.
The final checks also refreshed the app's host-observed configuration files.

Outside the two disposable fixtures, Palma's 1,517 and Lumi's 1,516 tracked
rows retain their original identity/status/ownership hashes. Original Gateway
sync policy and device-principal grants were verified after restart, console
logging was restored, and the lab listeners/containers stopped with data
retained. Private `restoration-verified.json` and `results.json` record the
assertions and experiment summary.

Python tests pass (15), including fault expiry, receiver/path/method scope,
unchanged backing object and 404/503 responses. Android build and lint pass.
