# User guide

## Layout

| Path | Role |
|---|---|
| `TextTools.ahk` | Entry point |
| `Lib/*.ahk` | Logic (AHK v2 includes are textual paste) |
| `lang/*.ini` | Shipped UI language packs |
| `lang/user/*.ini` | Your extra packs (installer does not recreate this folder) |
| `TextTools.ini` | Local settings including API key (**not in git**) |
| `actions.ini` / `apps.ini` / `langs.ini` | Your edits (**not in git**); factory sets live in `Lib/` |
| `TextTools_history.txt` | Recent results with source text (**sensitive**, gitignored) |

Install target: `%LOCALAPPDATA%\Scripts\TextTools\` via `install.ps1`.

## First run

1. Install AutoHotkey v2
2. Run `install.ps1`, or open `TextTools.ahk` directly
3. Tray → **Settings** → API: Base URL, key, default model
4. Optional: **Load from provider** / **Recommended models**
5. Select text anywhere → default hotkey `Ctrl+Alt+Space`

Optional: Settings → Interface → **Profile new apps** (off by default). When on, a new unknown window can send its exe name and title to the provider after a Yes/No prompt.

Clipboard wait is also on that tab. Tray → **Clear history…** / **Clear log…** empties the on-disk files.

To retarget factory action models after changing provider: Settings → API → **Retarget actions for this provider**.

## Actions

- Click a button or press its accelerator (`1`–`9`, `0`, `Q`…`Y`)
- Type a free-form instruction in the field (modifies any button, or runs alone)
- **Shift** + action → show the result window instead of pasting (handy when the selection isn’t editable)
- Tray → **Action editor** → buttons, prompts, apps, refine chips

## Providers

Any OpenAI-compatible `…/v1` base URL works. Examples:

| Provider | Base URL sketch |
|---|---|
| OpenRouter | `https://openrouter.ai/api/v1` |
| OpenAI | `https://api.openai.com/v1` |
| Ollama | `http://127.0.0.1:11434/v1` (any non-empty string as the “key”) |

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
