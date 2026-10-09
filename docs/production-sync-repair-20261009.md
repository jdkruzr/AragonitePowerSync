# Production native sync: channel grants and credential lifecycle

2026-10-09. Server fixes deployed at 07:50 UTC. Live recovery is ongoing;
transport, metadata, assets and rendered-page checks are reported separately.

## Confirmed failures

Both tablets signed into the public personal server with native sync enabled.
Seven recent original-owner Lumi notebooks were missing from Palma's native
provider. Four user deletions on Palma became status 0 locally; only one reached
server projections and none reached Lumi in two subsequent observations.

Private stock-process logs show 2,676 distinct Palma NOTE_TREE document push
conflicts (CBL 10409), including the three unpublished deletions. Direct Gateway
checks with the actual Palma session reveal that the user has only the owner-wide
channel, not the NOTE_TREE channel ksync filters by. A filtered device changes feed
lacks notebook documents while the admin feed returns them. The apparent one-row
device feed includes protocol/user bookkeeping; a count alone is not document
access proof. `CouchHolder` calls `setChannels` with its database name by default.
It uses PUSH_AND_PULL; the failed-push-blocks-a-sequential-pull hypothesis is not
supported by that configuration.

Server `updateSyncChannels` saved validated subscriptions only in PostgreSQL.
Session creation reset `admin_channels` to the owner UID. Successful publication
and unfiltered reads did not test the native filtered-pull path. This is a facade
implementation bug, not a reason to replace Couchbase conflict handling.

Lumi's last lab-length Gateway cookie expired at 07:27:12 UTC. Proxy closed the
socket; native MESSAGE retried with that cookie three times and got HTTP 401.
Native account bearer also expired. Local signed-in state therefore did not imply
healthy replication. The prior [account lifecycle findings](native-account-lifecycle.md:35)
explicitly describe one-hour tokens as deliberately short lab values, not stock
lifetimes. The SDK uses six-month validity and a one-month refresh interval.
Carrying the one-hour account/Gateway defaults into production was an implementation
error. OSS STS expiry is a separate protocol with an explicit expiration field.

## Preservation before changing grants

Read-only copies of each original owner's shared native NOTE_TREE SQLite database
and WAL have identical before/after source hashes. Host SQLite integrity checks
pass and SQLite's backup API produced consistent snapshot files. Device database
files were not changed. Snapshots and logs are private under
`artifacts/prod-sync-diagnosis-20261009/`.

Palma's three rejected deletions have native revision generations 6/6/7; server
copies have 55/55/56. The successfully published deletion is generation 4, while
Lumi's cached copy is generation 54. These local alternatives are preserved before
normal conflict reconciliation. Do not equate a transport fix with preservation
of every losing native branch or confirmed application of the user's intent.

## Repair implementation

Gateway users receive the owner channel plus requested channels validated against
the bound native UID namespace. Subscribe/unsubscribe and session issuance share
the same generation/device lock order. Requests do not authorize foreign channels.
An upstream failure does not acknowledge a subscription change. Existing Gateway
passwords are preserved when updating channel grants or issuing more sessions.

Migration 0017 adds backend session identity/expiry to the durable grant registry.
Existing mappings are copied without extending or reviving expired frontend grants.
Migration 0018 forces lazy backend renewal for legacy mappings on their next
still-authorized connection, reconciling their channel grants at the same time.
New client cookies are opaque Alexandria handles, separate from backend cookies.
Backend sessions last 24 hours and are renewed near expiry on reconnect. If Gateway
loses a still-unexpired backend cookie, the proxy renews and retries once internally;
it never exposes that recoverable backend 401 as a permanent native auth failure.
Backend Set-Cookie headers are removed so they cannot replace the stable handle.
Gateway outages remain transient failures. Native routing and private management
boundaries remain unchanged.

Production account validity is 180 days, matching the pulled SDK's six-month scale;
responses report the persisted deadline rather than inventing an extension on every
API call. Refresh retains the existing bearer and supports expired-but-unrevoked
credentials. Device session handles have a 180-day inactivity limit: connections
renew it, and open connections maintain it with approximately hourly writes.
Revocation and library generation are still checked every second. Idle-expired,
revoked or generation-invalid handles cannot be renewed. A process restart does
not lose mappings. This is explicit Alexandria policy, not a claim about Onyx's
Gateway TTL. The one-use enrollment code still expires after ten minutes; asset
STS retains its explicit one-hour expiry. No insecure origin or wildcard grant is
introduced.

## Tests

Fresh required PostgreSQL/S3 and real Gateway tests exercise the native channel
filter, existing records before subscription, grant addition/removal, foreign
owner denial, second-session preservation, backend expiry, deleted-backend repair,
revocation, credential separation and Set-Cookie suppression. Concurrent renewal
issues one replacement; restart resumes the durable mapping; keepalive does not
revive an expired handle. Account tests verify persisted expiry, production lifetime,
expired-token refresh and revoked-token rejection. Existing active BLIP revocation
still closes the socket. Final full-suite/deployment results follow below.

Reference: [Sync Gateway 3.2 Admin API](https://docs.couchbase.com/sync-gateway/3.2/rest_api_admin.html)
for explicit session TTL and session creation/deletion, and
[Couchbase channel access](https://docs.couchbase.com/sync-gateway/current/access-control/channels.html)
for document membership versus user grants and replication channel filters.


## Production recovery observations

Full `go test -race -count=1 ./...` with fresh required PostgreSQL/S3 and real
Gateway fixtures passed, as did `go vet ./...`. Migration checksum validation
caught a local attempt to change 0017 after fixture application; original bytes
were restored and the additional legacy-renewal behavior moved to 0018. Final
migration/BOOX tests passed. Both migrations and the rebuilt app are deployed.

07:51 UTC: host-assisted GETs using existing valid Palma frontend cookies caused
lazy renewal. Direct backend checks now return notebook documents through the
NOTE_TREE channel filter (ten sample results, matching admin), with seven scoped
admin channels. No backend Set-Cookie escaped through the public proxy. This is
host-assisted repair, not evidence of autonomous tablet reconnection.

07:57–07:58 UTC: ordinary Lumi Notes Sync and a package-scoped
`token.invalid.action` broadcast were used; native account refresh succeeded.
Palma received the same scoped refresh request. Native ksync was force-stopped
and restarted on both devices to discard stale in-memory replicators after older
cached cookies continued producing 401. No native database, account row, endpoint
or sync-flag mutation was used. Both native clients obtained new stable sessions
and persisted account deadlines in April 2027. This is assisted recovery from the
old production bug, not proof that every exhausted native worker self-recovers.

Palma pulled 2,690 NOTE_TREE changes, then imported the seven previously missing
notebooks. Provider rows increased from 1,526 to 1,533. Thousands of initial push
conflicts are historical divergence; their existence alone does not prove final
convergence. Three user deletions resolved back to active records, consistent with
the higher-generation server branches. Preserved pre-repair snapshots retain the
local alternatives. Three bounded `set_note_status` operations were previewed
against server revisions; concurrent native writes caused all three to be safely
blocked as stale. Fresh previews then published and both providers showed status 0.
The fourth deletion was already status 0 on server/Palma but still active on Lumi;
a separate same-status publication was queued to produce a fresh descendant and
notification. No global conflict-winner policy was changed.

The user reported an empty `20261008 Directors Meeting` page. Its pre-enrollment
Lumi backup contains no ink shapes, but its thumbnail does contain ruled lines.
The missing template is therefore a real difference. Original template JSON
(982 bytes) and SVG were received from Lumi; Palma subsequently uploaded an
845-byte page template with empty resource names and no image reference at
08:00:25 UTC. Original asset versions remain retained. A bounded original-version
repair was previewed and queued while the notebook was closed; rendering and
repeat-open stability remain pending. Do not treat no ink as an explanation for
missing template lines.

`20261008 Jeff` provides an ink-bearing control: three source shape ZIPs were
initially absent locally, then downloaded and visibly rendered on Palma. The
03:01 local proxy window included 349 successful asset GETs. These establish
actual content traffic and one rendered notebook, not complete library restore.


### Asset publication time and disabled-fixture correction

Static `TemplateJsonSyncFile` compares cloud `LastModified` to the local template
file mtime. The first retained-version restoration selected the correct bytes but
exposed the old version creation timestamp, so a newer blank local file could win
again. Migration 0019 adds a separate `boox_blob_live.published_at`, preserving
retained-version creation provenance. PUT/recovery update the live publication;
list and GET/HEAD consistently report it. Fresh required PG/S3 and real Gateway
regressions restore a 2020 version, verify new list/GET timestamps and exact bytes,
and verify the original version date remains unchanged. Existing bindings backfill
from their versions without making the whole library appear newly modified.

The fourth deleted notebook is a deliberately sync-disabled Lumi fixture:
`noteSyncStatus=0` there, versus 1 on Palma. Its remaining local status 1 on Lumi is
expected filtering, not failed transport. Keep this exclusion; do not enable sync
to manufacture convergence. The three enabled notebooks are status 0 on both
devices. The fourth is status 0 on Palma/server and active but excluded on Lumi.
Earlier references to all four being sync-enabled applied to Palma, not Lumi.


## Final bounded acceptance (08:13 UTC onward)

Migration 0019 and its server code deployed at 08:13:03 UTC. Migration needed the
runtime to release its advisory lock: stop the app during this upgrade, migrate,
bootstrap runtime permissions, then start the new image. PostgreSQL/Couchbase/S3
volumes remain intact. The new publication-time repair was previewed against the
then-current metadata revision and asset binding, then executed through the
owner API. It published at 08:13:24 UTC while preserving the selected version's
06:38:08 creation time. Native Palma download produced the exact original
982-byte template hash; screenshot shows lines, and a subsequent close/reopen
screenshot is identical. No host copy into the device cache was used.

The Jeff ink control's three shape ZIPs independently match the original backup
SHA-256 values. Its page template also acquired the cleared-reference replacement
after opening during catch-up; the same bounded retained-version repair was queued.
A server audit comparing all current template hashes with their initial versions
found only that remaining replacement after Directors was repaired. This is not
proof of the precise native code path that produced an empty template; early-open
ordering is a hypothesis, not a settled diagnosis. Do not introduce a blanket
server rule that rejects intentional template removal.

All seven target notebooks exist on both providers, eleven pages total. Three
enabled deletions are preserved on both devices; the fourth remains deliberately
excluded on Lumi. Disabled fixtures stay disabled. Asset transfer, a rendered ink
page, and the repaired template are verified separately from metadata receipt.
Whole-library rendering, complete fresh-device restore, automatic recovery from
every exhausted native worker, clearer product sync health and conflict review UI
remain unqualified or unimplemented. The shipped fixes are source/migrations/tests
for future builds, not VM-only SQL patches; the recorded manual recovery actions
address already-diverged production state.


Jeff's repaired page template subsequently downloaded with the original backup
hash and visibly rendered ruled lines behind the unchanged ink. Final server
comparison of current template hashes to first-upload hashes returns zero
differences. This comparison is a bounded initial-enrollment recovery audit, not
a rule that future user template changes should be rejected. Screenshots, exact
shape checks, seven-notebook provider checks, deployment/test logs and recovery
recipes are sealed with SHA256SUMS in `artifacts/prod-sync-diagnosis-20261009/`.
Local synthetic Gateway/Couchbase fixtures were stopped after the tests; their
volumes and all production data services remain intact.

Release provenance: this repair is included in the PowerSync implementation and
Alexandria Server native-BOOX commits. Future releases must include the server
source changes and migrations 0017–0019; an unrelated branch or previously
published image does not acquire these fixes automatically. Private device
snapshots, deployment credentials and raw evidence remain outside Git.
