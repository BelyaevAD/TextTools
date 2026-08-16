# Security Policy

## Reporting a vulnerability

Open a [GitHub Security Advisory](https://github.com/BelyaevAD/TextTools/security/advisories/new), or contact me privately if you’d rather not use the public tracker.

Don’t paste live API keys, tokens, or private document contents into issues.

## Secrets

- Never commit `TextTools.ini`, history, logs, or user `actions.ini` / `apps.ini` / `langs.ini`
- If a key leaks, revoke it at the provider and make a new one
- Prefer scoped keys with spending limits when the provider allows it

## Scope

TextTools is a local AutoHotkey script. It doesn’t ship a network service of its own; risk is mainly local state files and whatever you send to your chosen LLM provider.

Selected text is sent only when you run an action. Optional **app profiling** (exe name + window title) is off by default and asks before sending.

Temporary curl config files under `%TEMP%\TextTools_*` can contain the API key and proxy URL; they are deleted in `finally` and swept on startup. Do not put quotes or line breaks in the API key, Base URL, or proxy URL — they would break the curl `-K` file.

History (`TextTools_history.txt`) stores processed text. Clear it from the tray for sensitive work.
