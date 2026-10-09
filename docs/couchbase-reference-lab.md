# Native ksync with independently hosted Couchbase

2026-10-05: stock Palma 2 Pro ksync 32173 and Go 10.3 II Lumi ksync 32079
both authenticated to a disposable local account and replicated notebook metadata
through real Sync Gateway 3.2.2 Community and Couchbase Server 7.6.2 Community.
No custom BLIP implementation participates in this path.

## Follow-up: native UI text-note round trip

A second disposable notebook was created through the Palma's native Notes
creation screen, and `PowerSync native UI marker ALPHA 2026` was typed with the
native editor. The 10.3 received the same notebook ID and displayed that text in
its native editor. Adding `BETA edited on Lumi` there subsequently appeared in
the Palma's native editor. Final native exports on both devices contain both
markers, and their extracted text content is equal. The complete resource files
have different hashes because editor state/metadata differs; no full-file equality
is claimed.

Evidence: ignored `artifacts/gateway-lab/ui-note-roundtrip.json`, both devices'
`gateway/ui-note/final-export.{json,note}`, `received-alpha.png` on Lumi and
`received-beta.png` on Palma. Notebook ID: `f7f635344f624650aa300f84d114a1d3`.

This qualifies a real UI-created editable text notebook with native rendering in
both directions through our Gateway and file adapter. It does not qualify
arbitrary notebooks, handwriting, images, multi-page structure, conflicts or
unattended scheduling. The run used a bounded fixture sync-status toggle, tree
restarts, a daemon reconnect, scoped sync/pull requests and close/reopen. The
specific necessity of each intervention has not been isolated.

The earlier Binder-created resource uses `docVersion=1`; the native UI-created
editor uses `docVersion=2`. Native resource/commit linkage and editor lifecycle
also differ. This is a candidate explanation for the first fixture's failure,
not a proven single-field cause. Do not patch the version field as a purported fix.

`trace-native-processes.py` captures Notes and ksync logcat by PID on both devices
for up to 15 minutes, privately with mode 600 (logs may contain credentials).
Restart capture after daemon PID changes. `trace-gateway-fixture.py PHASE
--fixture ID` records read-only fixture file hashes and fixture-specific log lines.
No root, Frida attachment or vendor APK replacement was used. Both devices report
`ro.debuggable=0` and current ADB UID 2000; injection would need separate assessment.

The original account/cluster settings were restored again after this run. Other
identity/status/ownership fingerprints still match (1,517 Palma, 1,516 Lumi rows),
excluding the two explicitly allowlisted disposable notebooks. Both lab notebooks
remain on-device; no personal library cleanup or deletion was attempted.

## Demonstrated behavior (first fixture)

- A separately journaled Palma notebook, owned by `powersync-lab`, pushed its
  metadata into Gateway. Server REST changed its title; Palma received the change.
- Lumi received the same notebook ID, renamed it natively, and Palma received that
  revision. Each device has an independent Gateway user and session, with shared
  account channels. HTTP bearers are bound to hashed hardware identity.
- Native pulls were stimulated with daemon reconnects. Continuous scheduling,
  offline recovery, conflict resolution and deletion are not qualified.
- Eight archived ksync APKs have identical Couchbase Lite build metadata:
  `3.2.2-21`, commit `89a453d45e@4ac2f0bdc377`, build time
  `2025-02-24T21:01:56.058665049Z`. Four existing decompilations report Enterprise
  builds. This establishes matching embedded metadata, not identical entire APKs.

## File storage and the first fixture limitation

Stock Onyx stores notebook files through Alibaba OSS calls, separately from
Couchbase document replication. `oss-couchbase-lab.py` adapts a bounded subset of
that API to Gateway attachments. Signed object PUT/GET/HEAD/DELETE and paginated
notebook-prefix listing are implemented. STS credentials are generated separately
for each lab device. The recovered SDK uses OSS V1 HMAC-SHA1 signatures.

A synthetic attachment passed upload, HEAD size, byte-exact GET and delete against
real Gateway. Native Palma then uploaded six fixture objects (1,995 total bytes):
page-model PB, virtual-document PB, shape ZIP, point data, resource PB and text
resource. Stored attachment SHA-256 values match the upload hashes. Native Lumi
fetched these objects successfully after adding the required base64 Content-MD5
response header.

**The first fixture demonstrated transport but failed native application.** The receiving
native export omitted the source text resource, including after a scoped
`pullFullNoteData` request. Its shape/resource export also differed from the
source. Do not describe this as a complete editable notebook transfer. Investigate
native revision selection, document records, and Notes import/open notifications
before qualifying the Binder-created fixture path. The follow-up above independently qualifies a native UI text-note path. Export itself can regenerate records; do not
use archive identity as a stable revision identifier.

The facade rejects account-wide backup/listing, other notebooks, multipart,
delimited listing and objects over 1 MiB. It is an experiment, not production OSS.
Recreating deleted object keys and concurrent PUT conflicts need explicit support.
Internal asset documents use separate channels not granted to native replicators.

## Components and operation

- `scripts/gateway-lab.py start|stop`: isolated Podman reference stack, named volume
  retained on stop; loopback ports 18767 public, 18768 admin, 18769 Server console.
- `scripts/onyx-account-shim.py`: loopback 18765, fixed disposable email/code,
  registration/login/profile/token/quota and STS endpoints. Unknown authenticated
  routes return 501; 401 triggers native refresh/logout and is inappropriate for
  an unimplemented service.
- `experiments/ksync-blip/cmd/gateway-proxy`: loopback 18766, forwards BLIP unchanged
  to Gateway after validating the issued device session. No protocol translation.
- `scripts/gateway-fixtures.py REPORT...`: requires a journaled lab-owned fixture
  and preservation report before configuring the server write allowlist.
- Debug Android actions qualify native account login, bounded notebook operations,
  scoped sync/pull, native export and preservation. Every host observation must be
  refreshed after changing cluster configuration.

Private credentials/session registries remain under ignored `artifacts/gateway-lab`
with restrictive permissions. Request logs contain route/status/identity hashes
and body hashes, never bearer/session/signature values. Raw synthetic fixture
reports and exports are ignored artifacts too. No personal notebook was allowlisted.
This plain-HTTP/WS, loopback/ADB-reverse setup is not a deployment configuration.

## Preservation and cleanup

Before restoration, all other identity/status/ownership fingerprints matched:
1,517 Palma rows and 1,516 Lumi rows, excluding the single shared lab notebook.
Palma's ownership baseline starts after disposable login; Lumi's starts before
its account switch. These checks do not establish byte-level preservation of every
personal notebook asset.

Both original cluster maps were restored exactly (`en`/`eu`, active `en`). Lumi's
original cached master account was restored natively without exporting credentials;
its callback and original-account identity check passed. Its original note and
reader sync flags were restored to true. Palma returns to its previous logged-out
state with native note sync disabled. Lab markers/reverse mappings were removed;
lab processes and containers stopped, retaining host evidence and server volume.
The disposable notebook and cached lab-account rows remain; no library wipe or
uninstall was performed.

## Architecture decision

This evidence makes native retargeting a real candidate, while the approved
account-independent PowerSync adapter remains a separate fallback. A hosted native
route would use Couchbase Server + Gateway, a thin Onyx account/session service,
and either a qualified OSS-compatible store or a narrow file adapter. No need to
implement Couchbase replication ourselves. A private API over Gateway can serve
our own clients, but PowerSync/Rhizome receipt semantics are not implemented by
this experiment.

Next gates: native content application, automatic scheduling, concurrent/offline
revisions and deletions; then TLS/device enrollment, account isolation, larger
assets/retention, recoverable publication and deployment/license review. Community
image availability alone is not a conclusion about production redistribution rights.

Reference documentation: [Gateway compatibility](https://docs.couchbase.com/sync-gateway/3.2/product-notes/compatibility.html),
[database configuration](https://docs.couchbase.com/sync-gateway/3.2/configuration-schema-database.html),
[Admin REST and attachments](https://docs.couchbase.com/sync-gateway/current/rest-api/rest_api_admin.html),
[OSS V1 signatures](https://help.aliyun.com/en/oss/developer-reference/include-signatures-in-the-authorization-header).
