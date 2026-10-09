# Native PDF partial-erasure qualification

2026-10-07. The user erased the right side of the fresh Palma squiggle and saved it. Evidence: `artifacts/gateway-lab/qualification/reader-native-erasure/`, including `result.json`, passive logs, native exports, server snapshots and a bounded Gateway trace. These private files may contain credentials or device data.

## Result

Native publication and asset download succeeded without a host-authored refresh or source retry. Automatic receiver application did not: its passive export still contained the original active stroke. A scoped one-shot full-data pull recovered the erased state. Both exports then contained exactly three matching records: two active fragments and the disabled original. IDs, revisions, bounds, status and save types match; both pressure files are byte-identical (7072 and 7952 bytes). Repeating the full pull reported no new data. Original notebook and reader library guards passed.

The original shape `a0106f36-dd15-4690-a5fb-6cb8d85a4116` remains with status 1. Fragments `b1bceedf-422d-455a-9610-e3bcc9713cfe` and `619fffab-14d0-463c-9b9d-d89c4f93babe` have status 0 (omitted protobuf defaults). Native `ReaderNoteShapeModel.isEnabled()` tests status == 0. This is a logical shape deletion within an asset, not an observed Couchbase document tombstone.

## Native traffic and assets

The device revised reader commit `dd3c2b39f3ee4b7bb53b68790f6c3440`, mode-7 configuration `e61b286c49594b1686b4bca987a8274b`, book metadata and MESSAGE `32350877a4b44073a583019b16e1e8f3`. That MESSAGE ID was reused from an earlier host-authored doorbell; this establishes native update/dispatch, not native creation of that ID.

The source uploaded the new pressure and shape ZIP with HTTP 200; the receiver downloaded both with HTTP 200. The ZIP is 829 bytes, pressure 7072 bytes. The previous pressure asset remains available; no asset DELETE or CBL document deletion was observed. Native publication therefore carries replacement fragments plus a disabled record, while retaining the pressure referenced by the removed original.

The source PDF became 27316 bytes (SHA-256 `b08e9ad5e105beb38e404fd180bfb5615dd5fd8ebeda39815ed8d11da4ebdb00`), while the receiver retained the original 2340-byte PDF. Matching ink does not imply matching embedded PDF bytes.

## Decoder and recovery limits

Wire ZIPs use `ReaderShapeDocProto`; native exports use `ReaderNoteShapeProto`. Their field numbers differ. `delta-wrong-schema-diagnostic-private.json` is retained as a failed diagnostic, superseded by `delta-shapes-private.json`; it is not shape evidence. The nested page/subpage DOCUMENTUNIQUEID is not the book UUID.

This case establishes downloaded-but-unapplied ink, but does not independently establish the exact cache race seen in the earlier fresh-ink case. No incremental retry was performed in this erasure case. Full recovery set only the fixture's `pullFullUserDataFromSync` flag. Native code cleared it and legitimately changed receiver `needMigratePosition` from false to true. An initial assertion that all extraInfo must remain identical was too strict; the import itself succeeded.

The helper now has a finally guard restoring only the original full-pull flag while preserving other native metadata changes. That guard was added after initial recovery and exercised during replay. Earlier fresh-ink prose incorrectly attributed flag restoration to a helper finally guard that did not yet exist; native code had performed the observed restoration.

## Server implications

Retain native configuration, commits, MESSAGE routing and referenced immutable assets. Do not translate status-1 shapes into immediate CBL document deletion or delete their pressure eagerly. Asset reachability and retention policy need explicit treatment; no garbage-collection lifetime was established here. Recovery must distinguish successful replication/download from successful native import, and support bounded per-book replay without resetting the library. Native-compatible conflict handling remains the chosen policy, with optional separate retention of observed alternatives.

Next: controlled sibling divergence of the fresh native mode-7 configuration, with read-only verification of local branches before reconnecting.
