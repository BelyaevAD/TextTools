; TextTools - Чтение и запись TextTools.ini.
; Подключается из TextTools.ahk, отдельно не запускается.

CfgDefaults() {
    return {
        base_url: "https://openrouter.ai/api/v1",
        api_key: "",
        proxy: "",
        model: "anthropic/claude-haiku-4.5",
        models: "",
        models_known: "",
        hotkey: "^!Space",
        hotkey_repeat: "^+Space",
        markdown: "1",
        ui_lang: "en",
        app_profiling: "0",
        clip_wait: "0.8"
    }
}

LoadCfg() {
    d := CfgDefaults()
    return {
        base_url: IniRead(INI, "api", "base_url", d.base_url),
        api_key:  IniRead(INI, "api", "api_key", d.api_key),
        proxy:    IniRead(INI, "api", "proxy", d.proxy),
        model:    IniRead(INI, "api", "model", d.model),
        models:   IniRead(INI, "api", "models", d.models),
        models_known: IniRead(INI, "api", "models_known", d.models_known),
        hotkey:   IniRead(INI, "ui", "hotkey", d.hotkey),
        hotkey_repeat: IniRead(INI, "ui", "hotkey_repeat", d.hotkey_repeat),
        markdown: IniRead(INI, "ui", "markdown", d.markdown),
        ui_lang:  IniRead(INI, "ui", "lang", d.ui_lang),
        app_profiling: IniRead(INI, "apps", "profiling", d.app_profiling),
        clip_wait: IniRead(INI, "ui", "clip_wait", d.clip_wait)
    }
}

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
    IniWrite(c.HasProp("proxy") ? c.proxy : "", INI, "api", "proxy")
    IniWrite(c.model,    INI, "api", "model")
    IniWrite(c.models,   INI, "api", "models")
    IniWrite(c.models_known, INI, "api", "models_known")
    IniWrite(c.hotkey,   INI, "ui", "hotkey")
    IniWrite(c.hotkey_repeat, INI, "ui", "hotkey_repeat")
    IniWrite(c.markdown, INI, "ui", "markdown")
    IniWrite(c.ui_lang,  INI, "ui", "lang")
    IniWrite(c.HasProp("app_profiling") ? c.app_profiling : "0", INI, "apps", "profiling")
    IniWrite(c.HasProp("clip_wait") ? c.clip_wait : "0.8", INI, "ui", "clip_wait")
}

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

ActionCfg(a) {
    m := (IsObject(a) && a.HasProp("model") && a.model != "") ? a.model : cfg.model
    if (m = cfg.model)
        return cfg
    return {base_url: cfg.base_url, api_key: cfg.api_key, proxy: CfgFlag(cfg, "proxy", ""), model: m}
}

CfgFlag(c, name, fallback := "0") {
    if !(IsObject(c) && c.HasProp(name))
        return fallback
    return c.%name%
}
