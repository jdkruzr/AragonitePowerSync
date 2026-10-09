# Reader resource rediscovery trigger factors

2026-10-08. Follow-up to the earlier commit/MESSAGE recovery matrix. Owned clean-v2 PDF only; baseline 21 native shape IDs and 14 pressure files on both devices. Evidence: `artifacts/gateway-lab/qualification/reader-trigger-factor-matrix/`.

This experiment removes only the exact existing T shape ZIP from the two ksync caches after byte-for-byte SHA256 checks and escrow. Server object bytes, native ink records and pressure files are retained. It measures resource rediscovery, not first-time native ink application or complete fresh-install delivery.

An unchanged-body commit revision followed by a native-shaped reader MESSAGE drains earlier work before the first eviction. Each row starts with exact caches present, eight seconds settling, and independent cache eviction. Controls are no trigger, MESSAGE only (fresh syncAt), unchanged-body reader commit revision only, unchanged-body mode-7 revision only, and ordered commit revision plus MESSAGE. Four five-second observations give a bounded 20-second window. Native/server logs and request journals cover each row.

After each row, an ordered commit-plus-MESSAGE reset is sent, exact caches must return on both devices within 35 seconds, and eight seconds settling follows. This prevents a later MESSAGE row simply inheriting an unprocessed commit-only control. It is a bounded operational reset, not proof of an entirely clean native database or fresh installation. If reset fails, escrow restores only the removed cached ZIP, that cleanup is explicitly labeled assisted, and the matrix stops. No helper, SDK ink pull, native editor action or shape-row mutation occurs inside the matrix.

Final rows, positive-control qualification and native/library preservation are recorded after completion. Sync-library MESSAGE is account-wide by native semantics even though the cache eviction and changed commit/configuration are scoped to one owned book. Prior native/library guards must be checked; the test does not claim per-book MESSAGE dispatch.

| Control | Palma | 10.3 II |
| --- | --- | --- |
| No trigger | No refill in 20 seconds | No refill in 20 seconds |
| MESSAGE only | No refill in 20 seconds | No refill in 20 seconds |
| Unchanged-body commit revision only | No refill in 20 seconds | No refill in 20 seconds |
| Unchanged-body mode-7 revision only | No refill in 20 seconds | No refill in 20 seconds |
| Ordered commit revision plus MESSAGE | Exact refill, one target GET | Exact refill, one target GET |

All five intervening ordered-pair resets restore exact caches on both devices without SDK/host cache injection. Final exports retain all 21 records and 14 pressure files against each own baseline, excluding only local UPDATEDAT/OBJECTID; no added shapes. Original notebook and unrelated Reader guards pass with the known user-addition exception. Server mode-7 and commit bodies are unchanged apart from revisions, and every existing asset document remains unchanged.

This tightens the earlier state-dependent MESSAGE-only result: in this separately reset current state, MESSAGE alone is insufficient in the bounded window; the ordered pair is sufficient without a mode-7 body change. It does not establish necessity under all prior native states, exactly-once delivery, arbitrary outage recovery or first-time Reader application. Use durable resource publication followed by the journaled ordered pair as the qualified cache-reconciliation candidate; ink-import readiness remains separate.
