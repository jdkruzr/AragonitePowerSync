# Fresh Palma restoration — 2026-10-10

User performed a factory reset of Palma `bbb70bef`; Lumi and server were preserved. Before product installation, native NoteModel and Metadata providers both returned zero rows; PowerSync was absent. `.ksync` contained native Couchbase initialization but no previous shared document/point trees. Raw baseline is private under `artifacts/fresh-palma-20261010/`.

## Fresh-install defect and fix

The native `ONYX_SYSTEM_CONFIG_KEY` binder reply contained a zero-length string. PowerSync 0.1.3 attempted to parse it as JSON and stopped before enrollment. Native SystemConfigBean defaults to an empty configMap; native notebook/Reader getters default to false. PowerSync 0.1.4 now recognizes the empty system-config state, defaults absent flags to false, preserves unrelated existing settings, and still rejects malformed JSON/maps/flags and an empty cluster definition. Release unit tests (including five new configuration cases), signed release build and lint pass under Java 17. In-place installation succeeds and the native status screen reports signed out, both sync flags false, not_configured. Reading status did not write native settings.

BOOX auto-freeze also disabled the installed app. The user corrected the persistent freeze setting; `pm enable` alone is insufficient while that policy remains enabled. All Files Access granted through the standard Android settings screen.

## Enrollment and observations

SSH access resumed after the user restored VPN connectivity. Captured server notebook projections and live object bindings before enrollment. Created a one-use code through the authenticated public owner form, entered it in PowerSync, and enrolled while signed out of Onyx. Both sync domains were explicitly selected. Native connect completed without an Onyx login, manual provider injection, or copied library assets.

First concurrent provider/resource capture: Palma 1,380 total notebook/folder rows, 980 active and sync-enabled, 1,117 shared resource files. Lumi 1,523 total rows, 1,082 active, 1,080 active sync-enabled. This is an in-progress catch-up observation, not a complete restore claim. Ongoing native log capture is private; final resource closure and visible rendering remain to be assessed.

Subsequent catalog comparison reaches 1,523 rows on both devices. All 1,080 Lumi active sync-enabled IDs exist on fresh Palma with identical ownership, parent, type, status, scene, association, sync-enabled flag and page-name list. Palma has one additional active sync-enabled synthetic text fixture, `0c303e6f-7ce4-4acb-af55-fb60370f28a2`, which is active but sync-disabled on Lumi. No flag was changed on Lumi. The fixture has no server resource bindings in the pre-enrollment baseline; its empty editor is not evidence of lost server bytes. Fresh Notes displayed the four native folders plus this fixture. Resource count rose through 3,918 and 5,727; hashes and rendering remain separate checks.

Installed 0.1.4 signer SHA-256 matches the pre-reset 0.1.3 signer (`f61b114923bef983aa12e876ea12bbc52f017363ef7103a96c2f978dc19aeb35`). Source changes and tests are retained in the repository; installed APK/build log are in the private evidence directory.

## Fresh EPUB cold-cache result

The uploaded third-delivery EPUB initially showed `fileSyncStatus=3` and could not open. Its initial Reader catalog import preceded the source-file scan. At 17:06:19 local, BookSyncFile logged a pending-download record; metadata then changed to 2 without server repair. Ordinary cover selection downloaded the source; another selection opened NeoReader and rendered its known content. SHA-256 exactly matches the original (`5dec11d953c3abb67261847107ac11316a5bdd408b8d9716e5172bfe67454f62`). A first-run spacing-adjustment notice appeared. Thus this cold-cache book case passes; transient unavailable status during queued initial scanning must not be mistaken for permanent missing cloud bytes. No notification republish, forced pull or ADB book copy was used.
