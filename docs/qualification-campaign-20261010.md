# Production qualification campaign — 2026-10-10

The approved seven-part campaign has completed this qualification round; the table records the remaining limits and deferred cases. Real libraries are preserved; new writes use synthetic fixtures. The extended 48-hour soak is deferred. Private artifacts are in `artifacts/qualification-20261010/`.

| Package | Current evidence | Remaining |
|---|---|---|
| Fresh-device restore | 1,080 enabled Lumi entries match; all 654 initially missing resources recovered exactly. A real notebook renders ink and its template. All six audio wrappers decode to exact original backup bytes, but native restoration corrupts their local copies. | Synthetic MP3 repair/redownload is exact after explicit cache eviction; automatic existing-cache repair, PCM transport and broader rendering remain. |
| Exhausted retries / recovery | Actual interrupted notebook downloads recover after native Notes cloud action. Prior lab proves finite exhausted Reader uploads and assisted recovery. | Production EPUB GET exhaustion/manual cover retry and ink PUT exhaustion/Library retry pass; passive long-delay recovery remains unclaimed. |
| Disaster restore | Fresh physical backup/restore: 38,999 retained body objects and 12,961 revision objects hash exactly. Palma authenticates, syncs unchanged catalogs, and natively downloads exact EPUB and repaired MP3 bytes from clone; original production configuration restored exactly. Earlier rehearsal also validates restart inventories. | Nonempty Alexandria-client/WebDAV source restore remains outside this BOOX campaign. Clone view-feed startup required an isolated diagnostic recovery; not an automatic disaster-recovery guarantee. |
| PDF / EPUB lifecycle | Cold-cache EPUB opens; four PDF page geometries render on both devices; bidirectional ink, highlight deletion/history restoration pass. Missing local PDF redownloads exactly with ink and highlight preserved. | Native whole-book logical deletion/Recycle Bin restoration now pass; permanent source purge and server-originated undelete remain separate. Cross-pagination EPUB bookmark and exact source recovery pass. |
| Conflicts / history | Competing production fixture previews block stale execution; retained-history title and highlight restores pass. | Offline native reading-position convergence and retained-alternative restoration pass on both devices; simultaneous ink conflicts and unobserved losing branches remain separate. |
| Bounded observation | Oct11 two-hour interval completed: 121 samples, 119 readiness successes, two failures during planned cold backups; no observation errors or unexplained catalog transitions. Battery temperatures 24.4–27.1°C. | This includes deliberate qualification activity and is not heavy-load certification; 48-hour soak deferred. |
| Failure reporting | Anonymous protected routes reject; required fresh PG/S3 auth/history/OCR cancellation tests pass; actual worker-subprocess kill recovers after lease expiry in disposable DB. Native exhausted GET/PUT states and recovery are captured. Deployed readiness fails and recovers correctly for Gateway/S3 outages; liveness remains separate. Cold backup rejects forced/OOM shutdown. | Production worker crash and a real five-minute expiry wait were not exercised. Readiness and receipt remain distinct from tablet application. |

The current practical recovery controls are:

| Symptom | Qualified recovery | Boundary |
|---|---|---|
| New notebook resources remain absent | Native Notes cloud sync | Catalog/asyncStatus alone does not acknowledge complete resources. |
| Source EPUB download exhausted | Tap its native Library cover after service recovery; tap again to open once downloaded | The Library all-reading-data sync prompt explicitly excludes source books. |
| New ink upload exhausted | Native Library cloud sync and confirm | Merely reopening/closing an unchanged document did not retry the failed uploads. |
| Peer has ink bytes but shows stale ink | Explicit current-revision reading-data refresh, then normal native Sync acceptance | Server publication is distinct from device application; this is assisted recovery. |
| Close stuck after Back dismisses an incoming-data prompt | Home and reopen the same document preserved R in the observed case; use explicit dialog buttons afterward | Do not promise general crash recovery or discard/force-stop a live editor. |
| Native restored MP3 contains the duplicate-IV decoding defect | Explicit retained-version audio repair plus native redownload after controlled synthetic cache eviction | Existing filenames suppress automatic replacement. Personal caches were not rewritten; PCM transport remains unqualified. |
| Restore app responds but native sync is unavailable | Check dependency-aware readiness and private Gateway database/view state before cutover | Keep rollback controls; HTTP listener health is insufficient. |

Campaign resumed 2026-10-11. Native stale-ink recovery now qualifies through the existing server recovery API; see the latest evidence section. The bounded monitor completed at 11:12 UTC; final results are below. The later dated evidence sections supersede the historical preparation/request paragraphs below.

## Cold restore details

Backup `/mnt/alexandria/backups/cold-20261010T221433Z` predates the password reset and the new PDF. It includes the application image, configuration/encryption key, PostgreSQL, SeaweedFS, Couchbase and Gateway configuration. Production resumed after capture. All checksums validate. Offline extraction refuses existing destinations and unsafe archive members.

Local isolated project uses a private generated Compose file, separate bind directories, container names and loopback ports, internal-only network, and gateway-only application role (no cloned OCR worker). Restored Couchbase retains node name `cb.local`, so the isolated container has that hostname/network alias. Gateway's UID/GID 1000 needs group read on its mounted configuration: staging leaves secrets mode 0600 until deliberately assigned `root:1000`, mode 0640 for this file alone. Public origin is an HTTPS `.invalid` hostname; requests for operator checks use local loopback. No device has been retargeted to this clone.

## Resource caveats

The comparison excludes locally disabled notebooks; all 1,071 thumbnail bindings are also absent as files on Lumi. Do not infer corruption from those. Audio ONY1 transport decoding succeeds structurally, but the six fresh Palma local payload hashes do not equal independently decoded server bytes. Five decoded objects match Lumi; the sixth requires comparison with the original backup. This is an unresolved native audio restore issue, not a passed wrapper exemption. No audio has been rewritten or played.

The PDF fixture is `b86b1a31f2d9443dbc866c9286bfce09`, operation `b094c166-0525-4de0-b96e-9cde82bbe5c7`, SHA-256 `8fecbd22e4e69e52091db04bda1fdd9905006316dd5aa9976a42f289ead35e14`. Screenshots of all eight device/page combinations are retained. Lumi is prepared on page 1 for a user-written L, then Back to save. Palma closed the PDF before that edit.

## Native ONY1 audio decoder defect

Independent AES-CBC checks reproduce all six fresh Palma files exactly when following native `FileSecurityV1.decryptInputStream`: after the 1,024-byte header, it feeds the duplicated 16-byte IV into the cipher, then copies only the declared original length. Correct decoding skips that duplicate IV. Native output is a garbage leading block followed by the original bytes except their final 16 bytes. The observed six fresh files match this shifted/truncated result exactly; this is not an expected device-specific encryption difference. Five correctly decoded server objects match Lumi's current bytes; the sixth needs original-backup comparison. Existing recordings are not being rewritten or played. A compatibility workaround must preserve uploaded evidence and be qualified against listing/ETag/length semantics before release.

Static evidence: `~/booxreverse/go103lumi/decompiled/com.onyx.android.ksync/sources/com/onyx/android/sdk/scribble/security/FileSecurityV1.java` (`decryptInputStream`, `encryptFile`) and `.../data/request/SaveDataFileRequest.java`. Reproduction: private `audit-audio.py`, six successful native-byte predictions in `audio-wrapper-audit-private.json`.

## Interrupted download recovery

At 22:40:59 UTC a targeted rehash of all 654 previously absent resources found 654 exact matches, zero absent, zero different. Native activity resumed for all 25 affected notebooks around 17:40 local after using the Notes cloud button (the UI initially says background synchronization is in progress). No resource injection, local metadata edit, forced SDK pull or daemon restart was used. Native catalog providers report asyncStatus 0 for all owner rows even while resource recovery was incomplete; that field alone is not a completeness signal.

## Restored restart and authentication checks

All inventoried PostgreSQL table digests and Gateway document rows are identical before/after restarting the five isolated services (3,878 Gateway documents and projections; 9,591 revisions; 41,140 live bindings; 41,156 retained binding versions; four device records; 38 grants; 22 operation receipts). The intentionally minimal rehearsal Compose omits restart policies/dependency ordering: application startup raced SeaweedFS and failed its bucket health check, then started cleanly when object storage became ready. Production Compose already declares readiness dependencies and restart policy. Do not document an unordered `podman restart` as an automatic complete recovery procedure.

All three anonymous checks (`/settings`, `/boox`, `/api/v1/boox/admin/devices`) return 401. Required fresh PG/S3 race tests pass in `internal/boox` and `library` for authentication boundaries, retained history, recognition cancellation fencing, permanent provider failures and expired processing leases. No matching tests ran in `internal/jobs`; that package is not claimed as coverage for this invocation.

## PDF first ink leg

The owner wrote L on Lumi and closed it. Native Reader delivered the shape ZIP and pressure object to Alexandria, and both devices have exact server hashes for both resources. Palma opened the normal “new document data” prompt; sync was accepted with font/spacing synchronization unchecked and automatic future sync unchanged. The L renders inside the page-1 rectangle. Native source-book bytes were delivered by Alexandria, never ADB-pushed. The owner is now preparing P beside L on Palma for the reverse leg.

Palma-specific source confirmation: targeted JADX of its pulled ksync APK reproduces the same mismatch in `FileSecurityV1`: `encryptFile` writes the 1,024-byte header, then the IV, then ciphertext (lines 246–248 in the private extraction); `decryptInputStream` reads only the remaining 1,019 header bytes after magic/version and immediately creates `CipherInputStream` (lines 122, 162). This is verified against the Palma APK, not inferred solely from Lumi source.

The restored revision archive also passes: all 9,591 referenced revision objects (32,130,770 bytes) hash exactly to PostgreSQL's retained references, including objects not included in the 38,969 asset-body count. Evidence: `restored-revision-audit.json`. The first 30 one-minute production observations show no readiness failures, unchanged catalog digests on both devices, and maximum sampled readiness latency 0.413 seconds. This is a partial interval; the scheduled two-hour monitor remains running.

## PDF reverse ink leg

Owner confirmed P saved and closed on Palma. Server now retains two shape ZIPs and two pressure resources for the fixture; all four match exact local bytes on both devices (eight checks). Lumi offered the normal incoming-document prompt; font/spacing synchronization was unchecked and Sync accepted. Evidence: `pdf-after-P-resources-private.json`, `pdf-P-peer-hashes.json`. Visible reverse-leg rendering is checked separately.

Lumi reverse-leg screenshot confirms both blue L and black P visible on page 1 after accepting native sync. Both editors were then closed. Evidence: `screens/lumi-LP-visible.png`.

## Production stale-operation and history check

Two rename previews targeted the same revision of the existing disposable root fixture `0c303e6f-7ce4-4acb-af55-fb60370f28a2`. A published as revision `8-7046924b718113fd279b5746ea958d73`; B remained journaled with `blocked / stale_preview`. A new restore preview selected the actually observed original revision and published `9-a281feee83979d4626d7a5db943fd090`. A subsequent projection read confirms the original title exactly. No real notebook titles changed. This qualifies optimistic precondition enforcement and retained-history authoring, not simultaneous offline native editing. The original harness used the wrong response field for its cleanup revision; this was corrected immediately and cleanup was explicitly published and independently verified. Evidence: `stale-operation-private.json`, `conflict-restored-proof.json`.

Fresh Palma opened the restored 20261008 Jeff notebook and rendered handwriting plus its lined template. Native first-open guidance explains that the cross-device canvas exceeds the smaller screen; the observed initial crop is a viewport setting, not missing source content. The notebook was closed without drawing. A private screenshot is retained; this one sample does not certify every page's rendering.

After both pen edits, source PDF SHA-256 on both devices remains the original `8fecbd22e4e69e52091db04bda1fdd9905006316dd5aa9976a42f289ead35e14`. Ink is transported separately; no embedded-PDF rewrite was observed. Evidence: `pdf-source-after-ink.json`.

Next human action requested: on Lumi page 1, text-highlight “Alexandria”, then Back to save. ADB long-press at the text did not enter selection in the current reader mode; no global touch preferences were changed. Palma is out of the personal notebook editor. Highlight peer import, deletion/restoration, controlled native retry exhaustion, offline native conflict, process-kill OCR rehearsal and the remainder of the two-hour observation interval are not marked passed.

## PDF text highlight lifecycle

Lumi created the requested “Alexandria” highlight. Alexandria observes native Reader mode 1, unique ID `1dec7cd248744ea0b82c825eec0f2167`, quote “Alexandria”, pageNumber 0, status 0, initial revision `1-28865bae8f0c21f88decc59a3faf6d41ab778814`. Palma accepted the normal incoming-data prompt and displays the yellow highlight on the correct word, alongside unchanged L and P.

Using Palma's native Mark → clear-format broom removed that one selected highlight; closing the reader published revision `2-de2d7ee01fe48100ba67f53e17b75c02ce1fd1c3` with status 1. For Reader mode 1, 0 is active and 1 is deleted; do not apply NoteModel lifecycle constants. Original and deleted revisions are retained. A legacy common `Annotation` provider query by idString returned no rows on either device despite the visibly rendered native annotation; that legacy endpoint is not evidence of absence. Server body plus native rendering are the evidence used here.

The deleted highlight visibly disappears on Lumi with L and P unchanged. Recovery operation `b2b1a29d-d800-4627-8a73-8b165fcb5c7e` uses `restore_reader_fields`, the deleted expected revision, and the original observed selected revision. It publishes `3-d1ea4979b1f2e53780bd133559a7e206`. Both devices receive the normal incoming-data prompt and visibly restore yellow “Alexandria” after acceptance. Both readers finish closed. This qualifies native highlight deletion and server-driven retained-history restoration for the uploaded PDF, not merely transport acceptance. Evidence: `highlight-restore-preview-private.json`, `highlight-restore-result.json`, and paired restored screenshots.

## Actual OCR worker process death

Added `internal/boox/worker_crash_test.go` in Alexandria Server. A real child test process uses the parent's disposable PostgreSQL library and scoped S3 fixture, claims the job, renders the synthetic page and blocks inside a fake recognizer. Parent waits for the recognizer-entry marker and kills that process. The persisted row remains processing with a live lease and no text; another worker correctly declines it. Advancing only the lease timestamp past expiry permits recovery, publishes the expected synthetic transcript, clears the lease and leaves no duplicate runnable job. Fresh required integration/race run passes; vet passes. No production worker or real provider is involved, and the real five-minute lease timeout is not elapsed in this test. Evidence: `worker-process-kill-test.log`.

## Original audio backup closure and local PDF cache recovery

All six correctly decoded server audio bodies exactly match the pre-Alexandria `.note` backup, including the PCM whose current Lumi bytes differ. This closes original-byte retention, not playback or device-side repair. The original server objects remain unchanged. Evidence: `audit-audio-backup.py`, `audio-original-backup-private.json`.

With Palma's reader closed, the exact synthetic PDF was moved from its native book path to shell-private `/data/local/tmp` escrow. Tapping its existing Library cover redownloaded the original SHA-256; a second tap opened it with L, P and the yellow highlight visible. All four ink/pressure objects remain exact on both devices (eight checks). No source bytes were copied back by ADB. This qualifies local source-file loss and ordinary redownload, not whole-book deletion. Evidence: `pdf-cache-eviction.json`, `pdf-redownload-peer-hashes.json`, `screens/palma-pdf-redownload.png`.

## Concurrent offline reading positions and retained alternative recovery

Both devices started from the same PDF position (1/4). Wi-Fi was disabled on each while USB ADB remained available. Palma visibly moved to 2/4, Lumi to 4/4; both were closed before Wi-Fi was re-enabled. Server history retained Palma 2/4 as `10-26a75dc6353d9bfc7d0dbe46912a5683541aabea`, then Lumi 4/4 as `12-0689b3fcd2004190db1142429f3ebe53816be5be`. Palma offered the normal incoming-data prompt and both devices then displayed 4/4. Logs include a 409 for the separate commit document `b094c16605254de0b96e9cde82bbe5c7`; this must not be mislabeled a proven conflict branch on the mode-4 metadata document itself.

Owner-path `restore_reader_fields` selected the actually retained Palma 2/4 revision against current revision 12. Operation `374b5d1c-c47b-4599-ac47-58a96d80a98d` published revision `13-aa36dc84a76177219d493915d0ceaa70`. Both devices offered incoming Page 2 (50%) and displayed 2/4 after acceptance. Both readers finish closed and Wi-Fi enabled. This qualifies concurrent offline reading-position convergence and recovery of the retained alternative, not arbitrary simultaneous pen edits or every losing Couchbase branch. Evidence: `pdf-position-*-private.json`, `position-restore-result.json`, paired position screenshots, native log.

## Removed-book duplicate upload guard

Review found that exact-byte re-upload returned the original upload operation even after its native book metadata was tombstoned or its live source binding removed. Returning the historical `published` state could be mistaken for a successful restoration. `QueueBook` now rejects those known-removed cases with an explicit explanation. It preserves history and does not invent a new native identity or undelete a book. A still-queued duplicate remains idempotent.

New required PostgreSQL/S3 tests cover metadata removal and source-binding removal, asserting no new operation/version is authored. All book tests pass with race detection; vet passes. Deployed the one changed runtime source file, rebuilt/restarted the app, and verified matching deployed source hash plus public readiness, authenticated upload page and Settings (HTTP 200). This is honest failure reporting, not implementation of whole-book restore. Evidence: `book-lifecycle-tests.log`, `book-lifecycle-deploy.log`, `book-lifecycle-deploy-smoke.json`.

## OSS failure and durability rehearsal

New disposable PostgreSQL/S3 integration test stores bytes but injects a failed storage acknowledgment nine times. Every native-facing PUT returns 503, no binding/version is published, and the unreferenced stored object is not GET-visible. A later retry succeeds. Deliberate same-length object corruption makes both GET and HEAD return 503; restoring exact bytes recovers reads without extra metadata versions. Required race test passes. This exercises the server boundary with real storage, not the devices' native retry scheduler. Evidence: `oss-failure-recovery-tests.log`.

Additional restored-source inventory finds zero Alexandria object records, zero Rhizome chunks, zero client notebooks and zero client reader books. The physical backup therefore provides no nonempty client/WebDAV source-domain qualification; BOOX body/revision validation must not be generalized to those empty domains.

## Owner-requested pause — 2026-10-10 23:24 UTC

Owner confirmed both offline marks saved, then requested closing the session. Wi-Fi is re-enabled on both devices. Post-reconnection captures contain eight shape/pressure files per device; all 16 paired resource hashes match the server. Visible rendering has NOT converged: Palma shows B, Lumi shows a small blue mark; both retain L/P and the yellow highlight. Screenshots and full fixture resource directories are retained. Next investigation is native shape import/freshness, not missing transport bytes; do not mark concurrent ink passed. No repair or forced pull was attempted.

The initial offline resource copy failed because its destination parent did not exist. The following shell commands nevertheless re-enabled Wi-Fi; therefore no complete pre-reconnection resource snapshot exists. Subsequent snapshots are explicitly named `AB-reconnected-*`. Do not claim they prove isolated pre-sync shape sets.

Both readers were closed with Back. Native log capture and the bounded observation process were stopped at the owner's pause request. `monitor-paused.json` records the partial interval; the two-hour target was not completed. Production Alexandria stays running normally. No pending manual action. Resume from this ink-rendering discrepancy, then the remaining campaign gates above.

## Resume and server-driven stale-ink recovery — 2026-10-11

After the overnight pause, ordinary reopening still showed Palma B without Lumi's blue mark, and Lumi's blue mark without B. Both retained L/P and the highlight. Native logs on both readers report `needPull=false`: cloud ink time was 2026-10-10 18:22:02 local; Palma's last pull was 18:22:09 and Lumi's was 18:22:02. Matching cached resources did not imply native application. A legacy ReaderNoteShapeModel provider query remains unimplemented; it was not used as proof of absent shapes.

With both editors closed, the existing owner recovery API selected the current mode-4 revision as both expected and selected revision (`19-7a4075b1a4d3195f2003106c56d9c5b2eeb75ebf`). Operation `85ad726f-a5ae-4519-b836-229bfa0c329b` published `20-0744256647a6b48228e6e0fcf17e75ec`. Independent before/after comparison finds only `_rev`, `updatedAt`, `alexandriaOperationId`, and `extraAttributes.backend.user_doc_data_update_time` changed. Reading position, native identities and all ten source/ink/cover resource bindings remain exact.

Both readers then report `needPull=true` and offer the normal incoming-data prompt. Accepting Sync imports the already cached peer ink: both show B plus Lumi's blue mark, L/P and the yellow highlight. A second close/reopen preserves that combined rendering. All eight shape/pressure objects match on both devices (16 checks). No ADB ink injection, SDK forced pull, shape rewrite, mode-7 republication, server restart or new software deployment was used in this recovery. The physical Lumi mark is a short blue stroke, not a visibly complete A; report the observed fixture rather than assuming the requested letter was drawn.

This qualifies assisted freshness recovery using existing server publication and native UI acceptance. It does not turn the original unattended offline case into a pass, prove every native database record from the disabled provider, or justify automatically rewriting every incoming timestamp. Product implication: expose explicit received-data refresh with revision fencing and retained evidence; automatic recovery needs closure/delivery policy and feedback-loop guards.

Evidence: `artifacts/qualification-20261011/` contains before/recovered/reopened screenshots, native logs, metadata before/after, operation preview/result and paired resource hashes. Native ZIP decoding finds five shape records across four batches. A new 120-minute read-only monitor started during this session; the previous 68-sample interval remains partial. Both readers finish closed, Wi-Fi enabled. No human action is pending.

## Product reading refresh and active Reader status — 2026-10-11

Deployed a revision-fenced owner UI at `/boox/reading/refresh`, linked from the Reading table. It queues the existing `restore_reader_fields` operation against the current active mode-4 revision, preserving position and opaque attributes while advancing native ink freshness. UUID replay is bound to the same document/revision; newer native edits block publication. The status page explicitly distinguishes server publication from unverified device application.

Fresh required PostgreSQL/S3 race tests cover stale selection, deleted books, same-origin protection, idempotency, native edits after enqueue, retry after lost MESSAGE acknowledgment without a second metadata write, exact preservation of a large opaque integer and unchanged reading position. Public authenticated GET/form/POST pass; operation `7c437996-c34a-4a68-809a-634b61525e62` targets only the synthetic PDF. Its final publication/device result is recorded separately below when observed. Evidence: `reader-refresh-tests.log`, `reader-refresh-deploy.log`, `reader-refresh-public-smoke.json` in the 20261011 artifact directory.

The same deployment corrects Reader mode-1 highlights and mode-2 bookmarks in web listings: native status 0 is active, 1 removed, as confirmed in installed firmware's decompiled Annotation/Bookmark classes and the paired highlight lifecycle. The previous query reversed those states. Tests now include live and removed fixtures of both modes; the active PDF highlight appears in the public Reading page.

## Native audio codec compatibility prototype — 2026-10-11

An ADB-shell DEX probe invokes each device's installed ksync FileSecurityV1 encryptor and OnyxFileEncryptUtils decryptor by reflection. For deterministic synthetic payloads of 0, 1, 15, 16, 17, 4096 and 4097 bytes, removing only the duplicate IV block at wrapper offsets 1024–1039 makes the native decoder recover the exact original on both firmware versions. All nonempty unmodified wrappers reproduce the defect. This probe uses shell-private scratch files; it does not modify installed packages or native libraries. Evidence: `audio-probe/{palma,lumi}-native-codec.log` and probe source/DEX in the 20261011 artifacts. This qualifies the codec transformation, not full sync or audible playback.

Owner recorded a short synthetic test in notebook `9afe3a7b4c944331a9b02f681ff94b7c` (Alexandria Audio Qualification 20261011 267e31ef). After recording stopped, Back saved/closed the editor; initial pre-close server/resource absence was expected. The original recording is privately escrowed and must remain intact. No personal recording is selected for repair. A bounded owner repair-preview endpoint and required integration tests are being qualified before deployment; this is an explicit derived-version repair, not a global upload rewrite.

## Explicit audio repair: native download recovers original bytes

The fresh test recording reproduces the defect: 67,824 original MP3 bytes become a 68,880-byte ONY1 wrapper, and Palma's first restore exactly matches the shifted/truncated native decoder result. Correct independent decoding matches the original recording.

Deployed `POST /api/v1/boox/admin/audio-repair-preview` after fresh full required PostgreSQL/S3 BOOX race tests (10.193s) and vet. It accepts only an owned active notebook, its pinned current revision, an exact live MP3/PCM version, and a UUID idempotency key. Strict magic/version/length/duplicate-IV/AES-padding validation precedes derivation. Preview retains a new immutable version without changing live data; execution uses the existing revision/generation/asset-version-fenced repair journal. No schema migration, general upload rewrite or personal audio operation was performed. GET/HEAD/list length, MD5 and ETag consistency, origin/scope/stale guards and original retention pass integration tests.

Synthetic operation `4fa2c280-82d0-4077-9a56-b85b2011b1a4` published revision `2-bc155615e75a5f6d7472bc3cb2387c64`. Corrected live version 41187 is 68,864 bytes, SHA-256 `03b93b31050191d81627509f3097b7e139fa686d0e93fd3d54767783d0f38acf`; independent reads verify both the exact derived transformation and unchanged original object. The operation retains source version/hash and transform provenance.

Important native limit: `ResourceDataSyncFile.makeSyncDataBean` compares resource filenames; an existing MP3 is not redownloaded because its server bytes/mtime changed. Only PCM gets an additional size comparison, which is not a completeness guarantee. Publishing the repair alone left Palma's corrupt test MP3 unchanged. We moved that one synthetic local file into shell-private escrow, then opened and closed the note. Native logs show resource-data pull at 04:32:22 local after close; a later Notes cloud action also ran, so the result must not be attributed solely to the cloud button. No recording bytes were pushed to the device.

Palma's redownload and unchanged Lumi source now both exactly equal all 67,824 original bytes, SHA-256 `b6ef7bda3e43a54c6945a2c87264a8d84ebd9edc2a34ecbac04ee0f9a24ec2ea`. This qualifies repaired MP3 download/decoding with controlled cache eviction, not automatic existing-cache repair or listened-to playback. PCM transport, general client cache-repair UX and safe policy for future uploads remain distinct. Six personal recordings are untouched. Evidence: `audio-before-proof.json`, `audio-repair-preview-private.json`, `audio-repair-result.json`, `audio-cache-eviction.json`, `audio-repair-device-proof.json`, native logs and original/derived private objects in the 20261011 artifacts.

The new public reader-refresh form operation also reached published; its page continues to say device application is unverified, as intended. The previously documented paired refresh/rendering experiment supplies native behavior evidence, not a claimed acknowledgment for every subsequent form request.

## Whole-book logical deletion and native Recycle Bin restore

On Palma, deleted only synthetic EPUB `fb0c3c69ba6b45d49d0e3af0e9aca654` using its Library options, Delete, and the native Recycle Bin dialog with document data selected. Server mode-4 metadata advanced from revision 4 to `5-a6fe5e4ef3aec4ecfb004c880dd7e58373cd2cb3`, status 1. Read-only native Metadata queries on both devices confirmed status 1. The existing Lumi search screen retained a stale displayed row; it was not used to contradict the underlying native state.

The source cloud binding and retained body were unchanged; the prior metadata revision remains archived. Palma's local source was gone, Lumi's cached source remained. Thus this native deletion propagates a logical library tombstone, not a physical purge of all replicas. Only the synthetic book was touched; other Recycle Bin entries were left alone.

Palma's per-item Recycle Bin restore published `6-59738bf5024aeba3821ae62e527286d6032269aa`, status 0; both native providers returned to status 0. Tapping the restored cover redownloaded Palma's source, and subsequent opening rendered the book. Lumi also rendered it after normal incoming-data Sync acceptance. Font/spacing sync was unchecked and auto-sync unchanged. Both sources exactly match the original 2,168-byte EPUB, SHA-256 `5dec11d953c3abb67261847107ac11316a5bdd408b8d9716e5172bfe67454f62`.

No new server authoring code or host metadata injection was needed for this round trip. This qualifies native logical whole-book deletion and native restore through Alexandria. Permanent cloud source purge, server-originated book undelete UI, and retention/GC policy remain separate; exact re-upload still does not undelete a known removed book. Evidence: `epub-lifecycle-*-private.json`, deletion/restoration proof JSON, paired native provider receipts and rendered screenshots in the 20261011 artifacts.

## EPUB bookmark across different pagination

Palma renders the restored EPUB as two pages; Lumi renders the same source as one. Added a native bookmark on Palma page 2/2, quote “No USB book copy. No hand-placed device metadata.” Native mode-2 record `dc0415cc1e474589a7488f4fb196cce3` arrived at revision `1-37277ecebcf438458a49e1226dbea1a3fb584a64`, status 0. Its opaque positionV3 contains chapter ID `page`, nodePosition 208 and textOffset 72, serialized with native leading-zero numbers; preserve this string without normalizing it as standard JSON.

After normal incoming-data Sync acceptance, Lumi shows the bookmark on its page 1/1. Read-only common Bookmark provider queries on both devices match the exact quote and positionV3; the original pageNumber 1 remains in the record, rather than being rewritten to match Lumi's page count. The web Reading list shows the live bookmark quote. Both local EPUB sources remained exact after deletion/restore. This qualifies a cross-pagination native bookmark anchor for this small EPUB, not every EPUB layout/engine or cross-format mapping. Evidence: `epub-bookmark-private.json`, `epub-bookmark-proof.json`, paired provider receipts and `lumi-epub-bookmark-received.png`.

A bounded production-path native book-download failure test is now in progress on this same disposable EPUB. Nginx has a temporary exact-resource GET-only 503 location and a 15-minute systemd cleanup timer. Protected readiness, HEAD handling and unrelated resource routing remain normal. The original local EPUB is escrowed on Palma to force native redownload; no source has been injected. The original proxy configuration and enable/preflight receipts are retained. Cleanup must be verified before leaving this experiment.

## Native EPUB download exhaustion and recovery — 2026-10-11

The bounded exact-EPUB GET fault produced nine native download attempts between 09:44:32 and 09:44:35 UTC, all HTTP 503. Nginx returned its default HTML error page; the OSS SDK additionally reported an XML parse error and a terminal ReaderDocReplicator failure. The native cover continued to show Wait. This qualifies proxy-style HTML failure, not OSS XML response handling.

Removed the fault at 09:45:48 UTC, stopped its cleanup timer and verified the generated proxy configuration byte-for-byte against its original. During the next 60 seconds no new GET arrived and the source remained absent. One native cover tap at 09:46:58 caused HTTP 200 and downloaded all 2,168 original bytes with the exact original SHA-256; a second tap opened the EPUB on page 2/2 with its bookmark intact. No source bytes were restored by ADB. Explicit native retry works after this exhausted attempt; this short interval does not rule out later scheduled retry. Evidence: `epub-retry-recovery-proof.json`, proxy request receipts, native logs and `palma-epub-retry-recovered.png`.

## Ink upload experiment: close-dialog stall and safe recovery

Owner added a blue R offline to page 1 of the synthetic qualification PDF. Its new pressure resource was escrowed before reconnecting. Close reached an incoming-data prompt for the earlier explicit web refresh. Dismissing that prompt with Android Back left NeoReader at Saving the reading data, with no shape ZIP export or native upload request. Restoring Wi-Fi did not clear the stall. The initial PUT fault was removed with exact proxy restoration verified; it had caught no native request.

Static `ShowPullTipDialogAction` explains a likely lost continuation: positive/negative buttons emit and complete the BehaviorSubject; the dismiss callback only clears showingPullDialog. Close waits for the sync-check observable. At 09:57 UTC, Home followed by reopening the same existing PDF recovered the native page with R plus L/P/B, the blue mark and highlight all visible. No process kill, source injection or pen redraw was used. This is evidence for safe reactivation in this case, not a general crash-recovery guarantee.

The exact-fixture PUT-only OSS-XML 503 fault was re-armed with a new 15-minute cleanup timer at approximately 09:59 UTC. The resumed close prompt will be handled via its explicit Sync button. Upload exhaustion/recovery remains pending until actual native traffic is observed. Evidence: `lumi-R-recovered-before-upload.png`, `lumi-R-close-dialog.xml`, native save-stall logs and pressure escrow.

The resumed native Sync button exported R, then encountered 18 HTTP-503 attempts per new resource (36 total across the observed repeated push attempts). Valid OSS XML produced explicit ServiceUnavailable and terminal native Upload Error, unlike the HTML download fault's XML parser failure. Both the new shape ZIP and pressure object are escrowed; all eight earlier resources are unchanged. Independent server readback still has ten prior bindings and neither new resource, so failed PUTs did not masquerade as published content. The second fault was removed at 09:59:28 UTC; generated configuration matches its original, cleanup timer is stopped, unauthenticated target PUT returns normal 403 and readiness returns 200.

The resumed prompt had font/spacing sync checked when Sync was pressed; auto-sync was false. This particular acceptance must not be described as font sync unchecked. Only the synthetic PDF was open. The checkbox was left at its observed checked value; this acceptance does not qualify the unchecked-font case.

## Exhausted ink upload: native Library recovery passes

The fault was removed at 09:59:28 UTC; the first subsequent native document query occurred on explicit reopening at 10:00:27–28. No successful R upload occurred during that approximately one-minute unattended interval. Reopening and closing again also did not retry: `CheckSyncBeforeCloseAction` reported `needPush=false, dataChanged=false, progressChanged=false` while the reader retained Upload Error. This is a native scheduling distinction, not server receipt.

At 10:04:11 UTC used Lumi Library's cloud button and confirmed its native all-reading-data sync prompt (which explicitly excludes source books). At 10:04:13 both R resources uploaded successfully; at 10:04:14 NeoReader reported SYNC_SUCCESS. Independent server readback now has 12 bindings. All ten shape/pressure resources match exact server bytes on both devices (20 checks); the pressure SHA matches the pre-reconnect escrow. Palma's ordinary incoming-data Sync acceptance renders R with all prior marks/highlight. Font sync was unchecked on Palma; automatic sync remained off. No process restart, forced SDK push or resource injection was needed.

This qualifies production-path exhausted native upload recovery through the standard Library sync control. A passive later retry was not ruled out by the short observation. Closing an unchanged document alone is not reliable retry advice. ADB taps did not open the reader menu during this investigation, but the Library control remained usable; this is not proof of a user-facing menu defect caused by the earlier close stall. During UI exploration single-finger pan was briefly toggled off and immediately returned on; no permanent gesture change is intended. Evidence: `ink-failure-proof.json`, `ink-put-failure-count.json`, `ink-exported-proof.json`, `ink-library-retry-action.json`, native logs, `ink-R-paired-resource-proof.json`, `palma-R-after-sync.png` and reopen screenshot.

## Fresh physical restore and cutover preparation

Captured a new complete cold package at `/mnt/alexandria/backups/cold-20261011T100650Z`, including R and the audio repair. Production restarted; readiness and production Sync Gateway health independently pass. The observer recorded the planned backup interruption. A partial workstation transfer was stopped and explicitly marked incomplete; it is not a restore source. Verified all package checksums/archive paths and extracted instead into a new VM ext4 directory `/var/lib/alexandria-qualification-20261011/restored`. Original production directories are untouched.

The new five-service clone uses distinct containers, an internal-only Docker network, gateway-only app role and a disposable TLS tunnel. Docker's internal network did not expose the requested host port in this setup; a bounded SSH forward to the clone's actual private address supplies tunnel ingress without granting container egress. Palma's native endpoint/system/marker and catalog snapshots are escrowed; Wi-Fi is temporarily off. Its target has NOT yet changed.

Clone HTTP readiness returned 200 while Sync Gateway was still waiting for views. Direct non-stale view queries failed with `{status,4}`; Couchbase's log identifies DCP_STREAM_REQ on a non-DCP connection. Production Gateway remained healthy. Therefore app readiness alone is insufficient to gate a BOOX restore cutover. Clone-only diagnostics/restart are in progress; no device is exposed to this startup failure. All raw receipts are under `artifacts/qualification-20261011/cutover/`.

After the native retry had already succeeded and peer rendering was captured, Lumi NeoReader alone was restarted with its editor closed. This later diagnostic restart must not be attributed to the successful upload recovery. ADB page taps still did not open its menu; avoid attributing that automation limitation to the canceled-dialog issue without more evidence.

The fresh clone independently validates all 38,999 retained body objects (1,211,973,010 bytes), zero failures. An ordinary clone Couchbase-container restart did not clear its view-feed error. With Gateway stopped, restarting only the clone CouchDB child produced two successful DCP_OPEN handshakes (captured in a private loopback trace). Fresh channels/access/role_access queries then all succeeded and Gateway was started. No index files were deleted, no documents rewritten and no production process restarted for this diagnostic. This is an observed startup-recovery sequence, not a proven root cause or general automated repair policy.

## Restored production data: live Palma cutover

After clone Gateway health and authenticated `_all_docs` passed (3,902 rows), redirected only Palma's current cluster URLs and its corresponding marker URLs to the disposable TLS origin. The native UID and cluster ID remained unchanged; no enrollment/login, account switch, account-row edit or library-row repair was performed. Notes/reader sync were briefly paused, cached native processes restarted, original flags restored and Wi-Fi enabled. Exact original configuration/marker snapshots and a 20-minute host rollback timer bounded the change. This is an operator-assisted endpoint rehearsal, not a new PowerSync product transition.

Native Library sync authenticated to the restored Gateway; its log records HTTP 101 BLIP upgrades and successful native replication. All 1,524 notebook identity/status/sync-flag rows and 135 book identity/status/path rows are exactly unchanged. Moving only the synthetic EPUB source to shell escrow forced a native redownload: all 2,168 bytes match the original, and NeoReader renders page 2/2 with the bookmark. No bytes were pushed onto the device.

A second controlled cache eviction selected only the synthetic repaired MP3. Opening/closing the notebook caused native resource recovery. A private capture at the clone's actual app address shows the exact audio GET and HTTP 200 Content-Length 68,864; Palma's decoded local file is the exact original 67,824 bytes. This independently ties notebook resource restoration to the clone, not cache or the production object service. All four database/Gateway/object-service image identities match production exactly. No provider worker ran; the clone app uses gateway-only role. This qualification therefore covers retained native CBL/authentication/catalog and native source/resource delivery, not resume of cloned recognition jobs.

At 10:30:59 UTC restored the original production endpoint, exact marker bytes and both original sync flags. Final readback has identical 1,524 notebook and 135 book fingerprints, and six native SYNC_SUCCESS log lines after returning. The rollback timer is inactive; disposable TLS tunnel and SSH forward are terminated; all five clone containers were stopped. The clone storage is retained privately. Subsequent readiness checks may restart only this isolated copy, with no device attached. Evidence: `cutover/{apply-receipt.json,epub-redownload-proof.json,palma-restored-epub.png,audio-redownload-proof.json,native-resource-origin-proof-private.json}`, native logs and private Gateway receipts.


## Readiness now checks sync dependencies

The physical restore exposed a false-positive readiness response: PostgreSQL was ready while Gateway's view feed was unavailable. The standalone application now checks PostgreSQL, the S3 bucket and, when native BOOX is configured, the exact configured Gateway database state plus a bounded `_all_docs?limit=1` query. These checks share a two-second request context. `/readyz` returns generic HTTP 503 on failure; private addresses, credentials and upstream bodies are excluded. `/livez` and the legacy `/health` remain process probes. Neither readiness nor database receipt proves application/rendering on a tablet.

Required fresh PostgreSQL/S3 race tests pass for `internal/server`, `internal/boox` and `cmd/alexandria-server`; vet and diff checks pass. Tests cover offline/wrong databases, failed/malformed views, cancellation, dependency failure/recovery, shared deadlines, short-circuiting and independent liveness on both listeners. Deployed all three runtime files and independently matched source hashes. Public readiness/liveness/health return 200. No migration or settings changes. Evidence: `readiness-deploy-manifest.json`, `readiness-deployed-proof.json`, private deployment log. Isolated live outage checks pass with the deployed image: Gateway down gives readiness 503/liveness 200; Gateway startup automatically recovers to 200. Object storage down gives readiness 503 within 2.002 seconds/liveness 200; storage startup recovers to 200. All clone services were stopped afterward. Evidence: `cutover/readiness-{gateway-down,gateway-recovery,storage-recovery}-proof.json` and `clone-final-stop-proof.json`.


Fresh revision-object validation independently checks 12,961 distinct retained bodies (47,172,530 bytes), zero hash failures. Combined with the earlier 38,999 source/resource body checks, this validates the fresh clone's complete inventoried native immutable object classes. It does not qualify other source domains with no data. Evidence: `cutover/revision-audit-result.json` and `object-audit-result.json`.

Final paired catalog comparison has identical owner IDs on both devices. Exactly two synthetic notebooks have differing local flags/state: the existing root text fixture and `PowerSync ink 14f3a7ad-e78a-407b-8ef5-1fe50d734384`, both sync-disabled on Lumi. The latter is logically removed on the server/Palma but retained locally with sync disabled on Lumi. Do not enable or delete them to manufacture equal catalog counts. All 1,081 active sync-enabled Lumi entries agree on Palma for the final fingerprint fields: uniqueId, userId, type, status and noteSyncStatus. This final guard does not re-compare titles, parents or page-name lists. Non-owner rows also explain total-count differences. This catalog assertion remains distinct from per-page rendering or full resource completeness.

## Cold backup shutdown safeguard

The rehearsal's default ten-second Docker stops terminated SeaweedFS with exit 137. A final isolated stop allowing 60 seconds ended normally (exit 0) after 10.225 seconds for the storage group. No OOM flag was set. This is a concrete graceful-shutdown budget issue; it is not proof that the earlier Gateway DCP failure had the same cause.

SeaweedFS Compose now has a 60-second stop grace period. The cold backup explicitly uses `ALEXANDRIA_BACKUP_STOP_TIMEOUT` seconds (default 60), records stopped-container exit/OOM states, and refuses publication after forced/OOM termination. Its restart trap still runs on failure; incomplete evidence stays in the private `.partial` directory. Three synthetic tests pass, including rejected forced stop and failed copy with restart. Both files are deployed with exact hashes; production Compose validation passes. A final full package at `/mnt/alexandria/backups/cold-20261011T104536Z` completed at 10:46:44 UTC. App, PostgreSQL, Couchbase and SeaweedFS stopped with exit 0; Gateway exited 130; no OOM flags. Production restarted and dependency-aware readiness returns 200. A separate fresh restore startup check uses this package; no tablet is attached. Evidence: `final-backup-{result,live-proof}.json` and the package’s `stop-status.txt`.


## Final package verification and fresh startup

The clean-stop package `cold-20261011T104536Z` passes all manifest hashes and archive-path validation. A second new ext4 extraction boots the retained latest app image with a gateway-only role and no tablet endpoint. Native Gateway becomes healthy without the prior CouchDB-child intervention. The minimal rehearsal harness initially launched the app before object storage health; the app correctly refused startup, then reached readiness 200 when started after storage was healthy. Production Compose already has the dependency ordering. An early observer retained an empty address from the exited container; its URLError samples are invalid startup-timing evidence and are superseded by the independent successful probe.

This final extraction validates 38,999 body objects and 12,983 revision objects (51,982 total, 1,259,196,490 bytes), zero failures. The revision increase from the preceding package reflects intervening native metadata traffic. All three protected routes reject anonymous requests with 401 and accept the retained owner credentials with 200. There was no worker execution, provider request, device cutover or cache eviction in this last check. Evidence: `final-restore-{ready,auth,full-object,stop}-proof.json`, setup and audit logs. Restore copies remain private and stopped after qualification.

The final proxy readback exactly matches its original configuration, with no qualification markers or fault timers. Both earlier tunnel processes are terminated. The recorded production return already proves original endpoint/marker/flags and successful native synchronization; final catalog comparison preserves all 1,081 active sync-enabled Lumi entries on Palma. Evidence: `final-proxy-cleanup-proof.json`, `paired-final-active-catalog-proof.json` and `cutover/production-return-proof.json`.


After the final cold backup, passive logs did not establish automatic native reconnection. At 10:57:43 UTC invoked the normal Library cloud control and confirmed on both tablets. Both independently report successful pull/push/pull Library-tree callbacks, with no observed reader authentication failure. This qualifies explicit native reconnection following the final production restart, not unattended recovery. Both remain configured for production. Evidence: `final-library-sync-proof.json` and paired private native logs; production Gateway logging did not provide HTTP-upgrade receipts for this check.


## Completed bounded observation

Read-only observations ran from 09:12:20 to 11:12:30 UTC (2 hours 10 seconds), including the final snapshot: 121 samples. Readiness succeeded 119 times. Both HTTP-error samples fall inside deliberate cold-backup interruptions (10:08:19 and 10:46:19); no other readiness failure was observed. Median successful request latency was 0.183 seconds; maximum 2.291 seconds. One-minute sampling cannot exclude shorter incidents between samples.

There were no device observation errors. Each catalog has exactly two fingerprints: before and after the known synthetic audio notebook was added (Lumi at 09:21, Palma at 09:24). Catalogs then remain stable through the retry, cutover/return, restart and final quiet interval. Palma battery temperature is 24.9–25.0°C; Lumi is 24.4–27.1°C. Both finish at 100% battery. This checks battery temperatures and catalog/readiness behavior; it is not a CPU thermal profile or high-load capacity certification.

The observer and finalizer exited successfully. Both tablets remain on production, with successful explicit native Library synchronization after the final restart. Temporary proxy faults/timers and tunnel are gone; both fresh restore stacks are stopped. Private backup/restoration evidence is retained. The final cold package contains the deployed readiness image and recorded clean storage shutdown; tests and all 15 modified Go runtime source hashes match deployment.

Remaining limits are intentional and visible: extended 48-hour soak, automatic repair of existing native audio caches, PCM transport, broad arbitrary rendering, nonempty other-source restore, permanent book purge/server-originated undelete and simultaneous ink conflicts are not claimed by these results. Evidence: `bounded-observation-final.json`, `observations.jsonl`, `final-runtime-source-proof.json`, `final-library-sync-proof.json` and the restore/cleanup receipts above.
