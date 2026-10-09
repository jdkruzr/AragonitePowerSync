# Fresh native mode-7 sibling divergence

2026-10-07. Private evidence: `artifacts/gateway-lab/qualification/reader-config-divergence/`. This is a bounded ordinary-UID Binder/CBL publication experiment, not a native UI style-save test.

Both devices started with native configuration `e61b286c49594b1686b4bca987a8274b` for fresh book `ea6ad9be04304a399583fe557a21f63e`, width 2.35 and the same updatedAt. With only the replication proxy stopped, a helper requested native ISyncService transaction 3 saves: Palma width 3, Lumi width 4. Only width and timestamp may differ from the current native model; identity, other fields and fixture scope are guarded. Read-only transaction 11 verified both local saves before reconnect. The server revision stayed unchanged offline. No host server document write was used.

The wire trace shows sibling generation-4 proposals sharing parent `3-5f954f42e3e356d3ebf97aaf4a39e930f9ca7243`: Palma `4-5f037a5c678d863ab0c22d3dfdbe939ca9bc0718`, Lumi `4-2b5ac95d65920c82a0eb8d19ed19f74ec7948222`.

## Admission wrinkle and recovery

Session renewal temporarily removed reader grants; Lumi's local CBL configuration disappeared, despite unchanged eligible provider metadata. Global reader flags were reasserted/broadcast and a native transaction-16 replication restart restored its baseline. During first reconnect, Palma's proposal received HTTP 403 `sg missing channel access`; Lumi's width-4 revision reached the server. Initial `result.json` deliberately preserves that nonconverged observation and must not be read as final closure.

After restoring journaled device grants and starting native replication again, both devices queried width 3. The server accepted revision `5-d21b29b40814278d1c6393c72a6aa67b7f3edab9`, whose wire history names Lumi's generation-4 revision and whose body contains Palma's width-3 content. This establishes convergence through a native resolution revision. Palma had the earlier updatedAt, ruling out generic timestamp-newest selection in this case. Its sibling revision sorts higher, consistent with default CBL selection; one round with authorization recovery does not establish order independence or every resolver path.

The original ink/pressure export members remain unchanged on both devices, and notebook preservation guards pass. The helper build/lint passed; certificate-matched in-place upgrades preserved app data. Reader UI application of the winning style was not tested. The chosen winner remains in the disposable fixture's CBL configuration; both candidate bodies are retained in private evidence.

## Facade implications and remaining work

Keep native-compatible conflict resolution and optionally archive observed alternatives separately; do not impose a generic updatedAt resolver. More urgently, reader authorization must survive credential/session renewal: the lab shim currently recreates principals with notebook-only grants, requiring manual restoration in these tests. Fix that admission lifecycle before treating reconnect-order experiments as clean controls. Then repeat opposite order under stable grants and qualify native style application separately. Lab endpoints remain staged; personal libraries were not deliberately edited.
