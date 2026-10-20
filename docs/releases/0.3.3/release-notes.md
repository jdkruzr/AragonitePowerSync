Download **PowerSync-0.3.3.apk** below. This release removes the firmware allowlist and fixes preservation of cloud-only Library entries when changing servers.

## Changes since 0.3.1

- **No more “firmware not qualified” refusal.** PowerSync attempts the native interfaces on unfamiliar BOOX models and firmware versions, including endpoint changes. There is no model/API/ksync allowlist, warning override, or extra confirmation. Observed firmware details are logged for troubleshooting; actual interface, response and data-preservation checks still apply. Android 13 or later is required.
- **Preserve cloud-only Library entries.** A book does not need to be downloaded locally for its saved Library metadata to be restored after a server change. Existing rows and newer metadata are preserved.
- **Resume interrupted connections.** Resume connection uses the saved enrollment and original baseline when the selected server and account state match. It restores missing catalog entries, completes sign-in and enables notebook/reading sync without repeating the server switch or redeeming another enrollment code. Opening or upgrading the app does not automatically resume a connection.
- English and Spanish guidance for the recovery flow.

## Installation and build

Install over the existing PowerSync app. The application ID and original signing certificate are unchanged, preserving app data and configuration.

This is the exact signed APK from [GitHub Actions run #3](https://github.com/jdkruzr/AragonitePowerSync/actions/runs/39464804296), source commit `e720b4c867e0a56f8f66dbc29ff13ab48feece56`.

- 53 release unit tests passed; lint passed with zero errors and 12 warnings.
- Version **0.3.3**, version code **14**, application ID `dev.aragonite.powersync.client`.
- Signing certificate SHA-256: `f61b114923bef983aa12e876ea12bbc52f017363ef7103a96c2f978dc19aeb35`.
- `SHA256SUMS`, `source-commit.txt` and `signing-certificate.txt` accompany the APK.

## Known issue

The separate stale-device reconciliation issue remains: joining a device with old native state can reactivate previously deleted notebooks. This release fixes the firmware admission gate and Library recovery, not that reconciliation behavior. See the [TUCP incident](https://github.com/jdkruzr/AragonitePowerSync/blob/v0.3.3/docs/tucp-compatibility-20261019.md) and [Library recovery evidence](https://github.com/jdkruzr/AragonitePowerSync/blob/v0.3.3/docs/catalog-recovery-20261019.md).
