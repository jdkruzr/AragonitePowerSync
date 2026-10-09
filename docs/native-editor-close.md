# Ordinary-app native editor save/close qualification

2026-10-07. Fixed disposable rich-text notebook only. No production coordinator
or deployment. Evidence: `artifacts/gateway-lab/qualification/editor-close/`.

## Static interface

Both firmware receivers recognize `com.onyx.action.QUIT_NOTE`. The quit event
routes through native editor/menu Back handling into SaveDocumentAction with
closeAfterSave=true. Text-shape saving and pushContent default enabled; final
publication request is conditional on close/save, dirty state and pushContent.
Lumi explicitly registers this dynamic receiver exported; the older dump uses
legacy receiver registration without an explicit sender permission.

Neither inspected Notes Binder offers direct editor-open/dirty state or
save-and-close RPC. NoteStatus is process-local; our SDK copy cannot query Notes
state. Opening a different notebook invokes internal closing, but internal
NoteDocument.close alone does not prove text-save execution. QUIT_NOTE is the
stronger candidate. See the captured source manifest and static findings.

## Probe and observation boundaries

NativeEditorCloseLab guards the lab cluster and fixed selected notebook, opens
the native editor, then dispatches a package-targeted broadcast after 45 seconds
from the ordinary helper UID. A matching no-op has identical opening/timer flow.
At the delayed boundary no helper activity is launched. Host records the native
foreground window, screenshot, request receipt and subsequent cold active export.
The dispatch receipt explicitly says completed=false and does not pretend to
verify editor identity. Host fixture observation is separate. A delayed callback
can be killed or delayed; missing/late dispatch is unresolved, not save failure.

The broadcast carries no target document identifier. This is a narrowly scoped
lab action, not a safe general recovery button. Multiple windows, background
editors, selection changes and absence of an active editor remain unqualified.
The no-op cold check changes focus and reopens/exports the note; its marker
survival must not be presented as proof of pre-transition autosave.

After a quit/cold check, explicit native tree restart and controlled note-sync
resume are used to observe exact backend assets and peer cold application.
These assisted publication controls do not establish automatic queue draining.
A request, generic sync-finish notification, quiet interval or zero scoped
LocalRecordModel rows is insufficient proof that all native work has settled.

## Preservation and validation

Probe build and lint pass freshly under JDK 17. Initial default JDK 25 build
failed before compilation; the corrected log is retained. APK signer certificates
match each installed app, and both upgrades use install -r, preserving app data.

Lumi setup initially refused a changed original-library guard. Exact comparison
proved a pure one-row addition: removing that row reproduces the prior sorted
identity/status/ownership hash. The user confirmed a Go 6 II cloud sync addition.
The original baseline, added row and updated baseline proof are preserved; no
existing original row changed or was removed. Reader guards remain separate.

## Results

Per-device request, active-body and peer results are recorded in results.json.
Completed outcomes and closure checks are appended below after observation.

### Palma observed result

The delayed broadcast dispatched from ordinary UID 10216 with the rich-text
editor open. Five seconds afterward the same ScribbleActivity/editor and keyboard
remained visible. At its dispatch timestamp, Notes logged No subscribers registered
for QuitScribbleEvent. The no-op likewise left the editor open. Therefore ordinary
broadcast delivery passes, but native rich-text save/close through this command
fails in this fixture. Surviving markers after later cold checks are not evidence
that the broadcast saved them.

After the independent cold-check/editor transitions and explicit native tree
restart, the server stored an exact marker-containing body and Lumi cold-applied
it. That is assisted native edit propagation, not a successful broadcast flush.

Lumi static sources explain a candidate routing mismatch: the receiver posts on
the global event bus; CommonEventHandler inherits registerGlobalEventBus=false
and does not override it. Its quit subscriber is therefore not automatically a
global subscriber. This supports the runtime negative; do not generalize it to
all modes, firmware builds or other editor lifecycles.

### Lumi observed result

Ordinary UID 10133 dispatches the same request. Notes again logs no subscribers
for QuitScribbleEvent at that timestamp; the editor and keyboard remain visible.
The no-op is likewise open. Both builds therefore pass ordinary-app delivery but
fail this command's rich-text editor close. No full pending-work drain or safe
server-recovery barrier follows from this broadcast. Subsequent cold/assisted
publication results are separately captured and must not be attributed to it.

Next: investigate document-scoped native save/open transitions rather than the
global quit broadcast. Runtime state/dirty detection, a true editor exclusion
mechanism, intervening-edit freshness refusal and unattended retry remain gates.

### Assisted publication asymmetry

Palma's later restart window stored 18 fixture assets including the exact new
marker body, and Lumi cold-applied its marker. Lumi's corresponding 30-second
restart window stored zero new fixture assets and Palma did not cold-apply its
marker. Both Lumi local cold exports retained their markers with complete resource
references. Local save/application and explicit tree restart therefore remain
insufficient publication receipts in this case. Native Back is tested separately
as an ADB-driven UI positive control, not an ordinary-app API.

### Native Back positive control passes

With Lumi's fixture open and edited while note-sync remained paused, we enabled
note-sync without foregrounding the helper, dismissed the keyboard and tapped
native toolbar Back. The editor closed. A 30-second delivery window stored an
exact fresh-marker body, and both source and receiver cold-opened it with complete
resource references. Their selected active body SHA256 values are identical;
both earlier quit-case markers also survive. This is an ADB-driven native UI
control, not a newly qualified ordinary-app flush API or complete queue drain.

The facade must not use QUIT_NOTE as a recovery readiness acknowledgement.
Native Back is a known save/publication route for this fixture; discovering a
safe ordinary-app equivalent and verifying readiness/freshness remain next.

### Evidence closure

The watcher captured 58 successful native fixture asset PUTs, including 13 text
bodies, with zero escrow errors. Runtime observations match the unhandled native
quit event to each ordinary-app dispatch within one second. Source captures,
APK/code/build logs, delayed receipts, screenshots, active archives and exact
stored bodies are retained privately. Final restoration and preservation reports
and service closure are recorded alongside the SHA256 evidence manifest. Lab
services are stopped with disposable backend state retained after verification.
