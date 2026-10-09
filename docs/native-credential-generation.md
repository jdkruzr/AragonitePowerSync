# Permanent lab credential retirement and native re-enrollment

Local qualification, 2026-10-07. Lumi was retired and re-enrolled; Palma was the
healthy peer. Only the fixed lab account and owned conflict notebook were used.
Private replay drivers, grants, receipts and checksums are in
`artifacts/gateway-lab/qualification/credential-generation/`.

## Observed sequence

1. Capture the current Lumi bearer, OSS grant and Gateway cookie privately;
   mint a second historical Gateway session for the same device principal.
   Verify baseline access and open authenticated proxy sockets.
2. Request generation 1 retirement through the local management script.
   The facade persists retired state, rotates the bearer, forgets OSS/session
   grants, disables the Gateway principal and invokes all-user-session invalidation.
   The old active proxy socket closes; old bearer, signed resource and cookie
   accesses are rejected. Native logout is observed.
3. Rename the disposable notebook locally while logged out. Remove the lifecycle
   control file, then restart the facade. Retirement still rejects old credentials
   and even sign-in with the fixed lab credentials.
4. Explicitly allow fresh enrollment. The pending state still denies bearer APIs;
   native sign-in activates the new generation. The device principal stays stable.
   Native provider token hash equals the new server bearer hash.
5. Obtain new OSS/session grants and remove the control file again. Replay the
   old grants against the enabled principal, with fresh request signatures.
6. Explicitly re-enable note sync and cold-restart the source only. Its queued
   rename reaches Couchbase and Palma's native provider without another save.

| Capability after new enrollment | Retired credential | Fresh credential |
| --- | --- | --- |
| Bearer account request | 403 | Native token adoption verified |
| Signed resource GET | 403 | 200 |
| Gateway document read | Both historical cookies 401 | 200 |
| Proxy WebSocket | Old cookie 403 | New cookie 101 |

Bearer, OSS and session grants changed while principal identity stayed fixed.
The captured bearer and cookie deadlines had not elapsed at the post-replay
check. The first OSS grant had expired before that replay, so its 403 alone
cannot qualify rejection of an unexpired retired grant. A separate host-only
generation-2 repeat started with the fresh generation-1 OSS grant, retired it,
explicitly re-enrolled and removed the control. The old grant still had 3,257 seconds left when its signed GET was rejected with 403; the fresh
grant returned 200 and exact bytes. No physical-device account actions were
performed for that repeat. Native adoption/queued delivery belong to generation 1;
unexpired retired-OSS rejection is the separately journaled generation-2 result.
Palma's native bearer and Gateway session hashes stayed unchanged. Its native
provider applied the queued Lumi edit. The exact authored resource was deleted
with the new grant (204). Retirement survived an actual facade process restart;
this is not a full service restart or abrupt power-loss qualification.

## Implementation

`device-credential-lifecycle.py` is a local lab management tool for known native
lab principals. It atomically writes a retirement request with a monotonically
increasing generation. `allow-reenroll` requires the retired registry state and
completed Gateway retirement marker for that generation. Its output means
requested; facade and backend receipts establish completion.

The facade reconciles lifecycle state before account/session/asset authorization
and on startup. It persists the generation, retired state and replacement bearer,
removes the old OSS and session grants, then disables the Gateway principal and
invalidates its sessions. Registry/control writes flush files and directories.
Successful backend retirement has a generation marker, so normal reconciliation
and restart do not repeat that operation. Pending enrollment denies normal APIs;
only the fixed lab sign-in path transitions it to active.

The proxy now checks the original connection cookie against the current registry
while monitoring upgraded connections. Old connections cannot survive merely
because the principal becomes active again with a different session. The bearer
and random OSS/session values embody credential replacement; generation numbers
are internal lab lifecycle metadata, not a new native wire-protocol field.

Gateway's [documented all-user-session invalidation operation](https://docs.couchbase.com/sync-gateway/current/rest-api/rest_api_admin.html)
is part of retirement. Admin session lookup nevertheless returned a stored record
after that call in the live lab. The initial helper incorrectly expected 404 and
stopped; the continuation checked actual cookie authorization after re-enrollment.
Both historical cookies received 401. This combined lifecycle also disables and
re-enables the principal and replaces its password, so it does not isolate which
backend operation alone caused cookie rejection. Physical session-record removal
is not the acceptance criterion.

## Boundaries and validation

This is qualified lab capability retirement, not production author authentication.
The constant disposable OTP and hardware fingerprint are not secure enrollment
proofs. Alexandria must require a fresh authenticated owner/device enrollment
before granting the pending transition. No public lifecycle-management endpoint
was added, and no Alexandria deployment occurred.

Other open cases include multi-process writers/replicas, mirrored Palma retirement,
pre-admitted asset-request cancellation, direct active Gateway sockets, full-server
restart, and crash injection between lifecycle/backend persistence steps. There
is no production token-rotation grace/replay policy here; stale bearers use the
existing lab 403 response after the device becomes active again.

Fresh Python suite: 31 tests pass, including actual local management transitions,
control-file removal/restart, pending denial, stable identity and old bearer replay.
Unknown stored credential states are rejected by sign-in rather than activated;
that final guard was unit tested after the live valid-state run.
Go race tests cover scoped cutoff, malformed controls and connection-cookie
replacement; the proxy builds. No Android source changed in this run.

The first driver had a syntax error before execution. The next stopped after a
successful retirement on the incorrect admin-session 404 assertion. The retained
continuation completed re-enrollment and old-grant replay. These stages remain
separately journaled rather than being presented as one uninterrupted socket run.
No verbose Gateway trace was started; direct API status receipts, proxy journals,
private grant captures and native observations are the available evidence.

Closure restores both original accounts, endpoints, flags and server grants;
compares full original reader metadata and notebook preservation guards; stops
lab services; and checksums the private evidence. The active new generation is
retained in disposable server state, while retired secrets remain only in private
replay evidence and cannot authorize the facade's current registry.
