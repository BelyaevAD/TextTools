; TextTools - Цены на токены и форматирование чисел/денег.
; Подключается из TextTools.ahk, отдельно не запускается.
;
; У OpenAI нет публичного эндпоинта с ценами, поэтому прайс берется из каталога
; OpenRouter (GET /api/v1/models, без авторизации): он отдает цену за токен по
; всем моделям, включая openai/*. Цифры оттуда могут разойтись со счетом самого
; провайдера - в отчете это оговорено, это оценка, а не биллинг.

; Скачивает каталог не чаще раза в сутки. true - кэш пригоден к чтению.
PricesRefresh(force := false) {
    if (!force && FileExist(PRICESF)) {
        age := 999
        try age := DateDiff(A_Now, FileGetTime(PRICESF, "M"), "Hours")
        if (age < 24)
            return true
    }
    tmp := A_Temp "\TextTools_prices.json"
    cfgF := A_Temp "\TextTools_prices.cfg"
    try FileDelete(tmp)
    curlCfg := 'url = "https://openrouter.ai/api/v1/models"`n'
        . "silent`nshow-error`nmax-time = 20`n"
        . 'output = "' StrReplace(tmp, "\", "\\") '"`n'
    f := FileOpen(cfgF, "w", "UTF-8-RAW")
    f.Write(curlCfg)
    f.Close()
    code := RunWait('curl.exe -K "' cfgF '"', , "Hide")
    try FileDelete(cfgF)
    if (code != 0 || !FileExist(tmp)) {
        TT_Log("прайс: загрузка каталога не удалась (curl " code ")")
        return FileExist(PRICESF) ? true : false
    }
    ; битую закачку не пускаем поверх рабочего кэша
    head := ""
    try head := FileRead(tmp, "UTF-8 m400")      ; m400 - прочитать не больше 400 символов
    if !InStr(head, '"data"') {
        try FileDelete(tmp)
        TT_Log("прайс: каталог пришел в неожиданном формате, кэш не тронут")
        return FileExist(PRICESF) ? true : false
    }
    try FileDelete(PRICESF)
    try FileMove(tmp, PRICESF, true)
    PriceCacheReset()
    TT_Log("прайс: каталог цен обновлен")
    return true
}

; Когда каталог загружен (для оговорки в отчете); "" если кэша нет
PricesStamp() {
    if !FileExist(PRICESF)
        return ""
    t := ""
    try t := FormatTime(FileGetTime(PRICESF, "M"), "dd.MM.yyyy HH:mm")
    return t
}

; В каталоге модели именуются vendor/model, у прямых API вендора в имени нет
PriceSlug(model, baseUrl) {
    if InStr(model, "/")
        return model
    if InStr(baseUrl, "openai.com")
        return "openai/" model
    if InStr(baseUrl, "anthropic.com")
        return "anthropic/" model
    return model
}

; Сбрасывать надо оба кэша: они читают один и тот же файл каталога
PriceCacheReset() {
    ContextOf("", true)
    PriceOf("", true)
}

; {prompt, completion} - доллары за токен; "" если модели нет в каталоге
PriceOf(slug, reset := false) {
    static cache := Map()
    if reset {
        cache := Map()
        return ""
    }
    if (slug = "")
        return ""
    if cache.Has(slug)
        return cache[slug]
    txt := ""
    try txt := FileRead(PRICESF, "UTF-8")
    if (txt = "")
        return ""
    res := ""
    p := InStr(txt, '"id":"' slug '"', true)
    if p {
        q := InStr(txt, '"pricing"', true, p)
        stop := q ? InStr(txt, "}", true, q) : 0
        if (q && stop) {
            pr := JsonStrAfter(txt, '"prompt"', q, stop)
            co := JsonStrAfter(txt, '"completion"', q, stop)
            if (pr != "" && co != "")
                res := {prompt: pr + 0, completion: co + 0}
        }
    }
    cache[slug] := res
    return res
}

; Значение строкового ключа JSON без регулярок (PCRE здесь запрещен, см. §5)
; Размер контекста модели из того же каталога. Число, а не строка, поэтому
; читается отдельно от цен: "context_length":1047576
ContextOf(slug, reset := false) {
    static cache := Map()
    if reset {
        cache := Map()
        return 0
    }
    if (slug = "")
        return 0
    if cache.Has(slug)
        return cache[slug]
    txt := ""
    try txt := FileRead(PRICESF, "UTF-8")
    res := 0
    if (txt != "") {
        p := InStr(txt, '"id":"' slug '"', true)
        if p {
            q := InStr(txt, '"context_length"', true, p)
            ; ограничиваем поиск концом записи модели: у следующей модели
            ; тоже есть context_length, и без границы взяли бы чужой
            nxt := InStr(txt, '"id":"', true, p + 6)
            if (q && (!nxt || q < nxt)) {
                i := q + 16
                while (i <= StrLen(txt) && !IsDigit(SubStr(txt, i, 1)))
                    i++
                num := ""
                while (i <= StrLen(txt) && IsDigit(SubStr(txt, i, 1))) {
                    num .= SubStr(txt, i, 1)
                    i++
                }
                if (num != "")
                    res := Integer(num)
            }
        }
    }
    cache[slug] := res
    return res
}

JsonStrAfter(s, key, from, stop) {
    p := InStr(s, key, true, from)
    if (!p || p > stop)
        return ""
    i := p + StrLen(key)
    len := StrLen(s)
    while (i <= len) {
        ch := SubStr(s, i, 1)
        if (ch = " " || ch = ":" || ch = "`t" || ch = "`r" || ch = "`n")
            i++
        else
            break
    }
    if (SubStr(s, i, 1) != '"')
        return ""
    j := InStr(s, '"', true, i + 1)
    return j ? SubStr(s, i + 1, j - i - 1) : ""
}

; Стоимость запроса в долларах; -1 если цена модели неизвестна
CostOf(model, promptTok, complTok) {
    pr := PriceOf(PriceSlug(model, cfg.base_url))
    if !IsObject(pr)
        return -1
    return promptTok * pr.prompt + complTok * pr.completion
}

; Мелкие суммы не должны схлопываться в $0.00 - точность по величине
MoneyFmt(v) {
    if (v < 0)
        return "—"
    if (v >= 1)
        return Format("${:.2f}", v)
    if (v >= 0.01)
        return Format("${:.3f}", v)
    return Format("${:.5f}", v)
}

NumFmt(n) {
    s := String(Integer(n))
    out := "", cnt := 0
    i := StrLen(s)
    while (i >= 1) {
        out := SubStr(s, i, 1) out
        cnt++
        if (Mod(cnt, 3) = 0 && i > 1)
            out := " " out
        i--
    }
    return out
}

; Грубая оценка ДО отправки (точные токены придут в ответе):
; латиница ~4 символа на токен, кириллица ~2
EstTokens(s) {
    n := StrLen(s)
    if (n = 0)
        return 0
    ; на больших выделениях считаем по образцу: посимвольный проход по 100 КБ
    ; заметно тормозит поток, а точность оценки этого не стоит
    sample := (n > 4000) ? SubStr(s, 1, 4000) : s
    ascii := 0
    loop parse sample
        if (Ord(A_LoopField) < 128)
            ascii++
    share := ascii / StrLen(sample)
    return Round(n * share / 4 + n * (1 - share) / 2)
}
