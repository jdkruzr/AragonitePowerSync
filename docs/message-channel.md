# MESSAGE channel: decoded control traffic

Investigated from the retained Gamma wire capture and the Go 10.3 II Lumi
ksync decompilation on 2026-10-05. No device rejoin or server policy change was
needed. MESSAGE writes remain denied by the existing notebook-only lab policy.

## What actually appeared on the wire

Six incoming revision attempts contained MESSAGE documents. All were type 6,
with `replicatorName=NOTE_TREE`, `msgStatus=0`, and changing `syncAt` timestamps.
They represented three distinct payload states, each attempted twice. These
are attempts, not six successfully stored messages or six user edits.

The document fields were `createdAt`, `msgType`, `replicatorName`, `dbId`,
`syncAt`, `clusterLang`, `msgStatus`, `user`, `uniqueId`, and `updatedAt`.
The user/cluster were the synthetic lab account/cluster. Full device-derived
channel identifiers and document identities remain in private raw evidence.
Sanitized metadata is in the ignored Gamma run's `message-metadata.json`.

## Producer and consumer

`StartPushNoteTreeReplicatorAction` posts a StartNoteReplicatorEvent for enabled
note documents, then invokes AddReplicatorSyncMessageAction for NOTE_TREE.
This is a trigger path, not an assurance that every notebook asset has already
finished uploading.

`AddReplicatorSyncMessageAction` queries existing ReplicatorMessageModel records
by replicator name, reuses an existing model if present, or creates one, and
sets `syncAt=System.currentTimeMillis()`. AddSyncMessageAction fills user,
clusterLang, dbId and uniqueId, initializes missing created/updated timestamps,
and saves the local Couchbase document. A changing syncAt, rather than changing
updatedAt, is therefore expected in the captured payloads.

`MessageReplicator` is continuous and enabled while the account is logged in.
Its push filter permits all defined types except cloud configuration (type 4).
It can use a separate messageCouchbase URL from the cluster configuration.

On a received type-6 document, HandlePullMessageAction converts it to a
ReplicatorMessageModel and invokes StartReplicatorByMsgRecordAction. That action
deduplicates the batch by replicator name and dispatches the corresponding
native sync action. NOTE_TREE starts StartNoteTreeReplicatorAction. Other
supported targets are calendar library, reader library, group tree, app data,
and configuration. The examined dispatcher does not use syncAt as an asset
commit barrier or compare it with a last-seen timestamp.

This is a replicated sync notification: a persistent doorbell record whose
revision changes wake other replication domains. It is not the notebook body.

## Other message types, established statically

| msgType | SDK classification |
| --- | --- |
| 0 | Feedback message |
| 1 | Feedback reply |
| 2 | Push message |
| 3 | Parental control / FamilyMessageModel |
| 4 | Cloud configuration; excluded from native push |
| 5 | Group note message |
| 6 | Replicator notification |

These classifications come from SyncMessageType, not observed traffic for
every feature. Other received types are broadcast through ONYX_IM_MESSAGE_ACTION
with IM message type/content extras. Deleted records cause a refresh-push
broadcast. This channel is broader than synchronization notifications; do not
enable every message type merely to make notebook wakeups work.

## Consequence for the independent server

Blocking MESSAGE may explain why earlier notebook tests needed explicit
receiver sync/restart. The producer/consumer chain supports that hypothesis,
but automatic delivery has not yet been tested with MESSAGE accepted.

The next controlled experiment should admit only synthetic-account type-6
NOTE_TREE notifications, grant the corresponding receive channel to both lab
principals, and repeat a native edit without manually syncing the receiver.
Keep the existing notebook/asset allowlist and preserve the original devices'
accounts/configuration. Verify accepted notification revision, receiver
delivery, resulting tree/document fetch, and native UI/export content. A wakeup
can precede file completion, so trace subsequent commit handling and retries.

Source pointers under
`/home/jtd/booxreverse/go103lumi/decompiled/com.onyx.android.ksync/sources/`:

- `com/onyx/android/sdk/sync/ksync/model/message/SyncMessageType.java`
- `com/onyx/android/sdk/sync/ksync/model/message/ReplicatorMessageModel.java`
- `com/onyx/android/ksync/message/MessageReplicator.java`
- `com/onyx/android/ksync/message/action/AddReplicatorSyncMessageAction.java`
- `com/onyx/android/ksync/message/action/AddSyncMessageAction.java`
- `com/onyx/android/ksync/message/action/HandlePullMessageAction.java`
- `com/onyx/android/ksync/replicator/action/StartReplicatorByMsgRecordAction.java`
- `com/onyx/android/ksync/note/action/StartPushNoteTreeReplicatorAction.java`

Static consumer semantics are qualified against the Lumi dump. The captured
Palma producer payload matches that schema; this does not establish identical
implementation of all message features across firmware versions.
