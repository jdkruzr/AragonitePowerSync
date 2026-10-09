# Native reader MESSAGE identity and wakeup

Follow-up: [fresh stroke and listing repair](native-reader-message-fresh-ink.md)
qualifies seven-shape incremental closure and corrects native commit admission
and asset discovery. The initial six-shape observations below are historical.

2026-10-07. Private evidence: `artifacts/gateway-lab/qualification/reader-message-qualification/`
and `artifacts/gateway-lab/protocol-traces/reader_message_qualification/`.

## Controlled result

Both Palma (ksync 32173) and Lumi (32079) were joined to the disposable LAN lab.
Notebook sync is disabled, reader sync enabled, and only the authored PDF and
its enrolled assets are admitted. Original reader rows and notebook guards pass.
Both native backups still contain the same six shape identities and exact two
pressure files from the preceding Hi round trip.

A fresh type-6 READER_LIBRARY MESSAGE with `_id=user#UUID` and `uniqueId=UUID`
was stored and pulled by both MessageReplicators. During the 35-second observation,
neither device logged StartReplicatorByMsgRecordAction or reader-library startup.
A second fresh MESSAGE used the native identity convention: plain UUID for both
`_id` and `uniqueId`. Both devices logged that exact notification ID and started
the READER_LIBRARY replicator within approximately two seconds. No receiver
Binder start, pull or import action was issued during either observation.

Refreshing syncAt on that same plain-ID MESSAGE caused both devices to dispatch
it and start their reader-library replicators again. The 15-second observation
includes exact-ID native dispatch logs. The log suffix `replicator: false` is an
argument to the underlying start call, not a failure result.

These controls establish MESSAGE receipt versus native dispatch and reader
wakeup. They do not establish new ink application: the six shapes were already
present. A fresh real pen edit is pending. No commit was manually refreshed for
these controls; source native commit emission remains a separate gate.

## Static explanation

In the inspected Lumi dump, BaseReplicator collects raw replicated document IDs.
MessageReplicator passes those IDs directly to HandlePullMessageAction, which uses
LoadCouchDocListRequest. CouchHolder.loadDocuments queries the `uniqueId` property
against those IDs. Therefore a prefixed document ID paired with a plain uniqueId
can be received but not selected for handling. The plain-ID positive control
matches native MESSAGE documents already present in the server.

StartReplicatorByMsgRecordAction dispatches READER_LIBRARY to
KReaderSyncManager.sync(). With reader sync enabled this starts all enabled reader
documents, rather than the fixture-only scope of Binder transaction 17. Server
write admission is restricted to our authored fixture; do not describe MESSAGE as
a book-scoped trigger.

HandlePushLibraryDataAction queues reader document work, schedules replication,
and emits AddReplicatorSyncMessageAction(READER_LIBRARY). This is not an asset
completion barrier. AddReaderDocCommitAction separately gates type-4 commit
creation on pushed-data status. Trace asset durability, commit publication,
MESSAGE dispatch, fetch and native application separately.

## Harness corrections and next gate

The opt-in dynamic native asset policy admits newly generated point/shape names
only inside explicitly enrolled books. It preserves exact-key admission as the
default, excludes arbitrary resources, rejects traversal/cross-book keys, and
still requires the asset channel grant. Eight policy tests and ten OSS tests pass.
The receiver is eligible before observation; no one-shot full-pull flag was set.

Earlier server-authored reader MESSAGE controls using prefixed IDs must not be
cited as successful handler wakeups without dispatch evidence. Their separately
verified transfer/import results remain valid under their stated other controls.
This finding does not disprove the earlier late-asset incremental application gap.

The Palma is staged on the owned PDF; Lumi remains untouched at HOME. The lab
services remain running for a fresh user stroke. Original endpoints, accounts,
flags, server policies and full reader-row baselines are captured for restoration.
No production deployment is involved.
