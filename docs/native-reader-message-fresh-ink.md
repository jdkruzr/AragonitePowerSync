# Fresh reader ink: native publication, listing, and incremental application

2026-10-07. Private evidence: `artifacts/gateway-lab/qualification/reader-message-qualification/`.
Protocol traces: `reader_message_qualification` and `reader_message_fresh_recovery`.
The earlier 47-file doorbell checkpoint remains unchanged. This follows
[MESSAGE identity](native-reader-message-identity.md) and the six-shape Hi round trip.

## Qualified result

The user added one continuous stroke resembling S after Hi on the Palma and went
Back. Its native backup contains seven shapes and three pressure files: 6576,
10960, and 11152 bytes. A new 671-byte shape ZIP and 10960-byte point object were
accepted on their first upload attempts. The native mode-7 revision advances and
native reader MESSAGE revisions are published. The first notifications precede
completion of the new asset uploads; MESSAGE is not a durability barrier.

After the harness repairs and explicit recovery controls below, Lumi downloads
both new objects through native ksync. The downloaded shape ZIP matches its server
SHA-256 exactly. An ordinary-app request to native ReaderSyncService performs an
incremental pull: pullFull=false and isPullFullByMetadataExtraConfig=false. The
native importer logs the fresh revision and update sync shape count: 1. The
result is success/newData=true. Both native backups then have the same seven
shape IDs, document IDs, point revision IDs, bounds and save types, and all three
pressure files are byte-for-byte equal. Incremental replay returns success with
no new data. No full-user-data recovery flag or fabricated shape was used.

Both devices' pre-run full reader rows remain exact, and notebook preservation
guards pass. This qualifies incremental application of the added stroke under
the recorded controls, not an uninterrupted first-attempt end-to-end run.

## Harness faults and controls, in order

1. Fresh point/shape keys were already admitted for this enrolled PDF, and both
   first asset PUTs return 200. Source native publication then tries a type-4
   commit under a plain UUID. Our guard demanded a user-prefixed ID and rejects
   that commit. The policy now admits either matching native plain identity or
   the existing controlled prefixed identity, only for type 4/status 1, the
   enrolled parent, correct library/user, and required channel grant. Eight
   policy tests pass, including native identity and rejection controls.
2. Source library start alone does not resend the rejected revision. A native
   source checkpoint-reset restart stores the actual native commit and later
   native MESSAGE revisions. There were no host-authored commit updates before
   this native publication proof. The recovered marker retains earlier update
   timestamps; receipt of a changed marker is distinct from a fresh timestamp.
3. Lumi's retained short-lived session expires during the post-assets MESSAGE-only
   phase. Logs show 401 and no dispatch there. That phase is transport-confounded
   and cannot establish MESSAGE sufficiency or insufficiency. Lab account renewal
   also needs reader grants and global reader eligibility restored. Both new
   sessions are subsequently valid for an hour.
4. After renewal, correctly addressed MESSAGE dispatches, but a receiver checkpoint
   reset, normal PDF opening, a receiver daemon bootstrap, and fresh server
   commit/MESSAGE controls still do not fetch the new objects. The stored native
   metadata query confirms enabled mode-4 eligibility and the migrated cloud UUID.
   The manager-registration hypothesis was investigated but is not established.
5. The decisive fault is OSS listing: the new opt-in dynamic policy accepts direct
   PUT/GET keys, but the listing still filters against the old exact-key map.
   Newly uploaded objects are hidden from ksync's directory comparison. Listing
   and direct access now share reader_key_admitted. The signed-listing regression
   test admits fresh enrolled ink, preserves default exact-key behavior, and
   excludes unsupported paths. Eleven OSS tests pass.
6. After restarting only the lab shim, an explicitly controlled sequence refreshes
   the native commit revision and then the correct plain-ID MESSAGE, with assets
   already durable. Native listing grows from 2061 to 2674 bytes and Lumi fetches
   the 671-byte ZIP and 10960-byte point file. Earlier idle download observations
   are not evidence of a native asset-scheduling defect.
7. The PDF's autoSync option is false. Download leaves six applied shapes until
   the explicit incremental native application request imports the seventh.
   Background asset delivery and automatic editor application remain separate.

The native stored commit, source notifications, corrected listing, downstream
GETs, importer result, exact exports, and replay each have separate receipts.
Do not collapse the interventions into a fully automatic success claim.

## Alexandria implications and next case

Preserve native document identity conventions by model: MESSAGE uses plain UUID
identity; reader commits can also be native plain UUIDs. Do not impose the helper's
prefixed reader data identity on every record. A valid asset authorization policy
must expose the same eligible objects through listing and direct access. Reuse
Couchbase replication for revisions and notifications; the facade still owns
asset listing/storage, enrollment grants, session continuity, and recovery policy.

The first clean repeat must begin with valid sessions, enrolled metadata, plain
native commit admission, correct MESSAGE identity and complete dynamic listing.
Trace native asset PUTs, native commit revisions, native MESSAGE dispatch and
receiver GETs before issuing any receiver pull or server marker refresh. Automatic
reader application additionally requires the book's autoSync preference, or an
explicit user/application pull. Erasure and geometry remain separate gates.

Both devices remain staged reader-only, lab services and private log captures run,
and the Palma PDF is open for one more real stroke. Original accounts, endpoints,
flags, policies and allowlists are saved for later restoration; originals remain
preserved. There is no production deployment.
