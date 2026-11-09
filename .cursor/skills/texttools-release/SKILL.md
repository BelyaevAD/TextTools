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

## Notes and pictures

- `CHANGELOG.md` on `main` matches `archive/ahk`: `## vX.Y — YYYY-MM-DD`, then what changed for the user. Leave the AutoHotkey history on `archive/ahk`.
- README images are three shots of the current app (menu, settings, result) under `docs/assets/screenshots/en/`. Do not use `gh-pages` AutoHotkey shots. Do not publish a shot that shows a key, a proxy password, or a DSN.
- Release text links only the assets just built.

## Git

- Commit the changelog and README before moving the tag. `git tag -f -a vX.Y` on that commit, then push `main` and the tag. Tags `v0.1.0`–`v0.3.1` stay. Do not force-push `main`.
- The site is a separate commit on `gh-pages`: `index.html` and `en/` change together (title, description, download links, screenshots).
- Keep `.cursor/` ignored except this skill and `.cursor/rules/texttools-release.mdc`.

## Installer languages

`packaging/windows/main.wxs` uses `!(loc.ProductLanguage)`, `!(loc.AutostartDlgBody)` and `!(loc.AutostartCheck)`. Each `l10n-*.wxl` sets `ProductLanguage`, `AutostartDlgBody` and `AutostartCheck`. jpackage already passes the stock English, German, Japanese and Chinese files, so those four files add only the three strings. `ru-ru` (LCID 1049, code page 1251) also repeats the English stock strings, because that culture is linked on its own and a missing string fails light. Untranslated Russian phrases stay in English. Names stay `l10n-*.wxl`, not `MsiInstallerStrings_*.wxl`.
