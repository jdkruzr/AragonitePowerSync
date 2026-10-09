# Reader expiry and retained-publication recovery hole

2026-10-08. Intended experiment: fail owned Palma reader point/shape PUT requests with bounded 503, keep replication healthy, then observe native retry. User drew Y left of X and left the source editor open. Screenshot confirms pending Y. This experiment did not reach an OSS PUT: no upload-fault retry qualification is claimed.

## Separate phases

Initial preflight found cached Gateway and OSS lifetimes expired despite the previous successful login callbacks. Earlier login while a cached session was valid reused that session, rather than granting a new full hour. The first phase records this condition explicitly. Native Back/save reaches ksync, then the Reader replicator encounters HTTP 401 and makes bounded wrapper retries. No OSS asset request occurs during the approximately one-minute fault or 120-second passive window. The source PDF containing Y is escrowed byte-for-byte.

A separate assisted phase force-stops native ksync, invokes native lab login and restores STRING reader true/note false settings with config notifications. This time facade registry verifies fresh sessions with about 3589/3595 seconds remaining. A scoped original-book Reader start is requested. The upload fault is armed before assistance; no PUT occurs. The subsequent 120-second passive window publishes no Y asset.

A third phase verifies fresh session lifetime, arms the upload fault, reopens the owned source PDF and taps the native menu Sync control. This is explicit source UI assistance, distinct from the prior phases. Native logs confirm sync to ksync, but again no OSS asset request occurs during the bounded fault or 120-second passive observation. All fault markers are removed; lab facade/Gateway/proxy remain available. Facade PID after code reload is journaled.

## Retention and preservation

Native exports after all passive windows show sixteen Palma shapes and ten pressure files, versus thirteen shapes/nine pressure files on the 10.3 II. Y is three additional source shapes, retained in native storage; it has not reached the peer. Original thirteen shape semantics and nine pressure payloads remain intact on both devices. Only local UPDATEDAT and PDF OBJECTID are excluded from semantic comparison. No unrelated reader metadata drift; original notebook fingerprints pass with only the previously confirmed Palma user-addition comparison exception. Baselines and allowlists unchanged. No helper SDK source push, host publication, broad restore or destructive reset used. Do not describe this as lost local ink or successful peer recovery.

Build/lint passes, signature-matched helper updates installed in place, and only the exact observed owned Y PDF derivative was admitted for capture/pull. No replay pull was used on the pending source during verification.

## Harness and integration implications

The lab fault selector admits only authenticated, already-allowlisted native point/shape PUTs for one device/book. It returns 503 before backend lookup or mutation, rejects book PDF/other methods/cross-device/cross-book cases, and expires in at most 180 seconds. Twelve OSS tests and eight reader-scope tests pass. Actual fault injection count is zero in these phases.

A successful account-login callback is not a fresh-session guarantee. Enrollment/recovery must observe actual session preparation and lifetime; then verify publication separately from local Reader-to-ksync acknowledgement. Retained-edit reactivation after expiry is an open recovery-policy gate. Next control uses a fresh physical edit with verified session lifetimes, to distinguish fresh publication from the stalled retained edit. Native local data, cloud config, resource queue and pending-commit relationships need further inspection; no specific causal queue loss is proven here.

Evidence: artifacts/gateway-lab/qualification/reader-native-asset-upload-retry/, SHA256SUMS. Separately preserved session-blocked-first-attempt, assisted-session-recovery and native-ui-activation folders, native exports and qualification-result.json. Y native shape details are private. All requested qualification remains local; production integration is not implemented by this experiment.
