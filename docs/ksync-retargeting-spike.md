# Stock ksync custom-server spike

2026-10-05, Palma 2 Pro (`bbb70bef`), API 35, Notes 45960, ksync 32173.

## Result

Stock ksync can select a separately hosted endpoint through its existing custom
cluster hook. The unmodified daemon completed a credential-validated anonymous
WebSocket/BLIP connection to our loopback server, negotiated `CBMobile_3`, and
sent `getCheckpoint` and `subChanges`. This is anonymous messaging replication
setup, **not authenticated note-account sync or successful revision exchange**.
The probe returned BLIP errors for every replication request; it neither served
nor persisted document revisions.

This removes endpoint selection as a blocker. A Couchbase endpoint alone still
does not establish a working independent native sync service. PowerSync's
adapter architecture remains unchanged pending the account/note/asset phase.

## Device preservation and teardown

Before switching, captured the exact `ONYX_CLUSTER_SYSTEM_KEY` configuration
and an app-UID fingerprint of every NoteModel identity, type and status: 1,517
rows including 1,510 documents. After logout, switch-back, and restoration, the
fingerprint and counts were identical. Native export of the disposable ink
fixture while logged out preserved all 101 current point records byte-for-byte.
This checks metadata and a representative fixture; it is not an audit of every
personal asset or private database.

The actual Palma logout chain marks account rows logged out, disables native
reader/note sync, closes replicators and clears a Notes sync checkpoint. The
inspected handlers do not delete notebooks. Both native account rows reported
`loggedIn=0` after the experiment.

The original cluster configuration was restored with semantic JSON equality,
including removal of the extra lab entry. The custom marker was removed, both
ADB reverse mappings removed, and local probe servers stopped. Palma is back
on its original `en` cluster **but remains logged out**, with native sync disabled
by logout. Login was not silently recreated. Lumi cluster/account settings were
not changed.

## Native contracts

- `/sdcard/onyx_test_server` is a JSON list of ServerInfo objects. ksync merges
  custom regions only after a successful nonempty official index-server result.
  Hardcoded official index hosts therefore remain a bootstrap/refresh dependency
  in this demonstrated route. Cached custom config survived daemon restarts.
- Set `lang` and `serverId` to the same unique lab ID. `ServerInfo.setLang()`
  also changes serverId; using `lang=en` caused the custom definition to replace
  the saved US endpoints under `en`. Correcting the marker and refreshing restored
  stock US endpoints and added the separate `powersync_lab` entry. No local
  requests were observed during that initial collision.
- Custom host fields control account/API, data, log, content, send2boox, shop,
  material, note Couchbase and message Couchbase URLs. OSS endpoints and buckets
  must also be supplied to avoid falling back to stock defaults.
- ksync network security permits cleartext. The spike used ADB reverse and
  localhost HTTP/WS, not a TLS certificate modification.
- Verified ksync Binder ABI: descriptor
  `com.onyx.android.sdk.sync.ksync.service.ISyncService`; one-way transaction 80
  changes current cluster; 81 refreshes the list. Input is presence int, cluster
  ID string, status int, callback Binder. Callback descriptor
  `com.onyx.android.sdk.sync.ksync.service.SyncClusterCallback`, synchronous
  transaction 1, presence int, nullable cluster ID, status int (`2` success).
  Callback cluster ID was null; callback success alone does not prove the target.
- Our ordinary app UID invoked these exported Binder methods. Reading the system
  config through DeviceController reflection was blocked by hidden-API filtering.
  The host used the captured `android.onyx.services.IOnyxDeviceService` ABI:
  `onyx_device_service` transaction 3 reads the named key; transaction 4 restores
  only that key. Cluster observation/restoration here is ADB-assisted, not an
  established third-party app system-config capability.

## Observed service dependencies

| Interface | Evidence / status |
| --- | --- |
| `POST /api/1/devices/logout` | Arrived at local API with Authorization present; returned success without recording its value |
| `POST /api/1/users/checkSyncUser` | Anonymous setup; returning `data=true` allowed gateway construction |
| `GET /neocloud/_blipsync` | Authorization present, offered `BLIP_3+CBMobile_3,BLIP_3+CBMobile_2`; validated anonymous credentials and negotiated CBMobile_3 |
| BLIP `getCheckpoint`, `subChanges` | Decoded through upstream go-blip, refused with error 501; no revision exchange |
| `GET /api/v2/service/core/feature` | Requested during daemon startup; currently refused |
| `GET /api/certificates/findByNameWithKey` | Requested during daemon startup; currently refused |
| `users/signupByPhoneOrEmail`, `users/signin`, `users/me`, device binding | Note-account login plumbing identified in account sources; not emulated or exercised here |
| `GET users/syncToken` | Source constructs note gateway from cookieName/sessionId plus cluster Couchbase URL; not exercised here |
| OSS/STS and note/tree metadata APIs | Required investigation for full native note data and asset sync; not implemented |

Anonymous Basic authentication uses the device MAC without colons as username
and MD5(username) as password in the inspected request code. The isolated BLIP
probe checks that vendor algorithm; it is not our proposed production auth.
The Go listener accepts only that anonymous credential shape, not Onyx bearer
tokens or arbitrary user-account credentials. Events retain only acceptance,
protocol and whitelisted profile names. HTTP logs omit authorization values,
query values and bodies, retaining sizes/hashes and route metadata.

## Reproduction tools

`scripts/cluster-harness.py` generates the marker and serves a loopback API probe.
`--allow-anonymous-probe` allows anonymous gateway construction; `--blip-port`
selects the separate BLIP listener. Other routes fail closed.

`experiments/ksync-blip` is an isolated Go handshake probe using upstream
[Couchbase go-blip](https://github.com/couchbase/go-blip) at commit
`002c1b20b67a56d0be6bce44aea5a8e7bbd51654`; dependency versions/checksums are
recorded in go.mod/go.sum. Upstream licensing accompanies that dependency; it
is not included in the Android app. No Sync Gateway or Couchbase Server was
deployed in this spike.

`scripts/read-cluster-config.py` supports the captured Palma ABI and stages
bounded host observations in PowerSync's own private directory. Its restore
option requires native switch-back first, then verifies the saved JSON exactly.
Debug app actions: `cluster_baseline`, `cluster_update`, `cluster_switch`,
`cluster_restore`, `cluster_snapshot`. The baseline is durable and not overwritten
on retries. Fresh experiments need an explicitly reviewed fresh baseline;
do not reuse a baseline after unrelated library changes.

Raw reports, private baseline and disposable export are gitignored under
`artifacts/palma2pro/cluster`. Selected recovered source is retained with the
Palma dump under `analysis/classes/ksync-spike/`.

## Next gate

Emulate a separate disposable note-account login and session provisioning, then
connect the note replicator to a real compatible service. Verify one disposable
notebook/tree revision plus referenced assets in both directions, without
reassigning the personal library or accepting unrelated rows. Characterize
official-index bootstrap dependence and reconnect/offline behavior. Only then
compare total service scope with the independent adapter route.
