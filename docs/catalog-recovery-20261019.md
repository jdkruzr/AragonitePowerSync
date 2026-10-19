# Cloud-only Library recovery — PowerSync 0.3.3, 2026-10-19

The Go 6 II enrollment exposed a PowerSync bug: native cluster switching removed
Library rows, and PowerSync refused to restore a saved row without a local book
file. Synced metadata for undownloaded books is normal BOOX Library behavior.

## Change

`CatalogRecovery` restores missing UUIDs from the saved catalog independently of
local files. Existing entries (including newer metadata) are left intact. It
validates saved/live identities and all occupied-ID conflicts before insertion,
rechecks current state before each write and verifies the exact result afterward.
It never deletes or replaces live catalog rows. Retrying after an interrupted
insert restores only the still-missing entries. Native adapter insertion rechecks
both UUID and ID immediately before using the existing content provider.

New connection baselines record local file availability separately from catalog
membership. Availability changes are recorded in the transition journal and
logged as aggregate diagnostics. They do not prevent catalog restoration or
create another download requirement. Older baselines, including this Go 6 II's,
remain usable without an invented pre-transition file-availability history.

The attention screen offers **Resume connection** when an interrupted transition
has the enrolled cluster selected, matching installed configuration, a baseline,
storage permission, and either no active account or the matching enrolled account.
It does not offer continuation during restoration, on another server/account, or
during an interrupted endpoint update. Submission rechecks live status, saved
profile/journal identities and marker ownership. It pauses sync, restores the
catalog, completes native sign-in if needed, rechecks preservation, and enables
both sync domains. It never redeems another enrollment code, repeats the cluster
switch, or overwrites the original baseline. Failed retries retain that baseline.
A process loss around the switch request is recoverable when live state confirms
the switch actually completed. Opening/upgrading the app does not auto-resume.

Both forward connection and Return to previous server use the fixed catalog
restoration. The latter's account-recovery behavior is otherwise unchanged.
English and Spanish strings cover the new action, explanation and identity error;
the obsolete missing-book refusal was removed. No Server or firmware changes.

## Verification

- Fresh release tests: **53 passed**, zero failures/errors/skips. Regression
  fixtures cover undownloaded books and exact cloud/download/progress/null fields,
  unchanged existing/unrelated rows, late ID collisions before any write,
  duplicate identities, interruption after every insert, provider readback changes,
  concurrent edits, separate file availability and legacy baselines. Continuation
  policy tests cover interrupted phases, matching/foreign accounts, changed
  configuration, wrong cluster, permissions, rollback and settled states.
- Release lint: **0 errors, 12 existing warnings**; release and instrumentation
  assembly pass. Android read/service instrumentation passes both existing checks.
- Go6_2 / Android 13 / ksync 32198 upgraded in place with the existing signing
  certificate: `f61b114923bef983aa12e876ea12bbc52f017363ef7103a96c2f978dc19aeb35`.
- Before recovery: phase `failed`, signed out, both sync flags false; 128 saved
  catalog rows, 24 live rows, 104 missing UUIDs and no occupied missing IDs.
  Of the missing entries, 103 referenced unavailable local files.
- Exercised **Resume connection** through the ordinary app UI. It completed native
  sign-in and showed **Connected to Alexandria** with both sync domains enabled.
- After recovery: phase `active`, account identity and configuration match;
  **all 128 original UUIDs present**, 140 total live catalog rows as synchronization
  proceeds. Saved catalog digest is unchanged:
  `89e8ac53d77a809931e64178e2ef23ed71417575226eaf2ba068a1b251b2575d`.
  The original notebook identity/type/status/owner fingerprint still matches.
- Verification does not claim that all remote book files have downloaded or that
  every cloud source is reachable. The count of unavailable files in the diagnostic
  is scoped to missing catalog rows, not all books; zero missing rows does not mean
  all books are downloaded. Full file and note-content synchronization is separate.

Private evidence, before/after diagnostic summaries and previous/current APKs are
retained in ignored `artifacts/go6-catalog-recovery-20261019/`. The disposable test
package is removed after verification. The live baseline remains in the app's
original encrypted state, with the original signing identity/data preserved.
This records a local signed build and installation, not publication of a new
GitHub Release.
