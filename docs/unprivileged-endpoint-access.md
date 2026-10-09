# Ordinary-app endpoint configuration probe

On 2026-10-06, the installed PowerSync debug app successfully read and wrote
`ONYX_CLUSTER_SYSTEM_KEY` on both Go 10.3 II and Palma 2 Pro. This closes the
previously untested configuration-write boundary in the retargeting experiment.

`NativeSystemConfigProbe` executes `/system/bin/service` directly with
`ProcessBuilder`, from the ordinary Android app, without a shell, `su`,
`run-as`, or a host configuration write. The captured firmware ABI is read
transaction 3 and write transaction 4. Reports identify UID 10133 on Lumi and
10216 on Palma, with `u:r:untrusted_app:...` process contexts.

Both runs read the live configuration, added a previously absent
`powersync_uid_probe` entry with a synthetic loopback endpoint, obtained a true
write return, and read back the complete candidate configuration. The active
cluster stayed `en`; this unused minimal entry was never selected or used for
network traffic. Each run restored the original map in `finally`, and the host
independently verified equality with the saved original configuration.

Earlier ordinary-app Binder experiments demonstrated selection of the staged
lab cluster through stock ksync. Together these establish app-level map writes
and cluster selection on the two tested firmware builds. They do not yet
establish a complete standalone installer/retargeting flow: the replication lab
still uses ADB-created test markers and loopback tunnels, and native cached
configuration can overwrite a custom map during cluster refresh. Test-marker
necessity, app-controlled provisioning of a complete valid cluster definition,
production network/TLS setup, durable refresh behavior, and other firmware
permission policies remain separate qualification tasks.

The first probe hit a decoder bug for small single-line Binder replies. It is
not qualifying evidence; maps were checked/restored before repeating. The
corrected runs and independent host verification are captured privately in
`artifacts/gateway-lab/protocol-traces/fault-run/*-uid-probe.json`.

The helper is a debug-only diagnostic, guarded to the qualified API 35 firmware
and an `en` baseline. No Onyx package patch, root, or signature permission was
used. Required Android build and lint pass.


The authorized [reboot experiment](reboot-durability.md) now distinguishes exact
configuration/account retention from automatic startup and transport timing.
It also corrected unsafe fake-parent account semantics in the disposable shim.
