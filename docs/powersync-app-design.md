# PowerSync application design

Updated 2026-10-18 for PowerSync 0.3.0. Companion to [Alexandria's native BOOX integration](../../AragoniteAlexandriaServer/docs/boox-native-integration-design.md).

## Product boundary

An ordinary, unrooted app configures stock ksync to a user's server and restores its previous configuration. Stock native apps do normal sync. The companion adds enrollment, diagnostics and optional explicit recovery assistance; it is not a replacement sync daemon. Ship separately as `dev.aragonite.powersync.client`; preserve the existing probe package and all evidence. No fixture actions or hardcoded lab accounts enter the product build.

## Screens

Home has setup, enrolled, connected, and needs-attention states. Connection health
requires current selected server, account UID/cluster, and installed configuration
to match the encrypted profile; the journal alone is insufficient. Connected status
shows the server and each domain's actual enablement, never inferred sync completion.
Open Alexandria uses the server URL in a browser without companion credentials.

Setup accepts HTTPS URL and a one-use code, requests file access when necessary,
and confirms the destination and automatic sync enablement once. Pending credentials
are encrypted, excluded from Activity saved state and screenshots, and removed before
redemption. Ambiguous redemption requires a fresh code. Saved enrollment can continue
without another redemption; journaled native mutations are never automatically replayed.

One application-owned operation coordinator survives Activity recreation. Foreground
entry and operation completion refresh native state; operation notices are separate
from persistent connection status. Progress uses localized resources, and old notices
are cleared when the language changes. Busy controls are disabled.

Manage connection exposes offline return. Troubleshooting contains explicit endpoint
checks, recovery status, reviewed audio repair, native settings and technical details.
Automatic endpoint checks are read-only, foreground-triggered and throttled to 15 minutes
per process; their separate executor cannot block restoration. Applying updates remains
explicit, revalidates current state, preserves current flags and requires a tablet restart
for all native consumers. Diagnostics never display profile tokens or account credentials.

## Transition state

Persist immutable baseline plus checksum before any write: cluster selection/configuration, only changed sync flags, native account reference, original marker bytes/absence and preservation fingerprint. Persist candidate and phase before each native call. States are prepared, paused, configured, switched, authenticated, active, restore_requested, restored, needs_signin and failed. Completed baseline is retained after restoration; a subsequent new connection creates a new baseline. Unexpected external changes require reinspection, not automatic overwrite.

Connect pauses both native sync domains, provisions a unique cluster ID with matching lang/serverId and complete account/CBL/OSS endpoints, writes the marker only after All Files Access and preserves its original content, invokes native switch, verifies live selection and endpoints, uses generated native login material, then enables both notebook and reading sync and verifies their flags. An explicit Enable action on a verified existing connection does the same; app launch, upgrades and endpoint updates never override a deliberate pause. Never enable the lab's unrelated global defaults. Do not relabel/import personal library rows merely to switch servers.

Return performs native switch before exact app-owned restoration. Native account credentials remain native. Qualified cached account restoration may be used where uniquely identifiable; otherwise instruct the user to sign in through the native UI. Restoring configuration is not proof of authentication or resumed synchronization. Disable unavailable server grants locally without requiring a network request. Do not uninstall, clear app data or remove notes to recover.

## Compatibility and security

First support the two qualified API-35 device models and captured Binder descriptors/transactions; check them before writes. The system-service ProcessBuilder route runs without shell/su/run-as. All Binder operations are bounded and run off the UI thread. Explicit bind/unbind remain on the main thread. All Files Access is requested only when native marker provisioning requires it. HTTPS/WSS use platform trust; no trust-all verifier. Secret enrollment replies stay encrypted in app-private storage. Android backup is disabled. Release signing key persists outside source control.

## Checks

Pure state/definition validation tests; process timeout and malformed parcel cases; configuration/marker external drift; crash in each mutation phase; one-time enrollment/replay; offline rollback; lifecycle/rotation; both device fingerprints before/after. Product verification begins with read-only status on restored devices. New-server switching tests use disposable native fixtures and preserve original account/library mappings.
