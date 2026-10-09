# Reader configuration cache and native delivery

2026-10-08. One new U stroke on the original owned PDF. Separate first attempt and recovery; no interpretation of the assisted source recovery as fully unattended end-to-end delivery.

## First close: source eligibility failure

Palma's user drew U and went Back without tapping Sync. The server gained no documents or revisions and the foreground 10.3 II did not render U. Native `CheckSyncBeforeCloseAction` reported `needPush=true, dataChanged=true`. `ShowEnabledSyncTipDialogAction` logged global sync disabled; `SyncDocumentAction` logged `showTip=false` and did not proceed to ksync. Simultaneous persisted system reads on both devices returned reader sync STRING `true`, note sync STRING `false`.

This is an eligibility/cache failure before publication, not a demonstrated transport failure. Initial evidence was sealed before intervention. The owned PDF retained the saved stroke locally.

## Configuration notification and retained-edit recovery

Sent package-scoped `com.onyx.action.system.config.changed` broadcasts to Reader on both devices, then reopened and closed Palma's saved PDF through native UI. No manual Sync, helper push/pull, host-authored resource/config/commit/MESSAGE, account login or additional user edit occurred during this recovery window. This source reopen/close is explicit assistance.

The same Palma Reader tab PID changes from the earlier disabled gate to `sync to ksync`. Native publication stores a 672-byte shape ZIP and 12448-byte point resource, updates native mode-7/metadata/reader commit, and refreshes READER_LIBRARY MESSAGE. The foreground 10.3 II automatically downloads, merges the new information revision and visibly renders U before any helper capture or pull. Receiver application is autonomous in this recovery window; clean first-close source publication still needs a repeat with process eligibility already refreshed.

Native code corroborates a process-local settings cache: `OnyxSystemConfig` keeps an AtomicReference<SystemConfigBean>; the enabled getter consults its map, rather than performing a live Binder read. Its native save path persists settings then emits this config-change broadcast. Reader's `OnyxSystemConfigChangedReceiver` executes `LoadOnyxSystemConfigAction`. Direct Binder writes/readback alone cannot establish the active tab's eligibility. The combined notification/reopen control does not independently isolate the effect of each step, nor does it retroactively explain every prior book-switch dialog. The existing note-toggle script already emits a notification; the custom reader-only setup write used here did not.

## Exact data and preservation

After the screenshot and native importer logs established autonomous receiver application, signature-matched helper updates admitted only the two exact observed PDF derivatives for capture and pull-only operations. Build/lint passed; no new push guard was granted.

Both native exports contain eight shapes, four active/four disabled, and six pressure files. The new shape ID is `9c05fb2d-a140-4c43-acee-53068765dbf6`, pressure revision `b25c7064-c22b-49a5-a585-526beb1ea72c`. Its geometry, status, revision and other native semantic fields match across devices; all six pressure files match exactly. The original seven shapes' semantic fields and five pressure payloads are preserved. UPDATEDAT and PDF OBJECTID are local fields excluded only from the cross-device/source semantic comparison; later ordinary replay preserves every native field and pressure file exactly.

No unrelated reader metadata drift. Notebook preservation passes on 10.3 II and matches Palma's original baseline excluding only the previously confirmed Notebook-1 addition. Neither baseline nor fixture allowlist changed. Devices return Home after verification.

Evidence: `artifacts/gateway-lab/qualification/reader-unattended-native-delivery/initial-no-intervention/` and `config-refresh-recovery/`; separate checksum manifests preserve first attempt and recovery. Native/server observers stopped, Gateway logging restored to its prior level. Fresh clean-close repetition, expiry/server restart and ordinary-UID notification effect remain separate gates.

## Enrollment implication

Persist correctly typed flags, notify native processes through the supported settings-change path, verify per-book eligibility, and distinguish a notification request from observed process refresh. A successful flag readback or helper readiness check is not native-tab readiness. Record source publication, server storage, receiver cache, native import and visible application as separate stages.

2026-10-08 enrollment preflight follow-up: after scoped session renewal and native account_login_lab on both devices, persisted Reader flag is false while Notes remains false. The helper login implementation calls the fixed native account Binder ABI; it does not write these sync flags. Reader-only flags are reasserted, native config-change notification requested, and owned per-book start sent before staging any fresh edit. All 19 native records/13 pressures and unrelated-library guards remain exact afterward. This is observed behavior of this lab login flow, not proof that every vendor login must reset every sync domain. Evidence: reader-native-screen-off-delivery/session-preflight/.
