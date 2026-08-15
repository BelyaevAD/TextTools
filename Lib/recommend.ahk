; TextTools - Рекомендуемые модели по провайдерам.
; Подключается из TextTools.ahk, отдельно не запускается.
;
; Набор из четырёх ролей под задачи именно этого инструмента: тексты 0.5-4К
; знаков, 50-70 запросов в день, результат нужен за секунды. Отсюда два общих
; правила подбора: никаких reasoning-моделей (лишние секунды ожидания и
; оплаченные «размышления») и никаких batch-вариантов (отложенное исполнение).
;
; Роли (стабильный ключ `key`, подпись `role` — только для интерфейса):
;   mech - механическая правка (Вычитать, Короче)
;   main - рабочая лошадь, кандидат в «модель по умолчанию»
;   deep - разбор и ревью
;   alt  - альтернатива для сравнения
;
; Цены и контекст сверены с каталогом OpenRouter 2026-08-04; список моделей
; OpenAI - с ответом /v1/models рабочего аккаунта. Имена у провайдеров меняются,
; поэтому кнопка не молчит: чего нет в загруженном списке, она называет явно.

ProviderOf(baseUrl) {
    u := StrLower(baseUrl)
    if InStr(u, "openrouter.ai")
        return "openrouter"
    if InStr(u, "api.openai.com")
        return "openai"
    if InStr(u, "anthropic.com")
        return "anthropic"
    if InStr(u, "generativelanguage") || InStr(u, "googleapis.com")
        return "google"
    if InStr(u, "deepseek.com")
        return "deepseek"
    if InStr(u, "mistral.ai")
        return "mistral"
    if InStr(u, "api.x.ai")
        return "xai"
    if InStr(u, "localhost") || InStr(u, "127.0.0.1") || InStr(u, ":11434")
        return "ollama"
    return ""
}

ProviderName(key) {
    names := Map("openai", "OpenAI", "openrouter", "OpenRouter", "anthropic", "Anthropic",
        "google", "Google Gemini", "deepseek", "DeepSeek", "mistral", "Mistral",
        "xai", "xAI", "ollama", "Ollama (локально)")
    return names.Has(key) ? names[key] : key
}

; Четыре модели под роли mech / main / deep / alt. key — стабильный идентификатор.
RecommendedFor(baseUrl) {
    p := ProviderOf(baseUrl)
    mech := Loc("rec.role.mech", "механическая правка")
    main := Loc("rec.role.main", "основная модель")
    deep := Loc("rec.role.deep", "разбор и ревью")
    alt := Loc("rec.role.alt", "альтернатива для сравнения")
    switch p {
        case "openai":
            return [{id: "gpt-4.1-nano", key: "mech", role: mech},
                    {id: "gpt-4.1-mini", key: "main", role: main},
                    {id: "gpt-4.1", key: "deep", role: deep},
                    {id: "gpt-5.6-luna", key: "alt", role: alt}]
        case "openrouter":
            return [{id: "qwen/qwen3.7-flash", key: "mech", role: mech},
                    {id: "openai/gpt-5.6-luna", key: "main", role: main},
                    {id: "anthropic/claude-sonnet-5", key: "deep", role: deep},
                    {id: "deepseek/deepseek-v4-pro", key: "alt", role: alt}]
        case "anthropic":
            return [{id: "claude-haiku-4-5", key: "mech", role: mech},
                    {id: "claude-sonnet-5", key: "main", role: main},
                    {id: "claude-opus-5", key: "deep", role: deep},
                    {id: "claude-sonnet-4-6", key: "alt", role: alt}]
        case "google":
            return [{id: "gemini-3.5-flash-lite", key: "mech", role: mech},
                    {id: "gemini-3.5-flash", key: "main", role: main},
                    {id: "gemini-3.6-flash", key: "deep", role: deep},
                    {id: "gemini-3.1-flash-lite", key: "alt", role: alt}]
        case "deepseek":
            return [{id: "deepseek-chat", key: "main", role: main},
                    {id: "deepseek-reasoner", key: "deep", role: deep}]
        case "mistral":
            return [{id: "mistral-small-latest", key: "mech", role: mech},
                    {id: "mistral-medium-latest", key: "main", role: main},
                    {id: "mistral-large-latest", key: "deep", role: deep},
                    {id: "ministral-8b-latest", key: "alt", role: alt}]
        case "xai":
            return [{id: "grok-4.3", key: "main", role: main},
                    {id: "grok-4.5", key: "deep", role: deep},
                    {id: "grok-4.20", key: "alt", role: alt}]
        case "ollama":
            return [{id: "qwen3:1.7b", key: "mech", role: mech},
                    {id: "qwen3:4b", key: "main", role: main},
                    {id: "gemma3:4b", key: "deep", role: deep},
                    {id: "phi4-mini", key: "alt", role: alt}]
    }
    return []
}

; Слаг модели для роли: своя → main → пусто (тогда ActionCfg возьмёт cfg.model)
RoleModel(role, baseUrl) {
    if (role = "")
        return ""
    rec := RecommendedFor(baseUrl)
    pick := "", main := ""
    for r in rec {
        if (r.key = "main")
            main := r.id
        if (r.key = role)
            pick := r.id
    }
    if (pick != "")
        return pick
    return main
}

; Заводские действия с полем role получают слаг под текущий base_url.
; Формат actions.ini не меняется: там по-прежнему имя модели или "-".
ApplyFactoryRoles(acts, baseUrl) {
    byId := Map()
    for src in DEFAULT_ACTIONS
        if (src.HasProp("id") && src.id != "")
            byId[src.id] := src
    for a in acts {
        if !(a.HasProp("id") && a.id != "" && byId.Has(a.id))
            continue
        src := byId[a.id]
        role := src.HasProp("role") ? src.role : ""
        a.model := RoleModel(role, baseUrl)
    }
    return acts
}

; Чтобы модели действий были в списке отмеченных и попадали в карточку
EnsureModelsForActions(acts) {
    global cfg
    have := Map()
    for id in SplitList(cfg.models)
        have[id] := true
    changed := false
    for a in acts {
        m := (IsObject(a) && a.HasProp("model")) ? a.model : ""
        if (m = "" || have.Has(m))
            continue
        have[m] := true
        cfg.models := (cfg.models = "" ? m : cfg.models ", " m)
        changed := true
    }
    if (cfg.models_known = "")
        cfg.models_known := cfg.models
    if changed
        try SaveCfg(cfg)
}
