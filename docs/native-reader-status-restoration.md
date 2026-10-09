# Native erasure and Undo restore the same reader shape

2026-10-08 bootstrap qualification: retaining immutable resources does not imply exposing every historical alternative in the live listing. [Fresh-book replay](native-reader-fresh-history.md) shows older resources can revive or disable shapes. Keep optional archival history separate from the current native projection.

2026-10-08. Evidence: `artifacts/gateway-lab/qualification/reader-native-status-restore/`. Follow-up to [known-fragment deletion](native-reader-known-fragment-deletion.md) and [commit/MESSAGE recovery](native-reader-commit-trigger-recovery.md).

## Result

Lumi erased the blue L while remaining in the reader. Palma subsequently had that exact shape disabled. Native Undo on Lumi restored the same identity, and Palma subsequently had it active again. The identity is `804341fe-758f-4ab3-83cd-a4bdfa2017af`; the original pressure revision remains unchanged. No replacement shape ID or new pressure file was created. Before and after the round trip, both exports contain seven records, three active/four disabled. The intermediate Palma export has seven records, two active/five disabled. All five retained pressure files remain byte-identical to the baseline.

The deletion ZIP is 681 bytes, contains exactly the L record with STATUS 1, and has information revision `d3572f5b-dae3-449c-916c-2f0cb37939af`. The later restoration ZIP is 669 bytes and contains the same L identity with active/default-zero status. Both server attachments are SHA-256 verified and decoded using ReaderShapeDocProto. Neither transition is a Couchbase document tombstone or hard asset deletion.

## Controls and limits

Passive read-only capture after the user erase showed no changed server documents; the open reader visibly lacked L. Source PDF bytes were still the previous saved revision. Host UI actions were kept inside the reader to preserve its Undo history. The ink toolbar Save control did not publish. The reader main-menu Sync control then saved and published the native configuration, metadata, commit, MESSAGE and deletion ZIP. No source SDK push, host configuration/commit refresh, or cache injection was used in this trial.

Palma's ordinary native pull returned result 1/newData false; its subsequent native export proved L disabled. This does not establish whether it had already applied the deletion before the explicit pull.

The host tapped native ink Undo once after receiver deletion was confirmed. L visibly returned on Lumi. An initial attempt to navigate back to Sync failed to open that control; its separate snapshot showed no new asset. A paced UI navigation retry invoked Sync and published the restoration. The source reader was not closed during erase, deletion publication, Undo, or restoration publication.

Palma's first explicit pull after restoration returned result 0/newData false; the following export nevertheless proved L active under the original ID. Later ordinary replay returned result 1/newData false. Do not treat callback 0 as evidence of failed native application, or callback newData false as evidence of an unchanged ink state. Automatic first-attempt application is not independently qualified because no receiver export was taken before these explicit pulls.

After publication, source capture required adding the exact measured 22662-byte PDF digest to the helper's capture allowlist. This does not expand source push permissions. Build/lint passed and a certificate-matched in-place Lumi helper upgrade preserved app data. Source and receiver final exports agree on identity/status and pressure dependencies; local timestamps and PDF object metadata need not be byte-identical.

Both notebook guards pass. Unrelated reader metadata matches the immediately preceding baseline, with no additional drift. Lumi's previously journaled unrelated fileSyncStatus difference is neither reverted nor rebaselined. These checks concern metadata and the owned fixture's ink/dependencies, not a full personal-library byte audit.

## Alexandria policy consequence

An observed native erasure disables a shape; a subsequent native Undo can reactivate that same identity after the peer has applied the deletion. Preserve both admitted deltas and their pressure dependency. Do not convert STATUS 1 into an irreversible server-side purge, permanently reject later STATUS 0, or create a replacement identity on behalf of Undo.

This qualifies sequential erase/Undo propagation under explicit native UI Sync and ordinary receiver controls. Concurrent restore-versus-erase ordering, unseen out-of-order status deltas, and long-term garbage collection remain separate questions. The server can retain alternatives for later review while keeping native-compatible status behavior as the default.
