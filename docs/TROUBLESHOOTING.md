# Troubleshooting

## Hotkey does nothing

- Something else may own the combo (IME, voice tools, another AutoHotkey script). Change the menu hotkey in Settings.
- Confirm it’s running: Task Manager → `AutoHotkey64.exe`, or look for the tray icon.
- Check `TextTools.log` next to the script for a startup line with the PID.

## Result does not paste

- Some surfaces reject paste (read-only pages, chat bubbles). Use **Shift** + action to open the result window, then copy by hand.
- History (tray) keeps recent source + result pairs.

## API errors

- **Test API** in Settings isolates key / URL / model problems.
- An empty API key opens Settings on start on purpose.
- For local Ollama, start the server and `ollama pull` the model first.

## Stability (don’t reintroduce)

Earlier builds hit heap corruption (`0xc0000374`) with some native patterns. Keep HTTP on process **`curl.exe`**. Avoid bringing back:

- COM WinHTTP inside the AHK process for LLM calls
- Heavy PCRE on huge response bodies for JSON extraction
- Binary `ClipboardAll` for restore
- `OnMessage` hooks on activation that race the message pump

If crashes return on a clean script, try a known-good AutoHotkey build (for example 2.0.19) via a winget version pin.

## Conflicts with other hook tools

Low-level keyboard hooks don’t share well. Don’t bind TextTools and another hook-based app to the same global hotkey.
