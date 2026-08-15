# Changelog

## v0.2.0 — 2026-08-15

Reliability, privacy, and a diagnosable CI gate.

- Factory actions store a stable role (`mech` / `main` / `deep`) and resolve it to a provider slug on seed/reset; Settings can retarget the live set
- HTTP status from curl `dump-header`, human 401/402/404/429/5xx messages; retries only on curl 6/7
- App profiling is opt-in (off by default) with a preview of exe + window title
- Temp curl files with the API key are always deleted; log rotation; history/log clear from the tray
- CI: `/ErrorStdOut` on stderr, job timeout, `tests-logic.ahk` as the gate; `run-tests.ps1` and `uninstall.ps1`
- User language packs in `lang/user/` survive install

## v0.1.0 — 2026-08-06

First public release.

- AutoHotkey v2 script for Windows
- Action grid, Markdown result window, action editor, app profiles
- OpenAI-compatible API via system `curl.exe`
- Per-action models, `lang/*.ini` packs, usage/cost estimates
- Prompts and code under MIT
