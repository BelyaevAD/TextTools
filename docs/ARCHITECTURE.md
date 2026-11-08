# Architecture

TextTools 1.0 is one JVM desktop app. This page replaces the AHK layout notes for the new line. The old tree stays on `archive/ahk`.

- `:domain` has the scenarios. It does not depend on Compose, Ktor or SQLDelight.
- `:data` is SQLite and the OpenAI-compatible client.
- `:platform` is the clipboard order and the desktop session.
- `:app` is the Compose window.

| Need | Go to |
| --- | --- |
| Tables, files and setting keys | [schema.md](db/schema.md) |
| Run, prompt, keys, models, markdown | [domain.md](modules/domain.md) |
| Store, HTTP, import and prices | [data.md](modules/data.md) |
| Clipboard, hotkeys, secrets and windows | [platform.md](modules/platform.md) |
| Tray, capture and screens | [app.md](modules/app.md) |

Secrets stay out of SQLite. User language packs stay INI for import; this version's interface uses bundled XML. The model result is a markdown tree, not HTML.
