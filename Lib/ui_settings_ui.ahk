; TextTools - вкладка интерфейса окна настроек. Подключается из TextTools.ahk.

SettingsUiTab(sg) {
    sg.Add("Text", "x25 y44", Loc("set.ui_lang", "Язык интерфейса:"))
    uiLangs := UiLangPacks()
    uiItems := []
    for p in uiLangs
        uiItems.Push(p.name " (" p.code ")")
    ddUi := sg.Add("DropDownList", "x25 y64 w280", uiItems)
    for i, p in uiLangs
        if (p.code = cfg.ui_lang)
            ddUi.Value := i
    if (!ddUi.Value && uiItems.Length)
        ddUi.Value := 1
    sg.SetFont("s9 c707070")
    sg.Add("Text", "x25 y94 w590 h36",
        Loc("set.ui_lang.hint", "Пакеты — папка lang рядом со скриптом. Свои: lang\user (установщик её не трогает)."))
    sg.SetFont("s10")
    sg.Add("Text", "x25 y136", Loc("set.hotkey", "Хоткей меню (^ = Ctrl, ! = Alt, + = Shift, # = Win):"))
    eHk := sg.Add("Edit", "x25 y156 w280 h24 -Multi", cfg.hotkey)
    sg.Add("Text", "x25 y188", Loc("set.hotkey_repeat", "Хоткей повтора последнего действия:"))
    eHkR := sg.Add("Edit", "x25 y208 w280 h24 -Multi", cfg.hotkey_repeat)
    cbMd := sg.Add("CheckBox", "x25 y244 w590 h36" (cfg.markdown ? " Checked" : ""),
        Loc("set.markdown", "Рендер Markdown в окне результата (снять, если окна роняют скрипт)"))
    cbProf := sg.Add("CheckBox", "x25 y286 w590 h36" (CfgFlag(cfg, "app_profiling") = "1" ? " Checked" : ""),
        Loc("set.profiling", "Профилировать новые приложения (отправлять имя exe и заголовок окна провайдеру)"))
    sg.Add("Text", "x25 y336 w320 h24", Loc("set.clip_wait", "Ожидание буфера обмена, сек:"))
    eClip := sg.Add("Edit", "x345 y334 w80 h24 -Multi", CfgFlag(cfg, "clip_wait", "0.8"))
    sg.SetFont("s9 c707070")
    sg.Add("Text", "x25 y372 w590 h20",
        Loc("set.endpoints", "OpenRouter: https://openrouter.ai/api/v1     Ollama: http://localhost:11434/v1 (key: ollama)"))
    sg.SetFont("s10")
    return {ddUi: ddUi, uiLangs: uiLangs, eHk: eHk, eHkR: eHkR, cbMd: cbMd, cbProf: cbProf, eClip: eClip}
}
