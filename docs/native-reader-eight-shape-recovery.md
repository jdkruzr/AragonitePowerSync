# Fresh eight-shape reader trial: publication, receipt, and application

2026-10-07. Evidence: `artifacts/gateway-lab/qualification/reader-clean-native-ink/`.
The preceding seven-shape checkpoint remains unchanged.

## Result

The user added one real pen stroke on Palma and saved with Back. The final controlled
recovery has eight matching native shapes on both devices, identical shape IDs,
point revisions, document IDs, bounds and save types, and four byte-identical
pressure files (3680, 6576, 10960, 11152 bytes). Incremental replay succeeds with
`newData=false`. Original reader guard rows and notebook guards pass on both devices.
No full-userdata pull was used. **Automatic first-attempt synchronization remains unqualified.**

## Passive trial

The saved clean baseline had seven shapes on each device. Before any source retry,
receiver start/reset/pull, or server commit/MESSAGE refresh, logs and server inventory
were captured. Server document revisions did not change and no fresh asset transfer
occurred. Source logs at epoch 1791409521 detect one changed shape, then
`PreparePushMetadataRequest: metadataList size error ... size = 2`.

The Palma had two rows for cloud book `cbd2938779124b2b83413f76dbe8ab12`, IDs 139
and 140, both resolving to the source fixture path. Earlier evidence identifies 139
as the original locally authored fixture row; the second is the later fixture alias.
The attempted managed-path export failed with an empty native callback. Exporting
by the authored source path worked and preserved eight shapes; Lumi still had seven.
The exact transition that collapsed the alias paths was not isolated. Do not claim
that this proves a generic production duplicate-book defect.

## Journaled controls and observations

1. Preserve both metadata rows and native ink backups. Delete only fixture metadata
   row 140 with ID, UUID, and exact authored path predicates. Verify one cloud row
   remains; no ink database or asset deletion is performed.
2. Fix the helper source sync guard to accept the exact original authored PDF
   (2270 bytes, SHA-256 `6ccfeddb1f27d9d62975d78f0ec7d4c3f4c1bb8af69aa9786b5cd3974fa443bd`)
   as well as the previously admitted annotated derivative. Managed-peer admission
   remains exact to the derivative. Android assemble/lint pass; signer matches the
   preceding tested APK; Palma receives an in-place upgrade.
3. Explicit native source push succeeds. Native OSS PUTs admit a 671-byte fresh shape
   ZIP and 3680-byte pressure file. Source publishes a native plain-ID commit and
   updates the existing plain-ID READER_LIBRARY MESSAGE. Lumi dispatches that MESSAGE.
   Source logs also show CBL 10409 revision conflicts for the mode-7 document and
   shared MESSAGE. The server mode-7 updatedAt stays 1791408017504 while source native
   query returns 1791409520940. MESSAGE dispatch is therefore not publication closure.
4. Refresh the native commit on the server without a MESSAGE refresh. During the
   bounded 20-second observation no fresh Lumi asset GET occurs. This is a timing-
   bounded observation, not proof that commit-only delivery can never work.
5. Refresh the MESSAGE after the commit. Lumi natively GETs the exact fresh ZIP and
   point file (both HTTP 200, matching upload hashes). Its ordinary incremental pull
   still returns success with `newData=false` and remains at seven shapes.
6. Reset the source native replication checkpoint. The mode-7 conflict persists;
   server configuration remains old. This did not resolve the native conflict.
7. Explicitly copy the newer source-native mode-7 body into its existing server
   document with CAS, then refresh MESSAGE. This is a fixture-only server intervention,
   **not native conflict resolution or a production conflict-policy decision**.
   Lumi incremental application then returns success with `newData=true` and imports
   the eighth shape. Exact closure and idempotent replay pass.

ZIP SHA-256: `f3a9f64a26f09d828edc0f3268f7085355d355fe3cff181de85fbf0e8553bd8e`.
Pressure SHA-256: `fee41ebf9b233ac40a21018fb5c9ef31319c0cd58c3bc1b0429b7b31ecb6be57`.

## Implications for Alexandria

Keep three receipts separate: durable assets, accepted reader configuration/commit,
and native reader application. Do not mark a document synchronized from HTTP PUTs,
MESSAGE dispatch, or even successful incremental callbacks alone. Detect metadata
identity ambiguity before requesting publication. Archive rejected branch observations
when available; a winning server changes feed cannot preserve a revision it never accepted.

The downloaded-but-not-applied result followed by configuration-copy recovery supports
configuration freshness as an application gate in this case. It does not establish
all necessary conditions or generalize default conflict resolution from annotation
mode 2 to reader configuration mode 7. Existing native-compatible conflict policy
remains unchanged. No always-newer-updatedAt server resolver was implemented.

Next qualification should isolate mode-7 divergent revisions and use a fresh owned
book/revision lineage, with exactly one eligible source row and no prior host-authored
commit/configuration history. Reusing this fixture is useful for controlled recovery
but is not a clean first-attempt baseline. Ink erasure remains a separate pending test.

## Handoff

Both devices are at Home with the eight-shape ink retained; reader-only lab sync remains
staged, original endpoint restoration is still pending, and disposable services remain
running. The bounded trace is stopped and its logging restoration must be checked in
`control.jsonl`. Full raw logs/provider rows/exports are private; do not publish them.
The new evidence directory receives its own SHA-256 manifest after trace snapshots.
