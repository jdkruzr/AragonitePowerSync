# Native reader ink addition round trip

2026-10-07. Private evidence: `artifacts/gateway-lab/qualification/reader-ink-writeback/`.
Follow-up to [shape ingestion](native-reader-shape-ingestion.md). The user wrote
“Hi.” on the Palma and went Back to save. This is handwritten ink, not a text
resource or server OCR result.

## Passed behavior

The Palma native backup contains six shapes: the original imported squiggle plus
five new pen strokes. It has the original 11152-byte point file and a new 6576-byte
point file. A new 941-byte shape ZIP is generated. After exact-key admission,
native ksync uploads both new objects and updates the mode-7 configuration.
No host fabricates shapes or injects files into either device cache.

The Lumi downloads the new shape ZIP through native ksync. Native ReaderSyncService
imports five new shapes and returns success/newData=true. Its ordinary-app native
backup then has the same six shape IDs, document IDs, point revision IDs, bounding
rectangles and point save types as the Palma backup. Both point files are exactly
equal. A screenshot shows “Hi.” in the original Lumi PDF. PDF bytes on both sides
remain the original 22562-byte annotated derivative; therefore the new lettering
is delivered by the native ink graph rather than an updated embedded PDF stamp.
A repeated full native pull returns success/newData=false.

This qualifies native reader ink addition write-back from Palma to Lumi, after
prior Lumi-to-Palma shape ingestion, under the explicit controls below. It does
not qualify erasure, rotation/crop transforms, sibling ink conflicts, or a fully
automatic first-attempt sync session.

## Controls and negative observations

The lab initially denies unknown shape/point keys. The real edit generated new
keys, so its first upload attempt was rejected. Only the two observed keys for
this authored book were subsequently admitted; a native push retry stored them.
The normal online path with dynamic enrolled-book key authorization was not
exercised. Do not generalize these deliberately induced failures into a stock
protocol defect.

The existing server-authored type-4 commit point stayed at its original revision
and timestamp throughout peer publication. We refreshed that exact control only
after both new dependencies were stored, and used scoped Binder transaction 17
library start. This is a controlled server trigger, not native source trigger
emission proof. The Lumi fixture's per-book sync flag was also still off from
restoration; ordinary-app fixture-only eligibility publication and ksync cache
restart were needed. Both global and per-book reader eligibility matter.

An incremental application attempt consumed the mode-7 update before the new ZIP
was available. Later callbacks reported success/newData=false; the Lumi backup
still had only one shape, although both point files were present. The native
importer had no new shape data to process at the first merge. This is a reproducible
receipt-versus-application distinction, not a successful ink round trip.

Static PullDocDataAction supports MetadataExtraInfo.pullFullUserDataFromSync.
The ordinary-app helper sets this one-shot flag only on the fixed source fixture,
preserving the other extraInfo fields. NeoReader consumes it, clears it and uses
its native full-user-data pull. The subsequent importer logs the new shape
revision and `update sync shape count: 5`. Full replay adds no data. No raw reader
database editing or replacement of the original device libraries was used.

## Preservation

Before cleanup, every preexisting personal reader row is still byte-for-byte equal
to its full metadata baseline, and notebook guards pass (1517 Palma, 1518 Lumi).
Disable both lab sync domains, remove only the new managed fixture alias on Palma,
restore the source fixture flags to 0/0, and restore original clusters/accounts and
note/reader flags. Compare every pre-run reader row, including the previously
retained blank disabled Palma row; do not hide that row by resetting the guard.
Restore the pre-shape-experiment server sync policy, device principal channels and
both asset allowlists. Retain the native exports and user edit, then stop owned
services while keeping disposable lab volumes. Final restoration receipts and
checks are included in private evidence.

## Alexandria facade consequences

Persist native shape ZIPs and pressure files with exact bytes, then publish a
usable reader asset-fetch trigger and application notification after dependencies
are available. Admit reader commitType 4 separately from notebook commitType 1.
Do not equate a mode-7 CBL revision or reader success callback with usable ink.
Account for disabled per-book flags and consumed incremental records after late
asset arrival. The proven one-shot full-pull path is a bounded recovery option;
its replay acknowledgment still needs shape/point closure checks.

The remaining automatic-path test needs valid newly generated asset keys to be
admitted on their first attempt, then observation of native type-4 trigger emission
or the equivalent coordination path. This case's manual commit refresh and full
pull must remain explicit in the deployment plan. No production deployment is
included in these results.
