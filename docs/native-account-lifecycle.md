# Native account expiry, refresh failure, revocation and re-enrollment

Local qualification, 2026-10-07. Only the fixed disposable lab account and owned
synthetic notebooks were used. Private evidence and replay drivers are in
`artifacts/gateway-lab/qualification/account-lifecycle/`. No production enrollment
or authentication policy was deployed.

## Native results

| Case | Source | Observed outcome |
| --- | --- | --- |
| Expired bearer, successful rotating refresh | Palma | Protected API returned 401; refresh returned 200 with a different bearer. Native provider stored that exact token and remained logged in. Device principal stayed stable; Lumi bearer and Gateway session stayed unchanged. |
| Expired bearer, refresh temporarily 503 | Palma | Protected API returned 401, followed by refresh 503. Native account stayed logged in, bearer remained unchanged, note-sync flag stayed enabled, and queued local rename survived. Lumi account/credentials remained unchanged. |
| Recovery after clearing 503 | Palma | Refresh returned 200; queued rename reached the server without another save. Lumi applied it after cold native restart; earlier bounded restart/full-pull checks had not applied it. |
| Device-scoped refresh rejection 401 | Palma and Lumi, separate rounds | Native source logged out and disabled both note and reader sync flags. Local queued rename remained. Personal notebook and original reader guards passed. Peer stayed logged in with unchanged bearer/session. |
| Re-enrollment after rejection | Palma | Native login succeeded with the same device principal and retained local edit. Note sync remained disabled immediately after login; explicit enable and cold native restart delivered the queued edit to server and peer. |

Lumi re-enrollment also retained the same principal and local queued edit. Note
sync remained disabled until explicitly enabled. After native restart the queued
rename reached the server, but Palma did not apply it within the 70-second
observation window. Both Gateway sessions were valid and device clocks differed
by about two seconds; neither establishes a cause. A further restart investigation
did not complete before restoration began and is excluded from qualification.
Reciprocal peer delivery after re-enrollment remains open.

## Static explanation and test controls

Pulled ksync `RefreshTokenRequest` sets its token-invalid flag specifically for
HTTP 401. `RefreshTokenAction` calls `LogoutOnyxAccountAction` on that invalid
result. `LogoutLocalOnyxAccountRequest` marks the account logged out and disables
both global sync flags. Dynamic results above verify that path on both installed
builds; a regular protected API 401 followed by refresh 503 did **not** itself
log out the Palma account.

The pulled SDK compares expiry with `TOKEN_EXPIRED_TIME_MS - TOKEN_REFRESH_TIME_MS`:
constants are six months and one month. The lab's one-hour tokens are deliberately
short and do not establish stock server token lifetimes or refresh cadence.

Faults are exact device-principal matches in the private account shim, limited to
170 seconds in this run (helper rejects windows longer than 180 seconds):
`expire_rotate`, `refresh_503`, and `revoke_401`. The 503 control can additionally
expire the current bearer to drive the native invalid-token monitor. Expiry
state persists until successful refresh/sign-in; the response fault window
itself expires. Signed OSS and Gateway authentication were not rewritten by
these account controls. The one-time native expiry-column manipulation used
ADB with an exact lab UID/cluster selector; it does not qualify ordinary-app
expiry mutation. Reading that provider can wake ksync and renew immediately.

The new ordinary-app `account_lab_info` debug action checks the lab cluster and
sole logged-in lab account, then uses ISyncService transaction 69 to request
native user info. Its immediate result means requested, not network completion;
API receipt and provider-state observations qualify completion. This avoids
assuming a tree restart or a persisted expiry edit actually triggered refresh.

The LAN replication proxy was intentionally stopped when staging queued edits,
so server revision stayed unchanged during that disconnected interval. This is
separate from account HTTP faults; it does not prove peer replication remained
available during the shared proxy outage. The peer's **account credentials and
login state** were the isolation observations.

Initial 503 attempts never received 503 and are retained as inconclusive setup
artifacts, not passed fault tests. Fresh native login left note sync disabled.
Persisted flags, broadcasts and effective native process state were checked
separately; a cold native restart was needed for the receiver's earlier rename
application in this run. An initial mirrored control used an invalid fault mode;
it was rejected by the helper and replaced with the exact matched control.

## Facade behavior implied by these results

- Temporary refresh failure and deliberate rejection need distinct responses:
  using 401 for a generic outage can log out the device and disable sync.
- Renew against the same author/device mapping. Native adoption of a rotated
  token does not require a new Gateway principal or rewriting note ownership.
- Re-authentication is not proof of resumed sync. Track flags, daemon state,
  queued work, server receipt and native peer application separately.
- Preserve local pending data through logout; do not treat an auth transition
  as authorization to remove notebooks or attachments.
- Account bearer, Gateway cookie and OSS credentials are separate capabilities.
  This run's `revoke_401` rejects account API use and observes native logout; it
  does not revoke the underlying Gateway principal/session or cached OSS grant.
  Complete server-side credential revocation and active-connection cutoff remain
  an additional gate, rather than being silently inferred from client logout.
- The lab rejects a stale/wrong bearer with 403 and does not model a production
  rotation grace window. Concurrent refresh/replay policy remains to be designed;
  this experiment does not prove all in-flight requests survive token rotation.

This is synthetic account-protocol compatibility, not real Onyx account expiry,
real OTP authorization, cross-author isolation, or deployed Alexandria auth.
No reboot/power-loss test was introduced. Expiry rotation and 503 were exercised
on Palma; 401/logout was matched on both firmware builds.

## Closure and validation

Fresh account-shim tests passed (30 tests); the debug probe assembled and passed
lint, then installed in place on both devices. Original accounts, cluster selection,
sync flags and endpoint configuration were restored. Final preservation checks
compare full original reader metadata and guarded personal notebook rows.
The private results ledger, restoration receipts, replay drivers and SHA-256
manifest are retained with the evidence. No verbose Gateway trace was started
for this account run. Lab service shutdown and final receipts qualify closure.

Restoration detail: Lumi's original account had one additional notebook row after
restoration (created before these account experiments). The aggregate notebook
guard therefore changed from 1,516 to 1,517 rows. Excluding only that identified
addition reproduces the original identity/status/ownership digest exactly; every
pre-existing guarded row is preserved. The additional real-account row was kept,
not deleted or added to the synthetic allowlist. Its receipt remains private.
The timing does not establish which native operation introduced that row.

[Follow-up delivery run](native-reenrollment-delivery.md) observes the previously
stalled rename after fresh rejoin and qualifies a new reciprocal re-enrollment
edit with the proxy/receiver kept running. The original bounded failure remains
valid; the follow-up does not isolate its cause.
