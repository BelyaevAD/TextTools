# Security Policy

## Reporting a vulnerability

Open a [GitHub Security Advisory](https://github.com/BelyaevAD/TextTools/security/advisories/new), or contact me privately if you’d rather not use the public tracker.

Don’t paste live API keys, tokens, or private document contents into issues.

## Secrets

- The API key is stored as **plain text** in `TextTools.ini`, key `api_key` under `[api]`, in the script's own folder — `%LOCALAPPDATA%\Scripts\TextTools\` after `install.ps1`, or next to `TextTools.ahk` when you run it in place. There is no encryption and no OS credential store; anything running as your user can read it
- A proxy URL with user and password sits in the same file (`api.proxy`) and is the same kind of secret
- Never commit `TextTools.ini`, history, logs, or user `actions.ini` / `apps.ini` / `langs.ini`
- If a key leaks, revoke it at the provider and make a new one
- Prefer scoped keys with spending limits when the provider allows it

## Scope

TextTools is a local AutoHotkey script. It doesn’t ship a network service of its own; risk is mainly local state files and whatever you send to your chosen LLM provider.

Selected text leaves the machine and goes to whatever Base URL you configured, as the body of a `POST …/chat/completions`, every time you run an action or a follow-up — there is no local-only mode. Optional **app profiling** (exe name + window title) is off by default and asks before sending. The OpenRouter price catalogue is fetched without a key and carries none of your text.

Temporary files under `%TEMP%\TextTools_*` hold the API key and proxy URL (the curl `-K` config) and the selected text with the model's reply (the request and response bodies). The main call path deletes them in `finally`, the background app-profiling call from its polling timer, and anything left over is swept on startup. Do not put quotes or line breaks in the API key, Base URL, or proxy URL — they would break the curl `-K` file.

On-disk text:

- `TextTools_history.txt` — the last 20 operations, each storing the **source text and the result verbatim** (trimmed at 8000 characters). Tray → **Clear history…** deletes the file
- `TextTools_last.txt` — the last result verbatim. **Clear history does not touch it**; delete it by hand for sensitive work (tray → *Open script folder*)
- `TextTools_usage.log` — timestamps, action labels, model names and token counts, no text. Not cleared from the tray
- `TextTools.log` — action names and character counts, not the text itself. Tray → **Clear log…**
