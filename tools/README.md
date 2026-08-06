# Screenshot capture

From the TextTools repo root (do **not** redirect stdout/stderr — AHK GUI hangs on pipes):

```powershell
& "$env:LOCALAPPDATA\Programs\AutoHotkey\v2\AutoHotkey64.exe" .\capture-screenshots.ahk
```

Writes PNGs to `docs/assets/screenshots/`. Uses demo API settings only (no live keys).
Helper: `tools/capture-window.ps1` (PrintWindow → PNG).
