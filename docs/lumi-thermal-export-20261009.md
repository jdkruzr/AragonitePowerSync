# Lumi heat during automatic third-party export

2026-10-09: During server reader development, the user reported the Go 10.3 II
Lumi getting hot and behaving strangely, and restarted it. ADB remained available.
Two minutes after reboot, HAL reported CPU sensors around 80–90°C, skin about
43°C and battery 35.3°C. Android reported thermal status 3. ksync, Notes, media
scanning, and the e-ink display thread were all busy; this was not isolated to
native Couchbase replication.

The captured log contained 63 WebDAV upload starts across 33 distinct paths,
plus Notes third-party export actions. The user confirmed automatic UltraBridge
export had remained enabled; switching the native notes account caused a bulk
re-export. This explains the observed extra workload. Log evidence alone did
not prove an infinite retry loop.

Intervention: sleep key (223), then one force-stop of ksync. No data, account,
endpoint, or auto-export setting was changed. Onyx automatically relaunched
ksync. CPU idle rose to about 84%, ksync fell to roughly 7% of a core, and later
HAL readings fell to CPU 57.4°C, skin 36.9°C and battery 31.6°C. Media scanning
continued. The HAL's separate `soc` reading stayed around 94–95 despite cooling;
it was not treated as an independently reliable CPU temperature.

Private logcat and before/after evidence remain in ignored
`artifacts/lumi-thermal-20261009/`. Avoid publishing note names or destination
credentials from those logs. Server UI work resumed with the Lumi resting.
