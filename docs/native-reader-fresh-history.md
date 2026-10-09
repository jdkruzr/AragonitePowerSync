# Fresh per-book history replay

2026-10-08. Four new disposable book namespaces on both existing devices; no app data, private databases, or original import records were reset. Book, shape, information, pressure-revision and subpage IDs are remapped, including identity fields in point files. Nonidentity point bytes and structural offsets remain exact. Initial cloud queries contain zero note-document models.

| Feed | Palma | 10.3 II | Replay |
| --- | --- | --- | --- |
| One canonical ZIP containing seven selected records, five point files | Seven records, three active; selected semantics match | Same | Exact seven native records and five pressure files |
| Full history, forward server publication | Seven records, four active | Same after explicit trigger | Exact replay of the incorrect historical result |
| Same source history, reverse server publication in another namespace | Seven records, six active | Initially zero records; unchanged-body config/commit revision plus MESSAGE recovers seven/six | Exact replay of the incorrect historical result |
| Fresh canonical baseline, then one old L-deletion resource | Seven records, three active becomes seven/two | Same | Full and ordinary replay preserve late deletion |

All point-file bytes match the explicitly remapped fixture payloads. For comparison to the original exported canonical shapes, exclude `UPDATEDAT` (local import bookkeeping) and `OBJECTID` (native importer explicitly clears it). The subsequent replay comparisons include every native field and are exact. Do not describe the first canonical import as byte-identical to the prior device export.

The first canonical run includes a journaled setup correction of embedded subpage references and commitInfo; its initial pull may overlap that correction. The fourth book establishes its canonical baseline after correction and before publishing the late deletion. That deletion's wire UPDATEDAT is older than the canonical restoration's wire UPDATEDAT. Both retain the same shape identity, exact pressure dependency and unchanged mode-7 body values; explicit new configuration/commit revisions and MESSAGE activate discovery/application. This is assisted qualification, not unattended delivery.

The forward/reverse trials also use distinct remapped UUID namespaces. Server publication order is not proven to be the sole cause of their different results; UUID names, OSS listing, caching and native file iteration can affect import order. The controlled late-deletion trial establishes the stronger result: an unseen older resource can overwrite an already-correct same-ID status. Static importer code checks the file information-revision ledger, replaces same-ID shapes and clears object IDs; it does not resolve this by comparing each shape's original wire timestamp.

## Server implications

An optional archive of losing or superseded alternatives must be separate from the native OSS listing/feed. Merely preserving all history in that listing is not a safe fresh-book bootstrap: a configuration winner does not select per-shape status, and an old resource arriving later can undo restoration. Preserve alternatives and dependencies durably for server-side review, while explicitly defining the current native projection and journaled reconciliation path. A canonical snapshot works in these assisted tests, but complete production compaction, client acknowledgement/checkpoints, cached stale-resource draining and retention policy remain open. Do not delete shared pressure resources just because a shape is disabled.

## Preservation and evidence

All pre-existing reader metadata rows are exact against setup baselines on both devices. The original seven-shape/five-pressure reader fixture remains exact against the post-reboot archives. Notebook guards pass on 10.3 II and match the original Palma guard after excluding only the user's confirmed Notebook-1 addition; baseline and fixture allowlist remain unchanged. History books are retained as disposable lab fixtures and devices return Home.

Evidence: `artifacts/gateway-lab/qualification/reader-fresh-history/`, including publication journals, native archives, initial/received CBL queries, full/ordinary callbacks, corrections, UUID mappings and private shape comparisons. Android assemble/lint passed; signature-matched helper updates used `adb install -r`. Facade listener failure and explicit session renewal are separately journaled setup controls. This is new-book replay on existing devices, not qualification of an entirely fresh installation or the user's full-library restoration.
