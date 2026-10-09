# Concurrent native ink additions

2026-10-07. Private evidence: `artifacts/gateway-lab/qualification/reader-native-ink-conflict/`; separate initial, passive and closure checksums preserve the experiment boundaries. Follow-up to [reader grants and clean conflicts](native-reader-grants-and-clean-conflicts.md).

## Independent edits and native publication

With the replication proxy stopped, the user wrote P on Palma and L on Lumi in the same disposable PDF/page, then closed each to save. The asset/account facade remained available, but the passive server snapshot was unchanged. Each native backup contained the three previous records plus exactly one new active letter. Read-only CBL queries proved distinct native configuration/commit candidates before reconnect; no host-authored document refresh or native source push retry was issued.

P shape: `2e009087-e0af-495e-ba78-bf145b6b3df7`, new pressure 5216 bytes. L shape: `804341fe-758f-4ab3-83cd-a4bdfa2017af`, pressure 2944 bytes. Both native documents retained configured width 4. Exported THICKNESS differs by device display scale (2.91262126 at 1.3733333 vs 1.29032266 at 3.1); each product is approximately 4. Do not equate raw thickness across different viewports. Existing shape metadata is preserved when importing.

The exact user-authored PDFs became 36768 bytes on Palma (SHA-256 d9d7560d3bd96f74123de2f7d5507d78a30f9f0030d14dd489a54067293ccd10) and 8739 on Lumi (0db1571f6e2a37293f28197aa8bf2dc60086e05ffbdf54d90f8421a75673f548). Narrow capture and pull-only guards were extended to those measured revisions; Android build/lint passed and certificate-matched upgrades preserved app data.

## Configuration conflict versus ink merge

Both ksync processes were stopped, the proxy restored, and Palma started before Lumi. Native queued work uploaded P's 672-byte shape ZIP/5216-byte pressure and L's 674-byte ZIP/2944-byte pressure. Downloaded asset attachments were independently SHA-256 verified. Each wire ZIP contains only its new letter; neither is a replacement snapshot of all page ink. Both asset sets remain in the server.

Mode-7 configuration proposals are sibling generation 9 from parent `8-904b2c898f06eb6ad97a0817d0c55b33e126458d`: Palma `9-409d84ae444bc04e6395aac1a20ce70ad6116ab6`, Lumi `9-670bf79cbe72616142091b983f704fe7230d2bc1`. A native resolution revision `10-bcaedf86f61d5041423d8be3906039501e87be7f` carries Lumi commit `05f95e4ab14447e9b938b6446a4ab3a3`. Both CBL queries converged on that candidate, whose updatedAt is earlier than Palma's. The observed selection is consistent with native/default CBL conflict behavior, not generic timestamp-newest selection.

After reconnect, both pressure sets were cached, but native exports still had only their own letter. Automatic end-to-end ink application therefore does not pass this case. An ordinary incremental reader pull then added the missing peer letter on each device, with success/newData=true. Both exports now have exactly five records: four active shapes (two squiggle fragments, P and L) plus the disabled original squiggle. Every portable decoded shape field matches after normalizing omitted default status; local PDF OBJECTID and local UPDATEDAT differ. The creator retains its PDF object mapping, while the imported peer has no such mapping and receives a local import timestamp. These differences are recorded in shape-field-differences-private.json, not hidden by claiming raw export equality. All four pressure files are byte-identical (2944, 5216, 7072, 7952 bytes). Both reopened screenshots visibly show P and L. A subsequent scoped full pull adds nothing, as does incremental replay; full recovery was not needed. Original reader and notebook guards pass and the temporary full-data flag is false.

## Server responsibilities and limits

Configuration conflict selection is not page-ink replacement selection. Preserve all native shape deltas and their pressure dependencies, including assets associated with the losing configuration candidate. Here the existing native importer unions independent shape IDs after one CBL configuration wins; the server did not decode or merge strokes. Retaining alternative configuration bodies remains useful, but is distinct from retaining every referenced ink asset.

This case qualifies independent additions on the same page and later incremental application, not automatic first-attempt delivery, same-shape edit/erase conflicts, arbitrary concurrent page-map changes, asset garbage collection, or identical embedded PDF bytes. Native receipt, downloaded caches, persisted exports and visible rendering are separate gates. Next: simultaneous partial and full erasure of the same P shape to examine status records versus replacement fragment identities.
