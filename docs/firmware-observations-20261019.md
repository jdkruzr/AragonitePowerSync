# Firmware observations instead of an allowlist — 2026-10-19

PowerSync 0.3.2 (code 13) removes the model/API/ksync version admission gate.
The owner explicitly requested that unlisted combinations attempt normal operation:
we cannot pre-test every hardware and firmware combination. No warning override,
extra confirmation, or per-device exception is required. Android API 33 remains
the app's minimum installation requirement.

`NativeBridge.requireKsyncAvailable()` checks that the native package exists and
logs model, Android API, firmware build and ksync version using the
`PowerSyncCompatibility` tag. The log contains no account, enrollment or endpoint
credentials. Repeated checks of the same tuple within a process do not flood logs.
`NativeCompatibility` only formats observations; it contains no support table.
All callers, including enrollment, connection, restoration and maintenance, use
the package check. Obsolete refusal strings were removed in English and Spanish.

Actual service/provider availability, Binder descriptors, transaction results,
response validation, timeouts, inventory preservation, readback and rollback
checks remain. The existing Lumi-only account-recovery workaround is unchanged:
it applies after an actual account-preservation failure, not as an admission gate.
The TUCP stale-join reconciliation issue remains separate and unresolved.

## Verification

- Fresh JDK 17 release unit tests, lint, signed assembly and instrumentation APK
  assembly: **41 tests, no failures/skips; lint 0 errors / 12 existing warnings**.
- Diagnostic tests cover Go6_2, unknown/missing model/build fields, future Android
  and ksync versions, and single-line log formatting.
- Go6_2, Android 13/API 33, firmware
  `2026-09-24_20-45_4.2.2-rel_0924_f2c816851`, ksync **32198**: in-place upgrade
  from 0.3.1 to 0.3.2 passed. Installed and candidate certificate SHA-256 both
  `f61b114923bef983aa12e876ea12bbc52f017363ef7103a96c2f978dc19aeb35`.
- Both ordinary-product-UID read/service instrumentation checks passed: native
  cluster/system configuration, flags, account provider/cache, notebook
  fingerprint, reader catalog, controller availability and Binder descriptor.
  Only the disposable instrumentation package was removed afterward.
- UI opens to **Connect to Alexandria**, with no qualification error. Logcat
  records the real Go6 tuple and `attempting native interfaces`.
- Before/after hashes match for native cluster and system configuration, account
  identity/state projection, notebook identity/type/status/owner projection and
  reader UUID catalog. These checks cover configuration and selected metadata,
  not a full notebook-resource backup or end-to-end synchronization test.
- No enrollment, endpoint changes, or sync writes were performed during this
  verification. The owner can now use the normal connection flow.

Private local evidence and the previous/candidate APKs are retained under ignored
`artifacts/go6-permissive-20261019/`. Candidate APK SHA-256:
`301eabbee81147a8e4edca0d8c03888db1aaa10ac0c86e357b0be197e14c1220`.
This records a local build and device installation, not a GitHub release.
