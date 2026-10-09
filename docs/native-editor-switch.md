# Ordinary-app native notebook switching and freshness qualification

2026-10-07. Synthetic source rich-text notebook and an existing, separately
allowlisted live lab text destination only. No production deployment.
Evidence: `artifacts/gateway-lab/qualification/editor-switch/`.

## Corrected static save path

The internal EnsureCloseDocumentAction excludes the requested document, but that
is not the entire exported UI-open path. ScribbleActivity.onNewIntent loads the
new OpenNoteBean and ScribbleMainFragment.startNote explicitly calls
SaveDocumentAction(closeAfterSave=true, finishViewWhenClose=false) when a document
is already open, then opens the requested document. The same-note request can
also traverse this higher-level save path. Earlier statements about same-note
skipping are limited to the internal close action and must not be interpreted as
proof that an external UI reopen cannot save. Other top-fragment, already-open,
multiwindow and pending-open branches can behave differently.

## Delayed ordinary-app experiment

The helper validates the fixed source and allowlisted lab-owned destination,
opens the source, waits 45 seconds, and requests same-note or different-note
native activity opening without foregrounding the helper activity. Host inserts
a marker before dispatch, captures editor screenshots/logs before any helper
export, then observes an exported source graph before cold reopening and after
cold reopening. Dispatch receipts carry ordinary UID and completed=false.
Export/open UI side effects remain distinct from receipt or queue-drain proofs.

The destination is an existing disposable text notebook, not a new user note.
Its candidate identity/status/scene metadata are captured. A full before/after
destination graph comparison is not claimed; opening may itself serialize or
update native metadata. It is not silently treated as immutable.

Before the different-note edit, a fresh closed recovery graph is prepared against
the current source resource ID and body hash. After the intervening native edit,
the helper is asked to apply that earlier candidate. Expected outcome is explicit
freshness refusal before writing the incoming native body. Any previous lab
request/payload is backed up and restored; the rejected candidate may be retained
in the helper private inbox, not applied or activated.

## Validation and preservation

Fresh JDK 17 assemble/lint pass. Installed signatures match and both APK updates
use install -r. No original notebook writes, uninstall, production API or deployment.
Original reader/identity guards, accounts/endpoints and flags are verified at
restoration. Lumi's previously verified Go 6 II addition remains in its baseline.
The native asset watcher captures observed successful PUT bodies promptly; it is
not a complete private queue/branch archive. Publication and peer cold checks
follow separate explicit native restart/resume controls.

Results and closure receipts are appended after runtime qualification.

## Palma delayed-launch result

Both same-note and different-note delayed attempts hit Android background activity
launch blocking. The ordinary-app receipt contains no exception, but system logs
at the dispatch timestamp say BAL_BLOCK, result code 102. The different-note
screenshot still shows the edited source. Therefore neither background switch nor
background save/close is qualified by these marker-preservation checks. Export
and cold-open transitions occur later and can save/serialize content themselves.

The older prepared recovery is explicitly refused after the intervening edit;
its new native HTML body is absent and the edited marker remains active. Candidate
validation/private inbox retention is distinct from native application.
Later explicit restart/resume stores the source marker body and Lumi cold-applies
it. This is assisted edit propagation, not evidence of a background switch.

A separate visible-helper handoff is tested next. Its activity focus change is
explicit; any successful save cannot be attributed exclusively to onNewIntent.

## Visible helper handoff

Palma's explicit helper foreground transition is followed by an ordinary UID
native launch allowed as BAL_ALLOW_VISIBLE_WINDOW. Native logs identify the
allowlisted destination being opened, and its screenshot differs from the source.
The source's intervening edit is present in the export before source cold reopening
and in the subsequent cold export. Its exact marker body is stored and Lumi
cold-applies it. Note-sync was enabled immediately before this visible handoff;
publication spans that native save plus later explicit restart controls.
The foreground transition can itself cause native save behavior; successful
handoff is not attributed exclusively to activity onNewIntent.

This qualifies a bounded interactive handoff for the fixture, not an unattended
background save/close API, native editor lock, generic document targeting or
complete pending-work drain. Generic Android success returns are requests only.

## Final outcomes and limits

Both devices block delayed same/different activity opening from the ordinary
background helper (BAL_BLOCK/result 102), even though startActivity returns
normally. Both visible-helper handoffs are allowed, native destination opens are
logged, source edits survive export-before-reopen and cold checks, exact edited
bodies are stored, and peer cold application succeeds in both directions.

Both stale local recovery requests refuse with Active text changed since recovery
was prepared. New candidate native bodies remain absent and current edited markers
remain active. Previous lab request/payload inputs are restored exactly. Private
candidate inbox storage is not native activation.

After Lumi's native foreground edit advances a server commit, an admin REST PUT
of the older exact commit document/revision is rejected with HTTP 409. The current
commit document remains identical. This qualifies the lab Gateway REST revision
precondition for one commit. It does not provide an atomic multi-document graph
transaction or freeze private editors, nor block native conflict replication.
Local resource/body checks also remain subject to a concurrent-edit race after
checking; they are not a device lock.

In the earlier assisted windows, Palma stored its marker and Lumi cold-applied it;
Lumi's zero-upload restart window did not deliver its marker. Enabling note-sync
immediately before the later visible handoff produced exact stored marker bodies
on both devices; this suggests explicit save/publication scheduling matters, but
does not identify every earlier stalled task's cause. Stored marker PUT timing
is recorded relative to handoff and subsequent explicit restart, without claiming
unattended scheduling or complete queue drain.

The facade can support an explicit interactive handoff plus fresh export/hash
preconditions, server revision checks and per-device cold application receipts.
Unattended editor readiness, exclusion, generic multiwindow behavior and complete
pending-work drain remain gates. No extra accessibility/root/device-owner or
overlay permission was added to make the experiment pass.

## Closure

Original account/endpoint/flags, notebook identities/status/ownership and full
reader metadata pass restoration. Lab services are stopped and retained state
remains available. The watcher escrowed 73 successful source-fixture asset PUTs
and 14 text bodies with zero errors. All 325 private evidence files pass the
SHA256 manifest check. Background and foreground tested APKs are preserved
separately; build/lint and matching-certificate in-place upgrade evidence is
retained. No native APK patch or additional privilege was used.
