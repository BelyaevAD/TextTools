; TextTools - Чтение и запись TextTools.ini.
; Подключается из TextTools.ahk, отдельно не запускается.

LoadCfg() {
    return {
        base_url: IniRead(INI, "api", "base_url", "https://openrouter.ai/api/v1"),
        api_key:  IniRead(INI, "api", "api_key", ""),
        model:    IniRead(INI, "api", "model", "anthropic/claude-haiku-4.5"),
        ; models - отмеченные галочками, из них выбирается модель действия;
        ; models_known - всё, что видели у провайдера, чтобы не ходить за
        ; списком каждый раз (галочки снимаются, а строки остаются)
        models:   IniRead(INI, "api", "models", ""),
        models_known: IniRead(INI, "api", "models_known", ""),
        hotkey:   IniRead(INI, "ui", "hotkey", "^!Space"),
        hotkey_repeat: IniRead(INI, "ui", "hotkey_repeat", "^+Space"),
        markdown: IniRead(INI, "ui", "markdown", "1"),
        ui_lang:  IniRead(INI, "ui", "lang", "en")
    }
}

; Список из строки через запятую и обратно. Значения хранятся строкой, потому
; что INI - плоский формат; галочки живут в интерфейсе, а не в файле.
SplitList(s) {
    out := []
    for part in StrSplit(StrReplace(StrReplace(s, "`r", ""), "`n", ","), ",") {
        v := Trim(part)
        if (v != "")
            out.Push(v)
    }
    return out
}

JoinList(arr) {
    out := ""
    for v in arr
        out .= (out = "" ? "" : ", ") v
    return out
}

SaveCfg(c) {
    IniWrite(c.base_url, INI, "api", "base_url")
    IniWrite(c.api_key,  INI, "api", "api_key")
    IniWrite(c.model,    INI, "api", "model")
    IniWrite(c.models,   INI, "api", "models")
    IniWrite(c.models_known, INI, "api", "models_known")
    IniWrite(c.hotkey,   INI, "ui", "hotkey")
    IniWrite(c.hotkey_repeat, INI, "ui", "hotkey_repeat")
    IniWrite(c.markdown, INI, "ui", "markdown")
    IniWrite(c.ui_lang,  INI, "ui", "lang")
}

; Список дополнительных моделей из настроек: запятая или перевод строки.
; Модель по умолчанию в него не входит - в выпадающих списках она отдельным
; первым пунктом, чтобы действие следовало за настройками, а не фиксировало имя.
ExtraModels() {
    out := []
    for m in SplitList(cfg.models) {
        if (m = cfg.model)
            continue
        dup := false
        for have in out
            if (have = m)
                dup := true
        if !dup
            out.Push(m)
    }
    return out
}

; Конфиг для конкретного действия: своя модель перекрывает модель по умолчанию
ActionCfg(a) {
    m := (IsObject(a) && a.HasProp("model") && a.model != "") ? a.model : cfg.model
    if (m = cfg.model)
        return cfg
    return {base_url: cfg.base_url, api_key: cfg.api_key, model: m}
}

; Пара хоткеев применяется атомарно: сначала снимаются ОБА старых, потом
; вешаются оба новых. По отдельности нельзя: если новый хоткей меню совпал со
; старым хоткеем повтора, перерегистрация второго погасит только что
; назначенный первый (Hotkey "Off" действует на клавишу, а не на колбэк).
; Бросает при некорректной клавише - вызывающий откатывает пару.
