# Contributing

Thanks for considering a contribution.

1. Fork and branch from `main`
2. Keep changes focused; match existing AHK v2 style
3. Do not commit runtime state (`TextTools.ini`, logs, history, user `*.ini`, `prices.json`)
4. Run validation before opening a PR:

```powershell
& "$env:LOCALAPPDATA\Programs\AutoHotkey\v2\AutoHotkey64.exe" /ErrorStdOut /validate TextTools.ahk
& "$env:LOCALAPPDATA\Programs\AutoHotkey\v2\AutoHotkey64.exe" tests-usage-and-source.ahk
```

5. For prompt text changes, keep constant names (`TASK_*`, `BLOCK_*`) and the TASK/KEEP/FORM/NEVER shape so the editor and stores keep working

Discuss large UX changes in an issue first.
