# Architecture

TextTools 1.0.0-alpha is one JVM desktop app. This page replaces the AHK layout notes for the new line. The old tree stays on `archive/ahk`.

- `:domain` has the scenarios. It does not depend on Compose, Ktor or SQLDelight.
- `:data` is SQLite and the OpenAI-compatible client.
- `:platform` is the clipboard order and the desktop session.
- `:app` is the Compose window.

Secrets stay out of SQLite. User language packs stay INI. The model result is a markdown tree, not HTML.
