# Native Reader publication across a brief Gateway outage

2026-10-08. Palma user drew W below V and left the owned PDF editor open. A passive screenshot confirmed pending ink before the fault. Sync Gateway was gracefully stopped; Couchbase and its persistent volume stayed running, as did the proxy/account facade. The facade attachment backend also depends on Gateway, so this is not an isolated BLIP-only fault.

First ADB Back exited the pen toolbar without closing the document. A second Back was sent after confirming Gateway was still stopped. Native close logged changed data and sync to ksync at 1791493149.209. Gateway start was requested at 1791493154.560 and readiness observed at 1791493158.230. Thus document dispatch preceded restart by about five seconds. Total stopped interval was about forty seconds, mostly before the qualifying close; do not describe it as forty seconds of failed publication.

Both sessions had over 1000 seconds remaining at fault preflight. No helper, manual Sync, source reopen, host-authored publication, login or renewal occurred during the 120-second passive recovery window. Server acquired shape ZIP (675 bytes), point resource (14624 bytes), mode-7 configuration, metadata, reader commit and eventually READER_LIBRARY MESSAGE. Foreground 10.3 II imported information revision 4705926e-2471-4784-a0d3-e454a8bd5fa1 at 1791493184.564, about 26 seconds after readiness; screenshots show W before helper verification.

Both native exports contain ten shapes (six active/four disabled), including W shape 448980fd-2196-4790-9fd7-a2af604c6ffb with pressure revision bf5f6045-661a-4078-a06b-245c563addd1. All eight pressure payloads match byte-for-byte. Prior nine shape semantics and seven pressure files are preserved; cross-device semantic comparison excludes only local UPDATEDAT and PDF OBJECTID. Later ordinary replay preserves all native fields and pressure bytes exactly. No unrelated reader drift; notebook guards pass with only the previously confirmed Palma user-addition comparison exception. Baselines and fixture allowlists unchanged.

Helper capture/pull guards admitted only the two exact observed owned-PDF derivatives after passive evidence. Build/lint passed; matching signers verified before in-place installation. Devices returned Home after verification.

This qualifies autonomous foreground delivery across a brief graceful Gateway restart. It does not prove retry after a sustained failed asset upload, recovery with the source editor still open, Couchbase hard-crash behavior or background receiver delivery. A longer fault deliberately reaching a native publication error is the next control.

Evidence: artifacts/gateway-lab/qualification/reader-native-server-interruption/, SHA256SUMS; events.jsonl, native logs, timed server snapshots, screenshots and verification/exact-result.json. No Gateway logging changes were made for this case; lab Gateway is running again.
