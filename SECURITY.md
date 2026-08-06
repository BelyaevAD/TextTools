# Security Policy

## Reporting a vulnerability

Please open a [GitHub Security Advisory](https://github.com/BelyaevAD/TextTools/security/advisories/new) or a private contact if you prefer not to use the public issue tracker.

Do **not** paste live API keys, tokens, or private document contents into issues.

## Secrets

- Never commit `TextTools.ini`, history, logs, or user `actions.ini` / `apps.ini` / `langs.ini`
- If an API key leaks, revoke it at the provider and create a new one
- Prefer scoped keys with spending limits when the provider allows it

## Scope

TextTools is a local AutoHotkey script. It does not include a network service of its own; risk is mainly local state files and whatever you send to your chosen LLM provider.
