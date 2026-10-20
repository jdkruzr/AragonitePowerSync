# GitHub Actions release builds

Run **Actions → Release build → Run workflow** on `main`, or:

```sh
gh workflow run release.yml --repo jdkruzr/AragonitePowerSync --ref main
```

This manually dispatched workflow tests and lints the release variant, builds
PowerSync's signed `:client` APK with Java 17 / Android SDK 35, and verifies the
existing signing certificate before uploading it. Actions are pinned to commit
hashes. Signing runs only on this repository's `main`, with a read-only token;
there is no pull-request trigger and no automatic GitHub Release publication.

The `powersync-release-N` artifact contains `PowerSync-release.apk`, its SHA-256
checksum, source commit and signing-certificate report. Test and lint reports
are uploaded separately. The APK retains application ID and signing identity
for in-place upgrades; never uninstall to bypass a certificate mismatch.

Repository Actions secrets (never committed) are:

- `RELEASE_KEYSTORE_BASE64`: base64 of the existing release keystore.
- `RELEASE_KEYSTORE_PASSWORD`: its store password.
- `RELEASE_KEY_ALIAS`: the existing signing-key alias.
- `RELEASE_KEY_PASSWORD`: its key password.

The workflow reconstructs the same private signing configuration used locally,
then removes the key and properties file even if the build fails. Neither file
is cached or uploaded as an artifact. Keep the original local keystore backed
up; creating a replacement key would break existing installations' upgrades.

A successful CI build does not qualify a new BOOX firmware combination. In
particular, the [TUCP stale-join limitation](tucp-compatibility-20261019.md) still
applies to version 0.3.1.

## Published releases

The latest published version is [PowerSync 0.3.3](https://github.com/jdkruzr/AragonitePowerSync/releases/tag/v0.3.3),
using the unchanged signed APK from [run #3](https://github.com/jdkruzr/AragonitePowerSync/actions/runs/39464804296).
It removes firmware allowlists and fixes cloud-only Library recovery. The public
APK, source commit, package identity and signing certificate were verified;
see [verification](releases/0.3.3/verification.json). The separate stale-device
reconciliation issue remains documented in its release notes.

A successful Actions run creates build artifacts; publishing them to the
repository's Releases page is a separate step. The first published release is
[PowerSync 0.3.1](https://github.com/jdkruzr/AragonitePowerSync/releases/tag/v0.3.1),
using the signed APK from [run #2](https://github.com/jdkruzr/AragonitePowerSync/actions/runs/39224617064).

For publication, download the successful run's artifact, verify its checksum,
source commit, application version and original signing certificate, and attach
the APK and verification files to a GitHub Release targeting that exact source
commit. Publish with version-specific release notes and known limitations.
Verify the public asset download afterwards. Do not rebuild locally and silently
substitute a different binary for the CI-tested APK.
