# TextTools — LLM text actions for selected text (AHK v2)

**Script:** [https://github.com/BelyaevAD/TextTools](https://github.com/BelyaevAD/TextTools)  
**Requires:** AutoHotkey v2 (tested 2.0.26), Windows, system curl.exe  
**License:** MIT

Select text anywhere → hotkey (default `Ctrl+Alt+Space`) → action grid (proofread, rewrite, summarize, table, code explain, …) → replace selection or open a Markdown result window.

Highlights:

- OpenAI-compatible APIs (OpenRouter / OpenAI / local Ollama / …)
- Per-action models, action editor, per-app prompt hints
- HTTP via external curl.exe (avoids WinHTTP heap issues in-process)
- Token usage + cost estimate

Install: clone or grab the Release source zip → `install.ps1` → set API key in tray Settings.

Inspired by Writing Tools (theJayTea); prompts and code here are original.

Feedback and PRs welcome on GitHub.
