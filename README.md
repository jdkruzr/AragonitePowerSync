# Aragonite PowerSync

Dedicated BOOX native-note adapter and, eventually, Rhizome sync companion for
Alexandria Server. PowerSync is a separate source dataset from the Alexandria client.

The signed product app is `:client` (`dev.aragonite.powersync.client`). It enrolls
with an Alexandria native BOOX server, configures stock ksync and restores its
previous configuration offline. Qualified firmware is API 35 with Palma2_Pro_C
ksync 32173 or Go103_2Lumi ksync 32079. It installs beside the diagnostic probe.

Build: `JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 ./gradlew :client:assembleRelease`.
Release signing requires the persistent private configuration at
`~/.local/share/aragonite-powersync/signing/client.properties`; keep that key for
future upgrades. Install in place with
`adb -s SERIAL install -r client/build/outputs/apk/release/client-release.apk`.
Grant native configuration file access, enroll using the server's single-use
code, review sync choices, then Connect. Return works without the custom server.
Never uninstall or clear data to work around a signature mismatch.

See [product results and limits](docs/product-implementation-results.md),
[application design](docs/powersync-app-design.md) and
[Alexandria server setup](../AragoniteAlexandriaServer/docs/boox-native-setup.md).
Product setup tests keep custom-server sync off; they are separate from previous
native content-transfer experiments. Rendering, general content authoring and
whole-library import are unfinished.

## Diagnostic probe

The existing `:app` remains a diagnostic Android app, not a two-way sync release.
It probes Notes/ksync Binder access, metadata providers, page callbacks and storage
from an ordinary application UID. Fixture actions operate only on the disposable text and ink notebooks journaled
by this installation. Native export, text-resource writes, and single-stroke
revision staging are implemented.
Synthetic image replacement also preserves the native image shape identity.

Build: `JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 ./gradlew :app:assembleDebug`. Android 11+; SDK 35; JDK 17.
Install: `adb -s SERIAL install -r app/build/outputs/apk/debug/app-debug.apk`.

Read-only probe: `scripts/run-lab.sh SERIAL probe`.
Read the private report with `adb -s SERIAL shell run-as dev.aragonite.powersync cat files/probe.json`.
Debug text actions: `create_fixture`, `rename_fixture`, `capture_fixture`,
`export_fixture`, `open_fixture`, `write_fixture`, `disable_fixture`.
Ink actions: `create_ink` (native creation screen), `capture_ink`, `export_ink`,
`open_ink`, `disable_ink`, `image_ink`, `stage_ink`, `stage_ink_interrupt`.
`image_ink` inserts one synthetic PNG into the tracked notebook and journals the
request before opening Notes; subsequent requests only open the notebook.
Run each with `scripts/run-lab.sh SERIAL ACTION`; it restarts only PowerSync.
Close the fixture in native Notes before normal write tests. For ink staging,
first capture the app's private `ink-metadata.json`, then generate an entirely
synthetic payload with `scripts/make-ink-fixture.py METADATA OUTPUT.zip`, and push
it to `/sdcard/PowerSyncProbe/ink-inbound.zip`. `--previous OUTPUT.zip.json`
revises the same synthetic shape. `--remove` emits its tombstone.
`stage_ink_interrupt` injects an interruption after point staging for recovery tests.
For the tracked image fixture, `scripts/make-image-update.py METADATA NATIVE.note
OUTPUT.zip` creates a four-file replacement bundle; push it to the same inbound
path and run `stage_ink`.
Writes disable native Onyx cloud sync for the tracked fixture and verify the flag.
Text creation requests the same disable immediately after observing its new row;
native creation and disabling are separate commands, leaving a brief sync window.
This does not disable cloud sync for the rest of the user's library.
Grant All Files Access before creating a fixture. One-way Binder sends are verified
by polling native rows; they are not treated as completion acknowledgements.

Tests: `python3 -m unittest discover -s tests -v` (Python 3 and JDK required).

See `docs/native-adapter.md` for wire contracts and device evidence.
See `docs/current-state.md` for runtime evidence and release gates.
See `docs/ksync-retargeting-spike.md` for the live custom-endpoint/BLIP experiment,
its teardown state, and the separate note-account replication gate.
