# Product implementation and lab restoration

2026-10-08. No production rollout. The original PowerSync lab is stopped and its
volumes/evidence retained. Product qualification uses separate disposable
Couchbase containers, fresh PostgreSQL libraries, the test S3 namespace, and a
temporary TLS tunnel. Both native sync flags stay disabled during product setup.
Final state: both lab stacks and the owned temporary tunnel are stopped; private
fixture databases, blobs, container volumes and evidence are retained. Existing
Alexandria/test services were left running.

## Original restoration

Palma: original `en`, logged out, notebook/Reader flags STRING false.
10.3 II: original `en`, original cached account, notebook/Reader flags STRING true.
The original marker/configuration are restored. Exact Reader catalogs and
notebook identities/status/ownership guards passed during teardown.

One Palma metadata row disappeared during native logout/cluster transition.
Its backing file remained; the exact escrowed row was restored through an
ordinary provider insert with original ID/UUID and full field verification.
No file bytes changed. The 10.3 II's restored Onyx account pulled five new notebook
rows; every previous row remained, and excluding only these five new owned rows
reproduced the old guard. These legitimate Onyx additions were retained.
The native cluster refresh re-added the lab entry once; exact original config
was reasserted afterward. These outcomes are recorded, not silently normalized.

Sealed evidence: `artifacts/gateway-lab/qualification/final-lab-teardown-20261008-182107/`
with SHA256SUMS. Proxy/facade and original CB/SG services are stopped; volumes kept.

## Product and server

The signed `:client` module is `dev.aragonite.powersync.client`. It installs beside
the old probe, preserving probe data. Persistent signing key lives outside the
repo at `~/.local/share/aragonite-powersync/signing/`. Private state uses Android
Keystore AES-GCM and atomic writes. Baselines include exact marker content,
configuration, sync flags, account identity reference, Reader catalog and notebook
guard. Original account passwords/tokens are not exported by the app.

The product exposes enrollment, connection, offline return, native status,
server recovery status and native settings. Endpoint mutations require API 35
and the qualified model/ksync version. A new server's sync flags default off.
Setup does not migrate a previous account's notes.

[Alexandria implementation/setup](../../AragoniteAlexandriaServer/docs/boox-native-setup.md)
covers migration 0015, per-device capabilities, Gateway transport, signed assets,
observed history, source browsing and bounded publication/recovery. Runtime
lifecycle, generation checks and private Gateway management remain inside the
public library boundary. No Hosting/Rhizome changes or deployment were made.

## Product experiments and discoveries

1. Signed app installation/read-only diagnostics passed on both devices: restored
   cluster, account state and exact STRING sync flags observed through ordinary
   app interfaces. No endpoint mutation in that first check.
2. Palma initial setup saved a baseline and switched its persisted cluster, but
   native login returned 2001 (unregistered). No native account request reached
   the fixture: the daemon's cached cluster list still used Onyx.
3. Server-info broadcast alone was insufficient. Decompiled main ksync service
   does not subscribe to that observer. Explicit transaction 81 refresh before
   transaction 80 switch supplies the daemon's in-memory custom entry.
4. The account facade must own `/api/v2/...` as well as `/api/1/...` and legacy
   `/api/...`; native registration/sign-in actually use v2. This was corrected.
5. Palma then completed native sign-in with both flags off. The fixture observed
   actual registration, sign-in, me, device and requested-channel routes. Offline
   return from both failed and successful setup restores original en/logged-out/
   false settings. Product-process interruption during return followed by repeat
   return also completed. A previous intermediate restore_requested UI sample is
   retained and is not claimed as a completed restore.
6. The 10.3 II connected and returned offline to its cached original account with
   both original flags true. Its first connected-state observation also caught
   a forced logout: simultaneous refreshes raced because the server rotated its
   bearer on every refresh. Refresh now preserves the bearer and renews expiry;
   re-enrollment rotates capabilities. A dedicated regression test covers this.
7. Native SDK components vary or omit device headers. After sign-in binding,
   authenticated requests use only their verified per-device capability to select
   the device; advisory headers cannot redirect it. Missing/oversized/changed
   hints have regression coverage. A six-sample sustained login window passed
   on the 10.3 II with both flags off.
8. Requested-channel objects include an `action`. The facade now implements
   add/remove/set within the owner's native namespace; foreign channels never
   grant access. Native channel updates return 200. A real concurrent update burst exposed a
   shared-to-exclusive device-row lock upgrade deadlock; acquisition now fences
   library generation first and takes the device exclusively. Eight concurrent
   update calls pass the regression test.
9. The storage-accounting SDK sends a bare token in `Authorization`, while other
   native services send `Bearer <token>`. Native account routes accept both exact
   validated capabilities; companion routes still require Bearer. Invalid tokens
   and alternate schemes remain rejected. Initial failing traces are retained.
10. Read-only runtime admission now also fences mutating native GETs (session,
    STS and token refresh), in addition to bidirectional BLIP. A public-runtime
    integration test proves these routes cannot bypass read-only mode while
    ordinary identity reads still reach credential validation.
11. Exact numeric decoding preserves opaque large integers. Selected notebook
    lifecycle recovery is tested independently: restoring an observed active
    status must remain active, never silently convert into deletion.
12. Correct notebook lifecycle is native status 0 removed / 1 active. Reader
   status has its own semantics; it is never translated through notebook enums.

Qualification artifacts: `artifacts/product-device-qualification-20261008/`.
Private logs/snapshots may include account/library content and are not committed.
Root JSON results and prior snapshots separate completed and failed outcomes.
The paired sustained-login, per-device Gateway-session isolation, final offline
return and full preservation results are recorded alongside the request traces.

## Validation and limits

Android release/debug builds, lint and pure URL tests pass. Server build/vet and
fresh required PostgreSQL/object-storage full race suite pass. Real Gateway tests
cover native session creation, journal/projection, record/commit/MESSAGE, duplicate
and lost-ACK replay, verified asset repair and revocation. A separate upgraded-
socket control proves active BLIP revocation closes the socket within polling.
Runtime close cancels streams before admission drain. The final full race suite
required PostgreSQL, object storage and real Gateway; build/vet and Compose
configuration validation also pass. Final test logs are retained with SHA256SUMS
(447 product-qualification files and 10 signed-build/installation files). Installed
APK hashes match the final signed artifact on both devices.

Final paired product acceptance: both qualified builds enrolled into one fresh
Alexandria library, each completed native sign-in, and both remained signed in
through paired status samples with custom notebook/Reader settings false. SQL
proof establishes two distinct installation/native identities and independently
issued Gateway sessions. The final signed binary includes firmware checks before
rollback and repeated catalog/notebook preservation checks after native sign-in.
Both returned with the custom runtime stopped. Original cluster maps, account
references, STRING flags, marker absence and full Reader catalogs match their
sealed teardown baselines. Notebook identity/status/ownership hashes match the
original guards with only the already documented user/Onyx additions excluded.
No baseline or fixture allowlist was rewritten to achieve these matches.

This is a working setup/transport/recovery foundation, not all planned capabilities.
General creation/drawing, rich-text editing, shape-ZIP recovery, complete binary
resource closure, rendering/OCR/search, whole-library import and companion editor
inspection remain gated. No native note transfer is claimed for these product
setup runs, which intentionally keep sync off. Previous lab transfer results remain
valid separate evidence. All new work is uncommitted in preserved dirty worktrees.

## Native pre-enrollment backup — 2026-10-09

The Go 10.3 II Lumi (`dfef8c1`) completed its built-in Notes Settings → Backup and Restore local backup, named `PowerSync-before-Alexandria-20261009.note`. The final archive remains under `/sdcard/note/backup/local/` and is copied to `/home/jtd/booxreverse/backups/go103-ii-20261009/`. Size: 730,741,556 bytes; 60,018 regular archive members, all CRC checks pass. Device SHA-256 before transfer verification and after native UI completion matches the host archive. `verification.json`, `SHA256SUMS`, and `completion-ui.xml` preserve evidence. The long final “Backing up” phase includes compression and deletion of the staging directory; completion was confirmed only after that directory disappeared and the archive appeared in the native backup list. Scope is the native notebook backup, not a whole-device/NeoReader backup; restoration has not been exercised.
