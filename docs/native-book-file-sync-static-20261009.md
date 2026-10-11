# Native book-file sync — static evidence, 2026-10-09

Yes: ksync implements actual source-book upload/download, separate from Reader metadata and annotation replication. This inspection made no device or server changes and does not qualify end-to-end operation against Alexandria.

## Provenance

Fresh targeted JADX decompilation of `com.onyx.android.ksync.reader.file.BookSyncFile` from Go6_2 rel_0924 ksync 32198 and Palma 2 Pro rel_0810 ksync 32173. Outputs are byte-identical (SHA256 `bcbfec501c5666a81e518609cba6bfb7714580c9ac549eafab433414e4949c88`), saved in `/home/jtd/booxreverse/analysis-book-sync-20261009/`. These are the newer Go6 branch and the latest retained Palma capture, respectively; do not rank all firmware branches solely by versionCode.

Additional current Go6 launcher/library source: `/home/jtd/booxreverse/go6gen2/decompiled_0924/kcb_src/sources/`. Supporting daemon call-chain/PDF details inspected in `/home/jtd/booxreverse/tucp/decompiled_32174/sources/`; those auxiliary classes were not freshly compared across all builds. This is ksync plus launcher/shared Reader-library evidence, not a claim to have freshly decompiled the entire latest NeoReader APK.

## Mechanism and gates

- `com/onyx/common/library/action/sync/UploadDocumentAction.java:174` invokes `KSyncReaderHelper.uploadReaderBook` for a selected native Metadata record; `UploadDocumentListAction.java:424` supplies the batch path. `DownloadDocumentAction.java:34` calls `downloadReaderBook`.
- `KSyncReaderHelper` sends `SyncIntentArgs` over `ISyncService` with separate upload/download operations.
- `BookSyncFile` creates type-20 book file transfer beans with a real local file path and OSS key. Record status 2 queues an upload if the source exists and the cloud filename is absent; status 1 queues download to the native unique book path. Status 4 denotes pending download and does not itself enqueue a transfer. A newly discovered cloud book creates that pending state and prompts a library pull.
- Object key construction in `KReaderOssUtils.getBookOssFileKey`: `<user>/reader/<documentId>/book/<uniqueBookFilename>`. `KSyncConstant.KREADER_OSS_DIR` is `reader`. Book bytes use the object-storage route; they are not just another Couchbase book-metadata body.
- The same cloud filename being present causes this upload branch to skip replacement. This is not evidence of a general arbitrary-file conflict/overwrite protocol.
- Current Go6 UI explicitly offers `Upload source file`, including Library > Batch Management. Its no-cloud-file explanation distinguishes a preview-only book snapshot from its source file.
- `Metadata.FetchSource.isSupportSyncFileInKSync` accepts LOCAL source (0), excludes bookstore and other ksync resource paths except the recognized local book path. UI rejects unsupported sources and missing local files. Thus third-party cloud/storage/shop books must not be assumed eligible just because their metadata syncs.
- `UploadDocumentAction` has a 209715200-byte (200 MiB) default file limit. `onyxLibraryFileSize` permission configuration overrides it using `allTimes * 1048576`. Batch code also checks available cloud storage.
- Supporting TUCP `PdfUploadCacheHandler` can upload a temporary copy truncated to `fileOriginSize` when a PDF has grown. This supports keeping original PDF bytes distinct from later incremental changes; PDF/annotation round-trip semantics still require explicit testing.

## Alexandria implication

The stock clients already contain source-book transport, including user-driven upload and deferred download. Alexandria can potentially serve this through its Onyx-compatible object-storage and Reader metadata facade. Validate a tiny disposable EPUB/PDF: native upload, server object identity/bytes, peer pending state, native download/open, then annotation association. Verify object listing/authorization, permission/quota responses, notifications and PDF base-byte behavior. Do not infer automatic whole-library file mirroring or production support merely from this static path.
