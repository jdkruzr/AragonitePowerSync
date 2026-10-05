# PowerSync implementation state

Updated 2026-10-05. Native adapter lab builds and runs on Go 10.3 II Lumi,
Android API 35, Notes 45311 and ksync 32079, as an ordinary app UID.

## Device evidence

- Notes metadata queries and Notes/ksync Binder connections pass.
- Virtual-page callback passes through reconstructed Notes AIDL.
- Disposable text notebook creation, native opening, and rename pass.
  Rename preserves the notebook's unique ID; fixture requests are journaled
  before dispatch to avoid duplicate creation on retry.
- Native remote `.note` export passes through the Notes Binder. The fixture
  archive has 16 non-directory entries, 9,273 expanded bytes, including note,
  virtual-page, page-model, shape, point, resource, template and stash data.
  ZIP size/CRC and per-entry SHA-256 validation run inside the app UID.
- The exported resource contains the original fixture text. Opening showed an
  empty editor, and the archive also contains an empty editor resource; active
  page/content routing still needs investigation.
- The newer shape provider fails with `no such table: ShapeModel`. The old
  provider returns zero shapes. Neither qualifies arbitrary document capture.

Raw reports and the synthetic native archive live under ignored artifacts/lumi.
No personal note payloads belong in committed fixtures or documentation.

## Gates still open

Archive export is demonstrated, not complete snapshot qualification. Verify
all referenced assets, multiple pages/layers, images, handwriting, templates,
closed documents, and concurrent native edits. Entry hashing removes outer ZIP
timestamp differences, but native export may regenerate IDs, revisions and
nested ZIPs; the archive manifest hash is not a stable revision identity.
A semantic identity for single oil-pen shape records remained unchanged across
two native exports even though their archive manifests changed. Full notebook
identity still needs metadata, resources and multi-shape support.

Same-notebook write-back now has real fixture evidence: PowerSync wrote a text
resource, Notes displayed it and accepted a native edit, and both appeared in the
next export. A synthetic oil-pen stroke was imported and revised under the same
notebook and shape IDs. Re-export preserved all 101 point records byte-for-byte.
The same shape update also rendered with native noteSyncStatus=0. Shape deletion
and restoration revisions worked while the native document remained open. This qualifies
these fixture paths, not arbitrary full notebooks or physical handwriting.

The importer rejected a malformed point header before staging. An injected stop
after durable point staging left no published shape revision; retry completed,
and a duplicate retry succeeded. Private immutable receipts and per-receipt
journals preserve the received payload. Automatic recovery and sync receipt
integration remain unimplemented.

Full native snapshot write-back remains a critical gate. The native
KSYNC_NOTE_DATA_UPDATE_ACTION handler imports virtual-page PB metadata and
reloads an open document. Its existence does not establish that staging shared
files is sufficient, or that native dirty-state handling is safe. Do not invoke
broad RESTORE_NOTE: source inspection shows library-wide destructive behavior.

Native image replacement also passes: the next native export retained the same
image shape ID, its incoming updatedAt, the replacement PNG byte-for-byte, and
the existing oil-pen stroke. This qualifies the synthetic image fixture only.

The Palma 2 Pro is now connected. Its ordinary-UID probe passes Notes/ksync
binding, metadata queries, page callbacks and shared-storage access on API 35,
Notes 45960 and ksync 32173. The newer shape provider has the same missing-table
failure. Its library already contains the Lumi ink fixture ID; this existing
copy does not demonstrate independent PowerSync transfer. Its separate text
fixture passes creation, cloud-disable, same-ID rename and native export with
ZIP integrity validation. Use separate journaled fixtures for Palma writes.
Raw reports live under ignored artifacts/palma2pro.
The first Palma text-resource write reported success at the file/notification
level but remained blank in the native editor; its next export retained the
written resource only in history, while the live resource lacked the marker.
Native application ACK and dirty-editor handling remain unresolved on
this path. Do not generalize the Lumi text result to Palma compatibility.
The separate Palma ink fixture renders the synthetic imported native stroke.
An initial staging attempt stopped because cloud-disable was not observed;
after closing the fixture and confirming the flag, the same payload staged.
Native re-export preserves all 101 incoming point records byte-for-byte on Palma.
A second revision re-exports exactly one shape with the same ID, newer updatedAt,
and byte-exact revised points. This qualifies same-shape revision on both targets.

Palma static firmware capture is complete: 24 APKs and 222 readable firmware
files, device/local SHA256 verified. Its 95 ksync device profiles match the Go6
baseline; no new app catalog entries were found. Framework-only model labels
remain unbaselined. Full evidence is in
`~/booxreverse/palma2pro/2026-10-05_1810/REPORT.md`.

The Lumi currently has an existing Onyx account. Palma logout was confirmed
during the endpoint spike; native export of its existing ink fixture still
preserved point bytes. Fresh account-free library creation and full write-back
remain unqualified.

No server integration or Rhizome protocol change is implemented yet. The cloud
Rhizome port and a distinct PowerSync dataset remain necessary. Do not resume
unrelated historical deployment work. Stock Couchbase endpoint substitution
has live feasibility evidence on Palma ksync 32173: its custom cluster hook
connected to a local server with validated anonymous credentials, negotiated
CBMobile_3 and sent getCheckpoint/subChanges. Note-account replication and
revision exchange are not implemented. Original Palma cluster JSON was restored;
Palma remains logged out. Lumi cluster settings were unchanged. See
[ksync-retargeting-spike.md](ksync-retargeting-spike.md).

Stages: ordinary-UID probes; complete snapshot capture; same-notebook editable
write-back; isolated Rhizome/Server integration; durable lifecycle/conflicts;
two-device qualification and signed release.

Native API research: ../AragonitePowerSearch/docs/boox-independent-note-sync.md.

Detailed tested contracts and lifecycle limitations: [native-adapter.md](native-adapter.md).
