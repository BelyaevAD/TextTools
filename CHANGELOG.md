# Changelog

## v1.1.2 — 2026-11-13

- Hotkeys are registered when the app starts. Opening settings and pressing Save is not required.
- An older settings file is read as before and written back in the current form. Values already set are kept.
- Settings shows the app version beside the Save button.
- If the result cannot be pasted, the previous clipboard text is put back. The result stays in its window.

Downloads for this version are the Windows installer `TextTools-1.1.2.msi`. The Debian package and the macOS disk image are built on their own systems: `./gradlew :app:packageDeb` and `sh packaging/macos/package-dmg.sh`.

## v1.1.1 — 2026-11-12

- The action menu opens beside the input field when the selection is in a field, and beside the cursor when the selection is on a page. The side nearest the cursor is used when it stays on screen and does not cover that point.
- The result is pasted back into that field when the field can take text. A page selection opens the result window beside the cursor.
- The progress window names the application when that application has a prompt note.
- A model call waits up to 180 seconds for the response.
- The saved hotkeys are registered. Ctrl+Alt+Space and Ctrl+Shift+Space are used when Windows refuses the saved menu hotkey.

Downloads for this version are the Windows installer `TextTools-1.1.1.msi`. The Debian package and the macOS disk image are built on their own systems: `./gradlew :app:packageDeb` and `sh packaging/macos/package-dmg.sh`.

## v1.1 — 2026-11-11

- The action menu opens on the window where the text was selected. The mouse is used only when that window cannot be found.
- The progress window stays on that window's screen. While the request runs it shows an estimated token count and an approximate cost. When the answer arrives it shows the provider's token counts and the cost.
- A new application is recognised from its program file, window title and window class. With profiling on, TextTools looks the program up on the web and stores a prompt note for later actions. A lookup that failed can be tried again. If the model cannot identify the program, that answer is kept.
- The action editor shows the built-in application list until you save your own.
- The action, application and quick-refinement cards are taller, so the text field and the buttons stay on screen. A card that was saved smaller opens at this height.
- The Quick refinements tab says where those buttons are used: in the result window, on the Quick row, after an action opens its result there.

Downloads for this version are the Windows installer `TextTools-1.1.0.msi`. The Debian package and the macOS disk image are built on their own systems: `./gradlew :app:packageDeb` and `sh packaging/macos/package-dmg.sh`.

## v1.0 — 2026-11-09

TextTools on `main` is the Kotlin and Compose Desktop application. The AutoHotkey line remains on `archive/ahk` at tag `v0.3.1`.

- Downloads for this version are the Windows installer `TextTools-1.0.0.msi` and the Debian package `texttools_1.0.0_amd64.deb` attached to the GitHub release. The macOS disk image is built on a Mac with `sh packaging/macos/package-dmg.sh`.
- The Windows installer follows the language of Windows for English, German, Japanese, Simplified Chinese and Russian. Russian system phrases that have no translation in the installer toolkit stay in English; the question about starting with Windows is translated.
- Capture and paste into other windows work on Windows. The Linux and macOS builds open the tray.
- The Windows build starts. The runtime includes `java.sql` and `java.net.http`, which the database driver and the settings API check need. The Debian package on the v1.0 release includes the same modules. The installer can create a desktop shortcut and a Start menu shortcut, start with Windows, and launch the app when setup finishes. The license shown is the MIT text from this repository, and the about link is https://github.com/BelyaevAD/TextTools.
