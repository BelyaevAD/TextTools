# Changelog

## v1.1.4 — 2026-11-14

- Window positions are kept when the program is upgraded. They are stored in `%APPDATA%\TextTools`, and a newer file left in the install folder is moved there once.
- The answer is pasted into the window the text was taken from, not into whichever window is in front. The "Paste into window" buttons go to the window you were last in.
- Quick refinements in the result window work. Before, they never started.
- The result window shows the new result instead of the previous text.
- Provider errors say what happened: wrong key, no funds, rate limit, not found, or provider unavailable. Before, most of them said "request failed". Replies that are valid but unusual are accepted.
- Failures and warnings appear as Windows notifications, not only in the tray tooltip. Settings stays open when saving fails.
- The program starts when a saved key or proxy cannot be read, for example after moving the profile to another computer, and says so. It sends no requests without the proxy in that case.
- "Test API" and "Load models" in Settings use the proxy from the form. They used to ignore a SOCKS proxy. An invalid proxy now stops the request instead of connecting directly, and server and proxy addresses are checked.
- Import from the AutoHotkey version keeps the proxy on when the old file had a proxy address but no switch. If you already upgraded from that version, turn the proxy switch on again in Settings.
- An empty "repeat" hotkey means no repeat hotkey. Holding the hotkey no longer queues captures, and the copy waits until Alt, Shift and Win are released.
- A clipboard held by another program is no longer treated as empty, and the program says so. A paste that cannot happen leaves the clipboard alone.
- "Retarget actions" in Settings takes effect on Save, and Cancel discards it. The action card lists the ticked models, and recommended models are stored for the provider in the field.
- The tray "Restart" item works in the installed program.
- App profiling retries with the plain model when the web-search variant is refused.
- An action no longer waits for the price catalogue download.
- Clicking the "preparing request" banner cancels the request: nothing is pasted, no window opens, and the next hotkey press works right away.
- In browsers and Electron apps the action menu opens beside the text field that holds the selection, instead of at the mouse pointer.
- On a display scaled above 100% the action menu and the result window open beside the caret instead of at an offset.
- "Paste into window" no longer freezes the result window while it pastes.
- Result window: numbered lists keep their real numbers; `#42` and `#include` are no longer headings; nested and `*` bullets render as lists; lone `*` and `__` in text are no longer swallowed. `__bold__` is no longer bold; use `**bold**`.
- Result window: a failed or empty refine request shows the reason under the refine row.
- Table rows and drop-down lists can be used from the keyboard (Tab, Enter, Space, Arrow Down).
- Enter in Settings no longer saves while the focus is on Cancel or another button.
- The "Profiling model" list shows the saved model even when the provider did not return it.
- Statistics: the cost stays after the provider changes; actions with a comma in the name are counted separately. The statistics window opens faster with a long history.
- The "Send hourly request statistics" setting is removed: it never sent anything. Statistics in the program are unchanged.
- Error reports no longer carry the computer name; the connection-test error text hides the proxy login and password.
- An existing `sentry.properties` from the old install folder is copied to `%APPDATA%\TextTools` on first start; the documentation now gives the right path.
- Autostart is written to the registry with the path in quotes.
- A second launch exits quietly instead of looking for a window by its title.

Downloads for this version are the Windows installer `TextTools-1.1.4.msi`. The Debian package and the macOS disk image are built on their own systems: `./gradlew :app:packageDeb` and `sh packaging/macos/package-dmg.sh`.

## v1.1.3 — 2026-11-13

- Settings, the database and the API key are kept when the program is upgraded. They are stored outside the install folder, and the installer copies them before it removes the previous version.
- The first start reads the old script folder (`%LOCALAPPDATA%\Scripts\TextTools`, including its UTF-16 files) when the API key is still empty.
- A hotkey with no API key opens Settings on the API tab instead of the action menu.

Downloads for this version are the Windows installer `TextTools-1.1.3.msi`. The Debian package and the macOS disk image are built on their own systems: `./gradlew :app:packageDeb` and `sh packaging/macos/package-dmg.sh`.

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
