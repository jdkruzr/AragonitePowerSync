# Reverse delivery after native re-enrollment

Local qualification, 2026-10-07. Both existing devices were rejoined to the
fixed disposable account using the LAN facade and Gateway proxy. Only the
owned conflict notebook `9e6004bb-7bc3-4a7e-87d9-79b9f9ba6206` was renamed.
Private receipts and replay drivers live under
`artifacts/gateway-lab/qualification/reenrollment-delivery/`.

## What passed

| Observation | Result |
| --- | --- |
| Previously unobserved Lumi rename after fresh lab rejoin | Both native providers matched server revision 91's title and updatedAt 1791356724027, before the new tree-query probe ran. |
| Fresh Lumi-to-Palma edit with both devices connected | Source, server revision 92 and Palma all matched the new unique title. |
| Lumi protected API rejection followed by refresh 401 | Native logout observed; local edit authored while logged out survived. |
| Lumi re-enrollment with Palma and shared proxy left running | Login left note sync false. Explicit enable, source-only cold native restart and tree restart delivered the queued edit to server revision 94 and Palma within the 40-second observation window. |
| Native pending-tree callback, after convergence | Both builds returned status 2, completed true, offset 0, zero tiny records, and a protobuf descriptor path. Native total counts were 8 on Palma and 6 on Lumi. These counts are not complete library backup evidence. |

This repeat demonstrates reciprocal delivery after re-enrollment for the specified
sequence. It does not explain the previous bounded nonapplication. The earlier
run interrupted the shared proxy and restarted both peers; this repeat left the
receiver/proxy running. Those are multiple changed variables, not a controlled
proof that either caused the stall. Fresh rejoin also involved multiple actions.
No claim that a specific restart alone repaired the old delivery gap is warranted.
The original negative result remains in the account-lifecycle ledger.

## Probe semantics and static evidence

The ordinary-app `gateway_tree_query` action checks the lab cluster and selected
owned lab notebook, then invokes ISyncService transaction 21 (`pullNoteTree`)
with a callback. It validates the returned lab user/document context, retains a
private callback receipt, bounds an optional descriptor payload to 4 MiB, and
waits at most 35 seconds. It does not apply returned models to the native provider.

The pulled ksync `LoadNoteTreeRequest` sets inline content from TinyRecordBean
update/delete bookkeeping, saves a SyncNoteTreeBean protobuf descriptor, and
reports pending page offset/completion plus a user tree count. The pulled notes
`SavePullNoteTreeAction` separately loads the descriptor's noteTreePBPaths and
saves/merges the native models. Consequently this is a pending-tree export action
with native bookkeeping side effects, not a read-only complete CBL snapshot.
An empty pending result after native convergence cannot establish that an earlier
revision was absent from CBL. No protobuf bundle was decoded in this run.

Initial probe versions assumed a non-null JSON descriptor and then an object
inline response. Both assumptions failed dynamically: the successful native
callback had no payload descriptor and inline `[]`. Those failures are retained;
the final probe handles inline arrays and passed on both builds. The final debug
APK assembled, passed lint and installed in place on both devices. The main
re-enrollment delivery passed before that final probe was invoked.

## Implications and remaining work

Prefer a setup/recovery sequence that preserves the healthy receiver while the
source re-authenticates and explicitly re-enables sync. This is a qualified
candidate, not a universal recovery algorithm. Keep server revision receipt and
native application receipts separate; a completed pending-tree callback is not
proof of application. A controlled proxy-disconnect/reconnect comparison and
receipt of the exact stalled revision in ksync remain open causal investigations.
Complete Gateway-session/OSS-grant revocation remains a separate security gate.

The trace journal includes the exact fixture revisions and private protocol log.
Gateway logging was restored on orderly trace termination. Restoration receipts,
full original reader comparisons, notebook guard comparisons, service shutdown
and SHA-256 manifests accompany the run.

Before joining Lumi, its notebook guard was advanced to include the previously
identified original-account addition. Every pre-existing row had already matched
the original digest excluding that one addition. The old guard baseline remains
private in this run; no real row was deleted or added to the synthetic allowlist.
No production deployment, reboot, account-independent enrollment or full-library
backup was qualified by this experiment.

Palma's first restoration attempt failed on a transient empty JSON configuration
response after the original cluster had been selected. A retry completed; closure
requires the separate settled verification of both devices rather than treating
that initial attempt as successful.
