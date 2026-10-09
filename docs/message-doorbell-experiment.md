# NOTE_TREE doorbell live experiment

Run 2026-10-05, Chicago. Result: Palma's fresh `ZETA verified doorbell` edit
reached Lumi automatically, before any receiver UI open or manual sync, and
was confirmed in Lumi's native editor and active exported HTML resource.

## Accepted control plane

Gateway admitted only `user=powersync-lab`, `dbId=powersync-lab-MESSAGE`,
`msgType=6`, `replicatorName=NOTE_TREE`, numeric syncAt and non-deleted records.
The existing two-notebook/asset allowlist remained in effect. Both device
principals received MESSAGE access. Other message types and replicator targets
were denied. `gateway-fixtures.py --note-doorbell REPORT...` now configures
this optional experiment; its default removes the doorbell grant/marker.
The shim preserves that grant if it creates a fresh Gateway session.

## Successful window

Both native account logins were verified, both global native note flags enabled,
and native daemons restarted for baseline setup before the fresh Zeta edit.
No receiver sync/pull/restart command was issued after that edit. Palma Notes
was reopened after login to refresh its cached account state.

UTC times on 2026-10-06 (23:00 the prior evening Chicago):

| Time | Evidence |
| --- | --- |
| 04:00:15.590 | Palma notebook metadata stored as server sequence 111 |
| 04:00:15.880 | Palma MESSAGE revision stored as sequence 112 |
| 04:00:16.309 | Lumi logs StartReplicatorByMsgRecordAction, NOTE_TREE |
| 04:00:16.297–16.306 | Palma PUT of Zeta HTML succeeds |
| 04:00:21.310 | Palma native commit record stored as sequence 127 |
| 04:00:21.590 | Palma's next MESSAGE revision stored as sequence 128 |
| 04:00:22.001 | Lumi logs another NOTE_TREE message dispatch |
| 04:00:22.783–22.794 | Lumi GET returns exact Zeta asset revision, 1,686 bytes |
| Later | Receiver editor and active native export show Zeta |

The upload/download backend revision is `1-c232323f67f0635fda731577bdb61335`,
SHA-256 `55187ae07d17fa653030171d74d4e2fb158f73c4f6419a910f48c8ea2923aad8`.
Before receiver UI open: 11 successful Palma PUTs and 11 successful Lumi GETs,
including this exact matching asset. Source and receiver also issued HEAD,
other GET/PUT operations. Those counts are operations, not unique files or edits.
Unsupported object POSTs returned 501; despite these attempts the demonstrated
text update completed. The later [static audit](static-sync-semantics.md)
identifies these captured requests as bulk deletes (`?delete`), not multipart.
The facade still needs broader OSS operation coverage.

The notification can precede asset publication; a later notification followed
the commit. This run establishes functioning automatic delivery in the current
lab setup, not a proof that MESSAGE is its sole possible trigger or that one
notification alone always completes a notebook. No notification-disabled
control with identical timing was run in this turn.

## Setup failures, kept separate

Initial Delta/Epsilon attempts encountered asynchronous cluster-switch setup:
subsequent actions ran before the native switch/login had settled, leaving
devices back on en and logged out. MESSAGE handling appeared in those logs,
but those attempts do not qualify the complete experiment. Repeating setup
with an observed lab cluster, rewritten full lab cluster map, and successful
native login resolved publication. Subsequent actions did not invoke cluster
configuration refresh before the fresh Zeta edit.

The lab's scoped push helper also previously serialized a null KSyncContext.
The native default can be share_user, and logs showed a share_user / powersync-lab
mismatch. The helper now includes the explicit synthetic account and document
context for push as it already did for pull. The debug APK was upgraded in place
on Palma, preserving app data. The initial account/setup failure and this helper
defect are separate findings; the null context is not asserted as the sole cause
of the initial stalled run.

## Evidence, restoration, validation

Private artifacts: `artifacts/gateway-lab/protocol-traces/doorbell-run/` holds
intervention timestamps, parsed protocol, native receiver export and screenshot,
file correlation, account restoration and library preservation reports.
`doorbell-protocol/` holds the durable server journals and private raw trace.
Native logcat captures remain private because they contain broader device logs.

Original cluster maps, account state and note/reader sync flags were restored.
Palma is logged out; Lumi's original account restoration was verified. Test
markers and tunnels removed. Original deny-MESSAGE Gateway policy and original
principal grants restored, temporary doorbell marker removed. Capture reported
loggingRestored=true; listeners and lab containers stopped, data retained.
Outside the two fixtures, Palma's 1,517 and Lumi's 1,516 tracked rows retain
their original identity/status/ownership fingerprints.

Android build and lint pass; Python tests pass (14), including executed Gateway
sync-function admission/rejection checks for wrong user, message type, target,
scope, deletion and nonnumeric timestamp. General conflict handling, offline
recovery, multipart uploads and arbitrary handwritten notebooks remain open.
