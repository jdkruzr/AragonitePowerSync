# Direct LAN qualification

2026-10-06. Private listeners advertise the host's Wi-Fi address; underlying
Sync Gateway and Couchbase admin listeners remain loopback-only. Both devices
use native HTTP/WS endpoints without lab ADB reverse ports. Per-device proxy
connect events identify Palma and Lumi by their separate LAN addresses.

Both devices uploaded fresh native metadata edits. A direct host Couchbase
metadata write did not apply within 40 seconds on either device, but applied
after an ordinary-app Binder tree wake. That write did not publish the native
MESSAGE notification; it is not a complete native publication experiment.

After real reboots on both devices, the exact cluster map, lab account, test-server
definition and note/reader flags survived. Native ksync connected before the
post-boot app edit, with no ADB tunnel or explicit wake. A fresh native edit
reached Couchbase. Boot time is estimated from device uptime; connection peer
comes from the connect event, while the separate HTTP 101 response event has
no peer field. Correlate those events rather than reporting a missing response
peer as a transport failure.

A host metadata write followed by a fresh lab-account type-6 NOTE_TREE MESSAGE
record applied on Palma in 4.2 seconds and Lumi in 6.9 seconds without Binder wake
or receiver UI action. Both fresh post-boot native edits reached Couchbase.
This qualifies server-originated metadata publication plus native notification
in this window. It does not qualify all content/assets, indefinite background
survival, expired credentials, or exactly-once notification handling. The adapter
must distinguish stored data from wakeup publication and native application.

Both direct-LAN reboot tests have completed. Palma was restored before Lumi's
post-boot check; the scoped lab policy and services remained active throughout.
Both devices are restored to their original maps/accounts/flags. Test markers
and own-app LAN host overrides are removed, original-library guards pass, and
server policy is restored. Lab services are stopped with state retained.
Private evidence is under `artifacts/gateway-lab/qualification/lan/`.
