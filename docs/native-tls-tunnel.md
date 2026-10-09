# Native HTTPS/WSS qualification, 2026-10-07

A disposable Cloudflare Quick Tunnel carried stock Palma 2 Pro and Go 10.3 II Lumi
account login, Couchbase Lite replication and signed notebook assets. Native ksync
and Notes APKs were unmodified; the ordinary probe app provisioned reversible
HTTPS/WSS cluster endpoints. No custom CA or trust-store override was installed.

## Observations

| Native operation | Palma | Lumi |
| --- | --- | --- |
| HTTPS sign-in, status 200 | 1 | 1 |
| WSS upgrade, status 101 | 22 | 24 |
| Signed asset GET, status 200 | 12 | 15 |
| Signed asset HEAD, status 200 | 39 | 29 |
| Signed asset PUT, status 200 | 7 | 9 |

Counts cover the tunnel run through restoration; repeated requests are transport
receipts, not counts of unique notes. The configured account endpoints were HTTPS
and replication endpoints WSS. Backend services were bound to loopback; no ADB
reverse or direct LAN target was used for this run.

A new edit in the owned disposable native text notebook appended `TLS6a977c3a HTTPS`.
The source commit changed and five successful signed PUTs followed the edit.
Stored marker-bearing HTML was escrowed and its SHA-256 matched the source native
export. The receiver native export contained marker-bearing HTML whose SHA-256
matched a successful signed GET. The receiver editor screenshot visibly showed
the marker. These separately establish source publication, exact asset receipt,
native export application and visible editor application.

The first correlation attempt looked for the notebook ID in request paths, which
are deliberately redacted. It reported zero matching PUTs and failed its assertion.
That inconclusive attempt remains in `results.json`; the recovery used backend
asset IDs/revisions and document metadata, rather than weakening path redaction.

## Facade implications

The native OSS client treats the HTTPS hostname as a CNAME endpoint: note resource
paths omit the bucket prefix present in the earlier IP endpoint tests. The router
normalizes those paths, including root bucket listings, before signature checking;
the OSS canonical resource and signature remain enforced by the asset facade.
A real deployment must support the endpoint addressing style the native SDK uses.

The temporary router admits only account, signed lab-asset, exact BLIP upgrade and
health routes. Public preflight denied Gateway config/all-docs and account access
without native identity headers. Identity headers restrict lab routing; they are
not enrollment proof or a substitute for account/asset authentication. The final
router tests additionally deny an OSS-header request to a Gateway administrative
path. This final restriction was unit-tested after the live transfer; the live
router used the earlier asset-header routing rule, with facade denial downstream.

Cloudflare terminated public TLS and forwarded to a loopback HTTP origin. This
qualifies the native trusted-hostname HTTPS/WSS path, not origin TLS or negative
certificate/hostname controls. Quick Tunnels are temporary test infrastructure,
not an Alexandria deployment dependency. See [Cloudflare documentation](https://developers.cloudflare.com/tunnel/get-started/quick-tunnels/).

## Scope and validation

Fresh Android assembleDebug/lintDebug passed and both probe APKs were upgraded
in place. All 31 Python tests passed; router race tests passed, and existing proxy
race tests returned a cached passing result. The final router build passed.
No native APK was patched. No general firmware, reading/PDF/ink, restart-during-work
or invalid-certificate behavior is implied qualified by this text-notebook run.

Private evidence is under `artifacts/gateway-lab/qualification/tls-tunnel/`:
endpoint captures, router/API/proxy receipts, source and receiver exports,
asset escrow, screenshots, test/install logs, restoration checks and SHA-256
manifest. Original account/cluster/sync flags, notebook guard and full reader
metadata are checked before service shutdown; retained lab volumes are not deleted.

The user confirmed this integration has exactly one owner/library with multiple
devices. Independent cross-author isolation is therefore removed from this
project's required experiment list. Device credentials, revocation and scopes
remain separate per device. This does not change Alexandria Hosting's general
library-isolation obligations.

Closure passed: both original notebook guards and full reader metadata were
unchanged; Palma is logged out with note/reader sync off, Lumi has its original
account with note/reader sync on. Endpoint markers and host overrides are absent.
The tunnel and lab services are stopped, all six local lab ports are closed, and
78 private evidence files have verified SHA-256 entries. Persistent lab volumes
were retained.
