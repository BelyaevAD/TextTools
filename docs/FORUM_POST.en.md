# TextTools — LLM for selected text (AHK v2)

**Repo:** [https://github.com/BelyaevAD/TextTools](https://github.com/BelyaevAD/TextTools)  
**Needs:** AutoHotkey v2 (I tested 2.0.26), Windows, system curl.exe  
**License:** MIT

Select text anywhere → hotkey (default `Ctrl+Alt+Space`) → pick an action (proofread, rewrite, summarize, table, explain code, …) → replace the selection or open a Markdown result window.

Useful bits:

- OpenAI-compatible APIs (OpenRouter / OpenAI / local Ollama / …)
- Per-action models, action editor, per-app prompt hints
- HTTP via external curl.exe (avoids WinHTTP heap headaches in-process)
- Token usage + rough cost estimate

Install: clone or grab the Release source zip → `install.ps1` → set the API key in tray Settings.

Inspired by Writing Tools (theJayTea); prompts and code here are original.

Feedback and PRs welcome on GitHub.
