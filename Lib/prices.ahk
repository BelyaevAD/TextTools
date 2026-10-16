; TextTools - Цены на токены и форматирование чисел/денег.
; Подключается из TextTools.ahk, отдельно не запускается.
;
; У OpenAI нет публичного эндпоинта с ценами, поэтому прайс берется из каталога
; OpenRouter (GET /api/v1/models, без авторизации). Каталог разбирается один раз
; в Map при первом обращении; битая закачка рабочий кэш не затирает.

PricesRefresh(force := false) {
    if (!force && FileExist(PRICESF)) {
        age := 999
        try age := DateDiff(A_Now, FileGetTime(PRICESF, "M"), "Hours")
        if (age < 24)
            return true
    }
    tmp := A_Temp "\TextTools_prices_dl.json"
    try FileDelete(tmp)
    ; Каталог цен - дело необязательное, поэтому исключение наружу не выпускаем.
    ; HttpCurl бросает на битом прокси из INI (NormalizeProxy) или кавычке в
    ; URL/ключе, а зовут PricesRefresh() без try окно настроек и статистика:
    ; раньше настройки просто не открывались - то есть починить этот прокси
    ; было уже негде.
    res := ""
    try res := HttpCurl("https://openrouter.ai/api/v1/models", {timeout: 20, retries: 1})
    catch as e {
        TT_Log("прайс: запрос каталога не выполнен (" e.Message ")")
        return FileExist(PRICESF) ? true : false
    }
    if (res.curl != 0 || res.body = "") {
        TT_Log("прайс: загрузка каталога не удалась (curl " res.curl ")")
        return FileExist(PRICESF) ? true : false
    }
    if !InStr(SubStr(res.body, 1, 400), '"data"') {
        TT_Log("прайс: каталог пришел в неожиданном формате, кэш не тронут")
        return FileExist(PRICESF) ? true : false
    }
    try {
        f := FileOpen(tmp, "w", "UTF-8")
        f.Write(res.body)
        f.Close()
        try FileDelete(PRICESF)
        FileMove(tmp, PRICESF, true)
    } catch as e {
        TT_Log("прайс: не удалось записать кэш (" e.Message ")")
        try FileDelete(tmp)
        return FileExist(PRICESF) ? true : false
    }
    PriceCacheReset()
    TT_Log("прайс: каталог цен обновлен")
    return true
}

PricesStamp() {
    if !FileExist(PRICESF)
        return ""
    t := ""
    try t := FormatTime(FileGetTime(PRICESF, "M"), "dd.MM.yyyy HH:mm")
    return t
}

PriceSlug(model, baseUrl) {
    if InStr(model, "/")
        return model
    if InStr(baseUrl, "openai.com")
        return "openai/" model
    if InStr(baseUrl, "anthropic.com")
        return "anthropic/" model
    return model
}

PriceCacheReset() {
    global gPriceIndex
    gPriceIndex := ""
}

; Разбор каталога один раз: slug -> {prompt, completion, ctx}
PricesEnsureIndex() {
    global gPriceIndex
    if IsObject(gPriceIndex)
        return
    gPriceIndex := Map()
    txt := ""
    try txt := FileRead(PRICESF, "UTF-8")
    if (txt = "")
        return
    pos := 1
    loop {
        p := InStr(txt, '"id":"', true, pos)
        if !p
            break
        q1 := p + 6
        q2 := InStr(txt, '"', true, q1)
        if !q2
            break
        slug := SubStr(txt, q1, q2 - q1)
        nxt := InStr(txt, '"id":"', true, q2)
        stop := nxt ? nxt : StrLen(txt) + 1
        rec := {prompt: "", completion: "", ctx: 0}
        q := InStr(txt, '"pricing"', true, q2)
        if (q && q < stop) {
            pend := InStr(txt, "}", true, q)
            if (!pend || pend > stop)
                pend := stop
            rec.prompt := JsonStrAfter(txt, '"prompt"', q, pend)
            rec.completion := JsonStrAfter(txt, '"completion"', q, pend)
        }
        cq := InStr(txt, '"context_length"', true, q2)
        if (cq && cq < stop) {
            ; цифры должны идти сразу после ключа: прежний скан «до первой
            ; цифры» на "context_length":null подхватывал число из следующего
            ; поля записи (например 3 из "tokenizer":"Qwen3")
            n := JsonIntAfter(txt, '"context_length"', cq)
            if (n > 0)
                rec.ctx := n
        }
        gPriceIndex[slug] := rec
        pos := q2
    }
}

PriceOf(slug, reset := false) {
    if reset {
        PriceCacheReset()
        return ""
    }
    if (slug = "")
        return ""
    PricesEnsureIndex()
    if !gPriceIndex.Has(slug)
        return ""
    rec := gPriceIndex[slug]
    if (rec.prompt = "" || rec.completion = "")
        return ""
    return {prompt: rec.prompt + 0, completion: rec.completion + 0}
}

ContextOf(slug, reset := false) {
    if reset {
        PriceCacheReset()
        return 0
    }
    if (slug = "")
        return 0
    PricesEnsureIndex()
    if !gPriceIndex.Has(slug)
        return 0
    return gPriceIndex[slug].ctx
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

CostOf(model, promptTok, complTok) {
    pr := PriceOf(PriceSlug(model, cfg.base_url))
    if !IsObject(pr)
        return -1
    return promptTok * pr.prompt + complTok * pr.completion
}

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

EstTokens(s) {
    n := StrLen(s)
    if (n = 0)
        return 0
    sample := (n > 4000) ? SubStr(s, 1, 4000) : s
    ascii := 0
    loop parse sample
        if (Ord(A_LoopField) < 128)
            ascii++
    share := ascii / StrLen(sample)
    return Round(n * share / 4 + n * (1 - share) / 2)
}
