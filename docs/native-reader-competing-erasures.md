# Concurrent partial and whole erasure of one stroke

2026-10-08 bootstrap qualification: retaining immutable resources does not imply exposing every historical alternative in the live listing. [Fresh-book replay](native-reader-fresh-history.md) shows older resources can revive or disable shapes. Keep optional archival history separate from the current native projection.

2026-10-08. Private evidence: `artifacts/gateway-lab/qualification/reader-native-erase-conflict/`. Initial/passive/closure checksums preserve separate boundaries. Follow-up to [independent concurrent additions](native-reader-concurrent-ink.md).

## Result

Palma partially erased P's loop while Lumi erased the entire same P. The original P (`2e009087-e0af-495e-ba78-bf145b6b3df7`) is disabled on both. Palma's replacement fragments (`eb544b14-34a1-4fe9-a3f1-998ed3c4a82c`, `3f32a49f-91b5-45be-bc61-8a7174ae111f`) survive and render on both, even though Lumi's whole-erasure configuration won. This is survival of new identities, not re-enabling the old P. Both reopened screenshots show the remaining P stem/fragment and L.

After ordinary incremental pulls, both backups contain seven records: five active (two old squiggle fragments, L, two P fragments) and two disabled originals (squiggle and P). Geometry, status, point-revision dependencies and other drawing fields match. All five pressure files are byte-identical (2944, 3248, 5216, 7072, 7952 bytes). Full pulls and later incremental replay report no new data; full recovery was unnecessary. Fully automatic application is not qualified: before explicit pulls, native exports still held their respective local edits.

## Controlled boundary and publication

Replication was paused for the real edits. Passive PDFs/logs/server snapshots were captured before controls; the server was unchanged. Native backups and read-only CBL queries preserved both independent configurations. Overnight sessions had expired; ordinary login renewed credentials while replication remained paused, with note/reader flags reasserted false/true. Both local candidate commitId/updatedAt values were unchanged after renewal. No manual reader-grant repair was needed. The helper accepts only the two measured new PDF revisions for capture and pull-only operations; build/lint passed and certificate-matched upgrades preserved app data.

Both ksync processes were stopped, the proxy restored, and Palma connected before Lumi. Native queued work uploaded a 3248-byte fragment pressure file, an 835-byte ZIP containing disabled P plus two new fragments, and a 672-byte ZIP containing the whole-erasure disabled P alone. Attachments were SHA-256 checked and wire data decoded as ReaderShapeDocProto. The original 5216-byte P pressure remains stored and referenced by the disabled record; no immediate asset deletion was established.

The mode-7 configuration converged to Lumi commit `75d191f64a90404a8cbbccfbb665b950`, resolution revision `12-df8afa94d1058a5b28cbd825e7df34005804c616`. Its updatedAt is earlier than Palma's, again ruling out generic newest-timestamp selection here. No host document refresh or explicit source native push retry was issued.

## Residual record metadata divergence

The disabled P information revision does not converge: Palma carries Lumi's `5d86c3a2-07c8-4ea6-9524-9297178339e5`, while Lumi carries Palma's `559815e1-96f2-426b-9eed-8128584ee484`. Each imported the other's deletion metadata. Full replay leaves this split intact. Both still agree on disabled status, geometry and the original pressure revision. Local UPDATEDAT and PDF OBJECTID differences also remain, as in previous imports. Do not claim byte-identical native records or a globally ordered winner for every shape field. Persist both candidates for later same-ID status/reactivation analysis.

## Preservation check exception

Notebook guards pass on both devices and the original reader guard is exact on Palma. Lumi's strict original-reader comparison fails for one unrelated row: fileSyncStatus changed 0 to 1; every other compared original field and row remains unchanged. The UUID appears in native MergeMetadataHandler logs at epoch 1791438563, before this turn's passive capture (1791439089) and before renewal/reconnection. It is absent from the baseline, passive and post-reconnect lab-server documents. This supports earlier native metadata maintenance, but does not establish the precise initiating event or prove file-byte equality. The flag was not blindly reverted and the original baseline was not replaced. Detailed differences and earlier log excerpts are private evidence.

## Facade implications

Native erasure operates on observed shape identities. A whole erase cannot name fragments created independently on an offline peer. Preserve native-compatible semantics: select configuration through native CBL behavior, retain all admitted immutable deltas/dependencies, and let the native importer apply shape status and distinct fragment IDs. Do not infer that a winning whole-erasure configuration authorizes deleting every asset or every replacement identity. Optional server-side alternative review can explain a surviving fragment, but cascading deletion would be an additional policy decision, not compatibility behavior proven here.

Same-ID status restoration after these competing tombstones remains open, as does whether a later deliberate erase of the now-observed fragments removes them from both peers. Automatic download/application timing and garbage-collection lifetimes also remain separate gates. Replication and facade are running; devices remain on the disposable merged fixture, reader-session-grants marker active.
