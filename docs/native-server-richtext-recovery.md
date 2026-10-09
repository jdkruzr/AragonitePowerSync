# Server-created native text recovery, 2026-10-07

This continuation publishes the recovery payload through backend assets and native
commit/MESSAGE records, without helper import or shared-file staging. It narrows
the server-only gate; it does not qualify reliable recovery on both devices.

## Construction and controls

Only the fixed lab text notebook is changed. A fresh native source export and an
explicitly selected V2 text body feed the existing recovery builder. Fresh UUIDs
produce resource HTML, resource PB, point data and shape ZIP. Backend labAsset
objects use native OSS keys; point storage omits the local per-page directory.
All four writes finish before the existing commit record receives a newer
updatedAt and a fresh type-6 NOTE_TREE MESSAGE is published. No historical object
is overwritten, no broad restore runs, and no helper content import is requested.
The unique SRV marker distinguishes this test from the earlier assisted recovery.

The first round leaves existing editor processes available. The second also
advances notebook metadata updatedAt and closes Notes before publication. The
third requires both device session deadlines to be fresh, stops both Notes
processes, and inspects the result by cold native opening. These are successive
runs with different marker/revision identities, not identical branches replayed.
Do not infer a firmware difference or timestamp-causation from them.

## Observed first two rounds

Lumi downloads all four fresh backend objects in each 45-second passive window.
The first post-window export references the exact selected recovered bytes under
the original page/shape identity, although the existing native editor still shows
older text. Cold Notes reopening renders the first server marker; normalized
content and style match the selected recovery while native serialization changes
the body hash. This establishes a server-created text revision can be downloaded
and rendered without the helper import path.

Palma has no corresponding fresh-asset GETs in those windows. Its active exports
and cold editor remain on older text; explicit tree restart/scoped sync initially
does not resolve it. Proxy evidence exposes an expired Palma session and refused
unidentified connections. That transport failure invalidates a clean Palma
application comparison. Both native notebook rows are enabled, and both global
note-sync flags are true.

After a new cookie and accepted Palma BLIP connections, both second-round cold
exports omit its marker; Lumi had referenced it earlier. Native work and object
publication resumed during the continuation. This is observed regression of
active content, not proof of which branch/cache/save operation selected it. Keep
all earlier exports and transfer receipts rather than reporting the first GET or
export as durable recovery success.

## Clean transport repeat

Both registry session deadlines are fresh in the third run. Both Notes processes
are stopped before publishing, and both devices have accepted BLIP connections
in the continuation. The third 45-second window again records four fresh object
GETs from Lumi and none from Palma. Cold Lumi opening visibly renders the fresh
third marker; its export preserves the original page/shape IDs. Palma remains on
older active text. This narrows the expiry confound but does not establish which
subscription, native queue or commit-application state prevents Palma delivery.
A successful BLIP handshake alone does not prove receipt of the exact commit.

The next targeted experiment is exact NOTE_TREE commit/MESSAGE revision receipt
on Palma, followed by native scheduling and resource discovery. Keep server-only
recovery unqualified for the pair; avoid speculative firmware-specific fixes.

## Expired-cookie proxy fix

The lab proxy previously returned 403 for every unidentified session, including a
matching active cookie past its recorded deadline. It now returns 401 for that
specific expired-active-cookie case on the allowlisted BLIP route. Unknown,
retired, revoked and malformed grants remain forbidden. No grant becomes valid
because of this response change. Refusal events now record status, without raw
cookies. Fresh Go race tests cover expiry versus retirement, unknown cookies,
malformed registries and revocation, alongside existing socket-cutoff tests.

The native continuation force-stops/restarts Palma ksync, obtains a changed,
unexpired cookie and records accepted BLIP upgrades. This does not prove an
unattended 401 renewal path: no expired-cookie 401 was observed in that native
restart sequence. The HTTP status fix is tested separately from native renewal.

## Recovery contract implications

Receipt, active native reference, visible editor state, cold-open durability and
peer convergence must remain separate acknowledgements. A stale visible buffer
can coexist with an export referencing newly received bytes. Paused-state hash
checks in the assisted prototype do not provide an atomic editor lock. A reliable
recovery needs device coordination and observed final convergence; publishing a
closed resource graph plus a commit is not sufficient evidence on its own.
The first-round success does not justify unattended restoration, recovering
attachments, resurrecting removed notes or changing the native conflict policy.

## Evidence and scope

Private exports, screenshots, payloads, receipts, backend snapshots, fresh-asset
GET journals and run-specific proxy events live in
`artifacts/gateway-lab/qualification/server-richtext-recovery/`. Device changes are
scoped to the existing disposable notebook; original libraries remain guarded.
The probe APK is unchanged and its previous build/lint validation is reused.
Only the Go proxy changed and received fresh build/race-test validation.

## Closure

Both original endpoint/account selections and note/reader flags are restored.
All 1,517 original notebook guard rows on each device pass; original full reader
metadata matches pre-run captures exactly (28 Palma, 14 Lumi rows). Synthetic
reading aliases are disabled. Gateway policy/grants are restored. The owned LAN
facade/proxy and backend containers are stopped, with persistent volumes retained.
The private bundle contains 149 checksummed files, independently reverified.
No production service was changed or deployed.

## Receipt continuation

[Exact Palma receipt and isolated recovery](native-palma-commit-receipt.md)
now demonstrates native child revisions based on both exact published parents,
four exact downloads and cold text application in two isolated rounds. The
earlier simultaneous-device negative windows remain valid; they do not establish
a general Palma inability to apply a server-created graph. Pair convergence and
the specific older-content republishing operation remain open.
