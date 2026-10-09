# Deleting already-observed reader ink fragments

2026-10-08 bootstrap qualification: retaining immutable resources does not imply exposing every historical alternative in the live listing. [Fresh-book replay](native-reader-fresh-history.md) shows older resources can revive or disable shapes. Keep optional archival history separate from the current native projection.

2026-10-08. Evidence: `artifacts/gateway-lab/qualification/reader-native-known-fragment-delete-v2/`. Follow-up to [competing erasures](native-reader-competing-erasures.md).

The later deliberate erase removes the two previously surviving P fragments on both peers after controlled delivery and native import. Both exports have seven records, three active and four disabled. Final reopened screenshots visibly show L and the surviving squiggle with no P stem/tip. L and both squiggle fragments remain active. The two P fragment IDs retain their identity and pressure revision, but STATUS becomes 1 and information revision becomes `fa11b430-7ead-4417-bdde-343b273c48f3`. All five pressure files remain byte-identical between peers (2944, 3248, 5216, 7072, 7952 bytes). This is a soft deletion of known identities, not a Couchbase document tombstone or immediate pressure garbage collection.

## Passive source and explicit close

The user erased on Lumi with both devices connected. Initial read-only server capture showed no changed documents. Lumi native export had the two targets disabled and dirty information revisions, while Palma still had them active. A delayed source save changed the fixture PDF from 9243 to 9747 bytes; the helper's exact capture/pull-only digest guard was expanded to that measured revision. Android build/lint passed, and the certificate-matched Lumi upgrade preserved installed app data.

A subsequent host-controlled open and Back on the owned source fixture produced a confirmed reader exit and native publication: mode-7 configuration, mode-4 metadata, commit, MESSAGE, and one new ZIP. No explicit source SDK push retry was issued. The 741-byte ZIP contains exactly the two disabled fragment records, with unchanged pressure dependency. Mode-7 commit is `fc7afd3e56284dd4bca8910415246987`, updatedAt 1791441334389. Both CBL configurations received that version.

## Delivery and application controls

Palma did not initially download the new ZIP. Ordinary and one-shot full pulls both returned success/newData false and retained the five active shapes. A scoped library start, library restart, and receiver reader open/close also left the ZIP absent. The unscoped restart attempted unrelated pending local pushes, rejected by the existing disposable-fixture allowlist; that allowlist was not widened.

For isolation, the host copied the exact server-verified ZIP into the owned fixture cache. Ordinary pull still left the fragments active. Later full-pull and reader-process-restart controls returned result 0 and left the export unchanged; they do not qualify successful application. This failed cache experiment is explicitly journaled, not counted as native download. The injected file was then SHA-256 checked and removed, restoring the missing-asset condition.

The host changed only updatedAt on the exact native book commit (previously zero), retaining its other fields and leaving the mode-7 body unchanged. A scoped start followed. Native logs then show HandlePullLibraryDataAction and a ReaderDocDataReplicator task; the facade records Palma's HTTP 200 GET for the exact 741-byte deletion ZIP. This is native download after a host-authored commit-refresh control. It does not establish that a timestamp change alone is necessary or sufficient: the fresh revision and scoped start are part of the control.

A subsequent one-shot full native pull returned result 1/newData true and applied both deletions. Full replay returned result 1/newData false, preserving seven IDs, three active records, and all pressure bytes. No automatic first-attempt delivery/application claim is made. Ordinary import after this successful download was not independently tested before the full pull.

## Preservation and limits

Both notebook metadata guards pass. Unrelated reader rows match the previous sealed closure; there is no further drift. Palma also matches its original reader baseline. Lumi retains the earlier single unrelated fileSyncStatus 0-to-1 difference documented in the competing-erasure case; the original baseline is not replaced. The one-shot full flag is false on both devices. These are metadata and owned-ink checks, not a full personal-library file-byte audit.

The bounded Gateway trace ended with loggingRestored true. Initial/passive evidence remains separately sealed, and the final diagnostic bundle includes every subsequent control, native exports, private request/log snapshots, helper source, and build log.

## Alexandria facade consequence

Retain admitted native deltas and pressure dependencies, and preserve disabled records rather than inferring asset deletion from a winning configuration. A later erase can remove fragments once that device has observed their IDs. The prior concurrent whole erase could not name them.

Delivery needs separate observable stages: configuration replication, committed asset discovery/download, and native application. This trial demonstrates a recoverable commit/discovery failure in the lab path, not an established requirement to rewrite every native commit timestamp in production. Next diagnosis should separate commit revision delivery, timestamp handling, and replica/task scheduling using an already-admitted asset; retain failed controls as evidence. Same-ID reactivation and garbage-collection lifetime remain open.
