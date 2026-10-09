# Reverse reconnect order: delayed native ink divergence

2026-10-08. Reconnecting the 10.3 II before Palma initially left both devices showing restored L. A later unchanged-body mode-7 revision plus reader-commit/MESSAGE activated the cached competing deletion on the 10.3 II and exposed the same split as the first reconnect order. A separately journaled fresh restoration revision repaired both through ordinary import.

## Fresh candidates and controls

The user erased blue L on both devices, closed Palma, and left the 10.3 II reader open. Passive native exports qualified seven shape IDs and unchanged five pressure files with L disabled on both. The earliest deletion exports had no information revision yet; they are local edit observations, not published delta claims.

Native UI Sync displayed an owned-book Sync Switch-off dialog on both devices. The host accepted it for this fixture only. Global note sync remained false and reader sync true. The precise cause of the book-switch state is unresolved. On the 10.3 II, native Save/Sync staged the deletion, then one native Undo followed by Save/Sync staged restoration. Source UI restored L visibly; no helper source SDK push was used.

| Fresh native asset | Bytes | Information revision | L status |
| --- | --- | --- | --- |
| Palma deletion | 672 | fde4ea1c-fffa-4390-9b73-126163cf01ff | disabled (1) |
| 10.3 II deletion before Undo | 671 | 42eacd87-f491-467b-b0a5-11e521b16c5e | disabled (1) |
| 10.3 II Undo restoration | 677 | ba24ee17-b84e-4bfe-be8d-5001bdf4ca8c | active (0) |

All address shape 804341fe-758f-4ab3-83cd-a4bdfa2017af and retain the original pressure revision. Offline staging changed no server documents. The sessions had expired during the gap between turns; the host renewed them with replication still disconnected, restored native string-valued global flags, and verified both shape and CBL mode-7 candidates remained exact before reconnecting. No manual reader grant repair was needed.

Proxy evidence confirms the 10.3 II connected first at 17:21:14 UTC, Palma at 17:21:25 UTC. Scoped start order matches. Both CBL configurations settled on 10.3 II restoration commit ec3685d56e6a4afaa658f5c213858536, updatedAt 1791479883198, server revision 23-f0e3bbc2dbd987f2831e890bedf6ab8ba58d78d6. Palma's local candidate had newer updatedAt 1791479963456. This is not evidence of newest-timestamp status arbitration, nor a symmetric same-generation comparison: the source staged deletion and Undo separately.

## Apparent agreement versus actual competing-delta application

All three exact ZIPs reached the server and were cached on both devices. Palma ordinary import applied the source deletion and restoration, leaving L active under ba24ee17. The 10.3 II retained its own restoration. Its logs contained no fresh-delta importer entry; its full-pull callback returned result 0 without merging shape data. Initial replay also retained this agreement. These observations do not qualify conflict resolution on the 10.3 II.

The initial phase was sealed before any host document refresh. A separate control then published a new Couchbase revision of the identical mode-7 body, an identical-body reader commit revision, and MESSAGE with fresh syncAt. Mode-7 commitId, updatedAt and configuration values stayed unchanged.

Ordinary import now applied Palma's previously cached deletion on the 10.3 II. Palma retained ba24ee17 restoration. Subsequent full pulls and replay kept the split:

| After late activation | L status | Information revision | Active records |
| --- | --- | --- | --- |
| Palma | active (0) | ba24ee17-b84e-4bfe-be8d-5001bdf4ca8c | 3 |
| 10.3 II | disabled (1) | fde4ea1c-fffa-4390-9b73-126163cf01ff | 2 |

Both still had seven IDs and five exact pressure files. The unchanged configuration revision activates reader user-data bookkeeping; it does not resolve the conflict. The tested trigger bundle is qualified as a bundle, not proof that every component is individually necessary. Automatic application before explicit pull remains unqualified.

## Controlled restoration and server implications

After sealing the late split, the host retained all alternatives and copied the native Undo-restoration payload into a fresh information revision: 0dd0fd4e-7bca-4f0d-8e99-7b375f1d80bb. Only the protobuf information UUID and native page/revision/time filename changed; shape identity, selected active status, geometry, original shape timestamp and pressure dependency were retained. The selected state follows the native configuration winner for this disposable fixture.

A fresh unchanged-body mode-7 revision, reader commit revision and MESSAGE delivered this repair. Ordinary pulls applied it on both devices; final replay retains three active/four disabled records. No full import was needed for this repair. Preservation guards pass without further unrelated drift, and the full-pull flag is false after closure. The earlier unrelated Lumi fileSyncStatus exception remains recorded; this is not a whole-personal-library file-byte audit.

For Alexandria, matching CBL values and matching screens are insufficient to declare durable convergence. A cached conflicting delta can be unapplied and later change native ink when a routine revision activates its queue. Journal each alternative and explicit resolution separately, track asset receipt versus provider application, and use a fresh selected-state information revision when deliberate reconciliation is required. Do not silently equate a configuration winner with a shape-status winner. Cold native-process/full-pull replay is now qualified in the continuation below; device reboot and fresh-device replay remain distinct open gates.

## Evidence and final state

Private root: artifacts/gateway-lab/qualification/reader-native-erase-restore-reverse-order/. INITIAL_SHA256SUMS seals the baseline; CANDIDATE_SHA256SUMS seals offline candidates and renewal; INITIAL_PHASE_SHA256SUMS seals apparent agreement. late-doorbell/SHA256SUMS seals delayed divergence and the stopped protocol trace. reconciliation/SHA256SUMS seals fresh repair and replay. Prior seals are preserved.

Helper changes accept only exact newly observed fixture PDF hashes for capture, with Palma deletion and source restoration additionally allowed for pull-only operations. The intermediate source deletion hash is capture-only. Android assemble/lint passed; certificate-matched in-place helper upgrades preserved app data. The first source export was refused until its delayed PDF rewrite was captured and explicitly admitted; no broad guard bypass was used.

Both devices finish at Home with restored L under the fresh repair revision, lab proxy/account/asset facade live, note sync disabled and reader sync enabled. Protocol tracing is stopped and Gateway logging restoration verified. No production Alexandria runtime resolver was deployed.

## Cold native-process continuation

Cold native-process continuation passed on both devices: the host force-stopped com.onyx.kreader and com.onyx.android.ksync, restarted the scoped reader library, then full-pulled the owned fixture. Both new PID sets were verified. All seven native shape records and five pressure files equal the fresh repair closure exactly; notebook guards and unrelated reader comparison show no further drift. Both devices finish at Home. This is process restart, not device reboot or fresh enrollment, and does not establish a fresh device's historical-delta ordering policy.

Evidence: cold-process/result.json, exact after-full native exports, before/after PID controls, full-pull receipts, preservation guards, logs and SHA256SUMS.
