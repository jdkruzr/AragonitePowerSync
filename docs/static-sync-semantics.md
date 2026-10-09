# Static synchronization semantics audit

Audited 2026-10-05/06 against pulled ksync/Notes packages and pinned public
Couchbase Lite source. No device or server mutation was performed in this audit.
This separates executable branches found in source from behavior qualified by
the earlier live experiments. It is not an exhaustive firmware equivalence proof.

## Versions and source provenance

All eight archived ksync APKs contain the same embedded build literals:
`3.2.2-21`, `89a453d45e@4ac2f0bdc377`, and
`2025-02-24T21:01:56.058665049Z`. This was rechecked directly in their DEX files,
with APK SHA-256 recorded in ignored `artifacts/static-sync/cbl-build-inventory.json`.
Lumi's decompiled Couchbase BuildConfig identifies an Enterprise build.

Public sources were checked out under `~/booxreverse/upstream/`:

| Repository | Public release reference | Checked-out commit |
| --- | --- | --- |
| couchbase-lite-java-common | android/3.2.2-mr | 2292ecbefd9e67fd03fa1b8870dec65cf1b0eefa |
| couchbase-lite-core | 3.2.2 | bbd241d96dfbe4b244d65f5cb9a2c2ee25ce3020 |

The embedded build's abbreviated commits did not resolve in those public clones.
The public references match the release family, not the exact Enterprise binary.
Important Java behavior is cross-checked against the embedded decompiled classes;
LiteCore C++ conclusions remain public-release source evidence, not a claim of
byte-identical native implementation. Do not silently substitute current master.

Lumi ksync is the primary full decompilation used here. Four relevant classes
were additionally extracted from Palma ksync 32173 with JADX 1.5.6 into its
`analysis/classes/static-sync/`: NoteDocShapeReplicator, BaseReplicator,
DataReplicator and SaveDataFileRequest. The resolver, V2 missing-file branch and
download replacement behavior match at these inspected boundaries. This is
not a full cross-firmware diff. Notes consumer inspection below is Lumi-specific.
An ignored `artifacts/static-sync/source-index.json` records source hashes,
paths, anchors and upstream revisions.

## 1. Commit points: a scheduling gate, not a manifest

Producer chain:

`NoteDocReplicator.onSyncFinished` posts a completion event; NoteReplicatorManager
schedules close/finish processing; CloseFinishedDocReplicatorAction invokes
addCommitPoint; AddNoteDocCommitPointAction checks `hasPushedData`; then
NoteTreeReplicator.addNoteDocCommitPoint reuses an existing per-notebook
commitType=1 record or creates it, and updates its updatedAt timestamp.

The creation path sets documentUniqueId, user, dbId, commitType=1 and
commitStatus=1. It does not populate a list of required files, byte hashes or
an immutable notebook snapshot. The model also supports other fields/types;
resource commitType=3 has a separate consumer and must not be confused with
the whole-document commitType=1 marker.

Receiver chain:

SavePullNoteTreeRecordAction loads both note models and commit models from the
received changed-ID batch. NoteCouchUtils.loadNoteCommitModels restricts its
query to uniqueId values in that batch and commitType=1; it does not scan every
previously retained marker for a changed notebook.
SavePullNoteTreeRecordRequest sorts commit models by updatedAt and gathers their
document IDs as the set to start. Enabled notes outside that set receive
NOT_COMMIT_POINT_SYNC_ERROR. StartPullNoteReplicatorRequest schedules those
commit-associated notebooks.

This is stronger than “commit is merely cosmetic,” but weaker than “commit
guarantees every resource exists.” The receiver gate associates a received
commit-record change with its notebook, rather than validating an asset manifest.
An unchanged old marker alone does not automatically pass this particular
changed-batch gate. Whether previously scheduled work can overlap a later
metadata update remains a live question.

NoteDocSyncStatus.hasPushedData tests a pushed-state flag or a positive completed
push count, not all-task success. isFinished and isSyncSuccess are separate.
The examined close-action chain invokes addCommitPoint after close processing
without testing its passed success flag there. Partial success followed by a
terminal failure can therefore reach marker-generation logic in the inspected
flow; this prediction still needs an injected-failure runtime test.

## 2. Resource discovery combines durable work records and object listings

LoadNoteSyncDataListAction combines LoadV2DataRecordRequest with
CompareNoteDocSyncFileAction. The first loads pending LocalRecordModel work;
the second lists the notebook's object prefix and delegates comparison to
specialized file classes.

The comparison covers shape ZIPs, points, virtual document/page PB, page models,
resource PB/data/content, templates, TOC, links and thumbnails. This inventory
is discoverable statically; it does not qualify all those content types live.

Listing requests use maxKeys=1000. RxOSSManager accumulates pages, advances
nextMarker and repeats while isTruncated is true. “1000” is a page size, not
a hard notebook limit. The facade must preserve listing and pagination semantics.

ResourceDataSyncFile compares local and cloud filenames. Cloud-only files
generally become pulls and local-only files pushes; removed ResourceModel
entries instead generate cloud deletions. PCM has an additional file-size
comparison; special history HTML files are excluded from normal cloud-only pulls.
This is not a universal content-hash diff or a simple mirror directory.

ShapeSyncFile and ResourcePBSyncFile parse revision-bearing filenames into
shape/resource document arguments. Filename-set differences produce transfer
work; shape ZIP pulls lead through ImportPBtoCBAction, which imports shape
models and records page/revision associations, and can generate a second round
of point-resource pulls. A successful first batch of GETs therefore need not
complete the dependency graph. Server-side generation must preserve these
cross-file IDs and revisions, not merely create a notebook metadata row.

## 3. Missing V2 files can be acknowledged as done

In both examined firmware versions, DataReplicator's pull error handler:

- recognizes object-service 404;
- treats missing covers separately;
- for non-cover V2 data, invokes the normal task-success path and returns.

That path calls NoteDocDataReplicator.onDataSyncSuccess, which invokes
DoneV2RecordAction. DoneV2RecordRequest resets the non-cover LocalRecordModel
syncStatus to 0. The V2 work item can consequently be acknowledged without
a downloaded resource. Whether a later listing/PB import/commit regenerates
the necessary work is not established by this branch alone.

This changes the failure experiment design: a deliberately unavailable file
should be tested separately with 404 and a transient failure such as 503.
They are not equivalent ways to “delay an upload.” Covers have their own
retry/success rules and are a poor proxy for essential text or point resources.

Also, an empty local upload file can take the task-success path without a PUT.
Task completion must not be used as a blanket assertion that all bytes exist.

## 4. Downloads validate bytes, but replacement is not a notebook transaction

DataReplicator can skip downloading an existing local file when HEAD's
Content-MD5 matches its Base64 MD5. SaveDataFileRequest streams a GET into a
random temporary file in the destination directory, then validates Content-MD5.
A mismatch removes the temp file and throws before destination replacement.

After validation, it deletes the existing destination, attempts supported
decryption or renames the temp file, then applies the server Last-Modified time.
The inspected rename return value is not checked. No explicit fsync or atomic
multi-file publication is visible in this method. A crash between destination
deletion and successful replacement is a different failure from a corrupt GET.
The code does not establish retention of the previous complete notebook.

NoteDocDataReplicator may encrypt supported uploads to a cache path and removes
that cache after transfer; it falls back to the original file if the safe
encryption helper reports failure. We have not mapped all encrypted file types
or independently qualified encryption/decryption semantics.

## 5. Conflict policies differ by domain

NoteDocShapeReplicator installs BaseReplicator.handleConflictResolver:

- both deleted/missing sides: null;
- one surviving side: return it;
- both surviving: larger updatedAt wins; equal timestamps choose remote.

The public Java binding passes deleted revisions to a resolver as null sides.
Thus this resolver's code favors a surviving shape in a delete/edit conflict,
unlike the default resolver. This is a source-derived prediction, not a live
delete/edit result. It also makes clock skew and missing/zero updatedAt relevant.
It selects a whole document; no stroke-field or text merge appears here.

No custom resolver installation was found for the inspected NOTE_TREE or
MESSAGE implementations; CouchHolder passes their null resolver through.
The Java database falls back to ConflictResolver.DEFAULT. Its implementation
matches the embedded decompiled default: deletion wins, otherwise compare
document age, then revision-ID tie-break. Document.compareAge uses the native
document timestamp, not the application JSON updatedAt field. Details of the
native timestamp's derivation must not be replaced with “wall-clock edit time.”

This predicts different outcomes for metadata and shapes; it does not qualify
whole-notebook convergence. Shape component presence also does not prove every
V2 resource uses that conflict path. Shared immutable-looking object names and
mutable object overwrites need their own analysis.

Public source: [ConflictResolver](https://github.com/couchbase/couchbase-lite-java-common/blob/2292ecbefd9e67fd03fa1b8870dec65cf1b0eefa/common/main/java/com/couchbase/lite/ConflictResolver.java),
[AbstractDatabase](https://github.com/couchbase/couchbase-lite-java-common/blob/2292ecbefd9e67fd03fa1b8870dec65cf1b0eefa/common/main/java/com/couchbase/lite/AbstractDatabase.java).

## 6. Checkpoints track document replication, not OSS completeness

Public LiteCore Checkpointer derives checkpoint identity using database identity,
remote identity/URL, replication directions, channels, filters and document IDs.
It tracks pending/completed local sequences and an independent remote cursor,
and reconciles saved local/remote checkpoint state. Changing replication scope
can change the checkpoint identity.

Pusher distinguishes completed processing from successful synchronization.
Transient errors leave work pending; permanent rejection can advance the
checkpoint while reporting a document error. In the examined revision-reply
path, 403 is retried once. This is consistent with the paired rejected MESSAGE
attempts in our earlier capture, but is not proof of their exact binary cause.
Puller likewise treats transient and non-transient failures differently.

Nothing in these CBL checkpoints knows about Onyx's separately fetched OSS
resources or Notes editor application. They cannot establish a complete note.
Our facade's attachment revisions are implementation details, not native client
asset-replication checkpoints.

Sources: [Checkpointer](https://github.com/couchbase/couchbase-lite-core/blob/bbd241d96dfbe4b244d65f5cb9a2c2ee25ce3020/Replicator/Checkpointer.cc),
[Pusher revision handling](https://github.com/couchbase/couchbase-lite-core/blob/bbd241d96dfbe4b244d65f5cb9a2c2ee25ce3020/Replicator/Pusher%2BRevs.cc),
[Puller](https://github.com/couchbase/couchbase-lite-core/blob/bbd241d96dfbe4b244d65f5cb9a2c2ee25ce3020/Replicator/Puller.cc).

## 7. Retry exists at several independent layers

Public CBL 3.2.2 defaults allow 10 total connection attempts for one-shot
replication, very many for continuous replication, and exponential delay capped
at 300 seconds. CouchHolder does not set explicit max-attempt/wait overrides
in the inspected construction path. These are connection retries, not guarantees
that every rejected document or external file operation is retried indefinitely.

Onyx BaseReplicator adds its own bounded retry logic when enabled (three
additional starts with a roughly three-second spacing). NoteDocShapeReplicator
explicitly disables that wrapper retry; this does not turn off LiteCore's
independent connection retry logic.

OssTaskHolder ends a task after three counted errors, or immediately for network
error/storage exhaustion. Finished and successful are distinct. Missing
thumbnails have a special success rule after three errors. The durable record
queue provides another possible retry opportunity, but process-death recovery
and reconstruction still need runtime qualification.

Sources: [Java retry configuration](https://github.com/couchbase/couchbase-lite-java-common/blob/2292ecbefd9e67fd03fa1b8870dec65cf1b0eefa/common/main/java/com/couchbase/lite/AbstractReplicatorConfiguration.java),
[native connection retry](https://github.com/couchbase/couchbase-lite-core/blob/bbd241d96dfbe4b244d65f5cb9a2c2ee25ce3020/Replicator/c4RemoteReplicator.hh).

## 8. Unsupported POSTs were bulk deletion, not demonstrated multipart upload

The retained file journal contains 359 POST attempts with query key `delete`,
172-byte bodies and status 501 across the captured experiments. No captured
POST query establishes multipart upload. RxOSSManager uses asyncDeleteMultipleObject;
DataReplicator invokes it even for a singleton deletion. Its ordinary upload
path uses asyncPutObject.

ResourceDataSyncFile can schedule such deletions from removed local resources;
DeleteNoteDocDataAction also has bulk cloud cleanup. We have identified the
operation class, not decoded every requested deletion key or assigned all
attempts to one call site. Successful text delivery with refused cleanup does
not qualify deletion, storage accounting or eventual garbage collection.
Multipart support remains unqualified rather than an established requirement
of these captured POSTs.

## 9. Editor application is another stage

Lumi Notes dispatches KSYNC_NOTE_DATA_UPDATE_ACTION to HandlePullNoteDataAction.
It branches on whether the document is open. Open-document handling saves PB
state to the database, checks document identity/open state and posts ReloadPageEvent.
Closed-document handling imports virtual-page PB and updates page metadata/count.
Shape-update handling has its own disabled/open/backup/restore guards.

This locates the native application boundary, but does not establish safety for
unsaved editor content. The inspected request checks open/document identity;
no complete dirty-editor merge protocol was established here. Receiving a
notification is not an application ACK. Notes code still needs deeper tracing
around save/reload ordering, rich-text resource selection and active editing.

## What remains for runtime, after this audit

| Question | Static answer | Required experiment |
| --- | --- | --- |
| Is commit an immutable snapshot? | No, inspected document marker is mutable and has no manifest | Old marker plus new metadata/partial assets |
| Does a missing file always fail? | No, V2 404 can mark work done | Essential resource 404 versus 503, then make it available |
| Is file replacement crash-safe? | Temp/MD5 protects download; replacement has a deletion window | Kill between validation and replacement |
| Is conflict resolution uniform? | No, default and shape resolver differ | Edit/edit, equal timestamps, skew, delete/edit by domain |
| Does checkpoint mean notebook complete? | No, it concerns document processing | Compare checkpoint, local work queue, asset presence, editor ACK |
| Do unsupported POSTs imply multipart? | No, captured requests are bulk deletes | Qualify scoped bulk delete and retry, then large-file behavior |
| Can disconnected clients recover? | Retry/queues exist; composed recovery not proven | Offline edit, credentials/session expiry, process death/restart |

Highest-value next test is essential-resource 404 versus 503 while independently
controlling metadata, commit-record revision and MESSAGE delivery, followed by
restored availability and a fresh notification/commit. Retain the old marker
to reproduce ordinary updates, but do not mistake its presence for a received
commit change in the consumer's batch.
Inspect local pending records and native export, not only server logs/UI status.
Deletion and timestamp-controlled conflicts follow. Leave personal notebooks
and broad RESTORE_NOTE outside these disposable-fixture experiments.

## Local source navigation

Lumi ksync paths below are relative to
`~/booxreverse/go103lumi/decompiled/com.onyx.android.ksync/sources/`.

| Finding | Source / method |
| --- | --- |
| Commit construction | com/onyx/android/ksync/note/couch/NoteTreeReplicator.java: addNoteDocCommitPoint |
| Commit producer predicate | com/onyx/android/ksync/note/action/AddNoteDocCommitPointAction.java; sdk/sync/ksync/model/NoteDocSyncStatus.java: hasPushedData (under com/onyx/android/) |
| Changed-ID commit query | com/onyx/android/ksync/utils/NoteCouchUtils.java: loadNoteCommitModels |
| Commit scheduling / missing marker | com/onyx/android/ksync/record/request/SavePullNoteTreeRecordRequest.java: execute |
| V2 404 acknowledgement | com/onyx/android/ksync/replicator/DataReplicator.java: pull error handler z |
| Durable work completion | com/onyx/android/ksync/data/request/DoneV2RecordRequest.java: execute |
| Temporary-file validation/replacement | com/onyx/android/ksync/data/request/SaveDataFileRequest.java: a, execute |
| Shape conflict resolution | com/onyx/android/ksync/reader/couch/BaseReplicator.java: handleConflictResolver |
| Resolver installation | com/onyx/android/ksync/note/couch/NoteDocShapeReplicator.java: constructor |
| File inventory | com/onyx/android/ksync/note/action/CompareNoteDocSyncFileAction.java: o, create |
| Resource deletion work | com/onyx/android/ksync/note/file/ResourceDataSyncFile.java: b, makeSyncDataBean |
| Object pagination / bulk delete / PUT | com/onyx/android/sdk/data/manager/RxOSSManager.java |
| File retry terminal states | com/onyx/android/sdk/data/config/oss/OssTaskHolder.java: isFinished, isSuccess |

Lumi Notes paths are relative to its sibling `com.onyx.android.note/sources/`:
`com/onyx/android/note/note/action/sync/HandlePullNoteDataAction.java`,
`HandlePullOpeningNoteDataAction.java` in that same directory,
`com/onyx/android/note/note/request/ksync/HandlePullNoteDataRequest.java`, and
`com/onyx/android/note/note/utils/NotePBUtils.java`.
