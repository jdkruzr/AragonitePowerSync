# PowerSync application design

2026-10-08. Companion to [Alexandria's native BOOX integration](../../AragoniteAlexandriaServer/docs/boox-native-integration-design.md).

## Product boundary

An ordinary, unrooted app configures stock ksync to a user's server and restores its previous configuration. Stock native apps do normal sync. The companion adds enrollment, diagnostics and optional explicit recovery assistance; it is not a replacement sync daemon. Ship separately as `dev.aragonite.powersync.client`; preserve the existing probe package and all evidence. No fixture actions or hardcoded lab accounts enter the product build.

## Screens

Home shows active server, native account, notebook/Reader enablement, compatibility and any interrupted transition. Connect accepts an HTTPS server URL and short-lived enrollment code. Preview describes the target server and baseline, requested permissions, account transition and any migration not yet performed. Connect is an explicit action. Previous server restores configuration offline and reports whether native sign-in is still required. Recovery shows server operations and independent receipt/application states, with scoped assistance only when supported. Diagnostics redact account tokens, signed URLs, raw note bodies and personal names.

## Transition state

Persist immutable baseline plus checksum before any write: cluster selection/configuration, only changed sync flags, native account reference, original marker bytes/absence and preservation fingerprint. Persist candidate and phase before each native call. States are prepared, paused, configured, switched, authenticated, active, restore_requested, restored, needs_signin and failed. Completed baseline is retained after restoration; a subsequent new connection creates a new baseline. Unexpected external changes require reinspection, not automatic overwrite.

Connect pauses both native sync domains, provisions a unique cluster ID with matching lang/serverId and complete account/CBL/OSS endpoints, writes the marker only after All Files Access and preserves its original content, invokes native switch, verifies live selection and endpoints, uses generated native login material, then applies user-selected flags and refreshes native process state. Never enable the lab's unrelated global defaults. Do not relabel/import personal library rows merely to switch servers.

Return performs native switch before exact app-owned restoration. Native account credentials remain native. Qualified cached account restoration may be used where uniquely identifiable; otherwise instruct the user to sign in through the native UI. Restoring configuration is not proof of authentication or resumed synchronization. Disable unavailable server grants locally without requiring a network request. Do not uninstall, clear app data or remove notes to recover.

## Compatibility and security

First support the two qualified API-35 device models and captured Binder descriptors/transactions; check them before writes. The system-service ProcessBuilder route runs without shell/su/run-as. All Binder operations are bounded and run off the UI thread. Explicit bind/unbind remain on the main thread. All Files Access is requested only when native marker provisioning requires it. HTTPS/WSS use platform trust; no trust-all verifier. Secret enrollment replies stay encrypted in app-private storage. Android backup is disabled. Release signing key persists outside source control.

## Checks

Pure state/definition validation tests; process timeout and malformed parcel cases; configuration/marker external drift; crash in each mutation phase; one-time enrollment/replay; offline rollback; lifecycle/rotation; both device fingerprints before/after. Product verification begins with read-only status on restored devices. New-server switching tests use disposable native fixtures and preserve original account/library mappings.
