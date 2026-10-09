# Concurrent same-ID ink status divergence

2026-10-08. Native concurrent erase versus Undo restoration **does not converge** in this trial. Both CBL configurations converge to Lumi's restoration, but Palma ends with L active and Lumi ends with L disabled. Ordinary import, full import, and subsequent ordinary replay preserve this split.

## Observed sequence

Replication was disconnected while Lumi staged a native Redo deletion followed by Undo restoration. Palma then independently erased blue L and saved with Back. Passive exports qualified both local candidates before reconnecting; source staging made no server changes. Reconnect order was Palma then Lumi, with scoped native starts. No host commit refresh, source SDK push retry, cache injection, or PB ledger reset was used.

| Candidate | L status | Information revision | Mode-7 commit |
| --- | --- | --- | --- |
| Lumi Undo restoration | active (0) | ac64f17a-07e0-4466-823c-e7bd9c2d3721 | 078010c5697a43d489da0e18d7bafa5e |
| Palma erase | disabled (1) | f93ca35b-b66a-4d2b-803c-059d8e914f0a | 26c988462ad34b758fc83e1a8e485595 |

All candidates address shape 804341fe-758f-4ab3-83cd-a4bdfa2017af. Three native wire ZIPs were retained: intermediate Lumi Redo deletion (674 bytes, information revision aff68c74-ed2a-4f06-a3a9-f2d3ac8e8499), Lumi restoration (676 bytes), and Palma deletion (674 bytes). No new pressure blob was required.

The server first accepted Palma configuration revision 16-b2c6f5f09aabb78845e42b465cbb97e94cb83a87, then resolved to 17-0e6236e602f094c364fb2fbcef1b4f75d3c25fa4 carrying Lumi's older updatedAt 1791444232201 rather than Palma's 1791444405201. Both local CBL snapshots matched this configuration winner. This is not evidence of newest-timestamp resolution.

Before explicit ink import, each device still had its own candidate. Palma's ordinary import applied unseen Lumi Redo and restoration; Lumi's ordinary import applied unseen Palma deletion. The devices thereby exchanged opposite statuses. Full import and a final ordinary replay did not repair them.

| Final native export | Records | Active | Disabled | L information revision |
| --- | --- | --- | --- | --- |
| Palma | 7 | 3 | 4 | ac64f17a-07e0-4466-823c-e7bd9c2d3721 |
| Lumi | 7 | 2 | 5 | f93ca35b-b66a-4d2b-803c-059d8e914f0a |

All seven IDs and all five pressure files remain baseline-exact. Notebook guards pass; unrelated reader rows show no additional drift relative to the prior sealed closure. The earlier Lumi unrelated fileSyncStatus 0-to-1 exception remains recorded, not erased by rebaselining. The full-pull flag is false after closure. These checks are not a complete personal-file byte audit.

## Static explanation and repair boundary

Lumi's ReaderShapePBDocumentImporter.execute consults PBRecordProvider.loadDocRecordMap and skips already-synced information revisions. An unseen record goes through ReaderNoteDataProvider.updateShapeList, whose transaction removes the existing same-ID shape and saves the incoming record. This path has no shape timestamp comparison or status conflict resolver. The importer then marks the information revision processed. This supports the observed split and replay idempotence; it does not establish ordering for every firmware/import path.

PBRecordProvider exposes restoreStatus(documentId) and deleteDocRecord(documentId), but those operations affect the whole document's import ledger. They are not a justified repair: replaying all historical same-ID deltas still depends on ordering, and resetting bookkeeping may replay unrelated edits. We did not invoke either.

Alexandria must keep resource-operation provenance separately from the CBL configuration winner and avoid equating metadata receipt with provider convergence. Detect incompatible same-ID status revisions, retain both alternatives, and report unresolved convergence. A deliberate repair could publish a fresh information revision for an explicitly selected state, retaining shape ID and pressure dependency. That would be a separate server policy operation, with a journal and device-export verification; it has not been qualified here. The user's native-compatible default does not authorize silently inventing a winner for this case.

Opposite reconnect order with fresh candidates, controlled fresh-revision repair, and automatic first-attempt application remain open. Restarting already-processed candidates would not qualify an opposite-order experiment.

## Evidence

Private artifact directory: artifacts/gateway-lab/qualification/reader-native-erase-restore-conflict/. INITIAL_SHA256SUMS seals staging; PASSIVE_SHA256SUMS seals pre-reconnect candidates; CLOSURE_SHA256SUMS seals the remaining complete trial. closure.json, final native exports, three asset ZIPs, before/after CBL snapshots, import logs, preservation guards, helper source/build log, and stopped protocol trace are retained. Devices are left at Home; lab proxy and account/asset facade remain live.

A read-only audit is available as `python3 scripts/audit-reader-status-conflicts.py <decoded-assets.json> --output <report.json>`. It flags opposing same-ID statuses, preserves asset/revision provenance, and never selects a winner. Sequential erase/Undo also produces such candidates: concurrency needs separate causal/device evidence. Qualification checks cover this trial's three operations, duplicate restoration, and inconsistent same-information-revision rejection. The private report and tested audit source are sealed separately in AUDIT_SHA256SUMS.
