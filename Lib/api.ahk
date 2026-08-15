; TextTools - HTTP через curl.exe и ручная работа с JSON.
; COM WinHTTP и PCRE-регексы здесь запрещены - см. TEXTTOOLS_RUNBOOK §5.
; Подключается из TextTools.ahk, отдельно не запускается.

CallApiCfg(c, instruction, userContent) {
    return CallApiMsgs(c, [{role: "system", content: instruction}, {role: "user", content: userContent}])
}

CallApiMsgs(c, msgs) {
    global gLastUsage, gCallLabel
    gLastUsage := ""
    body := '{"model":"' JsonEsc(c.model) '","messages":['
    for i, m in msgs
        body .= (i > 1 ? "," : "") '{"role":"' m.role '","content":"' JsonEsc(m.content) '"}'
    body .= '],"stream":false}'
    static seq := 0
    seq += 1
    reqF := A_Temp "\TextTools_req_" seq ".json"
    try {
        f := FileOpen(reqF, "w", "UTF-8-RAW")
        f.Write(body)
        f.Close()
        chars := 0
        for m in msgs
            chars += StrLen(m.content)
        TT_Log("запрос отправлен (" StrLen(body) " символов)")
        BusyUpdate(Loc("api.sent", "📡 запрос отправлен, ждём ответ модели…"))
        BusyDetail(Format(Loc("api.sent.detail", "отправлено {1} симв. ≈ {2} ток."),
            NumFmt(chars), NumFmt(EstTokens(BodyText(msgs)))))
        res := HttpCurl(RTrim(c.base_url, "/") "/chat/completions",
            {timeout: 90, api_key: c.api_key, json: true, bodyFile: reqF, retries: 1})
    } finally {
        try FileDelete(reqF)
    }
    if (res.curl != 0)
        throw Error(Format(Loc("api.curl", "curl завершился с кодом {1} (сеть, таймаут или недоступный адрес)"), res.curl))
    resp := res.body
    if (resp = "")
        throw Error(Loc("api.empty", "Пустой ответ от API"))
    if (res.status >= 400) {
        detail := ExtractErrorMessage(resp)
        throw Error(HttpStatusText(res.status) (detail != "" ? "`n" detail : ""))
    }
    TT_Log("ответ получен, тело " StrLen(resp) " символов")
    BusyUpdate(Loc("api.got", "🧩 ответ получен, разбираю…"))
    u := ExtractUsage(resp)
    if IsObject(u) {
        gLastUsage := u
        cost := CostOf(c.model, u.prompt, u.completion)
        BusyDetail(Format(Loc("api.usage", "промпт {1} ток. · ответ {2} ток."),
            NumFmt(u.prompt), NumFmt(u.completion))
            . (cost >= 0 ? " · " MoneyFmt(cost) : ""))
        TT_Log("расход: промпт " u.prompt ", ответ " u.completion " токенов, модель " c.model)
        UsageRecord(c.model, u, gCallLabel)
    }
    return JsonUnesc(ExtractContent(resp))
}

FetchModelIds(c) {
    res := HttpCurl(RTrim(c.base_url, "/") "/models",
        {timeout: 30, api_key: c.api_key, retries: 1})
    if (res.curl != 0)
        throw Error(Format(Loc("api.curl", "curl завершился с кодом {1} (сеть, таймаут или недоступный адрес)"), res.curl))
    resp := res.body
    if (resp = "")
        throw Error("Пустой ответ от " c.base_url "/models")
    if (res.status >= 400) {
        detail := ExtractErrorMessage(resp)
        throw Error(HttpStatusText(res.status) (detail != "" ? "`n" detail : ""))
    }
    if !InStr(resp, '"data"')
        throw Error("Ответ без списка моделей:`n" SubStr(resp, 1, 300))
    ids := ParseModelIds(resp)
    if (ids.Length = 0)
        throw Error("В ответе нет ни одной подходящей модели")
    TT_Log("список моделей получен: " ids.Length)
    return ids
}

ParseModelIds(resp) {
    ids := [], seen := Map()
    pos := InStr(resp, '"id"', true)
    while pos {
        i := pos + 4
        while (i <= StrLen(resp)) {
            ch := SubStr(resp, i, 1)
            if (ch = " " || ch = ":" || ch = "`t" || ch = "`r" || ch = "`n")
                i++
            else
                break
        }
        if (SubStr(resp, i, 1) = '"') {
            j := InStr(resp, '"', true, i + 1)
            if j {
                id := SubStr(resp, i + 1, j - i - 1)
                if (id != "" && !ModelIsUseless(id) && !seen.Has(id)) {
                    seen[id] := true
                    ids.Push(id)
                }
            }
        }
        pos := InStr(resp, '"id"', true, pos + 1)
    }
    SortBy(ids, (a, b) => StrCompare(a, b) < 0)
    return ids
}

ModelIsUseless(id) {
    static bad := ["embed", "whisper", "tts", "dall-e", "moderation", "audio",
        "realtime", "transcribe", "image", "sora", "babbage", "davinci", "codex",
        "search-preview", "computer-use", "guard"]
    low := StrLower(id)
    for b in bad
        if InStr(low, b)
            return true
    return false
}

BodyText(msgs) {
    s := ""
    for m in msgs
        s .= m.content "`n"
    return s
}

ExtractUsage(resp) {
    p := InStr(resp, '"usage"', true)
    if !p
        return ""
    pt := JsonIntAfter(resp, '"prompt_tokens"', p)
    ct := JsonIntAfter(resp, '"completion_tokens"', p)
    if (pt < 0 && ct < 0)
        return ""
    return {prompt: (pt < 0 ? 0 : pt), completion: (ct < 0 ? 0 : ct)}
}

JsonIntAfter(s, key, from) {
    p := InStr(s, key, true, from)
    if !p
        return -1
    i := p + StrLen(key)
    len := StrLen(s)
    while (i <= len) {
        ch := SubStr(s, i, 1)
        if (ch = " " || ch = ":" || ch = "`t" || ch = "`r" || ch = "`n")
            i++
        else
            break
    }
    num := ""
    while (i <= len) {
        ch := SubStr(s, i, 1)
        if (ch != "" && InStr("0123456789", ch, true)) {
            num .= ch
            i++
        } else
            break
    }
    return (num = "") ? -1 : Integer(num)
}

; JSON-строка после ключа, начиная с from. Пустая строка, если не строковое значение.
JsonStringAfter(resp, key, from := 1) {
    pos := InStr(resp, key, true, from)
    if !pos
        return ""
    i := pos + StrLen(key)
    loop {
        ch := SubStr(resp, i, 1)
        if (ch = " " || ch = "`t" || ch = "`r" || ch = "`n" || ch = ":")
            i++
        else
            break
    }
    if (SubStr(resp, i, 1) != '"')
        return ""
    q1 := i + 1
    j := q1
    loop {
        q2 := InStr(resp, '"', true, j)
        if !q2
            return ""
        k := q2 - 1
        nbs := 0
        while (k >= q1 && SubStr(resp, k, 1) = "\") {
            nbs++
            k--
        }
        if (Mod(nbs, 2) = 0)
            return SubStr(resp, q1, q2 - q1)
        j := q2 + 1
    }
}

ExtractErrorMessage(resp) {
    p := InStr(resp, '"error"', true)
    if !p
        return ""
    return JsonStringAfter(resp, '"message"', p)
}

; content из choices[0].message; запасной путь — первое строковое "content"
ExtractContent(resp) {
    p := InStr(resp, '"choices"', true)
    if p {
        m := InStr(resp, '"message"', true, p)
        if m {
            c := JsonStringAfter(resp, '"content"', m)
            if (c != "")
                return c
        }
    }
    c := JsonStringAfter(resp, '"content"', 1)
    if (c != "")
        return c
    err := ExtractErrorMessage(resp)
    if (err != "")
        throw Error(err)
    throw Error(Loc("api.no_content", "Не удалось найти content в ответе:") "`n" SubStr(resp, 1, 400))
}

JsonEsc(s) {
    s := StrReplace(s, "\", "\\")
    s := StrReplace(s, '"', '\"')
    s := StrReplace(s, "`r`n", "`n")
    s := StrReplace(s, "`r", "`n")
    s := StrReplace(s, "`n", "\n")
    s := StrReplace(s, "`t", "\t")
    loop 31 {
        code := A_Index
        if (code = 9 || code = 10 || code = 13)
            continue
        ch := Chr(code)
        if InStr(s, ch, true)
            s := StrReplace(s, ch, Format("\u{:04X}", code))
    }
    return s
}

IsHex4(s) {
    if (StrLen(s) != 4)
        return false
    loop 4 {
        o := Ord(StrUpper(SubStr(s, A_Index, 1)))
        if !((o >= 48 && o <= 57) || (o >= 65 && o <= 70))
            return false
    }
    return true
}

JsonUnesc(s) {
    if !InStr(s, "\")
        return s
    out := ""
    i := 1
    len := StrLen(s)
    while (i <= len) {
        p := InStr(s, "\", true, i)
        if !p {
            out .= SubStr(s, i)
            break
        }
        out .= SubStr(s, i, p - i)
        d := SubStr(s, p + 1, 1)
        if (d == "n") {
            out .= "`n", i := p + 2
        } else if (d == "t") {
            out .= A_Tab, i := p + 2
        } else if (d == "r") {
            i := p + 2
        } else if (d == "b") {
            out .= Chr(8), i := p + 2
        } else if (d == "f") {
            out .= Chr(12), i := p + 2
        } else if (d == "u") {
            hex := SubStr(s, p + 2, 4)
            if !IsHex4(hex) {
                out .= "\u"
                i := p + 2
                continue
            }
            cp := Integer("0x" hex)
            if (cp >= 0xD800 && cp <= 0xDBFF && SubStr(s, p + 6, 2) = "\u") {
                hex2 := SubStr(s, p + 8, 4)
                if IsHex4(hex2) {
                    trail := Integer("0x" hex2)
                    if (trail >= 0xDC00 && trail <= 0xDFFF) {
                        out .= Chr(0x10000 + ((cp - 0xD800) << 10) + (trail - 0xDC00))
                        i := p + 12
                        continue
                    }
                }
            }
            out .= Chr(cp), i := p + 6
        } else {
            out .= d, i := p + 2
        }
    }
    return out
}
