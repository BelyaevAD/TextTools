# TextTools

**Language:** [Русский](README.md) | English

[![License: MIT](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)
[![AutoHotkey](https://img.shields.io/badge/AutoHotkey-v2-blue.svg)](https://www.autohotkey.com/)

LLM actions on selected text for Windows: select text → hotkey → action grid (proofread, rewrite, summarize, table, code explain, …) → result replaces the selection or opens in a Markdown window.

Built with **AutoHotkey v2**. HTTP goes through system `curl.exe` (more stable than COM/WinHTTP for this workload).

## Features

- Action grid with key accelerators and a free-form instruction field
- Repeat last action (`Ctrl+Shift+Space` by default)
- Action/prompt editor and per-app source profiles
- Any OpenAI-compatible endpoint (OpenRouter, OpenAI, Anthropic proxies, local Ollama, …)
- Default model plus per-action model override
- Token usage and cost estimate (OpenRouter catalog)
- UI localization via `lang/*.ini` packs

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

Or download a source zip from [Releases](https://github.com/BelyaevAD/TextTools/releases), extract, and run `install.ps1`.

The script copies files to `%LOCALAPPDATA%\Scripts\TextTools\`, registers autostart, and launches the app. Open **Settings** from the tray, set Base URL and API key (see [`TextTools.ini.example`](TextTools.ini.example)).

Without install: double-click `TextTools.ahk` if AHK v2 is installed (state files are still written next to the script).

## Usage

| Action | Default |
|---|---|
| Action menu | `Ctrl+Alt+Space` |
| Repeat last action | `Ctrl+Shift+Space` |
| Result in a window (no paste) | **Shift** + click / action key |

Hotkeys are configurable. `Ctrl+Space` is often taken by IMEs or other low-level hook tools, so the factory default is `Ctrl+Alt+Space`.

More detail: [docs/USER_GUIDE.md](docs/USER_GUIDE.md). Known pitfalls: [docs/TROUBLESHOOTING.md](docs/TROUBLESHOOTING.md).

## Privacy

- The API key lives only in local `TextTools.ini` (gitignored)
- Text is sent to your chosen provider only when you run an action
- On-disk history contains full processed texts — do not commit it; clear it for sensitive work

## Credits

The idea of system-wide LLM text actions is inspired by [Writing Tools](https://github.com/theJayTea/WritingTools) (theJayTea). TextTools code and prompt wording in this repository are original.

Runtime: [AutoHotkey](https://www.autohotkey.com/) (Lexikos and community).

## License

[MIT](LICENSE) — Copyright (c) 2026 BelyaevAD
