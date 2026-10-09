# Reboot durability, 2026-10-06

Later direct-Wi-Fi qualification supersedes the tunnel startup observations:
both devices reconnect after reboot without ADB reverse. Server metadata followed
by a native MESSAGE wakeup applies automatically on Palma and Lumi. The earlier
bare host writes omitted that publication step. See
[direct LAN evidence](direct-lan-qualification.md). The earlier measurements
below remain recorded with their original transport and timing limits.

User authorized sequential real reboots, with manual unlocking afterward.
Experiments use lab-owned fixtures; original accounts and libraries are restored
between runs. Transport reverses are expected to disappear on reboot and are
re-established separately from endpoint or account repair.

The first Lumi reboot changed the kernel boot ID and retained the exact lab
cluster map and selected cluster before unlocking. After unlock the user saw
kids-mode transitions and ADB disappeared. Automatic sync and account durability
were therefore not qualified. Palma was not rebooted in that attempt.

The shim supplied `parent: powersync-lab-parent` to prevent its account from
being counted by the lab's original-master restoration guard. That was an unsafe
use of native account semantics. Lumi launcher `StartupActivity` lines 95-97
checks the current account parent; a nonempty parent causes `startActivity(true)`
to invoke `QuitChildModeAction`, which restores a master account and restarts the
launcher. `EnterChildModeAction` disables ADB on child-mode entry. This connects
the fake parent to native child/account handling; the exact full entry/exit
sequence is not established by the captured post-reconnection log.

The shim now returns an empty parent. `NativeAccountLab` excludes only the exact
fixed disposable UID from its original-master uniqueness check, removes only that
disposable account cache before invoking native MASTER, and still verifies
the restored account against the saved original UID. The native MASTER request selects the most recent cached master, so exclusion
from our guard alone was insufficient: a live test selected the lab account.
Removing only its cached row makes the original master unambiguous. Personal
account rows are not edited or deleted. A regression assertion requires the authenticated account
response's parent to be empty. Java 17 offline build, lint and 17 Python tests
pass. The fixed app is installed on both devices. A corrected live login and restoration
passed: the lab parent is empty, its cached account was removed, the original
account returned, exact map/flags matched and original library guards passed.
The corrected reboot repeat passed configuration/account retention; see results below.

After the initial interruption, Lumi's original account was restored. A cached
configuration race caused one app writer readback failure; after stopping native
Notes/ksync and retrying, exact map/flag restoration passed and the original
library identity/status/ownership guard passed. No original content write was
performed. Raw first-attempt evidence is retained separately under private
`artifacts/gateway-lab/qualification/reboot/attempt-1`.


## Corrected Lumi reboot

The kernel boot ID changed. Exact lab map, selected cluster, account, test-server
JSON marker and note/reader sync flags survived without provisioning, refresh or
login repairs. The user reported normal mode after unlock; ADB remained available.
Native ksync was running. Only ADB reverse transport was restored.

No per-device WebSocket connection was observed during 35 seconds of passive
observation. A subsequently requested native fixture rename reached the server
without an explicit tree restart. A fresh server-side metadata rename did not
apply to native SQL within 40 seconds. These are separate results: configuration
and outgoing metadata durability pass; fully automatic reconnection and incoming
application are not qualified. The observation windows do not establish an
infinite failure or identify a unique cause. Lumi was restored afterward: exact
original map, sole original account, note/reader true/true and original library
guard all passed. Palma was tested separately below.


The Lumi passive reconnect check restored reverse transport after unlock; native
startup could have attempted the loopback endpoint before the host mapping was
ready. Palma's mappings were restored while Android user 0 was still
`RUNNING_LOCKED`, with a separate timestamped receipt. This timing difference
must be retained when comparing automatic reconnection results; the Lumi window
alone does not establish production-network boot failure.


## Palma reboot and final restoration

Palma's boot ID changed. Exact cluster map and selection, lab account, JSON
marker and note/reader flags survived, and ksync was running after unlock. The
native fixture rename reached Couchbase without re-provisioning or login. No
automatic WebSocket connection was observed in the 35-second window. A fresh
server-side rename did not apply within 40 seconds; the ordinary app's existing
Binder `restartTreeReplicator(null)` request then applied it within the following
30-second window. This qualifies explicit native wakeup recovery, not automatic
startup or page-content recovery.

The gap between the reboot and post-unlock capture was approximately 10.88 hours;
Gateway session TTL is one hour. One subsequent sync-token request and successful
proxy connections were recorded, with no per-device proxy/shim error status in
the captured window. These counts do not isolate session expiry as the cause of
the passive scheduling failure. A generic Connection-refused log line belonged
to another process and did not mention loopback, so it is not ksync evidence.
No child-mode entry was found in the captured Palma boot log; ADB remained
available. Lumi's corrected reboot was explicitly reported as normal by the user.

| Check | Lumi | Palma |
| --- | --- | --- |
| Exact target, account, marker and flags survive | Pass | Pass |
| Outgoing native fixture metadata edit | Pass | Pass |
| Passive reconnection in 35 seconds | Not observed | Not observed |
| Passive incoming metadata in 40 seconds | Not observed | Not observed |
| Incoming metadata after explicit native tree wake | Not tested | Pass |
| Original account/map/flags restored | Pass | Pass |

Both devices' original configurations were verified again after settling. Lumi
has its sole original logged-in account and note/reader true/true. Palma is
logged out with both flags false. Original library identity/status/ownership
checks pass on both; these checks do not compare all content bytes. Markers and
lab reverse ports were removed, original Gateway policy/grants restored, and
host lab services/containers stopped with volumes retained. Reboot receipts were
uploaded to the private preservation dataset and server read-verified before
shutdown. Raw evidence is under ignored `artifacts/gateway-lab/qualification/reboot`.

Remaining work is to qualify a deliberate startup wakeup with reliable network
transport, incoming application on Lumi, and real page-content updates after
reboot. These tests used metadata renames. Retained configuration does not imply
an unattended sync service is production-ready.
