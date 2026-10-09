# Fresh native ink: delivery succeeds, automatic application misses cache availability

2026-10-07. Evidence: `artifacts/gateway-lab/qualification/reader-fresh-lineage/`.
[Fresh baseline and static conflict audit](native-reader-fresh-lineage.md).

## Qualified result

A real user stroke on Palma publishes its native mode-7 revision, commit and MESSAGE,
and Lumi downloads its shape ZIP and pressure file without a host document/commit/
configuration/MESSAGE rewrite, source retry, or receiver start/reset. The earlier
configuration conflict does not recur for this fresh document. This narrows the old
failure but does not qualify native recovery of an intentionally divergent mode-7
history.

Automatic reader application fails in this trial despite per-book autoSync=true and
an active receiver reader. A later explicit incremental pull still imports nothing.
One-shot full-data application then imports one shape; both native backups agree on
shape identity, document ID, revision, bounds and save type, and the complete 7952-byte
pressure file. Full replay returns success with no new data. Original reader rows and
notebook guards pass. The full-data property is restored to its prior exact extraInfo
JSON. No server-side conflict-policy change was made.

## Passive publication and delivery

Palma mode-7 `powersync-lab#e61b286c49594b1686b4bca987a8274b` advances from native
revision 1 to 2, updatedAt 1791410631277. Native commit ID
`dd3c2b39f3ee4b7bb53b68790f6c3440` also advances to revision 2 with updatedAt=0.
The previously used plain-ID READER_LIBRARY MESSAGE is updated by native publication;
this proves native update/dispatch, not creation of a new independent MESSAGE ID.

Source OSS PUT receipts: shape ZIP 673 bytes and pressure 7952 bytes, both HTTP 200
at host epoch approximately 1791410632.594. Lumi's corresponding GETs both return 200
at approximately 1791410634.324 with the same SHA-256 as publication. Raw receipts
and server documents are private. Native cache ZIP bytes independently match the
server object SHA-256.

The user stroke causes Palma's native embed-after-sync behavior to replace the source
PDF with a 14954-byte, four-page derivative, SHA-256
`7930cf31cc219cd1171a5e030b4e4059663ad628bd24d55d89f150a24efb072d`.
Lumi's local PDF stays at the original 2340 bytes. Native ink equivalence is independent
of PDF-byte equivalence. The original backup helper guard rejected the derivative;
that failure is archived. Its exact digest was added to the source capture/sync guard,
Android build/lint pass, and a matching-signer in-place upgrade enabled read-only
source backup. This was after the passive logs/server inventory had been captured;
it was not a source publication retry.

## Receiver ordering

On the same receiver clock:

| Event | Time |
| --- | --- |
| Automatic pull begins, full-data config false | 17:03:55.151 |
| Importer reports `no pageFileList` | 17:03:55.191 |
| Native cached ZIP filesystem access timestamp | 17:03:55.925964 |
| Native cached ZIP filesystem change timestamp | 17:03:55.957964 |
| Document-data task completion log | 17:03:55.990 |
| Subsequent native pull check reports needPull=false | 17:03:56.042 |

The importer explicitly observes an empty file list before the cache timestamps and
task completion. A filesystem change timestamp alone does not prove first creation;
together these observations support a local cache-availability timing failure. HTTP
GET acknowledgement is not a native-cache-installation or ink-import acknowledgement.
The metadata cloud update is already treated as handled in the later pull check.

A subsequent explicit ordinary incremental pull reports success/newData=false;
its backup remains zero shapes. Thus waiting for downloads and replaying that ordinary
pull does not recover this consumed update in the observed case.

## Scoped native recovery

The ordinary-UID helper briefly enables `pullFullUserDataFromSync` only on the exact
fresh fixture row and invokes the normal native reader pull. Native code cleared
the flag in this run; the helper did not yet have a finally guard. A later guard
restores only that flag, preserving other native metadata changes (see
[native partial erasure](native-reader-partial-erasure.md)). Logs distinguish `pullFull=false` in the request from
`isPullFullByMetadataExtraConfig=true`: this is a full-data selection override, not
a full library replication reset. The importer then reports `update sync shape count: 1`.
The callback reports success/newData=true; exported shape and pressure equality prove
application. Repeating the same one-shot recovery reports newData=false.

Do not recast this as an automatic end-to-end pass. Native automatic publication and
asset download are qualified; reader application required a journaled device-side
recovery. No host refresh was issued after the user edit, and no mode-7 body/timestamp
was rewritten to obtain closure.

## Alexandria consequences and next tests

Maintain separate receipts for accepted metadata/configuration, durable assets,
receiver cache installation and native application. This supports a bounded device
recovery option when a normal pull has consumed an update without importing its
cached shapes. The current full-data override reloads the book's userdata; it is not
a narrowly targeted shape-import API, so production cost and conflict interactions
remain to be qualified. It does not justify an always-newer-updatedAt server resolver.
A server-only delayed doorbell is not yet shown to repair the consumed watermark.

Next: actual erasure semantics, then controlled divergence using fresh native
configuration lineage. Preserve the original and recovery snapshots before either.
Lab endpoints remain staged; original endpoint/account restoration is still pending.
