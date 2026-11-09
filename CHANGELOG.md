# Changelog

## v1.0 — 2026-11-09

TextTools on `main` is the Kotlin and Compose Desktop application. The AutoHotkey line remains on `archive/ahk` at tag `v0.3.1`.

- Downloads for this version are the Windows installer `TextTools-1.0.0.msi` and the Debian package `texttools_1.0.0_amd64.deb` attached to the GitHub release. The macOS disk image is built on a Mac with `sh packaging/macos/package-dmg.sh`.
- The Windows installer follows the language of Windows for English, German, Japanese, Simplified Chinese and Russian. Russian system phrases that have no translation in the installer toolkit stay in English; the question about starting with Windows is translated.
- Capture and paste into other windows work on Windows. The Linux and macOS builds open the tray.
