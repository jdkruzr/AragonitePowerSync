# Durable reader grants and clean configuration convergence

2026-10-07. Follow-up to [the initial native configuration conflict](native-reader-config-divergence.md). Private checksummed evidence is in `artifacts/gateway-lab/qualification/reader-session-grants/`, `reader-config-reverse/`, `reader-config-forward-clean/`, and `reader-config-native-application/`.

## Session authorization fix

The lab shim previously rebuilt each device principal with notebook-only grants whenever it minted a session. Manual reader grants could disappear while native replication was active, generating access removals and deleting cached CBL rows. Requested vendor channels remain untrusted.

A server-owned `reader-session-grants-enabled` marker now enables reader authorization derived from the bounded `reader-asset-allowlist.json` book IDs. Renewal grants READER_LIBRARY, MESSAGE and each declared book/data-asset channel; identifiers must be 32 lowercase hex characters, with at most 128 books and a bounded policy file. Malformed reader policy fails before principal updates. No arbitrary old principal grants or client-requested wildcard channels are copied. Remove the marker during eventual lab cleanup; it is deliberately active while these devices remain staged.

Four account tests and eight reader-policy tests passed. A new test checks identical reader grants across session rotation and rejects malformed/client wildcard scopes. The live shim was restarted with the change. Both real device sessions were deliberately removed server-side and replaced through ordinary lab login. Without manual grant restoration, reader grants remained present and each device still queried the width-3 native configuration. Global note/reader flags were reasserted false/true and broadcast; this is a separate account-login behavior, not fixed by the grant policy.

## Clean native configuration siblings

Each clean round disconnected only the replication proxy, saved width-3 and width-4 candidates through ordinary-UID ISyncService transaction 3, and verified both local models through read-only transaction 11 before reconnect. Server configuration stayed unchanged offline. No host server document edits were made.

The Lumi-first round converged both device CBL stores and the server to width 4, revision `6-876788c0ec1353cf987d8f554a799ab2d82658c7`. The subsequent Palma-first round converged to width 4 at resolution revision `8-904b2c898f06eb6ad97a0817d0c55b33e126458d`. These are separate fresh sibling pairs, not replay of identical revision hashes. Both connection orders are now exercised with stable reader authorization; this does not prove order independence for every possible conflict. Both private candidate bodies and the wire proposals are retained. The earlier contaminated round independently showed an earlier updatedAt candidate winning, so a generic newest-timestamp resolver remains inappropriate.

## Actual native document application

Before explicit reader application, both native backup document protobufs still held strokeWidth 2.35 although CBL had converged to 4. Scoped one-shot full-data pulls changed both exported native document STROKEWIDTH fields to exactly 4.0. Every native shape and pressure member remained byte-identical, and original reader and notebook preservation guards passed. The helper now allows only the exact known 27316-byte erased source PDF for pull-only operations; build/lint and certificate-matched in-place upgrades passed.

Both callbacks reported success with `newData=false` despite applying configuration. Therefore that flag is not a complete no-mutation receipt. Inspect the relevant native model/export when qualifying configuration application. Both fixtures reopened and their screenshots/UI trees were captured; a newly drawn stroke at the selected width is not yet qualified. The standalone configuration width and nested pen settings are distinct fields; do not infer drawing behavior from the scalar alone.

## Next boundary

Real concurrent native ink edits must establish which document/page/shape commits survive, whether both immutable asset sets remain recoverable, and what the renderer actually applies. Stage only the disposable fixture and keep both pre-edit native exports and candidate configurations. The server should retain observed alternatives separately while preserving native-compatible conflict semantics. Personal libraries remain protected and endpoints remain staged.
