# Native book transfer qualification

Completed byte-transfer qualification, 2026-10-06. Only the authored SHA-256-verified EPUB, its enrolled
UUID aliases and exact book/thumbnail OSS keys are admitted. Original-library
and account/endpoint guards apply. Private evidence is in
`artifacts/gateway-lab/qualification/reader-assets/`.

Static native ISyncService exposes uploadReaderBook (transaction 12) and
 downloadReaderBook (13). Both consume a LibraryModelBean in a file descriptor.
SaveReaderBookRecordRequest creates a recordType-20 transfer record, and download
can copy a matching local checksum instead of fetching. The qualification must
exclude that shortcut and prove signed server GET plus exact received bytes.
Native storage uses user/reader/documentUUID/book/documentUUID.epub; thumbnail
uses the neighboring thumbnail/documentUUID.png key. Books use the OSS byte
path separately from native reading metadata/user-data replication.

A hash-verified app-private escrow preserves the synthetic EPUB before hiding
its receiver copy. Only synthetic paths may be moved; originals remain guarded.
Metadata/aliases and native receiver application are separate gates from transfer.

Native upload passed: Palma used signed OSS PUT to the exact reader EPUB key;
the Couchbase attachment-backed object matches the authored source SHA-256.
The facade admits only enrolled UUID EPUB and default thumbnail keys, with
bounded reader-directory listing. Unknown books, other file types and cross-book
paths are refused. Unit tests exercise these boundaries. Receiver copy census
checks exact synthetic candidate paths before hiding any file.

Native receiver download passed with the original Downloads EPUB absent. Lumi
made a signed OSS GET attributed to its own device principal; the 3,527-byte
received file has SHA-256
`c75e1ac3d91d1082b4597a3fb100eee03f2681d17976066595c030c8eecc5683`.
This firmware writes `.ksync/powersync-lab/reader/<UUID>/book/<UUID>.epub`,
not the Android app-files path initially predicted by the helper. The initial
path-based wait timed out despite a successful transfer; the actual native
path, HTTP evidence and exact byte comparison establish success independently.
The original copy was preserved in both app-private and host escrow before
removal. Direct native VIEW of the downloaded EPUB renders the authored text.

Identity remains a separate gate: direct VIEW creates a fresh native UUID and
starts at page 1/32, rather than applying the previously synced alias's reading
data. Both enrolled metadata rows still pointed at the absent Downloads path
immediately after transfer. Static `KReaderKSyncDataConverter` can redirect
cloud metadata to the managed UUID path when its matched local file is absent;
`DocumentMatcher` also recognizes an existing managed copy. Transfer-success
notification alone updates library view download state, not this metadata
mapping. A post-download native metadata refresh is being tested. The fresh
synthetic row was journaled and removed with an exact UUID-and-path predicate;
no pre-existing metadata row was deleted.

Post-download reconciliation did not qualify automatic identity attachment:
source native metadata publication plus MESSAGE left both original paths intact.
A fresh server metadata revision and MESSAGE, with launcher started, also left
them intact. These are negative observations for this existing-alias scenario,
not proof that a clean native cloud-library import cannot map the path. The next
identity experiment must distinguish initial cloud import with no local file,
native library selection/open, and stale pre-existing same-content aliases.
Do not call direct file VIEW an end-to-end reading-state restore.

The receiver's original synthetic Downloads EPUB has now been restored from
host escrow with exact SHA verification; the newly created managed metadata row
and downloaded managed file were removed after journaling. Original metadata
aliases were retained. Final account/endpoint/library restoration checks pass on both devices.
The EPUB byte-transfer gate is complete; thumbnail transfer and downloaded-book
reading-state attachment remain separately unqualified.

Closing checks: original reader identity/status/sync-flag guards pass (28 Palma,
14 Lumi), original notebook guards pass, exact original cluster/account/global
sync flags are restored, fixture alias sync is disabled, and test-server markers
and host overrides are absent. Gateway policy and original principal grants are
restored. The scoped reader-asset allowlist is removed. 27 Python tests and
Android debug build/lint pass; NativeInk reports 41 integrity checks. Private
helpers, receipts and screenshots are retained with a SHA-256 manifest.

Replay caveat: helpers are captured run journals, not unattended idempotent
commands. The initial download helper's app-files path prediction was wrong;
its corrected companion uses the observed `.ksync` target. A new run must take
a fresh candidate census and baseline; never reuse an overwritten post-transfer
census as the pre-removal journal. Shell `content delete` returned no count here;
an exact scoped follow-up query verified the novel synthetic alias absent.

Lab services are stopped, all five lab listener ports are closed, and persistent
Couchbase state is retained. The closing manifest covers 69 private evidence
files. Nothing was deployed to Alexandria production.

Follow-up: [fresh cloud import/open](native-reading-cloud-open.md) now qualifies
managed-path conversion and UUID retention on Lumi, with restored progress and
a rendered bookmark. That clean fresh-identity result does not resolve this
run's stale pre-existing aliases.
