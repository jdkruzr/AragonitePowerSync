# Remaining native BOOX qualification campaign

2026-10-08: [BOOX sleep/display-doze delivery](native-reader-sleep-delivery.md) qualifies pause and unaided catch-up: two source PUTs succeed, zero peer asset GETs during 120 seconds with twelve Dozing/DOZE samples. Wake resumes GETs after 2.6 seconds while keyguard is still showing; native import completes after about 8.1 seconds within the unlock sampling interval. No SDK pull/reopen/host publication. Both devices have 21 shapes/14 exact pressures; prior ink and library guards pass. Deep idle and exact wake-versus-unlock import attribution remain open.


2026-10-08 in progress: [BOOX sleep/display-doze delivery](native-reader-sleep-delivery.md): T publishes with two HTTP 200 PUTs, but receiver makes zero asset GETs during 120 seconds; all twelve samples remain Dozing/DOZE. Fresh sessions and exact 19-shape/13-pressure preflight pass. Sleep-only evidence sealed. Receiver wake requested; user unlock and unaided catch-up verification pending. No full/deep-sleep claim.


2026-10-08: [Stopped Reader delivery](native-reader-stopped-delivery.md) passes: receiver Reader remains absent at all twelve Home-window PID samples while ksync downloads S. Ordinary cold launch imports/renders S unaided in about 4.76 seconds. Both devices have 19 shapes/13 exact pressures; prior ink and libraries pass. No SDK pull, host publication or manual Sync. Screen-off and stopped-ksync remain distinct.


2026-10-08. Current checklist; older chronological summaries remain historical evidence. Complete local qualification before production Alexandria integration.

| Case | Current evidence | Remaining test |
| --- | --- | --- |
| Fresh history | Repaired state passes replay on previously enrolled devices | Four new book namespaces qualified; canonical passes, full history revives shapes, and one late older deletion overrides restoration on both. [Findings](native-reader-fresh-history.md). Entirely fresh installation and production projection/retention remain open. |
| Reboot durability | Cold Reader/ksync processes preserve all seven shapes and five pressure files | Hardware reboot on both devices, passive startup observation before assisted pull, exact exports and guards. 10.3 II passes exact post-reboot comparison without login/start/pull. Palma also passes exact post-reboot state and guards with journaled user-addition exception. Both phases sealed. |
| Unattended delivery | [Clean first-close foreground delivery passes](native-reader-clean-close-delivery.md); nine shapes/seven pressure files exact | Brief and three-minute Gateway disconnects pass; actual asset retry exhaustion and assisted full-pull repair are now qualified. Home/background cache plus ordinary-open import now passes. Stopped Reader cache plus cold ordinary-open import also passes. BOOX sleep/display-doze pauses receipt and catches up unaided on wake. Autonomous exhaustion/expiry and deep sleep/unplugged remain; clean run used no helper/manual Sync/reopen/host publication during observation. |
| Minimal trigger | [Reset factor matrix](native-reader-trigger-factors.md) qualifies cached-resource recovery: ordered commit revision + MESSAGE works on both; single factors do not within 20 seconds | Fresh ink/configuration import gating remains separate; no universal necessity claim. |
| Book sync switch | Independent global/per-book checks identified statically | Runtime attribution for previously observed dialog and cross-firmware comparison. See [static findings](native-reader-sync-switch.md). |
| Interrupted publication | Real upload 503 exhaustion; exact cached-resource escrow and assisted full-pull recovery pass | Autonomous retained-work resubmission and server-only freshness-gate repair remain open. Receipt, resource storage, publication and application are distinct. |
| Dirty editor | [Native open-pen D/T experiment](native-reader-dirty-editor-status.md): incoming disable applied, pending D pushed, close does not resurrect T; later restoration/native guards pass | D auto-flushed before close; continuous pen contact, undo/eraser and further simultaneous-edit cases remain separate. |
| Library resources | Large export/resource capture; partial reference closure | Resolve remaining image plus page/template/audio/PDF dependencies with representative owned copies; no broad original-library writes. |
| Transport negatives | Earlier identity/session/transport controls exist | Trusted-chain wrong hostname, asset TLS and cutoff for already admitted requests. |
| Optional alternative archive | [Offline full-body archive](native-reader-alternative-archive.md) qualifies 24 status operations across five shape IDs with exact pressure closure and missing/tampered dependency rejection | Production durable storage, retention/GC, restore/UI and general resource closure remain. Native live listing is unchanged; SG history is not treated as an archive. |

Preservation exception at preflight: Palma notebook fingerprint changed from 1517 to 1518 nonallowlisted rows. Inventory comparison finds seven additions relative to the older full inventory and no prior-row identity/type/status/ownership change. Six additions are earlier lab fixtures; the newly observed lab-owned `Notebook-1` has ID `1022f717f43d4b0193bd6f002396f26d`. The user confirmed creating it; excluding only this addition reproduces the original guard SHA exactly. Do not silently allowlist, delete, restore or reset baseline. Private evidence is in `artifacts/gateway-lab/qualification/reader-repaired-hardware-reboot/`.

Historical U staging: original owned reader PDF on both devices, autoSync and global/per-book gates checked. Native/server observations run without helper actions during the user pen edit. Add one short U on Palma and Back to save, with no manual Sync; observe the 10.3 II foreground Reader before assisted export/recovery. Evidence folder: `artifacts/gateway-lab/qualification/reader-unattended-native-delivery/`.

2026-10-08: [Runtime cache/delivery findings](native-reader-config-cache-delivery.md) qualify autonomous foreground receiver import after an assisted native source recovery. First close was blocked by stale process eligibility despite persisted reader flag true. Eight shapes/six pressures and preservation/replay pass; clean repeat is staged with one V stroke, no helper/manual Sync/source reopen during observation.

2026-10-08: [Clean first-close Reader delivery](native-reader-clean-close-delivery.md) passes: native Palma Back/save publishes V and foreground 10.3 II imports/renders about 1.25 seconds later, before helper observation. Nine shapes/seven exact pressure files, prior ink, replay and preservation guards pass. No manual Sync, reopen, host publication or renewal in the observation window. Expiry, background delivery and interrupted publication remain open.


2026-10-08: [Brief Gateway interruption](native-reader-server-interruption.md) passes autonomous native publication/application after source close while Gateway was down. Receiver imports about 26 seconds after readiness; ten shapes/eight exact pressure files, prior ink, replay and preservation pass. Actual close preceded restart by five seconds, so sustained failed-upload retry remains open. Evidence sealed; Gateway restored.


2026-10-08: [Sustained Reader disconnect](native-reader-sustained-disconnect.md) passes: roughly three-minute Gateway outage, CBL 502 backoff through 128 seconds, native publication and foreground peer import recover unaided about 80 seconds after readiness. Thirteen shapes/nine exact pressure files, prior ink, replay and guards pass. No OSS upload occurred while disconnected; failed-upload retry remains separate. [Retry layers](native-client-retry-policy.md) documented.


2026-10-08: [Reader expiry/publication hole](native-reader-expiry-publication-hole.md): Y remains native-local on Palma (16 shapes/10 pressures); peer stays at 13/9. Initial expired-session 401, then verified fresh-session scoped start and native UI Sync still yield zero OSS requests in bounded windows. Prior ink and unrelated-library guards pass. Upload 503 harness passes tests but actual failed-upload retry is unqualified; fresh-edit control and retained-publication recovery remain open.


2026-10-08: [Actual Reader upload exhaustion](native-reader-upload-retry-exhaustion.md): four native objects receive nine HTTP 503 PUTs each (36 total), then no successful upload after fault removal or bounded UI/requeue/timestamp/targeted job controls. Exact native bodies remain cached. Separate host-assisted resource publication restores storage; notification downloads resources but Reader skips ink on its timestamp gate. Guarded full native pull recovers Y/Z; both have 17 shape IDs/11 exact pressures, prior per-device ink and library guards pass. Autonomous exhausted-upload recovery and server-only freshness-gate repair remain open.


Current next test: closed/background Reader delivery. Both devices start at 17 shapes/11 exact pressure files after assisted recovery. 10.3 II remains Home. Palma awaits a small R left of Z; leave the PDF open after drawing so a separately timed native close can begin the observation. No injected fault is armed. Evidence: artifacts/gateway-lab/qualification/reader-native-background-delivery/.

2026-10-08: [Home/background Reader delivery](native-reader-background-delivery.md) passes: ksync downloads R while 10.3 II remains Home; Reader explicitly skips import because the document is closed. Ordinary PDF open imports/renders R unaided in about 1.35 seconds. Both devices have 18 shapes/12 exact pressures; prior per-device ink and library guards pass. No SDK pull or host publication. Stopped Reader and screen-off cases remain separate.


Next staged case: stopped receiver Reader delivery. 10.3 II com.onyx.kreader is explicitly force-stopped, PID absence verified; ksync remains running. Both native baselines are 18 shapes/12 pressures. Palma awaits S left of R, PDF left open for a timed save. Evidence: artifacts/gateway-lab/qualification/reader-native-stopped-reader-delivery/. No outcome claimed before physical mark and observation.

Next staged case: BOOX sleep/display-doze delivery. Both lab sessions renewed to a fresh hour, Reader flag observed false after each native login and explicitly reasserted with native notification; exact 19-shape/13-pressure and library checks pass after renewal. Receiver Reader opened before sleep; power reports Dozing and display DOZE, not full Asleep/deep idle. Palma awaits a T above S, PDF left open for timed save. User unlock will be needed after the observation. Evidence: artifacts/gateway-lab/qualification/reader-native-screen-off-delivery/.

2026-10-08: [Reader trigger factors](native-reader-trigger-factors.md) qualify cached-resource rediscovery on both devices: no trigger, MESSAGE alone, unchanged-body commit alone and unchanged-body mode-7 alone yield no refill in 20 seconds; commit revision plus MESSAGE refills exactly with one target GET per peer. Every reset passes. Native 21-shape/14-pressure baselines, library guards, mode-7/commit bodies and existing asset documents stay exact. This is resource recovery, not first-time ink application.


2026-10-08: [Offline Reader alternatives archive](native-reader-alternative-archive.md) qualifies full-body status retention for one owned book: 24 operations/five opposing-status shape IDs, original protobuf/ZIP bodies and all exact pressure dependencies retained. Independent missing/tampered pressure controls reject incomplete packets. Server snapshots/listing unchanged; no device actions or winner selection. Production retention/UI/restore and broader resource closure remain open.


Next staged case: dirty native Reader editor. Palma awaits D above T, with pen toolbar/PDF left open. Original T bytes and exact 21-record/14-pressure baselines are preserved; reversible host deletion/restoration candidates for only the two T records are prepared, neither published. Native delivery/application and later close/save republication must be distinguished. Evidence: artifacts/gateway-lab/qualification/reader-native-dirty-editor-status/.

2026-10-08: [Dirty native Reader editor](native-reader-dirty-editor-status.md) qualifies incoming T disable while Palma pen mode and pending D remain open. Correctly nested ink freshness plus mode-7/commit/MESSAGE triggers autonomous import and push of two D strokes; subsequent native close does not resurrect T in the bounded window. Fresh later restoration restores exact T geometry/info revisions; both devices finish with 23 shapes/15 exact pressures, D semantics and all library guards pass. No SDK pull or broadened push permission. D was flushed before close, so continuously unsaved-buffer and other editor modes remain separate.
