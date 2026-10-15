# Translating PowerSync

PowerSync's text is in standard Android string resources. You don't need to read
any Java to translate it.

- English source: `client/src/main/res/values/strings.xml`
- Spanish: `client/src/main/res/values-es/strings.xml` (first draft by Claude,
  2026-10-15; corrections very welcome)

## Correcting a translation

Edit the text between the tags in `values-<locale>/strings.xml`. Keep:

- placeholders exactly: `%1$s`, `%2$d`, `%d` (reorder them freely);
- `\n` line breaks and `\'` / `\"` escapes;
- product names untranslated: PowerSync, Alexandria, BOOX, ksync.

Strings marked `translatable="false"` in the English file are not translated.

## Adding a language

1. Copy `values-es/strings.xml` to `values-<code>/strings.xml` (for example
   `values-fr`, `values-pt-rBR`) and translate every entry.
2. Plurals: give every quantity your language uses (`one`, `few`, `many`,
   `other`, …). Lint reports missing ones.
3. Build: `./gradlew :client:lintRelease`. It fails if any string is missing,
   so a partial translation can't ship by accident.

The new language appears automatically in Android's per-app language settings.
