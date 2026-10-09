# Palma commit/MESSAGE receipt and recovery scheduling, 2026-10-07

Palma can receive and apply server-created text under the original notebook,
page and shape IDs. Two isolated rounds download all four exact new assets and
retain the selected text/style in a cold native export. The second native editor
visibly renders its unique marker. No helper content import or shared-file staging
is used. This narrows the earlier Palma-negative result; pair convergence remains
unqualified.

## Exact receipt evidence

The short isolated protocol trace ties the exact host-written commit and fresh
MESSAGE revision to Palma's authenticated BLIP connections. Palma subsequently
publishes the next revision of each same document, with the exact host-written
revision in its parent history. This is stronger than a WebSocket handshake or
server send alone: the native client demonstrably uses those revisions as parents.
The MESSAGE's syncAt advances while msgStatus remains zero; do not describe this
as a status-1 acknowledgement. This is not an independent full local CBL snapshot.

Stock Palma logs record MESSAGE handling, NOTE_TREE start/pull, one enabled note
and the fixture's NoteDocDataReplicator task queue. Four successful native GETs
have object hashes equal to the four host-created payload entries. Cold export
references the recovery marker under the original page/shape IDs. Text content
agrees after deduplicating identical marks while preserving their order, and
contentStyle matches. Final native active bytes differ from the selected body;
exact wire equality and normalized editor equality remain separate receipts.

## Why the earlier comparison was misleading

With both device sync processes active during staging, the initial trace records
36 distinct incoming native commit revisions: 18 from each device. This occurs
before the successful isolated publication, not as 36 measured failures of the
new payload. Native logs also show resource deletion and repeated document tasks.
A source pre-open export omitted its resource reference; native opening hydrated
a valid reference but its body had no text node. Both failed preparation attempts
are retained; neither published a recovery graph.

The successful experiments pause Lumi note sync and stop its ksync/Notes processes,
leave Palma replication running, and explicitly select retained synthetic text
from the previous recovery escrow. The initial isolated recovery uses a hydrated
Lumi context; the repeat uses Palma's successful cold export as its context and
fresh resource/revision IDs. These preparations and different revisions prevent
claiming a one-variable causal experiment or a firmware-specific defect.

The isolated result rules out a general inability of Palma to consume this graph.
The two-device churn suggests competing native state/queue activity as the next
investigation target. It does not identify the exact cache/save/record operation
that previously selected older content, and does not qualify atomic dirty-buffer
coordination or reliable unattended pair convergence.

## Capture limits and implementation

The first verbose trace reaches the existing 64 MiB safety bound and restores
Gateway logging before the successful publication. It is useful for initial
churn only. A separate bounded isolated trace captures both exact publication
identities and their native child revisions, followed by successful object GETs.
Process logs are PID-scoped; Lumi's stopped process does not become continuous
coverage of a replacement PID. Later original-account restoration is outside
native lab process coverage.

[The parser](../scripts/parse-gateway-trace.py) now extracts sentRevision metadata
from the final outbound Properties envelope, never the body. It reports server
sequence separately from incoming client sequence. A focused test covers fake
property strings in payload text, credential omission, device mapping and the
sequence domain. All 35 Python tests pass. The probe and Go proxy are unchanged;
their earlier build/lint/race-test validation is reused, not claimed fresh.

## Facade contract and next gate

The server-side publication graph is usable on both tested builds in isolated
runs: immutable assets, then commit, then native MESSAGE. A future recovery
coordinator must establish current device state, distinguish staging from editor
application, and verify cold active content and peer convergence. Asset GET,
MESSAGE reflection and native commit advancement do not each establish final
library convergence. Keep original/losing bytes immutable and never silently
replace the chosen native winner.

Next is a matched two-device recovery with controlled pending native work:
record both current active graphs and exact queued revisions before publication,
resume one device at a time, and identify the first operation that republishes
older content. Compare against a stable native-origin edit as the control. Broad
attachment recovery, production recovery API/UI and automatic history coverage
remain separate gates.

Private evidence: `artifacts/gateway-lab/qualification/palma-commit-receipt/` and
`protocol-traces/palma_commit_receipt{,_isolated}/`. It includes both failures,
native logs, exact revision/parent proofs, asset GET hashes, native exports,
selected bodies, payloads, screenshots and preservation checks.

## Closure

Both original endpoints, accounts and sync flags are restored. All 1,517 guarded
original notebook rows on each device pass; full original reader metadata matches
the pre-run captures exactly (28 Palma, 14 Lumi rows). Synthetic reading aliases
are disabled; Gateway policy/grants and logging are restored. Owned lab services
and containers are stopped, persistent volumes retained. The private bundle's
159 evidence-file hashes were independently verified after closure.
