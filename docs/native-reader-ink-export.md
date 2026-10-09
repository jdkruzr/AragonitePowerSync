# Native PDF ink: export, transfer and remaining application gate

Follow-up: [native shape ingestion now passes](native-reader-shape-ingestion.md).
The empty peer shape table below describes the earlier sealed checkpoint.

2026-10-07. Private evidence: `artifacts/gateway-lab/qualification/reader-ink-export/`.
Follow-up to [the prepared geometry fixture](native-reader-pdf-ink.md).
The sealed checkpoint contains 212 evidence files, independently SHA-256 verified.
Owned LAN and backend services are stopped; retained lab volumes preserve the run.

## What passed

One real Lumi stylus stroke inside the authored page-1 rectangle produces an
incremental PDF update: 2270 original bytes remain an exact prefix of the 22562-byte
annotated file. The blue stroke renders in both the host PDF renderer and Palma
NeoReader after native ksync upload/download through our Couchbase/OSS facade.
The PDF annotation is a Stamp named #ONYX-STROKE, with proprietary point data and
an appearance stream. Its PDF rectangle is [145.991821,401.57956,298.15671,419.58081],
inside the authored [100,400,400,450] rectangle.

An ordinary app UID binds ReaderExportService and calls exportDocuments, Binder
transaction 4. Its successful callback supplies a native .booxc ZIP with CRC-valid
entries, document configuration, one 609-byte shape protobuf and an 11152-byte
point file. Explicit document export also includes library container configuration
and thumbnails; it is not a promise of fixture-only archive metadata. Raw archives
and decoded protobufs stay private.

The source point file has 689 points. Its big-endian 16-byte entries are x/y floats,
encoded-size short, pressure short and timing integer. Pressure spans 1068–3264,
timing 0–2164 ms. All coordinates, size, pressure and timing match the embedded
PDF point object after the respective encodings are decoded. Native top-origin
y coordinates reflect into PDF lower-left coordinates using the 800-point page
height. Missing original-width/height protobuf fields are defaults, not evidence
of a transform. Rotated/cropped pages remain untested.

Native ksync stores mode-4 metadata and mode-7 configuration in Sync Gateway,
plus exact separately signed OSS objects: annotated PDF 22562 bytes, shape ZIP
678 bytes, point file 11152 bytes and thumbnail 4047 bytes. The shape ZIP contains
one 608-byte native shape payload. Source configuration has commitId/commitInfo
and the reader page/subpage mapping. No fabricated mode-6 CBL document is used.
Server authorization admits only this book and exact observed shape/point keys;
unknown keys, cross-book access and traversal stay denied.

## Controls that matter

The stock source metadata retains fileOriginSize=2270. Our ordinary-app transfer
request explicitly declares 22562 to test whole annotated-file transport. This
is not proof that default native PDF upload sends the incremental append: native
BookSyncFile can cap its upload at declared original size. Keep original book
bytes and reader ink dependencies as separate concepts.

The first peer download reused its matching local pristine PDF rather than the
server's annotated file. Repeating the request skipped the existing managed file.
Escrowing only that owned managed copy and clearing hashTag only in the app request
forced a real native GET; source, stored attachment and downloaded file then
matched SHA-256 c0e26f46d2f365c13c2cf64bc8efc4b5eb756ba8b899060fdf9222d4d8a566a7.
The original Downloads PDF on Palma stays pristine. hashTag is a native matching
hint, not our independently verified whole-file digest.

Early server polls looked for attachment `body`; the harness stores `content`.
Those timeouts were observation errors, not failed native uploads. Early library
replication also stopped with CBL Unauthorized. Renewing lab account sessions
reset reader grants to the shim's note-only defaults; reapplying exact reader
fixture grants and enabling the reader flag allowed mode-4/mode-7 publication.
Do not attribute those failed attempts to a proven metadata-delta filter bug.
The facade must preserve its reader authorization policy through session renewal.

## What did not pass

Initially the peer rendered embedded PDF ink but exported an empty shape table
and no point files. Its mode-7 local CBL receipt was separately confirmed after
session/grant recovery. ReaderSyncService success is not native application proof.
Passing the cloud ID where a local book ID was required caused a native null-date
exception. Passing the local ID returned success with no data and selected the
local identity. An explicit ordinary-provider identity assignment on the owned
managed PDF allowed native mode-7 application. This is an adapter enrollment
control, not qualification of automatic stock identity matching.

The final peer native export contains the source point file byte-for-byte,
SHA-256 4af8f706b0f18c872520a574dd0ffaa19b08343062c488a933f74bb130315634,
but its shape protobuf remains zero bytes. Native logs merge document configuration
and report an empty shape list. Editable peer ink, erasure/write-back, shape ZIP
ingestion, native transform application and bidirectional ink remain open gates.
A callback reports success with newData=false; it must not turn that gate green.

## Preservation and integration consequences

Restore original clusters/accounts and note/reader flags, source fixture flags
0/0, both asset allowlists, server sync policy and principal channel sets. Remove
only the newly owned managed-PDF metadata alias; retain the source pen edit and
private phase archives. Every preexisting reader row remains exactly equal to its
full baseline (28 Palma, 14 Lumi), and notebook guards pass (1517/1518). Palma has
one additional blank metadata row with both sync flags off, first observed after
the enrollment/application/export sequence. Its creator is not yet attributed;
it is retained. Therefore Palma's aggregate reader guard does not pass, despite
exact preservation of all original rows. Do not reset the baseline to conceal it.

Alexandria needs explicit device/local/cloud identity mapping, durable mode-7 and
asset dependency storage, session-renewal authorization, and separate receipt and
native application acknowledgments. Visible PDF rendering, point receipt and
editable shape application are three different outcomes. Complete source export
is available without the manifest-selected ink provider, whose query method
throws UnsupportedOperationException on both tested firmware builds.

The next bounded investigation is ReaderShapePBDocumentImporter: it reads
ShapeDocumentUtils.docShapeFileList(fromDocId), decodes the native page payload,
assigns the target document/page IDs and updates ReaderNoteDataProvider. Mode-7
merge invokes this independently from point-file merge. Check the downloaded ZIP,
its extraction directory and page payload before interpreting a successful reader
callback as successful shape ingestion. These static sources are included in the
private evidence; extraction failure versus import filtering is not yet resolved.
