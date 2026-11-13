---
name: texttools-release
description: Cut a TextTools release. Use when publishing a version, writing the changelog, updating README screenshots, or refreshing the GitHub Pages site.
---

# TextTools release

## Rebuild first

Delete the build output and rebuild every package, even if `dist/` already contains one. A package built from an older tree must not be attached.

- Windows, from the repository root, JDK 21 and WiX 3 on `PATH`: `./gradlew clean :app:packageMsi`. `prepareLightShim` compiles `packaging/windows/LightShim.cs` and puts that `light.exe` first on the jpackage path. WiX light itself rejects a single call with every culture (`LGHT0100`); the shim builds one MSI per culture and embeds the others as language transforms, with `en-us` as the base. The verbose log must show those per-culture light calls and must not report `LGHT0311` or `LGHT0100`.
- Linux: copy the tree into Ubuntu (exclude `build`, `.gradle`, `dist`, `tmp`, `.git`) and run `./gradlew clean :app:packageDeb`.
- macOS: `sh packaging/macos/package-dmg.sh` on a Mac. If it was not run, the notes say so and do not link a DMG.

Copy the new msi and deb into `dist/`. Those files, and a DMG only when the Mac script produced one, are the only release assets.

## Version

The number has three parts, as in `1.1.123`. The changelog heading, the README version line, `packageVersion`, the installer name `TextTools-X.Y.Z.msi`, and the tag are that same number. A user-visible change gets its own heading `## vX.Y.Z — YYYY-MM-DD` and the last part increases by one. Do not batch that change into a two-part heading such as `v1.2`. Tags `v1.0` and `v1.1` stay as published. The first three-part release after `v1.1` is `v1.1.1`.

## Notes and pictures

- `CHANGELOG.md` on `main` uses `## vX.Y.Z — YYYY-MM-DD`, then what changed for the user. Leave the AutoHotkey history on `archive/ahk`. The published headings `v1.0` and `v1.1` stay two-part.
- Do not capture the screen in this agent. Follow `.cursor/skills/texttools-screenshots/SKILL.md`. README keeps the pictures it already links, and gains a picture when a new window, editor page, or settings tab is how a person uses a new feature. Refresh every catalog surface marked `readme: true`, in English and in Russian. A new window, page, or tab is appended to `docs/screenshots/catalog.yaml` by `packaging/screenshots/sync-catalog.ps1`. Set `readme: true` for that surface in the same release and show it on both GitHub Pages pages in the language of each page. An alert stays out of the README and the site. Do not use `gh-pages` AutoHotkey shots. An API key that shows only asterisks or dots may stay. Do not publish a readable key, a proxy password, or a DSN. Body text is `docs/screenshots/sample.txt`, unchanged from release to release.
- Release text links only the assets just built.

## Git

- Commit the changelog and README before tagging. `git tag -a vX.Y.Z` on that commit, then push `main` and the tag. Do not use `-f`. Tags `v0.1.0`–`v0.3.1`, `v1.0`, and `v1.1` stay. Do not force-push `main`.
- The site is a separate commit on `gh-pages`: `index.html` and `en/` change together (title, description, download links, screenshots). The Russian page links `assets/screenshots/ru/`. The English page links `assets/screenshots/en/`. The file names match. Do not point the Russian page at the English pictures.
- Keep `.cursor/` ignored except the release skill, the screenshot skill, and `.cursor/rules/texttools-release.mdc` plus `.cursor/rules/texttools-screenshots.mdc`.

## Installer languages

`packaging/windows/main.wxs` uses `!(loc.ProductLanguage)`, `!(loc.AutostartDlgBody)` and `!(loc.AutostartCheck)`. Each `l10n-*.wxl` sets `ProductLanguage`, `AutostartDlgBody` and `AutostartCheck`. jpackage already passes the stock English, German, Japanese and Chinese files, so those four files add only the three strings. `ru-ru` (LCID 1049, code page 1251) also repeats the English stock strings, because that culture is linked on its own and a missing string fails light. Untranslated Russian phrases stay in English. Names stay `l10n-*.wxl`, not `MsiInstallerStrings_*.wxl`.
