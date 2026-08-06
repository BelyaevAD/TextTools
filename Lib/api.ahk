; TextTools - HTTP через curl.exe и ручная работа с JSON.
; COM WinHTTP и PCRE-регексы здесь запрещены - см. TEXTTOOLS_RUNBOOK §5.
; Подключается из TextTools.ahk, отдельно не запускается.

CallApiCfg(c, instruction, userContent) {
    return CallApiMsgs(c, [{role: "system", content: instruction}, {role: "user", content: userContent}])
}

; msgs: массив {role, content} - используется и для follow-up диалога в окне результата
CallApiMsgs(c, msgs) {
    global gLastUsage, gCallLabel
    gLastUsage := ""
    ; запрос уходит через системный curl.exe: COM-объект WinHTTP внутри процесса
    ; подозревается в порче памяти, выносим сеть в отдельный процесс
    body := '{"model":"' JsonEsc(c.model) '","messages":['
    for i, m in msgs
        body .= (i > 1 ? "," : "") '{"role":"' m.role '","content":"' JsonEsc(m.content) '"}'
    body .= '],"stream":false}'
    ; имена файлов уникальны на запрос: RunWait - прерываемое ожидание, и пока
    ; один запрос в полете, пользователь может запустить второй; на общих
    ; путях запросы затирали бы ответы друг друга
    static seq := 0
    seq += 1
    reqF := A_Temp "\TextTools_req_" seq ".json"
    respF := A_Temp "\TextTools_resp_" seq ".json"
    curlF := A_Temp "\TextTools_curl_" seq ".cfg"
    try FileDelete(respF)
    f := FileOpen(reqF, "w", "UTF-8-RAW")
    f.Write(body)
    f.Close()
    curlCfg := 'url = "' RTrim(c.base_url, "/") '/chat/completions"`n'
        . "silent`nshow-error`nmax-time = 90`n"
        . 'header = "Content-Type: application/json; charset=utf-8"`n'
        . (c.api_key != "" ? 'header = "Authorization: Bearer ' c.api_key '"`n' : "")
        . 'data-binary = "@' StrReplace(reqF, "\", "\\") '"`n'
        . 'output = "' StrReplace(respF, "\", "\\") '"`n'
    f := FileOpen(curlF, "w", "UTF-8-RAW")
    f.Write(curlCfg)
    f.Close()
    ; объем показываем до отправки: точные токены придут в usage ответа
    chars := 0
    for m in msgs
        chars += StrLen(m.content)
    TT_Log("запрос отправлен (" StrLen(body) " символов)")
    BusyUpdate("📡 запрос отправлен, ждём ответ модели…")
    BusyDetail("отправлено " NumFmt(chars) " симв. ≈ " NumFmt(EstTokens(BodyText(msgs))) " ток.")
    code := RunWait('curl.exe -K "' curlF '"', , "Hide")
    try FileDelete(curlF)
    try FileDelete(reqF)
    if (code != 0)
        throw Error("curl завершился с кодом " code " (сеть, таймаут или недоступный адрес)")
    resp := ""
    try resp := FileRead(respF, "UTF-8")
    try FileDelete(respF)
    if (resp = "")
        throw Error("Пустой ответ от API")
    TT_Log("ответ получен, тело " StrLen(resp) " символов")
    BusyUpdate("🧩 ответ получен, разбираю…")
    u := ExtractUsage(resp)
    if IsObject(u) {
        gLastUsage := u
        cost := CostOf(c.model, u.prompt, u.completion)
        BusyDetail("промпт " NumFmt(u.prompt) " ток. · ответ " NumFmt(u.completion) " ток."
            . (cost >= 0 ? " · " MoneyFmt(cost) : ""))
        TT_Log("расход: промпт " u.prompt ", ответ " u.completion " токенов, модель " c.model)
        UsageRecord(c.model, u, gCallLabel)
    }
    return JsonUnesc(ExtractContent(resp))
}

; Список моделей у провайдера: GET {base_url}/models. Возвращает массив id;
; бросает с понятным текстом, если не получилось - вызывающий показывает его.
FetchModelIds(c) {
    static seq := 0
    seq += 1
    respF := A_Temp "\TextTools_models_" seq ".json"
    curlF := A_Temp "\TextTools_models_" seq ".cfg"
    try FileDelete(respF)
    curlCfg := 'url = "' RTrim(c.base_url, "/") '/models"`n'
        . "silent`nshow-error`nmax-time = 30`n"
        . (c.api_key != "" ? 'header = "Authorization: Bearer ' c.api_key '"`n' : "")
        . 'output = "' StrReplace(respF, "\", "\\") '"`n'
    f := FileOpen(curlF, "w", "UTF-8-RAW")
    f.Write(curlCfg)
    f.Close()
    code := RunWait('curl.exe -K "' curlF '"', , "Hide")
    try FileDelete(curlF)
    if (code != 0) {
        try FileDelete(respF)
        throw Error("curl завершился с кодом " code " (сеть, таймаут или недоступный адрес)")
    }
    resp := ""
    try resp := FileRead(respF, "UTF-8")
    try FileDelete(respF)
    if (resp = "")
        throw Error("Пустой ответ от " c.base_url "/models")
    if !InStr(resp, '"data"')
        throw Error("Ответ без списка моделей:`n" SubStr(resp, 1, 300))
    ids := ParseModelIds(resp)
    if (ids.Length = 0)
        throw Error("В ответе нет ни одной подходящей модели")
    TT_Log("список моделей получен: " ids.Length)
    return ids
}

; Разбор вынесен из FetchModelIds, чтобы проверялся без сети и без ключа.
; У записи модели ровно один ключ "id" - идём линейно, без регулярок.
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
    SortStrings(ids)
    return ids
}

; Отсев нетекстовых и служебных моделей: в списке провайдера их больше половины,
; и выбирать действию из ста строк с эмбеддингами невозможно
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

SortStrings(arr) {
    i := 2
    while (i <= arr.Length) {
        j := i
        while (j > 1 && StrCompare(arr[j], arr[j - 1]) < 0) {
            tmp := arr[j], arr[j] := arr[j - 1], arr[j - 1] := tmp
            j--
        }
        i++
    }
}

; Склейка текста сообщений - только для оценки объема, не для отправки
BodyText(msgs) {
    s := ""
    for m in msgs
        s .= m.content "`n"
    return s
}

; usage из ответа тем же линейным разбором, что и content: PCRE здесь запрещен
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

; Достает значение первого ключа "content" из JSON ответа без регулярок:
; PCRE на длинных ответах превышает лимит бэктрекинга и портит память процесса
ExtractContent(resp) {
    pos := InStr(resp, '"content"', true)
    while pos {
        i := pos + 9
        loop {
            ch := SubStr(resp, i, 1)
            if (ch = " " || ch = "`t" || ch = "`r" || ch = "`n" || ch = ":")
                i++
            else
                break
        }
        if (SubStr(resp, i, 1) = '"') {
            q1 := i + 1
            j := q1
            loop {
                q2 := InStr(resp, '"', true, j)
                if !q2
                    break 2
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
        pos := InStr(resp, '"content"', true, pos + 1)
    }
    throw Error("Не удалось найти content в ответе:`n" SubStr(resp, 1, 400))
}

JsonEsc(s) {
    s := StrReplace(s, "\", "\\")
    s := StrReplace(s, '"', '\"')
    s := StrReplace(s, "`r`n", "`n")
    s := StrReplace(s, "`r", "`n")
    s := StrReplace(s, "`n", "\n")
    s := StrReplace(s, "`t", "\t")
    ; остальные управляющие символы C0 недопустимы в JSON-строке: из Word
    ; приходит U+000B (мягкий перенос), из PDF - U+000C, из логов - ESC.
    ; Строгий парсер на стороне API отвечает 400 на такое тело.
    loop 31 {
        code := A_Index                      ; 1..31; U+0000 в тексте буфера не встречается
        if (code = 9 || code = 10 || code = 13)
            continue                         ; уже обработаны выше
        c := Chr(code)
        if InStr(s, c, true)
            s := StrReplace(s, c, Format("\u{:04X}", code))
    }
    return s
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
        } else if (d == "u") {
            out .= Chr(Integer("0x" SubStr(s, p + 2, 4))), i := p + 6
        } else {
            out .= d, i := p + 2
        }
    }
    return out
}
