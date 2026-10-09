# Reader commit revisions and MESSAGE recovery

2026-10-08. Private evidence: `artifacts/gateway-lab/qualification/reader-commit-trigger-matrix/`. Follow-up to [known-fragment deletion](native-reader-known-fragment-deletion.md).

## What this isolates

The tested asset is the existing, server-verified 741-byte deletion ZIP. Each trial removes only that exact cached ZIP after SHA-256 verification, then observes native refill. Native shape rows and server mode-7 contents are not changed. This is an asset rediscovery/recovery experiment, not a new ink edit or proof of first-time automatic application.

| Control | Palma | Lumi |
| --- | --- | --- |
| Fresh commit revision with updatedAt zero; ten-second passive observation | no refill | not tested |
| Scoped start after that zero-timestamp revision | exact native refill | not tested |
| Fresh current-timestamp revision; ten-second passive observation | no refill | not tested |
| Scoped start after current-timestamp revision | exact native refill | not tested |
| Scoped start without new commit, after renewal/settling | no refill in eight seconds | not tested |
| Zero-timestamp commit revision, then plain-ID native-shaped MESSAGE | exact native refill, no host start | not tested |
| MESSAGE without fresh commit in repeated trial | refill | no refill in fifteen seconds |
| Identical commit body saved as a new revision, then MESSAGE | exact native refill | exact native refill |
| Early MESSAGE, 350 ms later identical-body commit revision, then MESSAGE | exact native refill | exact native refill |

The commit's updatedAt need not become current. A new Couchbase revision with the identical body and zero timestamp can drive native recovery when followed by the reader MESSAGE. MESSAGE-only results differ across local states, so do not claim either that every doorbell requires a fresh commit or that every doorbell scans every book. Passive-window failures are bounded observations, not infinite-time claims.

Sessions were close to expiry after the initial zero/current matrix. Both were renewed before subsequent controls; reader sync was re-enabled and note sync kept disabled. No manual grant repair was needed. This renewal boundary is recorded and prevents treating all matrix rows as identical session/cache state.

## Original ordering and clock probe

The original deletion Gateway trace orders mode-4/mode-7 changes, MESSAGE sequence 4061, asset sequence 4062, commit sequence 4063, then MESSAGE sequence 4064. The first MESSAGE preceded durable asset publication. Its syncAt was 1791441335773; the later MESSAGE carried 1791441334612. Palma's captured logs show dispatch of the first but no matching second dispatch. This narrows the original failure to delivery/processing of the later trigger/commit combination, but does not prove a specific lost-message cause.

A separate live clock probe publishes a higher syncAt and then a value three seconds lower, eight seconds apart. Both devices dispatch both revisions. Therefore a simple monotonic syncAt rejection rule is not supported. The overlapping-doorbell repeat also succeeds on both peers; the original stall is not reproduced merely by two closely spaced notifications.

## Static path

Local Lumi decompilation provides the path:

- StartReplicatorByMsgRecordAction handles READER_LIBRARY by calling KReaderSyncManager.sync(), which starts all-document reader library replication when reader sync is enabled.
- ReaderLibraryReplicator.onSyncChangeFinished drains pulled document IDs through HandlePullLibraryDataAction.
- SavePullReaderLibraryRecordRequest recognizes commitStatus-bearing documents and adds their documentUniqueId to the start list. updatedAt is used for sorting, with no explicit nonzero-timestamp gate in this method.
- StartPullReaderReplicatorRequest filters ordinary book IDs through enabled, sync-enabled, migrated mode-4 metadata and posts StartReaderDocReplicatorEvent.
- ReaderDocDataReplicator loads CompareReaderUserDocSyncFileAction, which lists the book's OSS prefix and compares shape/point/resource/book files.

These explain why configuration receipt and SDK pull/import are not equivalent to scheduling the asset scan. They do not by themselves establish which original callback/revision was missed.

## Preservation and facade implications

Both final native exports retain seven IDs, three active/four disabled records, and all five pressure files byte-identical to the prior closure. Both notebook guards pass, unrelated reader metadata has no additional drift, and the mode-7 commit remains fc7afd3e56284dd4bca8910415246987. Lumi's earlier unrelated fileSyncStatus difference remains documented; no baseline is replaced. One Lumi export attempt timed out while the device was on the helper's all-files-access settings screen; permission was already allowed, Back and one bounded retry succeeded. No permission changes were made.

The compatibility facade should track durable assets, commit publication and outgoing doorbells independently, and keep a bounded retry/reconciliation path when those stages do not result in peer discovery. This experiment supports an ordered commit-revision-plus-MESSAGE recovery candidate. It does not justify rewriting all native timestamps or indiscriminately touching every book. Exactly-once delivery, arbitrary offline recovery, and automatic native ink application remain separate qualification gates.
