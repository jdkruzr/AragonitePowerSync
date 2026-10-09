# Reader delivery while the receiver is at Home

2026-10-08. Owned clean-v2 PDF; baseline 17 native shape IDs and 11 pressure files per device. Evidence: `artifacts/gateway-lab/qualification/reader-native-background-delivery/`.

Palma user drew R left of Z and left the document open. Its pen toolbar was visibly open; the driver sent Back to exit the toolbar, then another Back one second later to close/save. 10.3 II stayed at Home for 120 seconds. Native sessions had over ten minutes remaining; Reader sync was enabled and Notes sync disabled. No fault, helper call, native login, SDK pull, host publication or source reopen occurred in that observation window.

The receiver's running ksync downloaded the new book data in the background and notified Reader. Reader explicitly logged "disable pull, document not open." This qualifies Home/background operation with an existing Reader process, not force-stopped Reader, screen-off, rebooted, or OS-killed delivery. Transport/cache availability and native ink import are distinct.

A separately journaled ordinary Android VIEW intent opens the same owned PDF, followed by 60 seconds without helper/SDK import. Final exports and preservation verification follow the native-open window. Results are appended after verification.

The helper's read-only capture guard gains only the exact newly saved owned R PDF SHA256, validated after native save; source upload and recovery write guards are not broadened. Build/lint, signature-matched in-place installation and native export verification occur after both observation windows.

Result: PASS for background ksync storage followed by ordinary foreground Reader application. Two native PUTs succeed, with the last about 1.25 seconds after the timed source Back command; the first peer GET begins about 2.78 seconds after it. Both peer GETs succeed during the Home window. Reader import is explicitly skipped while the document is closed. On ordinary native open about 123 seconds later, needPull=true and native import completes roughly 1.35 seconds after the open intent; R renders in the pre-helper screenshot. Source native handoff occurs after the Back command, so command-relative timings are not claimed as exact native-save durations.

Final native exports on both devices contain 18 shapes and 12 exact pressure files. The one new R record is semantically identical across devices; every earlier record matches its own device baseline, excluding only local UPDATEDAT/OBJECTID. All previous pressure payloads match and all 12 pressures match across devices. Unrelated Reader rows and original notebook guard pass, excluding only the already confirmed Palma user-created notebook. No native SDK pull or host republication was needed.

Source and receiver save distinct exact annotated PDF derivatives. The initial receiver export guard rejects its new revision before native export; the read-only guard is then extended with that exact owned SHA256. Both helper builds/lint pass, and installs preserve signatures/data. The case retains original and updated APKs, native archives, server/native request journals, screenshots, drivers and checksum manifest.

Facade implication: report resources received/cached separately from ink applied. Home-state cache delivery is sufficient for automatic import on subsequent native open in this fresh-timestamp case. It does not prove screen-off delivery, stopped Reader behavior, or repair of the older timestamp-gate exhaustion case.
