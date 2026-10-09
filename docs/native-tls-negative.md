# Native TLS rejection qualification

2026-10-07. Private evidence: `artifacts/gateway-lab/qualification/tls-negative/`.

Both Palma 2 Pro (ksync 32173) and Go 10.3 II Lumi (ksync 32079) reject a
self-signed certificate at the account HTTPS boundary and independently at the
Couchbase Lite WSS boundary. The certificate has the correct private-IP SAN;
this qualifies an unknown trust anchor, not hostname mismatch.

| Control | Palma | Lumi |
| --- | --- | --- |
| HTTPS native account login | Rejected, callback result -1 | Rejected, callback result -1 |
| HTTP native account login before WSS test | Passed | Passed |
| Native NOTE_TREE over WSS | STOPPED, Couchbase Lite 5008 | STOPPED, Couchbase Lite 5008 |
| WSS exception cause | SSLHandshakeException, missing trust anchor | SSLHandshakeException, missing trust anchor |
| Return to ordinary plaintext lab login | Passed | Passed |

The owned TLS router listens only at the fixed LAN IP and port 19443 and uses a
one-day matching-IP self-signed certificate. Host curl succeeds when explicitly
trusting that certificate and fails with exit 60 using default trust. No device
CA, truststore, network-security policy or certificate-validation bypass was
installed. The router records only the host health request; device attempts fail
at TLS handshake, before account or replication HTTP routing. Router logs show
client rejection as an unknown certificate.

The account test uses HTTPS account/asset endpoints plus WSS replication. The
second test retains working HTTP account/asset endpoints and changes only the
replication transport to WSS. Note sync is enabled with the native configuration
broadcast and NOTE_TREE is explicitly restarted. Classification requires both
the trust-anchor exception and the NOTE_TREE error from the current WSS ksync
process. Timestamp-only log slices also contain late lines from the previous
account process; those are not the evidence used to classify WSS failure.

The endpoint helper refreshes an already app-owned test-server marker when
reasserting the endpoint, preventing its old plaintext definition from masking
the TLS target. Marker-free operation remains marker-free. The negative mode is
bounded to the fixed private host and port; the router rejects wildcard/private
plaintext listeners and arbitrary backend hosts. Default loopback and previously
qualified Cloudflare paths remain available.

Fresh Android assembly/lint and TLS-router race tests pass. Both helper installs
are certificate-matched in-place upgrades. Evidence includes the tested APK,
router source/binary, endpoint definitions, native logs, classifier, host controls,
restoration receipts and an independently verified 124-file checksum manifest. The test private key stays in the
private evidence directory.

The facade needs a valid certificate chain for both account HTTPS and Couchbase
WSS. An HTTPS health response alone does not qualify native replication. These
results complement the earlier [trusted hostname run](native-tls-tunnel.md), which
qualified account login, WSS and signed exact asset delivery on both devices.
A trusted-chain/wrong-hostname negative remains open, as do asset-client-specific
negative tests and certificate renewal during active replication.

Original cluster maps, account state, note/reader flags and full reader metadata
are checked after restoration, alongside exact original notebook guards (1517
Palma, 1518 Lumi, including the user-confirmed Go 6 II addition). Owned services
are stopped while persistent lab data is retained. No note edit is needed for
this certificate test.
