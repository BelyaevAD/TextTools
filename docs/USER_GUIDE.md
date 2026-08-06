# User guide

## Layout

| Path | Role |
|---|---|
| `TextTools.ahk` | Entry point |
| `Lib/*.ahk` | Logic (includes are textual paste in AHK v2) |
| `lang/*.ini` | UI language packs (shipped) |
| `TextTools.ini` | Local settings including API key (**not in git**) |
| `actions.ini` / `apps.ini` / `langs.ini` | Your edits (**not in git**); factory sets live in `Lib/` |
| `TextTools_history.txt` | Last results with source text (**sensitive**, gitignored) |

Install target: `%LOCALAPPDATA%\Scripts\TextTools\` via `install.ps1`.

## First run

1. Install AutoHotkey v2
2. Run `install.ps1` or open `TextTools.ahk`
3. Tray → **Settings** → API tab: Base URL, API key, default model
4. Optional: **Load from provider** / **Recommended models**
5. Select text in any app → factory hotkey `Ctrl+Alt+Space`

## Actions

- Click a button or press its accelerator (`1`–`9`, `0`, `Q`…`Y`)
- Type a free-form instruction in the field (modifier for any button, or alone)
- **Shift** + action → show result window instead of pasting (useful when the selection is not editable)
- Tray → **Action editor** → customize buttons, prompts, apps, quick refine chips

## Providers

Any OpenAI-compatible `…/v1` base URL works. Examples:

| Provider | Base URL sketch |
|---|---|
| OpenRouter | `https://openrouter.ai/api/v1` |
| OpenAI | `https://api.openai.com/v1` |
| Ollama | `http://127.0.0.1:11434/v1` (API key can be any non-empty string) |

Factory defaults in code: OpenRouter base URL, `anthropic/claude-haiku-4.5`, UI language `en`, hotkey `Ctrl+Alt+Space`.

## Factory vs user state

- Code holds factory actions/apps/langs and is the target of **Reset to factory**
- INI files next to the script hold your live edits
- A corrupt INI should fall back to factory, not brick the hotkey

## Development

```powershell
# syntax check (includes all Lib files)
& "$env:LOCALAPPDATA\Programs\AutoHotkey\v2\AutoHotkey64.exe" /ErrorStdOut /validate .\TextTools.ahk

# regression suite (no API key required for most checks)
& "$env:LOCALAPPDATA\Programs\AutoHotkey\v2\AutoHotkey64.exe" .\tests-usage-and-source.ahk
```
