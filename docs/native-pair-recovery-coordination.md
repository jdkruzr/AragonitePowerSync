# Coordinated pair recovery and open-editor publication

2026-10-07. Synthetic rich-text notebook only, Palma 2 Pro and Go 10.3 II Lumi.
Private evidence: `artifacts/gateway-lab/qualification/pair-recovery-coordination/`.

## Coordinated recovery passes

Both native note-sync flags were paused before publication. Palma's cold baseline
contained the retained text; Lumi's selected rich-text body was empty. Scoped
LocalRecordModel queries returned zero rows, which is not a complete pending-work
snapshot. Native opening/exporting can itself prepare pending synthetic work.

The server wrote four fresh immutable assets, metadata/commit, then a fresh
NOTE_TREE MESSAGE. Palma alone resumed and downloaded all four assets, then
paused and cold-opened the recovery. Lumi subsequently resumed with Palma paused
and cold-opened the same recovery under unchanged notebook/page/shape identities.
Both first recovered active bodies had identical SHA256. Both remained recovered
after a 30-second simultaneous sync window and cold reopens; their active bodies
again matched each other. Native serialization changed raw body hashes relative
to the server-created body; do not confuse byte identity with text application.

A subsequent native Palma edit reached Lumi, and both cold exports retained the
recovery marker plus native edit marker. This qualifies a bounded coordinated
fixture recovery, not arbitrary unattended recovery or an atomic device lock.
Root `results.json`, phase snapshots, screenshots and exported active graphs
record each separate receipt/application boundary.

## Historical uploads are not necessarily active regressions

A watcher immediately escrowed successful fixture asset PUTs from the backend,
including exact bytes and SHA256. Palma republished its older baseline text;
Lumi uploaded several older historical text bodies, none matching its empty
selected baseline. These uploads did not undo the coordinated recovery. An older
HTML upload by itself does not establish the receiver's selected active graph.
The watcher captures observed uploads, not every private native queue or branch.
Closure captured 67 native asset PUTs, including 24 text bodies, with zero
escrow errors. Content assertions prove the native control text equals the
recovered text plus its inserted marker and matches across devices.

## Open-editor control

Lumi stayed paused with the native rich-text editor open after inserting a local
buffer marker into the older recovered text. Native autosave/unsavedness was not
independently established. While that editor stayed open, a second server-created
recovery with a fresh marker was published and verified active on Palma alone.

Lumi's sync flag was enabled without foregrounding the helper app. After twelve
seconds the screenshot still showed the edited older buffer. We then hid the
keyboard and tapped native Back. About 3.6 seconds after the recorded close
request, the server received an exact native HTML body containing the buffer
marker and excluding the newer server marker. Lumi's subsequent cold export
selected that older edited text with a complete resource reference.

This is a controlled demonstration that an open edited native page can publish
older content as a fresh native save after a newer server recovery exists. It
does not prove the precise cause of the earlier uncontrolled restart/recovery
failure, nor prove that the editor had fetched/applied the new graph before close.
Palma subsequently resumed with Lumi paused and selected the same older edited
body. Both cold exports had identical SHA256, retained the local buffer marker
and lacked the newer server marker. See `live-editor/results.json`.

## Facade consequence and remaining gates

Follow native winners for ordinary sync. An explicit server-side recovery must
coordinate device/editor state, preserve the displaced candidate, and verify
cold native application. Native note-sync pause alone does not freeze a live
editor or prevent a later save. Recovery success on one device is insufficient.
A production coordinator still needs readiness, editor handling, bounded retry,
per-device receipts and a clear unresolved/conflict outcome. Do not silently
reactivate historical bodies to fight a user's native edit.

The peer protocol trace was bounded and restored original logging; later control
operations use phase snapshots, private current-PID native logcat captures and
exact asset escrow rather than a continuous protocol trace. Probe/proxy/parser
code was unchanged; earlier build/lint/race checks and 35 Python tests were reused,
not rerun or claimed fresh. Restoration and service closure are recorded in
`final-restoration-result.json` and `closing-result.json`; all private evidence
is covered by `sha256-manifest.json` after closure.

Next: production-ready editor/pending-work coordination remains a gate. PDF and
mode-6/7 ink dependencies, TLS rejection controls and unattended scheduling also
remain separate cases. No production Alexandria implementation or deployment.
