# PowerSync implementation state

2026-10-13: Shared Aragonite crystal icon refresh is staged for the next release.
The new transparent composition and all five legacy launcher sizes are packaged;
adaptive/themed icons use the existing inset. Source artwork, crystal reference,
prompt and export script are retained in [icon documentation](../design/icons/README.md).
JDK 17 release assembly and lint pass (0 errors, 20 existing warnings); APK artwork
transparency and rendering on white/black match the source, and the existing signing
certificate is verified. Version remains 0.1.5 (code 6). No device installation
was performed, as requested.

2026-10-11: PowerSync 0.1.5 adds explicit exact-hash MP3 cache recovery with private backups, paired with Alexandria’s owner audio preview/publication UI. Signed in-place upgrades preserve both device libraries/settings; ordinary app recovery restores the disposable recording exactly on both devices. Personal audio untouched; PCM and listened-to playback remain separate. Required affected server tests and Android release checks pass. A 48-hour read-only observer and verified independent-backup copy are running as user services; neither is marked passed yet. See the [qualification follow-up](qualification-campaign-20261010.md#companion-audio-recovery--2026-10-11-follow-up).

2026-10-11: Qualification round closed with documented limits. Two-hour observer completed: 121 samples; two readiness failures coincide with planned cold backups; no device-observation errors or unexplained catalog transitions; batteries 24.4–27.1°C. Both tablets remain on production and pass explicit native Library sync after the final restart. All temporary faults, timers, tunnel and restored stacks are stopped. Latest clean-stop backup restores all 51,982 native immutable objects exactly. Required tests pass; all 15 changed Go runtime files match deployment. See the PowerSync qualification campaign for evidence and deferred cases.

2026-10-11: Final clean-stop backup `cold-20261011T104536Z` and separate fresh restore pass: 38,999 body + 12,983 revision objects hash exactly, owner/anonymous access boundaries pass, latest app image ready after proper dependency ordering. SeaweedFS now gets 60 seconds to stop; cold backup records exit/OOM states and refuses forced-stop publication. Both changes are deployed and tested. Tablets retain production configuration; all 1,081 active sync-enabled Lumi catalog entries match Palma. The bounded observer completed at 11:12 UTC. Full evidence and limitations are in the qualification campaign.

2026-10-11: Fresh physical restore and live Palma endpoint rehearsal pass: 38,999 body objects verified; unchanged 1,524 notebook/135 book catalogs; native EPUB and repaired MP3 redownload exact from clone. Original production endpoint, marker and flags restored; tunnel stopped. Deployed dependency-aware readiness (PG/S3/configured BOOX database and bounded document query); required fresh race tests and public probes pass. Bounded two-hour observation is complete. See the qualification campaign for limits and private evidence.

2026-10-11: Native EPUB logical deletion/Recycle Bin restoration, cross-pagination bookmark, exhausted GET/cover retry and exhausted ink PUT/Library sync retry now pass. R renders on Palma; 20 paired resource hashes match. Reopen/close alone did not retry failed Reader uploads. A native Back-dismissed sync prompt stranded close; same-document reactivation preserved ink. Temporary proxy faults are removed with exact config restoration verified. See the PowerSync qualification campaign.

2026-10-11: Deployed explicit BOOX reading-data refresh UI with native revision fences, idempotent publication and separate server/device status. Corrected web highlight/bookmark lifecycle filters (Reader status 0 active, 1 removed). Required PG/S3 race tests and public form checks pass. Native codec probes on both devices validate removal of ONY1 duplicate IV; explicit audio repair endpoint is deployed. Synthetic MP3 native redownload recovers exact original bytes after controlled cache eviction; existing MP3 caches do not refresh on publication alone. Personal audio untouched. See the qualification campaign for evidence and limits.

2026-10-11: Qualification resumed. Stale offline ink recovered on both devices through current-revision reader-field publication, which advances only native ink freshness plus operation metadata. Both cached peer marks render after normal Sync acceptance and survive reopen; all 16 resource checks pass. No ink injection or forced SDK pull. [Evidence and limits](qualification-campaign-20261010.md#resume-and-server-driven-stale-ink-recovery--2026-10-11). New two-hour observation running; no manual action pending.

2026-10-10: [Seven-part qualification campaign](qualification-campaign-20261010.md) in progress. Isolated physical restore and all 38,969 retained BOOX body hashes pass; paired PDF bytes and four page geometries render. All 654 initially absent notebook resources recovered exactly after native Notes sync. Six audio files reproduce a native duplicated-IV decoder defect; all six decode to exact original backup bytes. Representative notebook rendering, PDF source redownload, offline reading-position convergence and retained-alternative restoration now pass. Campaign paused at owner request: A/B resources match server on both devices (16 checks), but visible ink has not converged. Wi-Fi enabled, readers closed, log/monitor stopped; see campaign pause handoff.

2026-10-10: [Fresh Palma restore](fresh-palma-restore-20261010.md) in progress after user factory reset. Zero native notebook/book rows captured. Fixed uninitialized native system-config handling in signed PowerSync 0.1.4; release tests/build/lint and live status pass. Signed-out enrollment and native connect succeed; initial catalog/resource catch-up observed. Complete restore and rendering not yet qualified.

2026-10-09: [Native book-file sync static evidence](native-book-file-sync-static-20261009.md): fresh Go6 ksync 32198 and Palma 32173 BookSyncFile decompilations match and implement real OSS source-book upload/download. Library UI supports explicit single/batch upload, gated by local source, quota and configurable file size. Pending discovery is separate from download. Alexandria end-to-end book transport remains unqualified.

2026-10-09: [Read-only production notebook comparison](device-library-comparison-20261009.md): 1,076 notebooks/four folders enabled on both devices, matching catalogs and 32,362 exact shared resources. One injection-test archive retains Palma-only marker text; all 97 native tags match despite an extra Lumi serialization. Four catalog differences involve disabled sync. No repair performed; server audio wrappers, thumbnail caches and full restore remain separate qualifications. Evidence: `artifacts/library-comparison-20261009/`.

2026-10-09 qualified: [Production channel/session repair](production-sync-repair-20261009.md) identifies missing Gateway scoped grants (2,676 Palma push conflicts; incoming notes absent) and lab one-hour expiry carried into production (Lumi 401/retry exhaustion). Native NOTE_TREE snapshots preserved before grant repair. Scoped grant propagation and stable frontend/renewable backend sessions are deployed and pass fresh PostgreSQL/S3/real Gateway tests. Both native accounts refreshed; seven missing notebooks imported on Palma, with one ink-bearing page visibly rendered. Three enabled deletions now converge; the fourth remains deliberately sync-disabled on Lumi. Both reported template losses are repaired; migration 0019 now separates live publication time from retained version creation. Original template redownload and repeat-open rendering pass.

2026-10-09: Signed 0.1.3 installed in place on Palma for [cached-account collision recovery](existing-library-enrollment.md#cached-account-collision-recovery--013-2026-10-09). Preserve all rows native login can displace by UID or numeric ID; compare again before login; restore with full preflight/readback, conditional mutations and interruption replay. Nine recovery-policy tests, all client tests, build/lint and upgrade account/library preservation pass. User retains enrollment and will drive Connect; actual multi-row native round trip remains pending. Palma live configuration was the old lab cluster despite `restored` journal status, correcting the earlier original-Onyx assumption. Lumi unchanged.

2026-10-09: [Existing-library enrollment](existing-library-enrollment.md) is deployed and qualified on Lumi with signed PowerSync 0.1.2. Original owner UID is preserved; paused Onyx return restores the account and catalog exactly. Enabled connection automatically uploaded 33,608 resources (1,056,234,304 logical bytes); all 31,430 distinct S3 bodies pass independent hash/length read-back. Eligible original-owner notebook/folder metadata is present, with explicit disabled-fixture exclusions. All backup-file exceptions are classified, including six audio wrappers decoded back to exact originals. Native Settings storage display is repaired; its 1 TiB quota remains a fixed compatibility value. Lumi stays connected with both domains enabled; Palma is unchanged. Full new-device restore and universal resource-reference closure remain separate gates.

2026-10-08: Approved aragonite/sync artwork is the standard product icon in signed release 0.1.1 (version code 2). Adaptive/themed icons and legacy density PNGs are included; the probe retains its identity. [Icon source and packaging](../design/icons/README.md). Build/lint pass; in-place upgrades preserve the signing identity and saved configuration. Evidence: `artifacts/product-build-20261008-icon-v0.1.1/`.

2026-10-08: Product module built/signed/installed beside the probe. Original lab restored/stopped. Paired setup/offline return and preservation guards pass. Native cluster cache, credential formats, channel concurrency and runtime read-only fences are covered; [implementation/results](product-implementation-results.md) records completed/failed gates and evidence. Native note transfer is not claimed for product setup runs.

2026-10-08: Native BOOX integration implementation authorized. Canonical [design](powersync-app-design.md) records server/app contracts, bounded authoring, explicit recovery and retained observed history. Device restoration and lab teardown precede product work; no production rollout implied.


2026-10-08: [Dirty native Reader editor](native-reader-dirty-editor-status.md) qualifies incoming T disable while Palma pen mode and pending D remain open. Correctly nested ink freshness plus mode-7/commit/MESSAGE triggers autonomous import and push of two D strokes; subsequent native close does not resurrect T in the bounded window. Fresh later restoration restores exact T geometry/info revisions; both devices finish with 23 shapes/15 exact pressures, D semantics and all library guards pass. No SDK pull or broadened push permission. D was flushed before close, so continuously unsaved-buffer and other editor modes remain separate.


2026-10-08: [Offline Reader alternatives archive](native-reader-alternative-archive.md) qualifies full-body status retention for one owned book: 24 operations/five opposing-status shape IDs, original protobuf/ZIP bodies and all exact pressure dependencies retained. Independent missing/tampered pressure controls reject incomplete packets. Server snapshots/listing unchanged; no device actions or winner selection. Production retention/UI/restore and broader resource closure remain open.


2026-10-08: [Reader trigger factors](native-reader-trigger-factors.md) qualify cached-resource rediscovery on both devices: no trigger, MESSAGE alone, unchanged-body commit alone and unchanged-body mode-7 alone yield no refill in 20 seconds; commit revision plus MESSAGE refills exactly with one target GET per peer. Every reset passes. Native 21-shape/14-pressure baselines, library guards, mode-7/commit bodies and existing asset documents stay exact. This is resource recovery, not first-time ink application.


2026-10-08: [BOOX sleep/display-doze delivery](native-reader-sleep-delivery.md) qualifies pause and unaided catch-up: two source PUTs succeed, zero peer asset GETs during 120 seconds with twelve Dozing/DOZE samples. Wake resumes GETs after 2.6 seconds while keyguard is still showing; native import completes after about 8.1 seconds within the unlock sampling interval. No SDK pull/reopen/host publication. Both devices have 21 shapes/14 exact pressures; prior ink and library guards pass. Deep idle and exact wake-versus-unlock import attribution remain open.


2026-10-08 in progress: [BOOX sleep/display-doze delivery](native-reader-sleep-delivery.md): T publishes with two HTTP 200 PUTs, but receiver makes zero asset GETs during 120 seconds; all twelve samples remain Dozing/DOZE. Fresh sessions and exact 19-shape/13-pressure preflight pass. Sleep-only evidence sealed. Receiver wake requested; user unlock and unaided catch-up verification pending. No full/deep-sleep claim.


2026-10-08: [Stopped Reader delivery](native-reader-stopped-delivery.md) passes: receiver Reader remains absent at all twelve Home-window PID samples while ksync downloads S. Ordinary cold launch imports/renders S unaided in about 4.76 seconds. Both devices have 19 shapes/13 exact pressures; prior ink and libraries pass. No SDK pull, host publication or manual Sync. Screen-off and stopped-ksync remain distinct.


2026-10-08: [Home/background Reader delivery](native-reader-background-delivery.md) passes: ksync downloads R while 10.3 II remains Home; Reader explicitly skips import because the document is closed. Ordinary PDF open imports/renders R unaided in about 1.35 seconds. Both devices have 18 shapes/12 exact pressures; prior per-device ink and library guards pass. No SDK pull or host publication. Stopped Reader and screen-off cases remain separate.


2026-10-08: [Actual Reader upload exhaustion](native-reader-upload-retry-exhaustion.md): four native objects receive nine HTTP 503 PUTs each (36 total), then no successful upload after fault removal or bounded UI/requeue/timestamp/targeted job controls. Exact native bodies remain cached. Separate host-assisted resource publication restores storage; notification downloads resources but Reader skips ink on its timestamp gate. Guarded full native pull recovers Y/Z; both have 17 shape IDs/11 exact pressures, prior per-device ink and library guards pass. Autonomous exhausted-upload recovery and server-only freshness-gate repair remain open.


2026-10-08: [Reader expiry/publication hole](native-reader-expiry-publication-hole.md): Y remains native-local on Palma (16 shapes/10 pressures); peer stays at 13/9. Initial expired-session 401, then verified fresh-session scoped start and native UI Sync still yield zero OSS requests in bounded windows. Prior ink and unrelated-library guards pass. Upload 503 harness passes tests but actual failed-upload retry is unqualified; fresh-edit control and retained-publication recovery remain open.


2026-10-08: [Sustained Reader disconnect](native-reader-sustained-disconnect.md) passes: roughly three-minute Gateway outage, CBL 502 backoff through 128 seconds, native publication and foreground peer import recover unaided about 80 seconds after readiness. Thirteen shapes/nine exact pressure files, prior ink, replay and guards pass. No OSS upload occurred while disconnected; failed-upload retry remains separate. [Retry layers](native-client-retry-policy.md) documented.


2026-10-08: [Brief Gateway interruption](native-reader-server-interruption.md) passes autonomous native publication/application after source close while Gateway was down. Receiver imports about 26 seconds after readiness; ten shapes/eight exact pressure files, prior ink, replay and preservation pass. Actual close preceded restart by five seconds, so sustained failed-upload retry remains open. Evidence sealed; Gateway restored.


2026-10-08: [Clean first-close Reader delivery](native-reader-clean-close-delivery.md) passes: native Palma Back/save publishes V and foreground 10.3 II imports/renders about 1.25 seconds later, before helper observation. Nine shapes/seven exact pressure files, prior ink, replay and preservation guards pass. No manual Sync, reopen, host publication or renewal in the observation window. Expiry, background delivery and interrupted publication remain open.


2026-10-08 staged: clean first-close repeat waits for one small V below U on Palma, Back without manual Sync. Both start with eight shapes/six exact pressure files; Reader-only flags, per-book eligibility and autoSync verified. Ordinary-UID config-change requests accepted (delivery itself not inferred from receipt). 10.3 II stays foreground Reader; bounded native/server observers run. No helper, host republication or source reopen is allowed during the initial observation. Evidence: artifacts/gateway-lab/qualification/reader-unattended-native-clean-close.


2026-10-08: [Reader config cache and delivery](native-reader-config-cache-delivery.md): first Palma close detects changed U but silently stops at a stale global eligibility gate despite persisted STRING true. Journaled native config notifications plus source reopen/close publish retained U; foreground 10.3 II autonomously downloads/imports/renders before any helper. Eight shapes/four active, six exact pressure files, prior ink, replay and library guards pass. Clean first-close repetition remains next; cache readiness is an enrollment requirement.


2026-10-08 staged: unattended native reader delivery awaits one short U stroke on Palma below the existing blue L, then Back without manual Sync. 10.3 II remains foreground Reader. Global/per-book sync and autoSync verified; native/server traces running for a bounded server capture. No helper pull or host publication is allowed during the initial observation. Evidence: artifacts/gateway-lab/qualification/reader-unattended-native-delivery.


2026-10-08: [Fresh per-book history replay](native-reader-fresh-history.md): canonical seven-record snapshots converge to three active shapes with exact pressure; full historical feeds yield four or six active shapes. A controlled older L deletion arriving after a correct canonical baseline disables L on both devices, surviving full/replay with unchanged mode-7 values. Existing reader rows, original ink fixture and notebook guards pass (Palma confirmed user addition excluded only for guard comparison). Optional alternatives must be archived outside the live native listing. Hardware reboot passes both devices; unattended delivery and a truly fresh installation remain open.


2026-10-08: [Repaired reader hardware reboot](native-reader-repaired-hardware-reboot.md): 10.3 II passes exact seven-shape/five-pressure comparison and endpoint/flag/library guards after true reboot, without login/start/pull assistance. Palma user-confirmed Notebook-1 addition is journaled; original notebook fingerprint matches excluding only that addition, baseline/allowlist unchanged. Palma also passes true reboot with exact shape/pressure/endpoint/flag state and unrelated reader metadata; notebook guard exact excluding only confirmed user addition. Both reboot phases sealed.

2026-10-08 campaign in progress: [current qualification checklist](native-boox-qualification-campaign.md). Hardware reboot of repaired 10.3 II sent after exact ink/pressure and preservation preflight; unlock pending. Palma reboot held on one newly observed lab-owned notebook; previous row identity/status/ownership comparison is unchanged. [Reader Sync Switch static checks](native-reader-sync-switch.md) distinguish global and per-book eligibility; exact prior dialog cause remains open.

2026-10-08: [Reverse-order status conflict](native-reader-reverse-order-status-conflict.md) confirms delayed divergence: 10.3 II connects before Palma, all three native deltas are stored/cached, and both initially retain restoration. The 10.3 II has not imported the competing deletion; its initial full callback is result 0. A separately sealed unchanged-body mode-7/commit/MESSAGE trigger activates that deletion and recreates the split, surviving full/replay. A fresh journaled restoration revision then converges both through ordinary import; all seven IDs/five pressure files and preservation guards pass. Sessions were renewed offline with candidates exact; owned-book Sync Switch controls are recorded. Evidence sealed; devices Home, trace stopped/logging restored, proxy/facade live. Cold native-process/full-pull replay also preserves all seven shape records and five pressure files exactly on both devices; device reboot and fresh-device replay remain open.


2026-10-08: [Native status reconciliation](native-reader-status-reconciliation.md) repairs the same-ID split with a journaled fresh restoration information revision, retaining all alternatives. Unchanged mode-7 ordinary pulls skip a downloaded revision; full pull applies it. Two clean pairs then qualify ordinary activation, including an identical-body new mode-7 revision plus reader-commit revision/MESSAGE: no commitId or timestamp value change required. Both devices converge to the fresh active-L revision; seven IDs, five pressure files, replay and preservation guards pass. Session-interrupted controls are recorded separately. Lab endpoints remain live; production repair policy and opposite reconnect order remain open.

2026-10-08: [Concurrent same-ID status divergence](native-reader-concurrent-status-divergence.md) is confirmed: CBL mode-7 converges to Lumi restoration, but native L is active on Palma and disabled on Lumi after ordinary/full/replay imports. Seven IDs and five pressure files remain exact; preservation guards show no further drift. Unseen information revisions replace same-ID shapes; processed revisions are skipped. Native-compatible configuration resolution alone cannot guarantee ink convergence. Opposite-order fresh candidates and explicit repair policy remain open; trial sealed, devices at Home, proxy/facade live.



2026-10-08: [Native status restoration](native-reader-status-restoration.md) passes erase/Undo after peer deletion: blue L is disabled on Palma, then restored under the same shape ID and unchanged pressure revision. Seven records return to three active/four disabled; all five pressure files remain exact. Source remained open and used native UI Sync; no SDK source push, host commit refresh, or cache injection. Ordinary replay adds nothing; guards show no further unrelated drift. Automatic first-attempt application and concurrent status ordering remain unqualified.




2026-10-08: [Reader commit/MESSAGE recovery](native-reader-commit-trigger-recovery.md) isolates asset rediscovery: zero timestamps work; an identical-body new commit revision followed by native-shaped MESSAGE refills the exact ZIP on both devices. MESSAGE-only results vary; lower syncAt still dispatches on both. Closely overlapping doorbells also recover. Original second-trigger loss is narrowed but not reproduced. Ink, pressure, mode-7 and unrelated-library checks pass without further drift.


2026-10-08: [Known-fragment deletion](native-reader-known-fragment-deletion.md) removes both surviving P fragments on both devices: seven records, three active/four disabled, five exact retained pressure files. Initial delivery stalled despite matching CBL configuration. A separately journaled commit updatedAt refresh plus scoped start recovered native ZIP download; full import applied deletion and replay added nothing. Both notebook guards pass; no further unrelated reader drift. Automatic delivery remains unqualified.




2026-10-08: [Competing erasures](native-reader-competing-erasures.md) leave original P disabled but retain two partial-erase fragments on both devices, despite whole-erasure configuration winning. Ordinary pulls give matching geometry/status and five exact pressure files; full replay adds no ink but leaves disabled-P information-revision IDs divergent. Both notebook guards pass; Lumi reader guard has one earlier unrelated fileSyncStatus 0-to-1 drift, fully journaled without rebaselining. Automatic application remains unqualified.


2026-10-07 staged: competing erasures await user on both devices: erase P loop only on Palma (keep stem), erase whole P on Lumi; leave L/squiggle and Back to save each. Both show the merged disposable PDF; five baseline records and four pressure files match. Initial evidence is sealed in artifacts/gateway-lab/qualification/reader-native-erase-conflict. Replication proxy 18766 is intentionally stopped; asset/account facade stays available. Capture both independent candidates before reconnect/retry.


2026-10-07: [Concurrent native ink](native-reader-concurrent-ink.md) preserves both independently written P/L strokes despite one mode-7 configuration winning. Ordinary incremental pulls merge both letters; five matching shape records and four exact pressure files, visible rendering and replay/personal-library guards pass. No host refresh or native source push retry; full recovery unnecessary. Automatic first-attempt application remains unqualified. Same-shape partial/full erase is next.


2026-10-07 staged: real concurrent ink experiment awaits user edits on both devices: P on Palma, L on Lumi, then Back to save each. Both open PowerSync-reader-clean-v2.pdf; exact ink/pressure and original-library guards pass. Initial baseline is sealed in artifacts/gateway-lab/qualification/reader-native-ink-conflict. Replication proxy 18766 is intentionally stopped, asset/account facade 18765 remains available. Capture passive local/server candidates before reconnect or retry.


2026-10-07: [Reader grants and clean conflicts](native-reader-grants-and-clean-conflicts.md) pass real session replacement without manual reader grants, two clean reconnect orders, and native document width application on both devices. Twelve policy/account tests and Android build/lint pass. Native configuration can change with callback newData=false. Ink/pressure and original-library guards remain exact. Real concurrent ink edits are next; lab endpoints and the explicit reader-session-grants marker remain staged.


2026-10-07: [Native mode-7 divergence](native-reader-config-divergence.md) verified offline sibling saves and eventual width-3 convergence on both devices/server, preserving exact ink exports. Initial reconnect was contaminated by a lost reader grant; journaled grant recovery yielded a native resolution revision. Fix reader grants across session renewal, then repeat opposite reconnect order and test UI application.


2026-10-07: [Partial erasure](native-reader-partial-erasure.md) is closed: native upload/download succeeded; automatic import stalled. Scoped full recovery produced two matching active fragments plus the disabled original, with exact 7072/7952-byte pressure files. Replay adds nothing and original-library guards pass. No CBL tombstone or asset deletion was observed. Configuration divergence remains next; lab endpoints remain staged.


2026-10-07 handoff: fresh ink closure is [documented](native-reader-fresh-ink-cache-timing.md) and checksummed.
That staged partial-erasure test is now complete; see the closure above. The
initial baseline and subsequent controls are preserved separately. Lab endpoints
remain staged.

2026-10-07: [Fresh native ink and cache timing](native-reader-fresh-ink-cache-timing.md)
qualifies native configuration/commit/MESSAGE publication and exact asset download
without host refresh. The old mode-7 conflict did not recur. Automatic application
saw no cached shape file before download task completion, then treated the update
as handled; later incremental retry remained empty. Scoped one-shot full-data recovery
imports one exact shape and 7952 pressure bytes, restores its flag, and replays without
new data. Original library guards and Android build/lint pass. Automatic end-to-end
application, deliberate mode-7 divergence, and erasure remain separate open cases.


2026-10-07: [Fresh reader lineage](native-reader-fresh-lineage.md) is staged with
one eligible metadata row per device, zero shapes, and matching native configuration
identity/timestamp. New book/configuration publication reached fresh server revision
1 without host-authored documents. Original library guards and Android build/lint
pass. Fixture-only autoSync is enabled and both readers are open; a user pen stroke
is pending. The old accepted mode-7 history is linear, with no rejected branch;
public LiteCore error paths explain why 10409 alone cannot identify the cause.
Automatic ink delivery and native divergent-configuration recovery remain open.


2026-10-07: [Eight-shape reader recovery](native-reader-eight-shape-recovery.md)
passes exact eight-shape/four-pressure-file closure and incremental replay; original
library guards pass. The passive trial failed before publication because Palma had
two fixture metadata rows. Controlled native retry uploaded assets and dispatched
MESSAGE, but mode-7 revision conflicts left the receiver configuration stale.
Assets downloaded after ordered controls; application only advanced after an explicit
fixture-only source-configuration server copy. Automatic delivery and native mode-7
conflict recovery remain open. No full pull or production resolver change was used.


2026-10-07: [Fresh reader ink and listing repair](native-reader-message-fresh-ink.md)
passes controlled seven-shape closure and exact three-point-file equality with
incremental application and idempotent replay; no full pull was needed. Native
plain-ID reader commit publication is proven after correcting its admission rule.
Fresh dynamic assets were uploaded but omitted from OSS listing; that harness
fault is fixed and covered by a signed-listing test. An expired receiver session
confounded one MESSAGE-only phase. Nineteen policy/OSS tests and Android build/lint
pass. Original reader/notebook rows remain exact. Devices stay staged for a clean
fresh-edit repeat; fully automatic first-attempt delivery remains open.


2026-10-07: [Reader MESSAGE identity and wakeup](native-reader-message-identity.md)
passes on both builds: prefixed MESSAGE IDs are pulled but do not dispatch;
plain UUID IDs dispatch READER_LIBRARY, and refreshing the same ID wakes it again.
This corrects server-authored MESSAGE controls, not reader data document IDs.
Six-shape native baselines and original reader/notebook preservation pass.
Both devices are staged reader-only for a fresh native pen edit; lab services run.
Fully automatic ink delivery remains pending.


2026-10-07: [Native reader ink write-back](native-reader-ink-writeback.md)
passes an addition round trip: user-written “Hi.” on Palma imports five new shapes
on Lumi, displays there, and matches all six shape identities/bounds plus both
pressure files exactly. PDF bytes stay unchanged. A controlled type-4 trigger,
per-book eligibility and one-shot full-pull recovery were used; repeat pull adds
no data. Fully automatic first-attempt trigger delivery and ink erasure remain open.


2026-10-07: [Native reader shape ingestion](native-reader-shape-ingestion.md)
passes on Palma: admitting reader commitType 4 and issuing scoped Binder start
fetches the exact shape ZIP; NeoReader imports one shape, with matching identity,
bounds and all 689 pressure points. Repeat application reports no new data.
The delivery trigger was a server-authored control; native source trigger emission
and peer editing/write-back remain open. Palma is staged on the fixture for the
requested real edit; Lumi remains on its original settings.


2026-10-07: [Native PDF ink follow-up](native-reader-ink-export.md) proves real
stylus export, visible annotated-PDF transfer and exact native pressure-point
receipt on Palma. Mode-4/mode-7 and the separate shape ZIP reach our server.
Peer shape export remains empty: editable ink application is an open gate.
Session renewal resets harness reader grants; explicit fixture identity enrollment
is distinct from automatic matching. All original reader rows remain exact;
one new disabled blank Palma row is retained and documented.


2026-10-07: [PDF/reader-ink checkpoint](native-reader-pdf-ink.md)
stages the exact authored four-page geometry PDF on both devices; ordinary-app
metadata reads and original-library preservation pass. The manifest-selected ink
provider rejects queries on both builds. Real stylus control is pending; PDF/ink
replication and coordinate application remain unqualified.

2026-10-07: [Native TLS rejection qualification](native-tls-negative.md)
now passes on both firmware builds: HTTPS account login and independently WSS
NOTE_TREE reject a matching-IP self-signed certificate. HTTP account controls
pass. Missing trust-anchor rejection is qualified; hostname mismatch remains open.

2026-10-07: [Editor switching and freshness qualification](native-editor-switch.md)
passes visible ordinary-app handoff and exact edited-text peer application in both
directions. Android blocks background same/different opens on both builds.
Both local stale recoveries refuse before native body writes; a stale server
commit PUT gets 409 and preserves the current commit. These are bounded guards,
not an editor lock, complete queue drain or unattended recovery qualification.


2026-10-07: [Ordinary-app editor quit qualification](native-editor-close.md)
proves package-targeted QUIT_NOTE delivery on both devices, but neither rich-text
editor closes: Notes reports no event subscribers. This exported broadcast is
not a qualified flush/recovery barrier. Cold/save/publication checks are separate.
A native toolbar Back positive control closes Lumi and propagates exact edited
text to Palma; both cold active bodies match. This is an ADB UI control, not an
ordinary-app flush. The delayed probe builds/lints and upgrades in place with
matching signers.


2026-10-07: [Coordinated pair recovery and open-editor control](native-pair-recovery-coordination.md)
now pass sequential recovery, simultaneous sync/cold reopens and native-edit
propagation. An open edited Lumi buffer subsequently publishes older text after
a newer server recovery exists; both cold devices select that edit. Native-winner policy
stands. Production editor/pending-work coordination remains open; narrower older
pair failures below are historical observations, not the current coordinated result.


[Palma exact commit/MESSAGE receipt](native-palma-commit-receipt.md) now passes:
native child revision histories include both exact server-written parents, four
exact assets download, and isolated cold native text application passes twice.
Both builds support isolated server-created text; reliable pair convergence is
still open. The initial two-device trace records 36 distinct native commit
revisions before publication. Next: controlled pending-work/pair convergence.
The trace parser gains outbound revision metadata; all 35 Python tests pass.

[Server-created text recovery](native-server-richtext-recovery.md) now publishes
fresh assets/commit/MESSAGE without helper import. Lumi downloads and cold-renders
the text; Palma does not fetch the fresh graph even after session renewal. A
second-round marker later leaves active state. Reliable pair convergence remains
open. The lab proxy now distinguishes expired active cookies (401) from retired
grants (403), with fresh Go race tests; native unattended renewal is unqualified.

[Explicit rich-text recovery](native-richtext-recovery.md) now restores the
interrupted server-restart marker under the same notebook/page/shape IDs. Native
Lumi publication reaches Palma, both rendered editors and cold exports retain
the text, and freshness refusal/idempotent replay pass. This is helper-app-assisted
recovery; unattended/server-only recovery remains open. Pre-open exports can omit
resource references even when later native opening succeeds; use post-save active
references and UI receipts. Fresh assemble/lint and 34 Python tests pass.

[Pending-work server restart](native-server-restart.md) now proves exact stored
asset/session survival and native BLIP reconnection on both devices. The clean
91-second repeat did not publish its pending commit; explicit receiver and source
requests did not recover the marker. After explicit recovery, the source reopened
older active text while its archive retained the interrupted bytes. A new save
advanced metadata without recovering that content. Complete unattended recovery
and active-content preservation remain open; archive bytes are not a live buffer.
The first run had a facade supervision error, retained and corrected in the repeat.

A [reverse re-enrollment continuation](native-reenrollment-delivery.md) now passes
Lumi-to-Palma queued-edit delivery with the receiver and proxy left running.
The earlier stalled rename also appeared after fresh rejoin; its specific cause
remains unresolved. The new native pending-tree callback is not a full CBL query.

[Device-scoped transport cutoff](native-transport-revocation.md) now denies cached
Gateway/OSS access and replacement credentials while the healthy peer publishes.
[Credential retirement and re-enrollment](native-credential-generation.md) now
reject old bearer/OSS grants and two historical cookies after fresh enrollment,
with stable principal and queued-edit delivery. Production enrollment proof,
in-flight HTTP cutoff and direct active Gateway sockets remain unqualified.

Remaining facade gates are tracked in [remaining-facade-qualification.md](remaining-facade-qualification.md).
The 2026-10-07 continuation verified native offline notebook delete/edit sibling
proposals and convergence in two rounds, with one edit winner and one deletion
winner. Upload store/ACK separation was observed. A [matched continuation](native-upload-interruption.md)
now qualifies retained-edit recovery through native reopen/close and receiver application;
scoped retry alone did not publish within 90 seconds, and exact original-resource
republishing remains unqualified. [Native account lifecycle](native-account-lifecycle.md)
now separates rotating renewal, refresh 503 and refresh 401/logout; both builds
disable sync on logout while retaining queued edits. Full transport-grant revocation,
negative TLS controls, complete restart recovery, PDF/ink and dirty-buffer cases remain gates.
The trusted-hostname [native HTTPS/WSS path](native-tls-tunnel.md) now passes
login, replication and exact signed text-notebook assets on both builds.
This integration is one owner/library with multiple devices; independent
cross-author isolation is not a required experiment. Per-device grants remain required.

2026-10-07 policy decision: follow native ksync/CBL winners for live BOOX sync;
retain the option for later server-side history/recovery/suggestions without
silently changing device state. Archive coverage must identify what was actually
observed; complete losing-branch recovery is not promised or a v1 prerequisite.
See [recorded policy](alexandria-native-boox-source.md#conflict-policy-decision-native-compatibility-with-optional-server-history).

Durable findings: [experiment results and evidence index](experiment-results.md).

Latest scope comparison: [matched Palma/Lumi results](native-reading-scope-comparison.md).
Both tested firmware versions behave identically under the matched sequence:
new book-scoped records return zero models; library scope plus native restart
delivers exact new revisions; moving records back to book scope returns cached
older library models. The earlier Lumi book-scope claim was a cached-revision
artifact, not a demonstrated firmware difference. Scope/revision/timestamp
receipts supersede row counts. Native application/progress findings remain valid.
Full original metadata rows unchanged (28/14); all restoration/preservation
checks pass. Accounts/endpoints/global flags, fixture EPUBs and server policy
restored; lab services stopped with evidence checksummed.

Latest reading delivery gate: [subscription lifecycle](native-reading-lifecycle.md).
Palma login starts continuous MESSAGE with note sync disabled. Sent MESSAGE
revisions did not establish unattended reader updates. The scoped transaction-17
probe controls library replication, not an explicit book-data replicator start.
Moving two exact user-data records into READER_LIBRARY and restarting the
library (transaction 16) proves local receipt; an explicit single-book native
pull applies the bookmark, retaining UUID and nine portable fields. A new
revision reached CBL after restart but remained unapplied through background,
HOME and cold-open observations until explicit native application. Native
metadata publication can rewrite server markers; journal revisions separately.
Cleanup detected two re-imported prior disposable UUIDs; original full rows
were unchanged. Exact synthetic row/path/hash cleanup and preservation guards
passed. Both original account/endpoint/flag states and server policy restored.

Latest Palma/trigger gate: [native trigger qualification](native-reading-trigger-qualification.md).
Fresh cloud-only UUID downloads exact authored EPUB bytes, imports automatically
without an explicit ReaderSyncService import call, and opens natively at 17/64.
Presence after MESSAGE is not causal proof because import preceded the MESSAGE.
Distinct background/foreground updates remain transport-inconclusive: CBL held
old metadata and no fresh book user-data, including after native UI confirmation.
Next gate is MESSAGE/fresh-book subscription lifecycle and revision receipt,
then native automatic application. Accounts/endpoints/global flags, original
reader/notebook guards and authored fixture bytes are restored.

Latest cloud-book gate: [native import/open](native-reading-cloud-open.md).
Fresh cloud UUID import on Lumi passes signed download, managed-path conversion,
UUID retention on cold open, progress 17/64 → 8/32 and a rendered bookmark with
nine portable fields preserved. Native replication needs the expanded identity
scope before library import; its book-scope inference was subsequently corrected
as cached library data, while native bookmark/progress application remains valid.
Explicit ordinary-app Binder requests drove this test. Unattended update triggers, thumbnail transfer and general alias conflicts remain gates.
Exact original accounts/endpoints/global flags are restored; reader guards
(28 Palma, 14 Lumi), notebook guards and fixture-disable checks pass. Lab
services are stopped with state retained; 93 evidence files are checksummed.

Previous asset gate: [native book transfer](native-reading-assets.md) established
signed EPUB upload/download. Its existing-alias direct-VIEW failure is retained
as evidence; the fresh cloud-library import qualifies the separate path above.

Latest reading gate: [native highlights and deletion](native-reading-annotations.md).
Palma → Lumi highlight rendering and annotation/bookmark logical tombstones pass.
Highlight repeat publication/cold reopen does not resurrect or duplicate it.
Native versioned anchors/style survive layout differences; anchor translation
and stale-device/concurrent edit-delete behavior remain unqualified. Both devices
are restored, original-library guards pass, and lab services are stopped with
state retained.

Latest experiments: [reader identity and reverse bookmark](native-reading-identity.md).
Reverse native bookmark application now passes Lumi → Palma with every portable
field preserved. Controlled creation-time ordering selects between two retained
aliases; it does not establish UUID deduplication. A disabled-reader publication
needed a fresh revision before native CBL delivery; transport and application
are separate gates. Exact original accounts/endpoints/flags and original-library guards are verified;
lab policy/grants restored and services stopped with state retained.

Latest reading gate: [native EPUB qualification](native-reading-qualification.md).
Both devices publish native reading metadata to Couchbase. A Palma bookmark
reaches Lumi's native reader database and renders; progress applies Palma 10/64
→ Lumi 5/32 and Lumi 8/32 → Palma 15/64 with receiver confirmation and layout
retention. Identical books produce two same-path UUID aliases; Lumi selects
the earlier source alias; general reconciliation remains unqualified.
Original reader guards pass (28 Palma / 14 Lumi); fixture alias flags are restored.
Both devices are restored to exact original endpoints/account states/sync flags;
original notebook and reader guards pass. Gateway policy/grants are verified
restored and lab services are stopped with data retained.

Accepted deployment target (2026-10-06): native BOOX Couchbase sync as a notebook
and reading source in Alexandria Server. See the [integration plan](alexandria-native-boox-source.md).
Next qualification: remaining reading assets/annotation/identity gates, original-library
migration semantics, remaining image/template closure, background/session lifecycle,
and dirty-editor/conflict publication. Downloaded-book identity, remaining reading assets, general deletion/conflict semantics,
handwriting and exact anchor translation remain unqualified.

Latest completed LAN results: both devices preserve exact configuration/account/
flags across real reboots, reconnect without explicit wake, and upload fresh
native edits over Wi-Fi without ADB reverse. Server metadata plus a native
NOTE_TREE MESSAGE applies automatically in 4.2 seconds on Palma and 6.9 on Lumi.
Bare Couchbase writes omitted that publication step. See
[direct LAN evidence](direct-lan-qualification.md). Both devices are restored to
original endpoints/accounts/flags, with markers and host overrides removed and
original-library guards passing. Server policy is restored; lab services are
stopped with state retained. Synthetic book/recovery fixtures remain.

The representative image catalog repair now passes fresh-copy native import and
re-export: eleven exact image files, eleven catalog entries and live image shapes,
and all 549 exact point payload spans survive. One page was visually checked.
See [catalog investigation](native-image-catalog-investigation.md). Original
notebooks and their preservation receipts remain untouched. Native reading
models/Binder surfaces have a [source map](native-reading-source-map.md), now
supplemented by ordinary-app metadata/progress access on both devices and a
Palma native bookmark read. The identical authored EPUB has different native
UUIDs despite matching idString/hashTag, and paginates 64 versus 32 pages.
Fixture file/user-data sync status was restored to 0 after scoped replication.
Lumi bookmark creation, highlights and handwritten reader annotations remain
unqualified; see the newer reading qualification for native progress/bookmark transfer. Earlier tunnel observations below are historical checkpoints;
the LAN test establishes the newer scheduling/publication evidence.

Reboot qualification completed on both devices. Exact target maps, accounts,
test-server definitions and sync flags survive. Outgoing native metadata edits
reach Couchbase after reboot. Passive reconnection/incoming application did not
occur within the observation windows; Palma incoming metadata applied after an
explicit native tree restart. The shim's fake parent field was removed and the
account restoration helper now removes only the disposable cached account before
native MASTER selection. Both devices are restored; settled account/map/flag and
library checks pass. Lab services are stopped with data retained. See
[reboot evidence, timing and limits](reboot-durability.md).

The 2026-10-06 qualification run completed preservation capture and server-only
reconstruction of **2,157 live native archives**: 1,079 Palma and 1,078 Lumi.
Shared-file preservation captured 88,371 unique byte objects; server readback and
independent reconstruction verified their hashes. Originals remained read/export
only; native recovery used fresh lab-owned copies. These preservation packs are
separate from stock ksync replication and are not a qualified complete backup.

Native retargeting, per-device sessions, text/ink/image/template/four-page and
multilayer fixture transfer, missing-file semantics, corrupt/truncated downloads,
interrupted-transfer recovery and scoped deletion/retry have live evidence.
Original notebook-copy recovery retained 549 point payloads and two PNGs.
Sixteen image assets omitted by original native exports were recovered from the
server's shared-file preservation into five derived archives. One empty image
reference remains unresolved; a repaired copy retains eleven local assets but
its native export still omits them after page opening. Offline delete/edit states
remained split during observation; automatic convergence is not qualified.

Reboot qualification results are recorded in
[reboot findings](reboot-durability.md). The first Lumi reboot preserved the exact
map but exposed unsafe fake-parent account semantics in our shim, now corrected. Explicit sync/pull converged
the offline image move and incoming stroke in final native exports; unsaved-buffer
safety remains unqualified. Both devices now have exact original cluster maps,
accounts and sync flags, verified after settling. Original library guards pass.
Markers and lab forwarding were removed; server policy and logging restored;
lab services and containers stopped with data retained. See
[qualification run](qualification-run.md) and
[full-library results](full-library-runthrough.md). No Rhizome integration or
production sync service is implemented.

2026-10-06: [ordinary-app endpoint probe](unprivileged-endpoint-access.md)
confirms live cluster-map reads and arbitrary unused-entry writes from
untrusted_app on Palma and Lumi, with original-map restoration. Combined with
the earlier Binder selection evidence, ADB is no longer an established
requirement for cluster-map writes. App-only provisioning with an app-written
test-server definition, native refresh, login and process-restart durability now
pass on both devices. Marker-free provisioning failed on Palma after native
cached-map rewriting. Actual reboots now retain configuration and accounts on
both devices; unattended startup remains unqualified.

[404/503 resource experiment](missing-resource-experiment.md): native text
asset downloads made 3 HTTP attempts for 404 and 9 for 503. After exhaustion,
neither retried during 60 seconds after removing the fault. Fresh source
commits recovered both exact stored objects and text appeared in the receiver
editor and active export. This exercised generic file-discovery errors; the
special V2 missing-file acknowledgement was separately observed in the current
run: one injected 404 cleared a synthetic pending row without creating its file;
503 retained pending work, then a released fault and scoped request recovered it.
The earlier generic-resource experiment restored both
devices, accounts, sync flags, library fingerprints and original Gateway
policy restored; lab stopped. A late cached Lumi map overwrite was corrected
and the final live map checked again.

Static follow-up: [source audit](static-sync-semantics.md) maps commit gating,
V2 missing-file acknowledgement, temp-file replacement, distinct conflict
policies, checkpoints and retry layers. Captured unsupported object POSTs are
bulk deletes (`?delete`), not established multipart uploads. No device changes.

Latest server tracing: [Gamma protocol trace](server-protocol-trace.md).
MESSAGE decoded: [control-channel findings](message-channel.md). Captured
type-6 NOTE_TREE records are native replicator wakeups. The
[live doorbell experiment](message-doorbell-experiment.md) delivered a fresh
Palma text edit automatically to Lumi before receiver UI open, confirmed by
native editor/export. Both devices and original server policy restored.
Palma's native text update was stored as notebook metadata, file assets, then
native commit record, and appeared in Lumi's native editor and export.
Per-device BLIP messages, exact asset revisions and durable metadata journals
are captured. Both devices and lab logging were restored; lab stack stopped.

Updated 2026-10-05. Native adapter lab builds and runs on Go 10.3 II Lumi,
Android API 35, Notes 45311 and ksync 32079, as an ordinary app UID.

## Device evidence

- Notes metadata queries and Notes/ksync Binder connections pass.
- Virtual-page callback passes through reconstructed Notes AIDL.
- Disposable text notebook creation, native opening, and rename pass.
  Rename preserves the notebook's unique ID; fixture requests are journaled
  before dispatch to avoid duplicate creation on retry.
- Native remote `.note` export passes through the Notes Binder. The fixture
  archive has 16 non-directory entries, 9,273 expanded bytes, including note,
  virtual-page, page-model, shape, point, resource, template and stash data.
  ZIP size/CRC and per-entry SHA-256 validation run inside the app UID.
- The exported resource contains the original fixture text. Opening showed an
  empty editor, and the archive also contains an empty editor resource; active
  page/content routing still needs investigation.
- The newer shape provider fails with `no such table: ShapeModel`. The old
  provider returns zero shapes. Neither qualifies arbitrary document capture.

Raw reports and the synthetic native archive live under ignored artifacts/lumi.
No personal note payloads belong in committed fixtures or documentation.

## Gates still open

Archive export is demonstrated, not complete snapshot qualification. Verify
all referenced assets, multiple pages/layers, images, handwriting, templates,
closed documents, and concurrent native edits. Entry hashing removes outer ZIP
timestamp differences, but native export may regenerate IDs, revisions and
nested ZIPs; the archive manifest hash is not a stable revision identity.
A semantic identity for single oil-pen shape records remained unchanged across
two native exports even though their archive manifests changed. Full notebook
identity still needs metadata, resources and multi-shape support.

Same-notebook write-back now has real fixture evidence: PowerSync wrote a text
resource, Notes displayed it and accepted a native edit, and both appeared in the
next export. A synthetic oil-pen stroke was imported and revised under the same
notebook and shape IDs. Re-export preserved all 101 point records byte-for-byte.
The same shape update also rendered with native noteSyncStatus=0. Shape deletion
and restoration revisions worked while the native document remained open. This qualifies
these fixture paths, not arbitrary full notebooks or physical handwriting.

The importer rejected a malformed point header before staging. An injected stop
after durable point staging left no published shape revision; retry completed,
and a duplicate retry succeeded. Private immutable receipts and per-receipt
journals preserve the received payload. Automatic recovery and sync receipt
integration remain unimplemented.

Full native snapshot write-back remains a critical gate. The native
KSYNC_NOTE_DATA_UPDATE_ACTION handler imports virtual-page PB metadata and
reloads an open document. Its existence does not establish that staging shared
files is sufficient, or that native dirty-state handling is safe. Do not invoke
broad RESTORE_NOTE: source inspection shows library-wide destructive behavior.

Native image replacement also passes: the next native export retained the same
image shape ID, its incoming updatedAt, the replacement PNG byte-for-byte, and
the existing oil-pen stroke. This qualifies the synthetic image fixture only.

The Palma 2 Pro is now connected. Its ordinary-UID probe passes Notes/ksync
binding, metadata queries, page callbacks and shared-storage access on API 35,
Notes 45960 and ksync 32173. The newer shape provider has the same missing-table
failure. Its library already contains the Lumi ink fixture ID; this existing
copy does not demonstrate independent PowerSync transfer. Its separate text
fixture passes creation, cloud-disable, same-ID rename and native export with
ZIP integrity validation. Use separate journaled fixtures for Palma writes.
Raw reports live under ignored artifacts/palma2pro.
The first Palma text-resource write reported success at the file/notification
level but remained blank in the native editor; its next export retained the
written resource only in history, while the live resource lacked the marker.
Native application ACK and dirty-editor handling remain unresolved on
this path. Do not generalize the Lumi text result to Palma compatibility.
The separate Palma ink fixture renders the synthetic imported native stroke.
An initial staging attempt stopped because cloud-disable was not observed;
after closing the fixture and confirming the flag, the same payload staged.
Native re-export preserves all 101 incoming point records byte-for-byte on Palma.
A second revision re-exports exactly one shape with the same ID, newer updatedAt,
and byte-exact revised points. This qualifies same-shape revision on both targets.

Palma static firmware capture is complete: 24 APKs and 222 readable firmware
files, device/local SHA256 verified. Its 95 ksync device profiles match the Go6
baseline; no new app catalog entries were found. Framework-only model labels
remain unbaselined. Full evidence is in
`~/booxreverse/palma2pro/2026-10-05_1810/REPORT.md`.

The Lumi currently has an existing Onyx account. Palma logout was confirmed
during the endpoint spike; native export of its existing ink fixture still
preserved point bytes. Fresh account-free library creation and full write-back
remain unqualified.

No Rhizome protocol integration is implemented yet. The account-independent
adapter and separate PowerSync dataset remain the approved architecture; native
retargeting is now a demonstrated alternative requiring a decision after the
remaining content gate is qualified.

The real Couchbase reference lab passed native disposable-account login on both
devices, independent device sessions, server-to-native metadata changes, and a
same-ID Palma-to-Lumi-to-Palma native rename round trip. Native Palma uploaded six
fixture files into Couchbase attachments through a thin OSS facade; native Lumi
fetched them. The first Binder-created fixture's receiving export omitted its source text.
A follow-up native UI-created text notebook now renders on Lumi and accepts a
native edit that renders back on Palma under the same notebook ID. Both final
native exports contain equal extracted text with both markers. This qualifies
the disposable text-note path, not arbitrary full notebook application. Scheduling needed explicit
reconnect/scoped requests. See [couchbase-reference-lab.md](couchbase-reference-lab.md)
for evidence, limitations, components and restoration.

Both original cluster configurations were restored exactly. Lumi's original
account and note/reader sync flags were restored. Palma remains logged out with
note sync disabled. Other library identity/status/ownership fingerprints remain
unchanged, excluding two new disposable notebooks. Lab network mappings, markers,
processes and containers were removed/stopped; evidence and server volume remain.

A retained-server transaction audit reconstructs the ALPHA/BETA resource transfers
with per-device attribution and timestamps. The UI notebook currently has one
metadata document, one commit-point record and 24 attachment-backed asset objects.
Normal Gateway logs omit per-message BLIP details; the audit distinguishes stored
state from transaction history. See [server-transaction-audit.md](server-transaction-audit.md).
The audit did not rejoin devices; host lab containers were stopped again afterward.

2026-10-06: [Paired repeat/offline application test](native-reading-workflow.md)
proves cached bookmark/highlight application after replication interruption on both
devices, duplicate-free replay, and retention of an ordinary-UID newer local comment
under unchanged-revision replay. Lumi required a retry after RESULT_NONE; native
provider acknowledgement remains a separate gate from CBL receipt. Mode 7 is reader
note configuration, not reading progress.

2026-10-07: [Offline reading conflicts](native-reading-conflicts.md) qualify four
real sibling-branch rounds on both devices. Winners match the larger sibling
revision ID, not a general newest-updatedAt policy. A newer logical deletion lost
to an older enabled edit, which reappeared on Lumi; an older deletion also beat a
newer edit. Both provider states and annotation-list UI agree. Palma had repeated
application timeouts in one round; its already-correct provider state is distinct
from callback success. Preserve native winners and losing operation provenance
separately; complete losing-intent capture remains an integration design gate.
