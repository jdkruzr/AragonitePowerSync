# Go 6 II enrollment interruption — 2026-10-19

Read-only investigation after the owner attempted enrollment with PowerSync 0.3.2.
The current screen reported `A pre-transition book is missing; preserve the
baseline and inspect before continuing`.

## Findings

- Server enrollment/profile is saved and the installed custom cluster matches.
  The transition journal is `failed`, native account is signed out, and both
  notebook and Reader sync flags are false. Native sign-in has not completed.
- Baseline Reader catalog: **128 rows**. Current catalog: **24 rows**. The
  **104 missing UUIDs** all have cloud references. Of those, **103** currently
  have no file at `nativeAbsolutePath`; one file still exists. Alternate
  `nocasePath` checks also find only one file. All Files Access is granted.
- Baseline notebook fingerprint still matches the live identity/type/status/owner
  projection. This is not a full notebook-content comparison.
- `connect()` switches the native cluster, then calls `repairMissingCatalog()`
  before requesting native sign-in. That repair checks `File.isFile()` for every
  absent catalog row and aborts on the first missing file, before restoring it.
- `restore()` calls the same repair method, so Return to previous server is also
  exposed to this failure. No retry, restoration or native write was attempted
  during this diagnosis.

The defect is treating every saved catalog entry as requiring a downloaded local
file. Cloud references can exist without one. The baseline preserves complete
catalog rows but does not capture prior file availability, so it cannot establish
whether any of these files disappeared during switching. **Do not describe the
103 unavailable paths as confirmed newly deleted books or confirmed previously
undownloaded books.** The observations are consistent with cloud/catalog entries
without local downloads.

Repair should preserve catalog metadata independently of file availability,
record pre-transition file availability for future loss detection, retain
UUID/ID collision and exact readback checks, and support recovery of this already
interrupted transition. This investigation does not implement that repair or
claim enrollment completion. The saved baseline remains intact.

## Repeatable diagnostic

The release-signed, product-UID read-only instrumentation now accepts:

```
adb -s SERIAL shell am instrument -w -e enrollmentDiagnostic true \
  dev.aragonite.powersync.client.test/dev.aragonite.powersync.client.NativeReadCompatibilityTest
```

It emits selected state flags and aggregate catalog/file observations, never
account IDs, enrollment secrets, titles or raw provider rows. Without the flag,
it retains the existing two read/service checks. The disposable test package was
removed after use; the product app remains installed at 0.3.2/code 13.
Private local evidence: `artifacts/go6-enrollment-20261019/` (ignored).
