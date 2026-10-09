# Qualification run, 2026-10-06

The user authorized the complete experiment list and full-library runthrough.
Original notes remained read/export only; mutations used disposable fixtures or
fresh lab-owned recovery copies. Private payloads, identities, receipts and logs
remain under ignored `artifacts/`. Both devices are restored; the lab is stopped.

## Results

| Experiment | Observed result | Qualification limit |
| --- | --- | --- |
| Ordinary-app endpoint provisioning | Both devices accepted app-written cluster map and JSON test-server definition, native refresh, Binder selection and login | Transport used ADB reverse; unattended production provisioning is not implemented |
| Restart durability | Target survived native refresh and Notes/ksync process restart | Reboot deferred at the user's request |
| Marker-free provisioning | Palma selected custom ID but cached-map rewriting removed its definition and login failed | Does not establish universal marker necessity |
| Commit/MESSAGE ordering | Metadata and files alone did not fetch new text; fresh MESSAGE with old commit did not fetch for 53 seconds; new commit alone did not fetch for 49 seconds; subsequent MESSAGE fetched and applied LAMBDA | Host control records were clones, not an atomic producer replay |
| V2 404 | One GET acknowledged pending status 1 as 0 with no file; releasing fault and scoped retry did not fetch it | Missing-file acknowledgement can hide absence; not visual recovery |
| V2 503 | Six observed attempts retained pending work; released fault plus scoped request fetched exact bytes and acknowledged 0 | Full retry exhaustion was not established |
| Corrupt/truncated download | Each rejected transfer retained the old 40-byte file and pending work; released fault recovered exact 65,581-byte asset | Destination delete/rename crash window remains untested |
| Interrupted transfer | Killing ksync during an observed slow GET retained old bytes and pending work; restart recovered exact asset | Does not prove power-loss durability or fsync safety |
| Native deletion/retry | Injected bulk-delete 503 retained asset and status 3; release deleted server asset and acknowledged 0 | Narrow synthetic queue row only |
| Repeated native deletion | Palma acknowledged both first and already-absent deletion as 0 after daemon restart | Earlier scoped requests did not wake the new queue; automatic provider wakeup unqualified |
| Session revocation | Revoked Palma Gateway session renewed natively and delivered queued edit; Lumi session unchanged | Gateway cookie revocation, not all account/bearer expiry paths |
| Offline edit/edit | Real sibling generation-12 revisions produced Gateway 409; both devices converged to Palma ALPHA despite Lumi's later JSON updatedAt | Exact default resolver tie policy remains a source-based inference |
| Online soft deletion | Native Palma removal reached server as status 0 and Lumi's native row became 0 | Recycle-bin removal is ordinary metadata, not a Couchbase tombstone |
| Offline delete/edit | Two attempts left Palma status 0 and Lumi status 1 during bounded observation; fixture restored to status 1 | No qualified conflict winner or convergence; final trace does not show Lumi edit publication |
| Rich fixture transfer | Native ink, image, Cornell template, four pages and authoritative multilayer state transferred | Synthetic content; receiver's first unopened export omitted its image |
| Local edit plus incoming revision | Offline image movement on Lumi and newer stroke on Palma both survived and converged in final native exports after explicit sync/pull | Does not prove unsaved selection-buffer safety or automatic scheduling |
| Server-only native imports | Text LAMBDA, rich four-page fixture and a three-page original notebook copy imported under fresh IDs; original copy retained 549 point spans and two PNGs | Representative copies, not full-library restore |
| Missing-image repair | Sixteen server-preserved assets inserted into five derived archives; one native imported copy has eleven local assets | Native re-export still omits those eleven assets after page opening; self-contained repair recovery fails |

## Preservation and completeness

Palma inventory contains 1,512 original notebooks; Lumi contains 1,511. Live
populations are 1,079 and 1,078, sharing 1,078 IDs. Both also have 433 removed
notebooks and seven folders (four live, three removed). Ownership and per-device
versions remain distinct; original notebooks were not reassigned to the lab.

Captured shared files: 63,261 Palma files (1,181,059,620 bytes) and 67,032 Lumi
files (1,181,081,562 bytes). A stalled Lumi tar stream was completed with bounded
reads of 7,103 missing paths, without final read errors. Captures are not atomic
snapshots. All 88,371 unique byte objects were uploaded, read-verified and
independently reconstructed; all captured manifest references resolve with zero
missing or wrong-sized objects.

All 2,157 live native exports were captured, uploaded and reconstructed solely
from server attachments, with archive hashes and CRC checks. Lumi had one
transient empty-JSON export failure, recovered on retry; its failed receipt was
retained. The private `powersync_library` preservation dataset is separate from
stock ksync's `neocloud` replication. Original-owner full-library native
replication has not been qualified.

The server-native audit checked 467,241 Palma and 467,306 Lumi live point
references, with no missing point files, absent note-info metadata or nested
shape ZIP CRC failures. Archive namespace can differ from original notebook ID;
provenance must preserve both. An earlier missing-note-info count came from
assuming those IDs were equal and was corrected.

Live image audit found one missing Palma asset and fifteen Lumi assets across
five archives. Each has one matching independently preserved byte object.
Derived repairs preserve original exports and include insertion/hash receipts;
repair and audit packs were server read-verified. One image shape has an empty
reference object and remains unresolved. Page, template, audio, PDF, encrypted
content and full database dependency closure are not comprehensively qualified.
**Complete native backup remains false.**

## Native content semantics

Current layer state is `NoteInfo` protobuf field 12's JSON `pageInfoMap`.
The rich source, receiver and recovered copy agree on four pages with layer
counts 1/1/1/2, visibility and current-layer IDs. Legacy `NotePageModel.layerList`
alone reports an incomplete view. Shape ZORDER corresponds to native layer ID;
UI layers 1 and 2 use IDs 0 and 1. A temporary placement probe was removed.

Point containers can multiplex multiple shapes. Compare every 44-byte xref
entry's shape ID and payload span; comparing only the last entry or outer ZIP
hash misses semantic content. Native export can regenerate metadata and nested
archives, and can omit assets before opening. CRC success is not asset closure.

The final offline delete/edit trace records Palma's removal revision with
status 0. The following status-1 revision is the explicit restoration, initially
rejected with 409 against the removal and then proposed against that parent.
It does not establish publication of Lumi's offline rename. The native split
therefore cannot yet be attributed solely to a Couchbase conflict resolver.

## Device availability and verification

The launcher's “No result” was its empty Apps panel, not PowerSync output.
A missing cluster definition is a candidate cause, but launcher fallback means
that alone is insufficient proof. No launcher data was cleared; Lumi showed a
populated Apps panel after reconnection.

Both devices lost ADB during UIAutomator inspections and required the user to
re-enable USB debugging. Logs correlate UiAutomation registration/death with
MTP-only USB within a second; causality remains unproved. Further tests used
input events and screenshots instead of `uiautomator dump`. Secure keyguards
make reboot tests require user unlocking; user explicitly deferred that test.

Java 17 offline debug build and Android lint passed for the installed lab app.
The final Python suite passes all 17 tests, including scoped OSS signatures,
all-key validation, idempotence, preservation capture, protocol parsing and
concurrent shim registry updates. Registry saves are serialized under the
existing reentrant lock; concurrent two-device channel updates pass.

## Remaining gates

- Automatic boot startup scheduling and page-content updates; both devices retain
  target/account/flags after reboot. Palma incoming metadata needs an explicit
  tree wakeup in the tested window. See [reboot findings](reboot-durability.md).
- Unsaved native editor/selection-buffer behavior and automatic scheduling.
- Publication and convergence of offline delete/edit changes.
- The destination-delete/rename crash window and power-loss durability.
- Complete resource/database dependency closure and library-wide native recovery.
- Self-contained repaired image export and the empty image reference.
- Production endpoint/session lifecycle and Rhizome integration.

Private evidence includes `rich-dirty-final-result.json`,
`rich-authoritative-layer-check.json`, `delete-edit-wire-summary.json`,
`native-session-revoke-result.json`, `native-repeated-delete-result.json`,
`final-fixture-server-pack.json`, and the full-library reconstruction/audit
manifests. Restoration evidence is `final-restoration-result.json`; its receipt
was also uploaded and server read-verified before shutdown.


## Restoration

Both exact original cluster maps were checked again after settling. Palma has
zero logged-in accounts and note/reader sync false/false; Lumi has its one
original account and note/reader sync true/true. Original library
identity/status/ownership fingerprints still match on both, excluding explicitly
allowlisted disposable/recovery copies. This guard does not compare all content
bytes. Test-server markers and only lab reverse ports 18765/18766 were removed.
Generated PowerSync-export archive copies were cleaned up; recovery copies and
original notebooks were retained. Gateway policy/principal grants and trace
logging were restored, temporary fault files removed, and the shim, proxy and
Couchbase/Gateway containers stopped without deleting volumes. Neither device
was rebooted. USB debugging was not modified by cleanup.
