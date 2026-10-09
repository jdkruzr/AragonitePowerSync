# Reader Sync Switch: static qualification

2026-10-08. Source inspection, not a newly reproduced runtime transition.

`SyncDocumentAction.C` requires both `OnyxSystemConfig.isSyncReaderDataEnabled()` and `Metadata.isUserDataSyncEnabled()`. If either fails and tips are enabled, it displays the same book-switch confirmation; with tips disabled it returns false silently. Missing metadata instead displays a disabled-book tip. The confirmation handler enables global reader sync and sets the current metadata user-data sync status to 1. `EnabledUserDataSyncRequest` also persists status 1.

These are independent persisted gates. Setting the global flag does not establish that an existing book is eligible. `Metadata.updateUserDataSyncStatusBySystemConfig()` assigns 1 only when global reader sync is enabled and inferred storage is INTERNAL; otherwise 0. Creation/import code invokes this helper. `MergeMetadataRequest` also copies the incoming metadata user-data status. Existing books need not follow a later global toggle automatically. The exact cause of our previous fixture dialog is still unqualified: candidates include a per-book flag, an earlier global reset during account login, and imported metadata. No observed cause is claimed from these candidates.

Alexandria enrollment must verify global and per-book eligibility independently, scope changes to enrolled books, and journal both before/after values. A successful account login, a cached resource, or `extraInfo.autoSync=true` is not proof of either gate. The dialog text is insufficient diagnostic evidence because it conflates the checks.

Evidence: decompiled 10.3 II Reader sources copied with checksums into `artifacts/gateway-lab/qualification/reader-sync-switch-static/`. Cross-firmware equivalence and runtime flag transitions remain open.

Runtime follow-up: [config cache and delivery](native-reader-config-cache-delivery.md) finds a native tab reporting global disabled while persisted flags read enabled; normal config notification plus reopen/close recovers a retained user stroke in the same tab process. Process cache readiness is an additional gate; historical per-book dialog attribution still needs separate evidence.
