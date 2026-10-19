# TUCP compatibility and stale-join incident

2026-10-19 UTC; device/user local date October 18. Branch
`feature/tucp-compatibility`, based on the streamlined setup release.

## Installation

PowerSync 0.3.1 (code 12) lowers minSdk from 35 to 33, keeping compile/target
SDK 35. The native compatibility policy separately pins model, Android API and
ksync version: Palma2_Pro_C/35/32173, Go103_2Lumi/35/32079,
TabUltraCPro/33/32174. Unknown combinations remain blocked; login and cluster
transactions also check this policy at their entry points.

TUCP ksync's AccountIntentArgs, SyncClusterArgs, both callbacks, cluster helper,
and account provider decompile identically to Palma 32173. ISyncService differs
only in decompiler field aliases; relevant transaction numbers match.
This is interface evidence, not conflict-resolution or end-to-end qualification.

40 unit tests pass; release lint reports zero errors and 12 existing warnings.
Signed assembly and two release-instrumentation checks pass on TUCP as the product
UID: native configuration/account/catalog reads and service binding/descriptor.
The disposable instrumentation package was removed. BOOX automatically disabled
the newly installed product; it was enabled and its setup Activity launched.
Native cluster, flags, account identity, marker and inventory hashes matched
before/after installation (1,534 notebook rows, zero reader metadata rows).
The owner performed enrollment afterwards.

Signing SHA-256: `f61b114923bef983aa12e876ea12bbc52f017363ef7103a96c2f978dc19aeb35`.
Release APK SHA-256: `dd0458d35deb99b857285beae61af209e7bff9e9fb91c8fb30a74ee2efc4a918`.
Private local evidence: `artifacts/tucp-20261018/`.

## Recognition count diagnosis

The reported 1,724 of 3,495 jobs was a page-job counter, not a notebook count.
A read-only comparison found no duplicate current jobs for the same notebook/page.
The TUCP had 1,548 distinct notebook/folder IDs after joining, versus 1,534
before installation. Server canonical records increased from 1,536 to 1,549;
active records rose from 1,048 to 1,100. Of existing records, 41 changed from
deleted (status 0) to active (status 1), with 39 reverting to older native
updatedAt values. The remaining 11 active additions were new server records.

One reactivated notebook, MDO 2025 - Sunday, 12h, Cover 1
(`1084add9-c254-43b5-949f-c1bf12bfad2d`), accounts for 1,791 new page jobs.
Its existing server deletion was explicitly published October 9, 19:30 UTC.
Revision 77 remained deleted until the join. Revision 78 arrived October 19,
01:53:32 UTC with active status and a native updatedAt from September 2, 2025;
revision 79 arrived 01:55:48. TUCP enrollment was 01:51:23.
No planner asset versions arrived during this window. The automatic recognition
path selected the reactivated notebook after its quiet period and queued its
previously unindexed pages at 02:01:27. The metadata's page list remained unchanged.

This establishes stale active state superseding a newer deletion, followed by
normal automatic-recognition enrollment. Exact native conflict/rebase origin
still needs isolated reproduction; interface equality was insufficient protection.
Missile has one recognition job, completed, and its audio minutes were separately
queued. Its audio did not create the 1,791 planner jobs.

## Owner-authorized MDO cleanup

The owner explicitly requested deleting this planner and all related jobs.
The normal owner skip-notebook endpoint first invalidated pending recognition
leases and installed a persistent recognition exclusion for this native ID.
The revision-fenced preview/execute API then published deletion operation
`143764ca-cf3d-409b-874e-5f45361269a4`, resulting in revision
`80-ace0905986ba76740faf06219070a523`, status 0. TUCP's native NoteModel query
confirmed status 0 before the device disconnected.

A scoped transaction, guarded by exact title/ID/deleted status and the persistent
skip row, removed 1,791 boox_page_index rows, 1,791 recognition job rows and one
recognition batch. No related library-run entries, minutes, transcription or
note-action jobs existed. Native deletion/history remain for sync propagation;
resource blobs were not physically purged. No other notebook was deleted.

Final server check: planner status 0, zero page/job rows, one skip guard.
Recognition totals were 1,705 current jobs: 1,702 ready, two cancelled, one skipped,
zero queued/processing. Counts may change as ordinary work continues.
Private evidence: `artifacts/tucp-duplication-20261018/` (provider snapshots,
revision comparisons, job breakdowns, guarded SQL receipt and final checks).

The other 40 reactivated notebooks were left untouched. Fixing stale-device join
reconciliation remains required; the planner exclusion prevents automatic
recognition recurrence but is not a universal deletion fence.
