# Changelog

## v0.3.0 — 2026-10-16

A full audit of the code, the tests and the documentation. The quality gate itself turned out to be the largest defect: it could not fail, and the checkers it ran were partly blind.

### Fixed — data and behaviour

- **A click on a list header silently acted on the wrong record.** Six ListViews lacked `NoSort`. AHK sorts rows on a header click by default while rows are mapped to the backing arrays by index, so after one click Delete / Move up / "Set as default model" / "Open result" operated on a different row — and persisted it. Affected the settings tabs, the action editor and the history window.
- **Settings could not be opened when Settings held the error.** With an unusable proxy scheme in `TextTools.ini`, refreshing the price catalogue threw out of the thread that opens Settings and Statistics — the only place where the proxy could be corrected.
- **Statistics stopped opening permanently** when an action name contained a pipe: the usage log is pipe separated, the label was not escaped, and parsing threw.
- **The app could start with no tray icon and no hotkeys** when the first run could not write its state files: the initial write sat outside any guard, in the auto-execute section.
- An empty model response was treated as a result — it overwrote the selection, went into history and was reported as success.
- A busy clipboard killed the action thread after the clipboard had been cleared, leaving the user's clipboard empty for good.
- The busy flag was released before the result was delivered, so a hotkey pressed during the paste could swap in a different selection.
- Deleting every quick-refine button did not survive a restart.
- Duplicating a factory action kept the original id: the copy was indistinguishable in a non-Russian UI and was retargeted as a factory action.
- "Show last result" and "Usage statistics" answered with a tray tooltip when they had nothing to show — indistinguishable from a dead menu item.
- The API key went unescaped into the background app-profiling curl config, and that request left temp files behind when it failed.
- Context length could be read from the neighbouring catalogue field when a model declared none.

### Fixed — the gate

- **CI could not fail.** Two checkers ran in one PowerShell step, so only the last exit code reached the job: `check-shadowing` had been failing — and printing the failure into the log — since 16 August while every run stayed green.
- **The checkers were partly blind.** An optional brace in the declaration pattern made a bare call register as a declaration; comments were stripped before string literals, so a `;` inside a string truncated the line; shadowing of global *variables* — the very example in the file's own header comment — was never checked; the `` `" `` escape form broke literal parsing; the builtin list was missing `CoordMode` and others.
- `build-lang-en.mjs` was documented as a check but ran nowhere. It now runs in CI and locally and fails if the committed pack is out of date.
- The two window suites were validated but never run in CI, leaving their assertions outside the gate.
- A suite that produced no report at all counted as "no failures".
- One localisation key served two different texts, so one of its call sites showed the wrong English string.

### Tests

167 → 406 assertions across the three suites.

### Documentation

`SECURITY.md` now says where the key is stored in plain text, that clearing history leaves the last result on disk, and that temp files hold the processed text. `USER_GUIDE.md` gained the output-languages tab, the result window, history and the usage report. Both READMEs gained uninstall instructions. Both site pages carry the same structured data and state that the cost figures are an estimate, not billing.
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
