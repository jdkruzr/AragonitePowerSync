# Device-scoped Gateway and OSS cutoff

Local qualification, 2026-10-07. Lumi was the revoked lab device; Palma was the
healthy peer. Original accounts/libraries were preserved. Private receipts,
source snapshots, tests and replay drivers are under
`artifacts/gateway-lab/qualification/transport-revocation/`.

## Live outcomes

| Capability | Lumi after cutoff | Palma during cutoff |
| --- | --- | --- |
| Existing proxy WebSocket | Observed socket closure | Existing probe socket remained open |
| New proxy WebSocket with cached session | 403 | Pre-cutoff handshake 101; healthy native publication continued |
| Direct Gateway read with cached cookie | 401 | 200 |
| Signed resource GET/HEAD/PUT/DELETE with cached OSS grant | All 403 | GET 200, exact authored bytes; DELETE 204 |
| Signed resource listing | 403 | 200 |
| Account info, syncToken, stss, token refresh | All 401 | All 200 |
| Native account state | Logged out after native account-info request | Native rename reached Couchbase during revocation |

Both source and peer had 200 Gateway reads, signed asset access, and 101 probe
handshakes before revocation. The fixture resource was authored on the host using
Lumi's existing lab grant; this is a server authorization test, not native asset
upload qualification. Native peer publication supplies a separate device-driven
observation. Lumi's native account-info action establishes client logout after
API rejection; WebSocket closure alone does not imply logout.

The recorded 7.746-second value spans cutoff plus subsequent HTTP checks and
account observation. It is not precise WebSocket closure latency. The proxy polls
revocation every 250 ms, which is an implementation interval, not a measured
end-to-end service-level guarantee.

## Implemented lab control

The facade's private `shim/revoked-devices.json` is an exact-principal list, written
atomically by the experiment driver. Account authentication, sign-in, Gateway
session issuance and OSS operations consult it. The proxy consults the same file
on admission and polls active upgraded connections, closing only those belonging
to listed principals. BLIP payloads continue to be forwarded unchanged.

The experiment also deleted Lumi's known Gateway session and disabled its Gateway
principal with empty grants. This protects direct Gateway access independently
of the LAN proxy. It is a combined cutoff test: the individual contributions of
session deletion and principal disabling were not isolated here. The existing
facade session path can mint a replacement after session deletion; therefore
session deletion alone is not a durable device ban.

Checks deny cached OSS GET/HEAD/PUT/DELETE, listing and bulk-delete paths before
backend operations. Bulk-delete denial is covered structurally by the shared
control; a revoked signed bulk-delete request was not exercised dynamically in
this run. Unit assertions cover signed object operations and replacement account
credentials including sign-in. Malformed existing proxy controls fail closed,
including whitespace-wrapped null; an absent control means no lab revocations.

## What remains unqualified

- Permanent credential retirement and secure re-enrollment with a new credential
  generation. Removing this lab control is rollback and can reactivate old bearer/
  OSS grants; it must not be copied as a production re-enrollment procedure.
- Cancellation or linearizable rejection of HTTP/object transfers admitted before
  revocation. The experiment used fresh requests after cutoff and existing idle
  WebSocket probes; it did not interrupt an asset upload mid-flight.
- All historical Gateway sessions, direct active Gateway sockets, mirrored Palma
  revocation, multiple service replicas, server restart durability, or production
  author authentication. Only the known session was deleted, and proxy sockets
  were the dynamically observed active cutoff path.
- Cross-library isolation and TLS. The registry is disposable lab identity state,
  not Alexandria's production authorization system.

A production adapter needs a durable disabled-device state, explicit capability
retirement, safe re-enrollment, and cutoff across every exposed transport. The
healthy peer result supports device-level revocation instead of author-wide
credential replacement.

## Validation and closure

Fresh Python suite: 30 tests passed, including API issuance denial and signed
object rejection without backend mutation. Gateway proxy tests pass with Go's
race detector; scoped socket closure and malformed controls have explicit tests.
The final proxy builds. Its additional whitespace-null fix was unit tested after
the live valid-list experiment; that malformed-input case was not tested against
a running native device.

The first live helper stopped after cutoff on a missing HTTPError import. Its
partial fixture was retained until exact-byte cleanup. The successful retry
completed all cutoff and peer-publication observations, then its cleanup assertion
expected 200 instead of the actual successful DELETE 204. A separate cleanup
verified and removed the remaining exact authored object and cleared the lab
control. Initial failures are preserved as failures, not passed complete runs.

Original endpoint/account/flag restoration and full reader/notebook preservation
are verified separately at closure. Gateway policy/grants are restored; lab
services stop with persistent state retained. No verbose Gateway trace was started
for this run; the proxy/API/OSS journals and direct authorization receipts are the
available transport evidence. Evidence is private and SHA-256 checksummed.

[Follow-up credential lifecycle](native-credential-generation.md) supersedes the
lab rollback mechanism for deliberate retirement. Persistent generation state
survives control-file removal and facade restart; old grants stay rejected after
explicit native re-enrollment. The original temporary-control results remain valid.
