; TextTools - Рекомендуемые модели по провайдерам.
; Подключается из TextTools.ahk, отдельно не запускается.
;
; Набор из четырёх ролей под задачи именно этого инструмента: тексты 0.5-4К
; знаков, 50-70 запросов в день, результат нужен за секунды. Отсюда два общих
; правила подбора: никаких reasoning-моделей (лишние секунды ожидания и
; оплаченные «размышления») и никаких batch-вариантов (отложенное исполнение).
;
; Роли:
;   mech - механическая правка (Вычитать, Короче): качество почти не отличается,
;          выигрыш в цене и скорости
;   main - рабочая лошадь, кандидат в «модель по умолчанию»
;   deep - разбор и ревью (Разбор кода, Ревью требований, Вопросы, Задача):
;          здесь модель должна понимать текст, а не переписывать его
;   alt  - альтернатива под ту же роль от другого поставщика или поколения:
;          есть с чем сравнить, не перенастраивая всё
;
; Цены и контекст сверены с каталогом OpenRouter 2026-08-04; список моделей
; OpenAI - с ответом /v1/models рабочего аккаунта. Имена у провайдеров меняются,
; поэтому кнопка не молчит: чего нет в загруженном списке, она называет явно.

; Провайдер по адресу из настроек; пустая строка = не опознан
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
    ; local OpenAI-compatible endpoints (Ollama default port 11434)
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

; Четыре модели под роли mech / main / deep / alt
RecommendedFor(baseUrl) {
    p := ProviderOf(baseUrl)
    mech := Loc("rec.role.mech", "механическая правка")
    main := Loc("rec.role.main", "основная модель")
    deep := Loc("rec.role.deep", "разбор и ревью")
    alt := Loc("rec.role.alt", "альтернатива для сравнения")
    switch p {
        case "openai":
            return [{id: "gpt-4.1-nano", role: mech},
                    {id: "gpt-4.1-mini", role: main},
                    {id: "gpt-4.1", role: deep},
                    {id: "gpt-5.6-luna", role: alt}]
        case "openrouter":
            ; смысл OpenRouter в выборе между поставщиками, поэтому набор смешанный
            return [{id: "qwen/qwen3.7-flash", role: mech},
                    {id: "openai/gpt-5.6-luna", role: main},
                    {id: "anthropic/claude-sonnet-5", role: deep},
                    {id: "deepseek/deepseek-v4-pro", role: alt}]
        case "anthropic":
            return [{id: "claude-haiku-4-5", role: mech},
                    {id: "claude-sonnet-5", role: main},
                    {id: "claude-opus-5", role: deep},
                    {id: "claude-sonnet-4-6", role: alt}]
        case "google":
            return [{id: "gemini-3.5-flash-lite", role: mech},
                    {id: "gemini-3.5-flash", role: main},
                    {id: "gemini-3.6-flash", role: deep},
                    {id: "gemini-3.1-flash-lite", role: alt}]
        case "deepseek":
            ; У самого DeepSeek API всего два алиаса вместо версионных имён,
            ; поэтому здесь честные два, а не четыре ради счёта: одна и та же
            ; модель под четырьмя ролями - это одна строка с одной галочкой.
            return [{id: "deepseek-chat", role: main},
                    {id: "deepseek-reasoner", role: deep}]
        case "mistral":
            return [{id: "mistral-small-latest", role: mech},
                    {id: "mistral-medium-latest", role: main},
                    {id: "mistral-large-latest", role: deep},
                    {id: "ministral-8b-latest", role: alt}]
        case "xai":
            return [{id: "grok-4.3", role: main},
                    {id: "grok-4.5", role: deep},
                    {id: "grok-4.20", role: alt}]
        case "ollama":
            ; small local models; pull them in Ollama before use
            return [{id: "qwen3:1.7b", role: mech},
                    {id: "qwen3:4b", role: main},
                    {id: "gemma3:4b", role: deep},
                    {id: "phi4-mini", role: alt}]
    }
    return []
}
