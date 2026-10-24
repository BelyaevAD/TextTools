# User guide

## Layout

| Path | Role |
|---|---|
| `TextTools.ahk` | Entry point |
| `Lib/*.ahk` | Logic (AHK v2 includes are textual paste) |
| `lang/*.ini` | Shipped UI language packs (Russian is built into the code, so there is no `ru.ini`) |
| `lang/user/*.ini` | Your extra packs; the installer creates the folder if missing and never deletes it |
| `TextTools.ini` | Local settings including API key (**not in git**) |
| `actions.ini` / `apps.ini` / `langs.ini` | Your edits (**not in git**); factory sets live in `Lib/` |
| `TextTools_history.txt` | Last 20 operations with source text (**sensitive**, gitignored) |
| `TextTools_last.txt` | The last result verbatim (**sensitive**, gitignored) |
| `TextTools_usage.log` | One line per API call: time, label, model, token counts (no text) |
| `TextTools.log` | Event log, rotated to `TextTools.log.old` past 1 MB |
| `prices.json` | Cached OpenRouter model catalogue (context and prices) |

Install target: `%LOCALAPPDATA%\Scripts\TextTools\` via `install.ps1`. Every file above lives in that same folder — the script keeps its state next to itself, not under a separate config path.

`uninstall.ps1` stops the process, removes the `HKCU\…\Run\TextTools` autostart entry, and deletes the whole install folder — settings, history and log with it. AutoHotkey is left installed.

## First run

1. Install AutoHotkey v2
2. Run `install.ps1`, or open `TextTools.ahk` directly
3. Tray → **Settings** → API: Base URL, key, default model. Optional: tick **Proxy** and paste `http://user:pass@host:port` or `socks5h://host:port` (localhost Ollama skips it).
4. Optional: **Load from provider** / **Recommended models**
5. Select text anywhere → default hotkey `Ctrl+Alt+Space`

Optional: Settings → Interface → **Profile new apps** (off by default). When on, a new unknown window can send its exe name and title to the provider after a Yes/No prompt.

The Interface tab also holds the UI language, both hotkeys, the **Markdown render** checkbox for the result window, and the clipboard wait in seconds (clamped to 0.2–5). Tray → **Clear history…** / **Clear log…** empties the on-disk files.

To retarget factory action models after changing provider: Settings → API → **Retarget actions for this provider**.

## Settings tabs

| Tab | What is on it |
|---|---|
| API | Base URL, key, proxy checkbox, default model, the model list with context and price columns, **Load from provider** / **Recommended models** / **Test API** / **Set as default model** / **Retarget actions for this provider** |
| Output languages | 18 languages ship in the list, `ru` and `en` ticked. Ticked ones appear in the action card's **Output language** dropdown; **Add your own…** takes a code, a label and the English name that goes into the prompt |
| Interface | UI language pack, menu and repeat hotkeys, Markdown render, app profiling, clipboard wait |

Changing the UI language restarts the script so the new pack takes effect.

## Actions

- Click a button or press its accelerator (`1`–`9`, `0`, then `Q W E R T Y` — 16 keys, matching the 16 factory actions)
- Type a free-form instruction in the field (modifies any button, or runs alone)
- **Shift** + action → show the result window instead of pasting (handy when the selection isn’t editable)
- Tray → **Action editor** → buttons, prompts, apps, refine chips. If you pick a key another action already uses, choose **Cancel**, **Swap** (exchange keys) or **Take** (this action gets it, the other is left without a key).

## Result window

Actions marked “open in a window” (and anything run with **Shift**) land here instead of replacing the selection.

- **Quick:** chips from the editor's *Quick refinements* tab send a prepared instruction as the next turn of the same conversation and replace the text in place
- **Refine:** free-form follow-up, same conversation. Only the system message plus roughly the last eight turns are sent
- Follow-ups reuse the model that produced the text, not the default from Settings
- **Copy** / **Paste into window** (pastes into the original source window) / **Source** toggles between the rendered Markdown and the raw text
- If the Markdown render fails, the window silently falls back to a plain text box

## History and usage

Tray → **History…** keeps the last 20 operations (time, action, model, tokens, cost, source and result) in `TextTools_history.txt`, so it survives a restart. Source and result are stored verbatim, trimmed at 8000 characters each. **Show last result** reads `TextTools_last.txt`.

Tray → **Usage statistics** combines two things: how often each action ran (counted from `TextTools.log`) and tokens plus cost per model (from `TextTools_usage.log`).

Token counts are exact — the API returns them in `usage`. The money column is an **estimate**: prices and context come from the OpenRouter catalogue (`GET https://openrouter.ai/api/v1/models`, cached in `prices.json`, refreshed at most once a day) even when your provider is OpenAI or anything else, because there is no public price endpoint to ask. Expect it to diverge from your provider's bill, and expect a dash for models the catalogue does not list.

The report text itself is currently Russian only — it does not go through the `lang/*.ini` packs.

## Providers

Any OpenAI-compatible `…/v1` base URL works. **Base URL is an editable dropdown**: pick one of the eight providers
the app recognises, or type any other address by hand.

| Provider | Base URL |
|---|---|
| OpenRouter | `https://openrouter.ai/api/v1` |
| OpenAI | `https://api.openai.com/v1` |
| Anthropic | `https://api.anthropic.com/v1` |
| Google Gemini | `https://generativelanguage.googleapis.com/v1beta/openai` |
| DeepSeek | `https://api.deepseek.com/v1` |
| Mistral | `https://api.mistral.ai/v1` |
| xAI | `https://api.x.ai/v1` |
| Ollama | `http://localhost:11434/v1` (any non-empty string as the “key”) |

The dropdown holds exactly the addresses the provider detector knows, so **Recommended models** and the role
models of factory actions work for every entry. A hand-typed address is fine, but an unrecognised provider has
no recommended set and no role mapping — actions fall back to the default model.

**The model list belongs to the address it was loaded from.** Change the Base URL and press **Load from
provider**: the list is replaced outright and the previous ticks are cleared, because the former provider’s
models are not served by the new one — they would fail with HTTP 400 the first time an action used them.
Afterwards run **Retarget actions for this provider**. While the address is unchanged, loading merges: a model
that is ticked is never dropped, since an action may be holding it.

Optional `proxy` in Settings (checkbox **Proxy** / `proxy_on=1` in `TextTools.ini` `[api]`): `http://user:pass@host:port`, `https://host:port`, `socks5://…`, `socks5h://…`. `host:port` without a scheme is treated as HTTP. Loopback targets are not proxied. The URL is kept when the checkbox is off.

Factory defaults in code: OpenRouter base URL, `anthropic/claude-haiku-4.5`, UI language `en`, hotkey `Ctrl+Alt+Space`.

## Factory vs user state

- Code holds factory actions/apps/langs and is what **Reset to factory** restores
- INI files next to the script hold your live edits
- A corrupt INI should fall back to factory instead of killing the hotkey

## Development

```powershell
powershell -ExecutionPolicy Bypass -File .\run-tests.ps1          # logic gate
powershell -ExecutionPolicy Bypass -File .\run-tests.ps1 -Gui     # + editor/settings windows
```

`tests-logic.ahk` is the CI gate (no GUI). `tests-editor-open.ahk` and `tests-usage-and-source.ahk` stay local. Do not redirect AHK stdout/stderr.

```powershell
# syntax check (pulls in all Lib files)
& "$env:LOCALAPPDATA\Programs\AutoHotkey\v2\AutoHotkey64.exe" /ErrorStdOut /validate .\TextTools.ahk
```
