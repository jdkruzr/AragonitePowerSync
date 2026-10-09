# Full-library replication and recovery runthrough

Requested by the user on 2026-10-06. Preservation capture and reconstruction
completed; both devices are restored and the lab is stopped. This run does not
qualify full native library replication or complete backup.

| Population | Palma | Lumi |
| --- | ---: | ---: |
| Original inventoried notebooks | 1,512 | 1,511 |
| Live notebooks captured as native archives | 1,079 | 1,078 |
| Removed notebooks inventoried | 433 | 433 |
| Shared-storage files captured | 63,261 | 67,032 |
| Server-reconstructed native archives | 1,079 | 1,078 |

The two live populations share 1,078 IDs; preserve their per-device versions.
Native export capture had one transient empty-JSON failure on Lumi, recovered by
retry with the original failure receipt retained. Shared-file captures deduplicated
to 88,371 byte objects, uploaded and independently reconstructed from Couchbase
attachments. These packs use the separate private `powersync_library` dataset;
they are not native replication documents in `neocloud`.

All native archives passed outer integrity checks. Auditing server-reconstructed
archives found 467,241 Palma and 467,306 Lumi live point references, with no missing
point files, absent note-info records or nested shape ZIP CRC failures. This is a
point/image-reference audit, not complete page/template/audio/PDF closure.
Sixteen missing image assets across five archives matched independently preserved
shared-storage objects. Derived archives and insertion receipts are retained
separately, uploaded and read-verified on the server. One empty image-reference
object remains unresolved. An imported repaired copy contains eleven assets in
its local resource directory, but native re-export still omits them after page
opening; that repair has not qualified self-contained native recovery.

An isolated three-page original-notebook copy reconstructed from server packs
and imported under a fresh lab-owned ID retains 549 point spans and two PNGs
byte-for-byte in native re-export. Synthetic text and rich four-page/multilayer
copies also passed native recovery. These are representative results, not a
library-wide restore guarantee. Original ownership was never reassigned.

See [qualification evidence](qualification-run.md) for transport/error cases and
remaining semantics. Private per-device inventories, failed receipts, CAS maps,
archive hashes, repair provenance and server-only reconstruction manifests live
under ignored `artifacts/full-library`; personal identities and payloads must not
be copied into committed documentation.

## Intended outcome

Account for every notebook in the two devices' libraries, transfer the complete
eligible note data to our Couchbase stack, and demonstrate recovery into native
Notes. Report separately what was inventoried, uploaded, verified on the server,
downloaded, and actually applied by the native editor. A successful metadata
replication or native export request is not a complete backup result.

## Run order

1. Capture a private inventory on each device: notebook and folder identities,
   ownership, sync flags, versions, page counts, and available asset references.
   Compare shared IDs across devices; retain divergent versions separately.
   Counts of metadata rows must not be presented as notebook counts. Record
   original account/cluster settings and preservation fingerprints.
2. Establish an independent recovery baseline before broad retargeting. Capture
   native exports and referenced files where available; verify archive integrity
   and identify missing assets. Keep immutable originals, per-device provenance,
   and a resumable journal. Measure device/server space and retained history.
   Incomplete captures stay explicitly incomplete.
3. Qualify the full-library route. Determine whether stock ksync can upload
   existing original-account notes to our endpoint without changing their owner
   or identity; the synthetic-account fixture results do not prove this. If
   another ingestion route is required, keep a documented mapping to original
   IDs and ownership. Do not blindly reassign the library to the lab account.
   Use a separate full-run dataset/policy; retain the current narrow fixture lab.
4. Run representative notebooks first: text, handwriting, images, templates,
   multiple pages/layers, folders and closed documents. Check the generic and
   V2 resource paths, file publication ordering and recovery after exhaustion.
   The OSS facade now supports scoped signed bulk delete, including native V2
   delete/error/retry qualification. Keep personal notes unchanged; use copied fixtures for
   destructive cases and concurrent-edit tests.
5. Upload the full inventoried population in journaled batches. Track each
   notebook's metadata, referenced resources and attachment revisions/hashes;
   distinguish accepted requests from durable server data. Reconcile omissions,
   failures and duplicates against the original per-device inventory. Preserve
   distinct versions of same-ID notebooks until conflict behavior is qualified.
6. Validate server completeness independently of device caches: resolve all
   required references, verify stored bytes and hashes, and produce a per-note
   completeness report. Explicitly account for tombstones, disabled notes,
   shared/foreign ownership, encrypted or unsupported content, and history.
   No silent exclusion from the final totals.
7. Exercise download/application and server-based recovery using isolated
   destination copies, preserving existing libraries. Verify native opening,
   page counts, representative rendering and semantic re-export. Prove that
   recovery reads our server's stored assets rather than an existing local
   cache or Onyx cloud. Use disposable copies for edit round trips, restart,
   interruption and deletion tests. Never invoke broad RESTORE_NOTE.
8. Restore original accounts, exact cluster maps and sync flags; remove lab
   plumbing. Recheck after cached native processes have settled, since a late
   configuration write has already been observed. Verify source-library
   identity/status/ownership preservation and record any intended fixture
   additions separately. Retain verified server data and recovery artifacts.

## Completion evidence

Deliver a private per-device/per-notebook manifest and a concise report with
inventory totals, durable upload totals, complete asset sets, native recovery
results, unresolved exceptions, and restoration checks. Broad upload alone
qualifies replication only; call it a verified backup only for the population
whose completeness and independent recovery have both been demonstrated.

This run builds on [the reference lab](couchbase-reference-lab.md),
[static semantics](static-sync-semantics.md),
[resource fault results](missing-resource-experiment.md), and
[ordinary-app endpoint access](unprivileged-endpoint-access.md).
