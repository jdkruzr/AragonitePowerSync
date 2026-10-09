# Native reading trigger and Palma import qualification

Active run, 2026-10-06. Private evidence:
`artifacts/gateway-lab/qualification/reader-cloud-palma/`.

Repeat the fresh cloud identity receiver experiment on Palma's newer firmware,
using the exact authored EPUB and a new cloud-only UUID. Keep the original two
synthetic aliases and original libraries intact. Escrow the receiver fixture,
exclude reachable checksum-copy candidates, then prove signed GET and exact
managed bytes. Native replication scope must include the new identity.

Before invoking native reader import explicitly, publish fresh metadata plus a
READER_LIBRARY MESSAGE and observe native metadata while the launcher is in the
background, then in the foreground. Record native Couchbase Lite metadata query
separately from provider application. A read-only transaction-5 query added to
the ordinary-app probe requires the enrolled UUID and exact managed EPUB, but
permits a cloud UUID with no native provider row yet. Explicit transaction-2
reader library import remains the control when automatic phases do not import.

Static launcher ReaderLibrarySyncEventHandler debounces SyncLibraryTreeEvent
and FilesChangedEvent for five seconds; file-change events invoke full library
reconciliation to match paths. Static event routing is a candidate until live
application evidence establishes the trigger. Receiver account/endpoint/global
flags and original reader/notebook guards must be restored afterward.

Palma initial import observation: after exact signed native download and scoped
ksync restart/start, the cloud UUID and managed path were already in the native
provider before the test MESSAGE was published. No `ReaderSyncService` import
transaction was invoked. Both subsequent presence-only phases observed that
existing row, so they cannot establish MESSAGE causality. A read-only native
transaction-5 query returned one matching cloud metadata model, independently
establishing CBL receipt. The new callback metadata count and scoped library start
probe distinguish receipt/bootstrap from explicit reader import.

The follow-up publishes a distinct synthetic metadata title in each phase,
book-scoped copies of one active native bookmark and one mode-7 user-data model,
and updated `user_doc_data_update_time`/lastAccess metadata plus MESSAGE. It
observes new provider content with the launcher backgrounded and foregrounded,
then opens the managed book normally. No reader Binder import/application call
is planned unless these native-trigger phases fail and a control is needed.

Distinct update observations: neither background nor HOME/launcher foreground
applied its new title marker or bookmark during approximately 33 seconds. The
server held the new title, but transaction 5 still returned the older metadata
revision (updatedAt approximately 243 seconds earlier); transaction 11 returned
zero book-scoped models. These are delivery-inconclusive results, not evidence
that the native reader ignores a received update. Device/host epochs matched
within one second; timezone display differences do not explain the gap.

Normal VIEW of the fresh managed EPUB retained its cloud UUID and showed page
17/64. It displayed the native “New document data detected, sync?” prompt. The
operator-equivalent UI path retained local layout, enabled Auto sync only on
the disposable book, and confirmed Sync. The dialog dismissed; the native
provider still held zero bookmarks and a subsequent read-only CBL query still
returned zero book user-data models. No explicit reader import/application
Binder call was used in these trigger phases.

Static lifecycle lead: KSyncBundle.startSync() starts MESSAGE alongside broader
sync components; updateSyncGateway() invokes startSync(). The scoped reader
transaction 17 used after force-stop does not itself establish MESSAGE startup.
This is a hypothesis requiring live subscription/receipt evidence, not a
confirmed cause. Next experiment must establish the MESSAGE and fresh-book
subscriptions, prove receipt of a new revision, then test native application
with the launcher backgrounded, foregrounded, and the book open. Avoid turning
on unrestricted note sync merely to bootstrap that lifecycle.

Correction from live lifecycle follow-up: transaction 17 scopes the library
replicator; it is not an explicit per-book data replicator start. The method
name/report now states READER_LIBRARY and bookDataReplicatorStartRequested=false.
See [lifecycle qualification](native-reading-lifecycle.md).
