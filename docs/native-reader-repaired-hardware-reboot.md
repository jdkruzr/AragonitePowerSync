# Repaired reader state across hardware reboot

2026-10-08. 10.3 II passes a real hardware reboot after the explicit fresh-information-revision restoration. Boot ID changed; endpoint configuration and global sync flags survived exactly. All seven native shape records and five pressure payloads match the pre-reboot export exactly. Notebook preservation and unrelated reader metadata checks pass. No login, scoped replication start or pull was requested after reboot. Native export is an assisted observation, not proof of unattended application of a new update.

Palma preflight initially stopped because a new notebook changed its preservation fingerprint. The user confirmed creating `Notebook-1` (`1022f717f43d4b0193bd6f002396f26d`). Excluding only that confirmed addition reproduces the original notebook guard SHA exactly. No original baseline or fixture allowlist was modified. Palma preflight ink/pressure then passed; hardware reboot completed. Changed boot ID, exact endpoint/global flags, all seven shapes/five pressure files and unrelated reader metadata pass, with the notebook guard exact after excluding only the confirmed user addition. No login/start/pull was requested.

Evidence phase: `artifacts/gateway-lab/qualification/reader-repaired-hardware-reboot/lumi-pass-palma-staged/SHA256SUMS`. The root preflight manifest is superseded for the repeated Palma export; use the sealed phase copy. This test does not qualify fresh-installation history replay or unattended new-update delivery.

Final Palma evidence is separately sealed in `both-reboots-pass/SHA256SUMS`; the earlier 10.3 II phase remains unchanged. Both devices passed.

Later history setup found account/asset facade port 18765 absent while the replication proxy remained live and the old sessions were expired. Thus reboot evidence qualifies persistence of repaired local data/settings only; it cannot establish healthy automatic post-boot account/asset traffic. The facade was restarted separately and sessions explicitly renewed for history setup.
