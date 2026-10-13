# Changelog

## Unreleased

Fixes to the quality gate itself — it had been unable to fail.

- **CI gate could not fail.** The `Static checks` step ran two checkers in one PowerShell step, so only the last exit code reached the job. `check-shadowing` had been failing — and printing the failure into the log — since 2026-08-16 while every run stayed green. Each check now verifies `$LASTEXITCODE`; `run-tests.ps1` did not check them at all and now does.
- **Checkers were blind on CRLF.** `check-shadowing` and `check-cyrillic` now read lines with `split(/?
/)`. With a CRLF working tree the `/;.*$/` comment strip never matched (`.` does not match `` in JS), so comment text was parsed as code — the source of the false `less()` report.
- **`check-shadowing` understands callable parameters.** `SortBy(arr, less)` may call `less(a, b)`, and only inside its own body.
- The API key is escaped with `CurlQuote` before it goes into the curl config, like the URL and the proxy already were.
- Editor card dialogs are checked for overlapping controls; auto-sized windows grow instead of clipping, so overlap is the failure mode worth asserting. The "dialog opened" checks now verify the window exists instead of asserting `true`.

## v0.2.3 — 2026-08-23

If a menu key is already taken, the action card offers **Cancel / Swap / Take** instead of a dead-end warning. Swap exchanges the two accelerators; Take assigns the key here and leaves the other action without one.

## v0.2.2 — 2026-08-16

Proxy field is gated by a checkbox; API/UI hint text no longer overlaps neighbouring labels.

## v0.2.1 — 2026-08-16

Optional HTTP/HTTPS/SOCKS proxy for API calls via curl (`api.proxy` in Settings). Localhost (Ollama) stays direct.

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
