# Existing-library enrollment

2026-10-09: implemented, deployed and qualified for the bounded checks below.

A new user expects connecting PowerSync to populate Alexandria with their eligible
existing notebooks and reading data. The device should retain native record IDs
and ownership. Connecting must not silently create a different BOOX owner and
then require hand-migrating all existing records.

## Identity and authentication

PowerSync supplies the active native account UID when redeeming an owner-issued,
one-use enrollment code. Alexandria binds its native namespace on the first
successful enrollment, transactionally with code consumption. Later devices must
supply the same UID, or omit it when signed out. A mismatching UID is rejected
without consuming the code. Existing installations retain their previous generated
UID through migration 0016; changing an established library needs an explicit
migration, not automatic rebinding.

The UID is an ownership namespace, not a credential. Original Onyx tokens never
leave the device. Alexandria still issues separate login, companion, bearer,
Gateway-session and asset capabilities for every enrolled installation. Gateway
requires access to the owner channel, preserves document ownership across
revisions, and validates the document's scope. Runtime does not provision Gateway
databases. The management-installed rule permits the subsequently bound namespace.

## Returning to Onyx

The pulled ksync `LocalAccountProvider.logIn` deletes an old account by UID **or**
numeric accountId, independently of cluster. Preserving the UID therefore requires
preserving the original cached credentials, not merely saving endpoint settings.

PowerSync 0.1.2 captures the potentially replaced account row into its existing
AES-GCM/Android Keystore state before any endpoint mutation. It refuses ambiguous
rows or a conflicting numeric account ID. Account snapshots are not exported or
sent to Alexandria; application backup is disabled. On return, synchronization
stays paused while the original row is restored and read back. Unrelated rows and
row-ID collisions are protected. Cached master login is attempted only where its
selection is unambiguous and qualified. Otherwise native sign-in remains required,
with sync disabled. Returning does not depend on Alexandria being reachable.

## Verification boundaries

Fresh required PostgreSQL/S3 race tests pass. Real Sync Gateway tests pass with an
adopted original-style UID, including successful device publication and rejection
of a foreign owner. Concurrent first enrollments cannot bind two different owners.
Android signed release build, lint and existing unit tests pass; signing identity
matches 0.1.1. Device installation uses an in-place upgrade.

The native backup before this experiment is recorded in
[product results](product-implementation-results.md). Device acceptance first verified a paused connect/return round trip, then observed
native existing-library publication and audited the uploaded assets. Metadata counts alone are not a full
backup or proof that every page resource can be reconstructed.

Evidence: `artifacts/identity-enrollment-20261009/` (private access).

## Lumi paused round trip

The signed ordinary-UID client connected to the public instance with the original
native UID, then restored Onyx without a new sign-in. Exact cluster map, original
account identity/region, both flags, 1,529 NoteModel identity/status/ownership rows
and all 22 reader catalog rows match the pre-transition baseline. Notebook and
reader flags were false during the connected phase. MESSAGE records nevertheless
replicate independently, as observed in the earlier protocol work.

Reconnecting with both choices enabled starts native publication without opening
individual notes or changing notebook ownership. The initial, superseded comparison matched
5,878 server object ingestion hashes and lengths exactly to the independent native
backup; no mapped-byte mismatches. The backup also contains 26,486 historical stash
files, which must be distinguished from the live resources expected in native sync.
Final results are recorded below.

The first new cold-backup attempt copied data but failed when `compose images`
encountered a stale one-shot container referencing a removed build image. The
partial directory is retained. Backup image inventory now reads container image
references using `docker inspect`; a fresh complete backup and all SHA-256 checks
pass before enabling bulk sync. Original device backup remains untouched.

## Completed bulk-upload audit (2026-10-09)

The enabled product connection automatically populated the public Alexandria
instance. No per-note opening, ownership rewrite or host publication was used.
Lumi remains connected with notebook and Reader sync enabled. Palma was not
changed during this run. The original native backup remains intact.

| Check | Result |
| --- | --- |
| Current native resource bindings | 33,608; 1,056,234,304 logical bytes |
| Distinct stored bodies | 31,430; 1,049,851,098 bytes |
| Independent S3 read-back | All 31,430 pass SHA-256 and length checks |
| Original device metadata preservation | All 1,529 identity/type/status/owner rows match baseline |
| Original-owner notebook/folder metadata | 1,521 present; two deliberately sync-disabled test fixtures absent |
| Backup live tree | 1,087 IDs; 1,085 present; the same two disabled fixtures excluded |
| Eligible original-owner live notebooks | 1,081 of 1,081 present (1,083 including the two disabled fixtures) |
| Cached account return | Exact paused round trip, including 22 reader catalog rows, passes |

Bulk publication ran approximately 06:27–06:38 UTC, about eleven minutes including
the tail. This is an observation for this device/network, not a promised upload
rate. The independent object-store audit ran 06:41:19–06:41:31 UTC. Counts are
snapshot results; subsequent native MESSAGE/commit revisions can continue.

The independent pre-enrollment `.note` backup contains 60,018 regular files,
1,095,699,033 uncompressed bytes, in a 730,741,556-byte archive. Path comparison
accounts for every file:

- **33,478** map to current uploaded note-resource paths. Native point paths
  flatten one directory level; the comparison explicitly normalizes that layout.
- **26,486** are historical `stash` files, separate from current live resources.
- **48** belong to two original-owner test notes with `noteSyncStatus=0`. The
  native replication filter intentionally excludes these. Six other local lab
  metadata fixtures belong to other owners and are not this library's records.
- **One `note_tree`** is the native backup index. Its 1,087 IDs map as above to
  Couchbase notebook/folder records; this proves ID coverage, not full protobuf
  field equivalence.
- **Four root PNGs** are cached thumbnails for the four live folders. The backup
  calls `LibraryThumbnailData.exportToNoteFile`; `ThumbnailPaths` writes
  `<documentId>.png` in the backup root. Their IDs match server folder metadata.
  The cached PNG bytes themselves were not uploaded. This is not a missing page
  image or a custom cover asset; do not claim pixel-identical folder restoration.
- **One `tag/pb`** contains a tag represented in Couchbase. ID, document/page,
  owner, creation time, content and status match; server `updatedAt` is newer.
  The old backup protobuf is not a byte-identical uploaded object.

All mapped non-audio resources have exact backup length/hash matches. Six audio
resources initially differ: five MP3 files and one PCM file are wrapped by
`NoteDocDataReplicator.beforePushImpl` using Onyx's `ONY1` version-1 file format.
Independent server reads, header decoding, AES-CBC decryption and padding removal
reproduce **all six backup originals exactly**. For this format the AES key and IV
are stored in the header; decoding does not require the original Onyx account
credential. This wrapper is not user-secret encryption. The observed files have a
1,024-byte header, another 16-byte IV, then padded ciphertext. Native playback is
a separate acceptance test, not inferred from the offline decoder.

The server additionally has 126 non-notebook resources and four note protobuf
objects absent from this earlier backup. The backup is a notebook snapshot, not
an authoritative inventory of all reading assets or later native changes.

### Native storage widget

The BOOX Settings response must use the direct `CloudStorageStatusBean` shape:
`{"total":...,"data":{"note":{"name":"Notebook resources","data":...},
"reader":...,"left":{"name":"Available","data":...}}}` at
`/api/1/statistics/v2/user/storage`. A generic account envelope/flat statistics
object returns HTTP 200 but leaves Settings on **Loading...**. Source inspection
and an actual native UI refresh confirm the corrected shape displays
**1,007.3 MB / 1 TB**. Used bytes count current, nondeleted owner-scoped resource
bindings, not deduplicated physical bytes, retained history or filesystem usage.

**The advertised 1 TiB allowance is a fixed compatibility value.** It is not
measured free space and is not an enforced quota. The older `statistics/user/left`
compatibility route still reports zero used. Configurable quota policy and
consistent legacy accounting remain follow-up work.

### Evidence and remaining boundaries

Private evidence is under `artifacts/identity-enrollment-20261009/`:
`paused-roundtrip-proof.json`, `note-metadata-coverage.json`,
`backup-resource-comparison.json`, `backup-path-coverage.json`,
`backup-extras-proof.json`, `audio-server-decode-proof.json`,
`object-store-verification-final.json`, `qualified-active-private.json`, build and
required integration logs. Personal names, bodies, device IDs and audio remain
private; documentation publishes aggregate results only. Initial wrong-prefix
verifier and incomplete thumbnail-hypothesis outputs are retained as superseded
investigation evidence, not data-loss findings.

`AragoniteAlexandriaServer/cmd/boox-verify` is a read-only operator tool: build it
with `GOWORK=off go build ./cmd/boox-verify` and run with the deployed runtime's
normal database/S3 environment. It resolves the library-scoped blob prefix and
reads each currently referenced distinct body, emitting a JSON summary. Exit is
nonzero on failure; it does not mutate storage. `-fetch-hash` writes verified
binary bytes to stdout, so redirect to a private file. Missing, truncated,
overlong, changed and failed-close negative controls are covered by tests.
PowerSync's `scripts/audit-enrollment-resources.py` compares a native ZIP to a
private server manifest; it is complementary and does not itself read S3.

These checks establish automatic initial publication, metadata coverage,
independent durable-body integrity and backup correspondence for the observed
live resources. They do **not** establish a full new-device restore, every native
resource reference graph, audio playback, exhaustive page rendering, physical
server-backup restore, or all Reader content. Do not replace the independent
native backup with this sync result. Qualifying those operations remains separate.

## Cached-account collision recovery — 0.1.3 (2026-10-09)

The Palma's first user-driven production connection stopped before mutation:
`A cached account conflicts with the enrolled server; inspect before switching`.
Its signed-out old lab account used numeric account ID 2000000001 under a different
UID. This was the intended fail-closed behavior in 0.1.2, but not an adequate
normal setup workflow. The eight-character enrollment code redeemed successfully;
the error was unrelated to code length.

Version 0.1.3 captures the union of all cached rows native login will delete:
matching native UID **or** numeric account ID **2000000001**. Full rows, including cached
credentials, remain encrypted in Android Keystore-backed PrivateState only on the
device. No original credentials are uploaded. Unrelated accounts are not included
in the credential snapshot. Duplicate native UIDs across clusters can be preserved
as distinct original row IDs.

Immediately before native login, affected rows are compared to the snapshot.
Native logout changes (`loggedIn`, `logOutAt`, `logoutReason`, `updatedAt`, and an
initially null `createdAt` initialized by BaseData.beforeSave) are permitted;
identity or credential changes fail closed. Thus a new collision after the initial
snapshot does not silently escape preservation.

On return, synchronization remains paused and no account may be active, except an
already-restored original account whose other displaced rows have been verified.
Recovery preflights every affected original ID and UID before mutation; removes
only the enrolled UID in the app-owned cluster; restores every displaced row under
its original ID; and checks full readback, including unrelated rows. Updates and
deletes compare every observed column, including nullable fields. The qualified
native provider uses primary-key uniqueness and ABORT insert conflict behavior,
not replacement. Foreign occupied row IDs, changed credentials and new conflicting
accounts preserve the snapshot and stop restoration. Automatic cached master login
still requires the qualified Lumi ABI, one matching master, and no other row that
that login could delete. Other signed-in return cases require native sign-in.

This provider exposes separate mutations, not an atomic multi-row transaction.
Recovery is idempotent after each completed mutation; the sealed original snapshot
is retained across interruptions. Preflight/readback/conditional mutations detect
observed concurrent changes; this is not a claim of a global native-account lock.

Nine pure recovery-policy tests pass: UID/numeric-ID union, exact restoration with
unrelated rows preserved, interruption/retry after each mutation, foreign ID and
credential guards, native ID reuse, same-UID multiple clusters, logout side effects,
new pre-login collisions, and the no-original-cache case. Signed release build,
Android lint and all client unit tests pass. In-place Palma upgrade matches the
installed certificate and preserves hashes of all full native account rows and
notebook identity/type/status/owner rows. Credential hashing happened on-device;
no full account dump was copied to the host. Evidence:
`artifacts/account-cache-recovery-20261009/`.

The user's actual multi-account connect/return remains the next device acceptance
step; policy tests and installation are not represented as a completed live round
trip. Enrollment is preserved and no host-driven Connect was performed.

The Palma still selected the old disposable lab cluster at diagnosis, despite the
old product journal saying `restored`. This corrects the previous assumption that
it was on Onyx `en`. The next connection saves whichever cluster is selected;
choose the intended original Onyx region first if that should be the return target.


## Two production devices — 2026-10-09 07:24 UTC

After the user's Palma Connect, both devices report the production Alexandria
cluster, a signed-in native account, and notebook/Reader flags enabled. Server
records show separate active device principals, unexpired native bearers, and
active Gateway sessions (two Lumi, three Palma at this snapshot). The Palma has
published one resource version. Current live resources total 33,609 bindings and
1,056,236,972 logical bytes. The old synthetic qualification device remains revoked.

Native providers share 1,522 notebook/folder IDs (1,516 under the active original
owner), with zero type/status/owner differences among shared IDs. Lumi has 1,529
rows and 1,083 original-owner live notebooks; Palma has 1,526 rows and 1,077 such
notebooks. Seven original-owner Lumi notebooks are absent on Palma; all seven are
present, active and sync-enabled in server metadata. This is an unresolved local
metadata/application gap, not proof of seven missing server uploads. The Palma
also has prior lab data, so shared counts must not be described as newly downloaded
from this production connection. Whole-resource receipt and rendering remain
separate checks. No host-triggered sync, notebook open or native writes occurred
during this status inspection. Full multi-account Return remains untested.

Private snapshots and aggregate receipts: `artifacts/two-device-prod-20261009/`.


## User deletion observation — 2026-10-09 about 07:30–07:32 UTC

The user deleted four items on Palma. Two subsequent native-provider samples
show all four status transitions 1→0 and no other projected field changes; the
Lumi provider remains unchanged. Two server projection samples show one of the
four (an original-owner PowerSync ink fixture) arriving as status 0 at 07:30:16
UTC. The other three remain at their earlier active revisions. All four have
the same original owner and noteSyncStatus=1; neither account ownership nor the
per-note enable flag explains this split. This is native soft deletion, not a
physical row removal. No automatic full propagation claim is made.

This observation was passive: no host-triggered sync, notebook reopen, endpoint
change or replacement edit. Partial publication and receiver application remain
open investigations. Private titles/IDs and exact samples are retained under
`artifacts/two-device-prod-20261009/deletion-observation/`.
