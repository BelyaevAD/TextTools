; TextTools - обвязка curl.exe: конфиг -K, статус HTTP, подчистка временных файлов.
; COM WinHTTP и PCRE здесь запрещены. Подключается из TextTools.ahk.

CurlExe() {
    static p := ""
    if (p != "")
        return p
    cand := A_WinDir "\System32\curl.exe"
    if FileExist(cand) {
        p := cand
        return p
    }
    p := "curl.exe"
    return p
}

; Значение для двойных кавычек в curl -K: внутри кавычек curl понимает \\ \" \t \n \r \v
CurlQuote(s) {
    s := StrReplace(s, "\", "\\")
    s := StrReplace(s, '"', '\"')
    return s
}

ValidateCurlValue(name, v) {
    if (v = "")
        return
    if (InStr(v, '"') || InStr(v, "`n") || InStr(v, "`r"))
        throw Error(name " содержит кавычку или перевод строки — так нельзя записать в конфиг curl")
}

; Пустая строка = напрямую. Без схемы — http://. SOCKS: socks5:// / socks5h:// (DNS через прокси) / socks4://.
NormalizeProxy(s) {
    s := Trim(s)
    if (s = "")
        return ""
    if (SubStr(s, 1, 1) = '"' && SubStr(s, -1) = '"')
        s := SubStr(s, 2, StrLen(s) - 2)
    low := StrLower(s)
    known := ["http://", "https://", "socks4://", "socks4a://", "socks5://", "socks5h://"]
    for p in known
        if (SubStr(low, 1, StrLen(p)) = p)
            return s
    if InStr(s, "://")
        throw Error("Неизвестная схема прокси. Нужны http://, https://, socks5:// или socks5h://")
    return "http://" s
}

UrlIsLoopback(url) {
    u := StrLower(url)
    return InStr(u, "://127.0.0.1") || InStr(u, "://localhost") || InStr(u, "://[::1]")
}

; Строки для curl -K. Локальный Ollama не гоняем через прокси.
CurlProxyCfg(url, proxy) {
    proxy := NormalizeProxy(proxy)
    if (proxy = "" || UrlIsLoopback(url))
        return ""
    ValidateCurlValue("proxy", proxy)
    return 'proxy = "' CurlQuote(proxy) '"`n'
        . 'noproxy = "localhost,127.0.0.1,::1"`n'
}

CleanupTextToolsTemp() {
    loop files A_Temp "\TextTools_*"
        try FileDelete(A_LoopFileFullPath)
}

ResolveProxy(opts) {
    if (IsObject(opts) && opts.HasProp("proxy"))
        return opts.proxy
    if (IsSet(cfg) && IsObject(cfg))
        return EffectiveProxy(cfg)
    return ""
}

; GET/POST через curl -K. opts: timeout, api_key, json (bool), bodyFile, retries, proxy
; Возвращает {curl, status, body}. Временные файлы снимаются в finally.
HttpCurl(url, opts := "") {
    if !IsObject(opts)
        opts := {}
    timeout := opts.HasProp("timeout") ? opts.timeout : 90
    retries := opts.HasProp("retries") ? opts.retries : 0
    ValidateCurlValue("URL", url)
    if (opts.HasProp("api_key"))
        ValidateCurlValue("API key", opts.api_key)
    static seq := 0
    seq += 1
    respF := A_Temp "\TextTools_resp_" seq ".json"
    curlF := A_Temp "\TextTools_curl_" seq ".cfg"
    hdrF := A_Temp "\TextTools_hdr_" seq ".txt"
    try FileDelete(respF)
    try FileDelete(hdrF)
    curlCfg := 'url = "' CurlQuote(url) '"`n'
        . "silent`nshow-error`nmax-time = " timeout "`n"
        . 'dump-header = "' StrReplace(hdrF, "\", "\\") '"`n'
        . 'output = "' StrReplace(respF, "\", "\\") '"`n'
    curlCfg .= CurlProxyCfg(url, ResolveProxy(opts))
    if (opts.HasProp("json") && opts.json)
        curlCfg .= 'header = "Content-Type: application/json; charset=utf-8"`n'
    if (opts.HasProp("api_key") && opts.api_key != "")
        curlCfg .= 'header = "Authorization: Bearer ' CurlQuote(opts.api_key) '"`n'
    if (opts.HasProp("bodyFile") && opts.bodyFile != "")
        curlCfg .= 'data-binary = "@' StrReplace(opts.bodyFile, "\", "\\") '"`n'
    f := FileOpen(curlF, "w", "UTF-8-RAW")
    f.Write(curlCfg)
    f.Close()
    code := 0
    try {
        attempt := 0
        loop {
            attempt++
            code := RunWait('"' CurlExe() '" -K "' curlF '"', , "Hide")
            if (code != 6 && code != 7)
                break
            if (attempt > retries)
                break
            Sleep 400
        }
        status := ParseHttpStatus(hdrF)
        body := ""
        try body := FileRead(respF, "UTF-8")
        return {curl: code, status: status, body: body}
    } finally {
        try FileDelete(curlF)
        try FileDelete(respF)
        try FileDelete(hdrF)
    }
}

; Последняя строка HTTP/x.x NNN в dump-header (после редиректов)
ParseHttpStatus(hdrF) {
    txt := ""
    try txt := FileRead(hdrF, "UTF-8")
    if (txt = "")
        return 0
    last := 0, pos := 1
    loop {
        p := InStr(txt, "HTTP/", true, pos)
        if !p
            break
        sp := InStr(txt, " ", true, p)
        if sp {
            n := SubStr(txt, sp + 1, 3)
            if (StrLen(n) = 3 && IsDigit(SubStr(n, 1, 1)) && IsDigit(SubStr(n, 2, 1)) && IsDigit(SubStr(n, 3, 1)))
                last := Integer(n)
        }
        pos := p + 1
    }
    return last
}

HttpStatusText(status) {
    switch status {
        case 401:
            return Loc("http.401", "HTTP 401: ключ отклонён. Проверьте API key в настройках.")
        case 402:
            return Loc("http.402", "HTTP 402: не хватает средств на счёте провайдера.")
        case 404:
            return Loc("http.404", "HTTP 404: модель или адрес не найдены. Проверьте имя модели и Base URL.")
        case 429:
            return Loc("http.429", "HTTP 429: слишком много запросов, подождите и повторите.")
        default:
            if (status >= 500)
                return Format(Loc("http.5xx", "HTTP {1}: ошибка сервера провайдера."), status)
            if (status >= 400)
                return "HTTP " status
    }
    return ""
}

; Сортировка вставками. less(a, b) = true, если a должен стоять раньше b.
SortBy(arr, less) {
    i := 2
    while (i <= arr.Length) {
        j := i
        while (j > 1 && less.Call(arr[j], arr[j - 1])) {
            t := arr[j], arr[j] := arr[j - 1], arr[j - 1] := t
            j--
        }
        i++
    }
}
