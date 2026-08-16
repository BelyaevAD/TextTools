# TextTools

**Language:** [Русский](README.md) | English

[![License: MIT](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)
[![AutoHotkey](https://img.shields.io/badge/AutoHotkey-v2-blue.svg)](https://www.autohotkey.com/)
[![Site](https://img.shields.io/badge/site-belyaevad.github.io-0f8f73.svg)](https://belyaevad.github.io/TextTools/en/)

A small AutoHotkey v2 script. Select text in any Windows window, hit a hotkey, and send it through an LLM: proofread, rewrite, summarize, make a table, explain a code snippet, and so on. The result either replaces the selection or opens in its own window.

**Site:** [belyaevad.github.io/TextTools](https://belyaevad.github.io/TextTools/en/) · [Русская версия](https://belyaevad.github.io/TextTools/)

## Screenshots

| Action menu | API settings |
|---|---|
| ![Action menu](docs/assets/screenshots/en/01-action-menu.png) | ![API settings](docs/assets/screenshots/en/02-settings-api.png) |

| Actions editor | Prompt editor |
|---|---|
| ![Actions editor](docs/assets/screenshots/en/04-actions-editor.png) | ![Prompt editor](docs/assets/screenshots/en/05-prompt-editor.png) |

| UI settings | Result window |
|---|---|
| ![UI settings](docs/assets/screenshots/en/03-settings-ui.png) | ![Result window](docs/assets/screenshots/en/06-result-window.png) |

HTTP goes through system `curl.exe` — more stable than COM/WinHTTP inside the AHK process for this.

## What it does

- Action grid with key accelerators and a free-form instruction field
- Repeat last action (`Ctrl+Shift+Space` by default)
- Button/prompt editor and “where this text came from” profiles
- Any OpenAI-compatible endpoint (OpenRouter, OpenAI, Anthropic proxies, local Ollama…)
- Optional HTTP/HTTPS/SOCKS proxy for curl (`http://user:pass@host:port` or `socks5h://…`)
- Default model plus per-action override
- Token usage and cost estimate (OpenRouter catalog)
- UI language via `lang/*.ini` packs

## Requirements

- Windows 10/11
- [AutoHotkey v2](https://www.autohotkey.com/download/) (tested on 2.0.26)
- System `curl.exe`
- An API key for your provider (or local Ollama)

## Install

```powershell
winget install -e --id AutoHotkey.AutoHotkey
git clone https://github.com/BelyaevAD/TextTools.git
cd TextTools
powershell -ExecutionPolicy Bypass -File .\install.ps1
```

Or grab a source zip from [Releases](https://github.com/BelyaevAD/TextTools/releases), extract, and run `install.ps1`.

The script copies into `%LOCALAPPDATA%\Scripts\TextTools\`, registers autostart, and launches the app. From the tray open **Settings**, set Base URL and API key (see [`TextTools.ini.example`](TextTools.ini.example)).

Without install: double-click `TextTools.ahk` if AHK v2 is already there (state files still land next to the script).

## Usage

| Action | Default |
|---|---|
| Action menu | `Ctrl+Alt+Space` |
| Repeat last | `Ctrl+Shift+Space` |
| Result in a window (no paste) | **Shift** + click / action key |

Hotkeys are configurable. `Ctrl+Space` is often taken by IMEs or other hook tools, so the default is `Ctrl+Alt+Space`.

More detail: [docs/USER_GUIDE.md](docs/USER_GUIDE.md). Common pitfalls: [docs/TROUBLESHOOTING.md](docs/TROUBLESHOOTING.md).

## Privacy

- API key only in local `TextTools.ini` (gitignored)
- A proxy URL with user/password is the same kind of secret as the key — don’t commit it
- Text goes to your provider when you run an action
- Profiling new apps (exe name and window title) is **off by default**. If you enable it in Settings, you see what will be sent before it goes
- On-disk history holds processed texts — don’t commit it; clear history and the log from the tray

## Credits

The idea of running LLM actions on selected text from any window is inspired by [Writing Tools](https://github.com/theJayTea/WritingTools) (theJayTea). Code and prompt wording here are original.

Runtime: [AutoHotkey](https://www.autohotkey.com/) (Lexikos and community).

## License

[MIT](LICENSE) — Copyright (c) 2026 BelyaevAD
