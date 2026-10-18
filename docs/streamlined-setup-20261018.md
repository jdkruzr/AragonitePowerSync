# PowerSync 0.3.0: streamlined setup and connection status

2026-10-18. Implementation branch: `feature/powersync-streamlined-setup`.
Release: version 0.3.0, version code 11, existing signing identity and package.

## Delivered behavior

- One setup flow: HTTPS address/code, file access when needed, destination/sync
  confirmation, enrollment, native connection, then both sync flags enabled and
  verified. Individually excluded notebooks/books remain excluded.
- Encrypted pending enrollment survives permission settings and process death.
  Code removal is durable before redemption; uncertain responses need a fresh
  code. A saved enrollment continues without redeeming again. Native mutation
  journals always route interruptions to attention/recovery, never automatic replay.
- Connected status depends on live server selection, native account UID/cluster,
  installed configuration and settled journals. It does not claim content sync
  completion. Upgrades, foreground entry and status refresh never change flags.
- Explicit Enable notebook and reading sync revalidates the live connection and
  verifies the resulting flags. Manage connection exposes offline return.
- Troubleshooting contains endpoint checks, audio recovery, recovery status,
  native settings and technical details. Automatic endpoint checks run separately
  from native mutations, at most once per 15 minutes per process; applying an
  update remains explicit and preserves current flags.
- Process-owned work survives Activity recreation without retaining an Activity.
  Conflicting controls are visibly disabled. Persistent status and operation
  messages are separate. English/Spanish resources cover the new flow; language
  changes clear stale notices and progress uses current-language resources.
- Existing encrypted profiles/baselines/journals are retained. No Server API,
  database migration, native protocol, Hosting or Rhizome changes.

The Server wording companion is on `feature/powersync-setup-copy` in
`/home/jtd/AragoniteAlexandriaServer-powersync-setup`. It updates setup/help/audio
recovery directions and both catalogs, retaining instructions for older clients.
It is **not deployed**; the live application tests used the existing deployed
Server. The original Server checkout and its quality branch are unchanged.

## Verification

Commands (JDK 17 for Android):

```sh
./gradlew :client:testReleaseUnitTest :client:lintRelease :client:assembleRelease
go test -count=1 ./internal/alexandria/web ./internal/boox -run 'TestCatalogsCoverEveryMessage|TestBOOXViewsRender|TestTemplateHubPageIsAnonymousAndScriptless'
```

- Android: 38 unit tests, zero failures/errors. Release lint: zero errors,
  12 existing platform/icon warnings. Signed release assembly passes.
- New policy tests cover actual-vs-journal connection health, paused/unknown flags,
  missing permission, restoration routing, initial/saved enrollment, ambiguous
  redemption, interrupted enrollment continuation, duplicate taps and delivery
  to a replacement observer. These are isolated policy/sequencing tests, **not**
  a physical first-time connection or native crash-injection campaign.
- Server: uncached catalog/translation coverage, BOOX page rendering and anonymous
  template-hub checks pass. No runtime/deployment claim is based on these tests.
- Palma: installed 0.3.0 in place over 0.2.0 after matching signing certificates.
  Before/after upgrade snapshots match exactly for cluster configuration, marker,
  account identity, both flags, 1,534 notebook inventory rows and 135 reader rows.
  Technical details confirms the saved restoration baseline is still present.
- Palma UI: connected screen, secondary navigation, restore confirmation/cancel,
  live endpoint check, Spanish, large text (1.3), Activity recreation and process
  restart pass. Original 0.85 font scale and system-default language restored.
- Lumi: certificate-matched in-place upgrade from 0.2.0 preserves cluster,
  marker, account, both flags, 1,541 notebook rows and 138 reader rows exactly.
  Connected screen, saved baseline, secondary navigation, restore confirmation/
  cancel, live endpoint check, Spanish, large text, Activity recreation and
  process restart pass. Original font scale and system-default language restored.
- Both installed APKs match the final signed release SHA-256:
  `2d0d128d6b52ea72e1cd0cce3c0c09b527261f5bbec03160d3422514829e9153`.
- A controlled pause of notebook sync remains paused across app restart and an
  in-place upgrade. Explicit Enable restores both flags to their original `true`
  values. The test preserves unrelated native configuration.
- Native production smoke: a uniquely named disposable blank notebook reaches
  the deployed Server's notebook projection through stock sync. A page change in
  the existing synthetic PDF (`b86b1a31f2d9443dbc866c9286bfce09`) publishes a new
  reading metadata revision with changed progress/extraAttributes/lastAccess.
  No SDK forced pull or server metadata injection is used. This is not new proof
  of editable-stroke round trips or complete library synchronization.
- The blank smoke notebook `PowerSync 0.3 smoke a96790c23c`
  (`b49ca0b0b6a2464cbce93c0f776e855c`) is retained for inspection. It is the
  only added Palma inventory row; final checks preserve every pre-existing row
  on both devices. The existing synthetic PDF is left at page 2 of 4.

Private evidence is under `artifacts/product-build-20261018-v0.3.0/` (ignored,
owner-only directory): snapshots, signing comparison, screenshots, provider
receipts, synthetic Server records, build logs and APK hashes. Keep private
provider output and account identifiers out of public reports.

## Remaining device qualification

- First-time connection with both defaults enabled, permission denial/process
  death during that flow, and native mutation/endpoint rollback fault injection
  need an isolated physical fixture setup. Do not switch a personal library to a
  disposable server merely to fill this coverage gap.
- Audio recovery keeps its established validation/backup path; no personal
  recording was repaired during this UI release.
- Enrollment links and QR support are deferred by agreement.
