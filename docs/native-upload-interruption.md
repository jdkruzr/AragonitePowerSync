# Native notebook upload interruption and retained-edit recovery

Matched continuation, 2026-10-07. Only the disposable UI text notebook was edited.
No personal notebook or reading-library rows were enrolled in this experiment.
Private evidence is in `artifacts/gateway-lab/qualification/upload-closure/`;
Gateway protocol journal is in `artifacts/gateway-lab/protocol-traces/upload_closure/`.

## Observations

1. With Lumi's Notes and ksync processes stopped, Palma's native editor appended
   a unique XI marker. The signed fixture/device-scoped OSS response hold ran
   after durable storage but before the upload acknowledgement.
2. The exact backend revision attachment was immediately escrowed: 3,616 bytes,
   SHA-256 `892dfd2be965616305b1c037d768f247b7eb2e6a2c4097345eb442b0c5744704`.
   It matched the source local resource byte for byte and contained the marker.
   Source ksync was force-stopped before the hold expired. The notebook commit
   record was unchanged at that observation.
3. After releasing the hold and requesting native scoped sync, the server commit
   did not change within 90 seconds. Native progress reported a successful pull;
   it did not establish completion of the interrupted push. LocalRecordModel
   returned no rows for this fixture, so absence in that queue is not evidence
   that all native pending work is absent.
4. A native source export still contained the exact held bytes and marker, and
   reopening the native editor displayed the marker. No loss of the edit was
   observed. Closing that reopened editor, without adding text, produced a later
   server commit. This was an explicit native resubmission, not unattended retry.
5. On reconnecting Lumi, signed GET returned HTTP 200 for an exact stored asset
   whose bytes were also present in the receiver's native export. The native
   editor rendered the unique marker. No host-created commit or MESSAGE clone
   was introduced to fabricate that application.

## Why the final resource hash differs

These `.html` files contain editor JSON. The held body included repeated identical
`textStyle` marks. Receiver exports contained 2,958- and 2,739-byte resources with
that marker; one matched a resubmitted server asset exactly. Compared with the
held JSON, document `content` is equal after deduplicating identical marks while
preserving first occurrence/order, and `contentStyle` is unchanged. Remaining
editor-state differences are `selectionMarks` and `storeMarks`. This is an
analysis of this fixture, not a proposal to normalize arbitrary notes on the
server or a general rendering-equivalence guarantee.

The original held asset later returned 404 both as current object and at the
held historical revision. Immediate escrow therefore matters: an obsolete asset
or its historical attachment cannot be presumed retrievable forever. This does
not establish the general Couchbase tombstone-purge policy or GC deadline.
The first run's later local-read artifact was an ADB `cat` error message, not
resource bytes; do not infer local resource retention from that older artifact.
This run validates bytes immediately by comparing with the backend hash.

The archive reference scan did not establish an active page-to-original-held-asset
chain after reserialization. Exact original-resource republishing is **not**
qualified. Evidence instead establishes retained-edit recovery with native
resubmission, exact transfer of a later stored asset, and native visible application.

## Facade implications and limits

- Asset durability and notebook commit publication remain separate stages.
- Accept an obsolete stored resource without treating it as a published revision;
  do not synthesize commits merely because PUT completed.
- Server-only history should escrow observed asset bodies promptly if recovery
  retention is desired; retaining only object IDs or revision IDs is insufficient.
- Scoped sync success is not proof of an interrupted push's recovery. A future
  adapter needs a separately qualified retry/resubmission mechanism or an explicit
  native action; the 90-second negative observation does not prove retry impossible.
- Native editor serialization can replace resource identity/bytes while retaining
  content. Keep original raw bytes and distinguish wire equality from application.

This is force-stop interruption, not power-loss/fsync qualification. Recovery was
observed for one native text fixture on Palma as source and Lumi as receiver.
Automatic recovery, reverse firmware roles, and arbitrary ink/image notebooks
remain outside this result. The existing installed probe and already validated
hold hook were reused; no APK/code change was needed in this continuation.

## Evidence capture limits

Verbose Gateway protocol capture stopped at its configured size safety bound
and restored logging before later recovery observations. Its journal is partial;
post-retry publication/application evidence comes from timestamped server
snapshots, exact asset escrow, the facade's operation/hash receipt journal,
native exports and screenshots. Do not describe the protocol trace as continuous
through the entire recovery. No production code or fresh APK was built for this
continuation; previous probe build/lint/test receipts are retained as reused
validation, rather than claimed as fresh checks.

Both devices' original cluster/account selections and sync flags were restored;
notebook preservation guards passed and original full reader metadata matched
pre-run captures exactly. Endpoint overrides and fault controls were removed,
lab services stopped, and persistent volumes retained. SHA-256 manifests cover
75 qualification evidence files and four protocol trace files.
