# Reading scope comparison: Palma and Go 10.3 II Lumi

Matched qualification, 2026-10-06. Private evidence:
`artifacts/gateway-lab/qualification/reader-scope-pair/`.

## Correction to earlier scope evidence

The earlier Lumi `bookscope-cbl-userdata-private.json` returned two models whose
`dbId` remained `powersync-lab-READER_LIBRARY`, with updatedAt values identical
to the initial library-scoped publication. The intervening server move to a
book-scoped dbId did not establish receipt of that new revision. Native bookmark
and progress application were real, but the earlier claim that fresh user-data
needed or qualified book-scoped delivery was unsupported. Cached library
models confounded the scope observation. This correction supersedes that claim
in native-reading-cloud-open and earlier summary documents.

## Matched protocol

Both devices join the same synthetic lab with note sync disabled. Each receives
a new cloud-only UUID and exact authored managed EPUB; originals are escrowed.
The same two model templates and operation order are used for each receiver.

1. Publish new bookmark and reader-note configuration (mode 7) record IDs directly in book scope. Restart
   the native library, then query local user-data by the fresh book identity.
2. Move those records into READER_LIBRARY with a distinct quote and timestamp.
   Restart identically and compare exact local model content.
3. Move them back into book scope with another distinct revision marker.
   Restart identically; distinguish old retained library models from new
   book-scoped revisions. Never treat row count alone as proof of delivery.

Wire channel subscriptions, sent revision IDs and native CBL model scope/quote/
timestamp are recorded separately. This compares transport/scope semantics; it
does not infer firmware causality from device model alone or qualify unattended
native application. Original guards/accounts/endpoints/flags and EPUBs must be
restored; known synthetic metadata replays are journaled separately.

## Results

| Phase | Palma | Go 10.3 II Lumi |
| --- | --- | --- |
| New IDs directly in book scope | 0 models | 0 models |
| Move to library scope, new quote/timestamp, restart | 2 models; exact new marker and timestamp | 2 models; exact new marker and timestamp |
| Move back to book scope, another marker/timestamp, restart | 2 old library models; new revision absent | 2 old library models; new revision absent |

Each phase used the same operation order with approximately 10.7–10.8 seconds
from restart to completed query. The wire trace includes 13 library subscription
requests and no fresh-book channel subscriptions. Each test model has two sent
revision requests in the capture (the library revision delivered to the two
principals); native per-receiver CBL queries independently verify exact content.

Live package versions: Palma ksync 32173, NeoReader 38999; Lumi ksync 32079,
NeoReader 38698. Their version difference is real, but it does not account for
the earlier claimed scope difference: matched results are identical and
reproduce the historical cached-library observation. This does not claim all
firmware behavior is identical or that book-scoped delivery is impossible
through other subscriptions/entry points.

Use READER_LIBRARY plus explicit native library restart as the demonstrated
transport route for this adapter, and verify exact received revisions before
requesting native application. Local retained models can survive a server
scope/channel move, so a nonempty callback is not a delivery acknowledgement.

## Preservation and evidence

Full original metadata rows match the pre-run captures on both devices (28
Palma / 14 Lumi), including identity/status/sync flags. Two new synthetic native
rows per device were separately journaled and removed. Exact 3527-byte authored
Downloads EPUBs restored from SHA-256-verified escrow; disposable managed files
removed only at known UUID/path/hash identities. Original accounts, cluster
maps/endpoints/global flags, original notebook guards and old-fixture disabled
flags pass final checks. Server policy/grants and Gateway logging restored.
Lab services stopped with durable state retained; evidence and driver scripts
are checksummed. The same built/linted probe APK was used on both devices; its
SHA-256 and Lumi's in-place installation receipt are preserved. No firmware
upgrade, root access or APK uninstall was used.
