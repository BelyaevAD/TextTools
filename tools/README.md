# Screenshot capture

From the TextTools repo root (do **not** redirect stdout/stderr — AHK GUI hangs on pipes):

```powershell
& "$env:LOCALAPPDATA\Programs\AutoHotkey\v2\AutoHotkey64.exe" .\capture-screenshots.ahk ru
& "$env:LOCALAPPDATA\Programs\AutoHotkey\v2\AutoHotkey64.exe" .\capture-screenshots.ahk en
```

Writes PNGs to `docs/assets/screenshots/{ru,en}/`. Uses demo API settings only (no live keys). Pass `ru` or `en` so the UI language pack matches the page.

Helper: `tools/capture-window.ps1` (PrintWindow → PNG).

Static checks: `node tools/check-shadowing.mjs`, `node tools/check-cyrillic.mjs`.
English pack: `node tools/build-lang-en.mjs` (fails if a `Loc()` key has no translation).
Local suite: `run-tests.ps1` (`-Gui` for window tests).
