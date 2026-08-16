; TextTools - определение приложения-источника и хранилище профилей (apps.ini).
; Подключается из TextTools.ahk, отдельно не запускается.
;
; Приложение определяется по окну, которое было активно в момент хоткея
; (gPrevWin): имя процесса + при необходимости подстрока заголовка. Найденный
; hint дописывается к системному промпту в BuildInstruction.
;
; Незнакомое приложение добавляется в список само, с нейтральным профилем
; (hint пустой = работаем как обычно), и в ФОНЕ уходит запрос к модели за
; описанием. Промпт запроса требует ответить UNKNOWN, если модель не уверена, -
; тогда профиль остаётся нейтральным, а не заполняется выдумкой.

CloneApps(list) {
    out := []
    for a in list
        out.Push({proc: a.proc, title: a.title, name: a.name, desc: a.desc, hint: a.hint})
    return out
}

LoadApps() {
    path := APPS_INI
    if !FileExist(path) {
        TT_Log("apps.ini не найден, раскладываю заводской список приложений")
        SaveApps(DEFAULT_APPS)
        return CloneApps(DEFAULT_APPS)
    }
    out := []
    try {
        ; как и в actions.ini: подряд до первой отсутствующей секции, с потолком
        loop MAX_ACTIONS {
            sec := "app" A_Index
            proc := IniRead(path, sec, "proc", "")
            if (proc = "")
                break
            ; "-" разворачивается обратно в пустую строку сразу при чтении,
            ; чтобы дальше по коду поля были обычными строками
            out.Push({
                proc: JsonUnesc(proc),
                title: AppField(JsonUnesc(IniRead(path, sec, "title", ""))),
                name: AppField(JsonUnesc(IniRead(path, sec, "name", ""))),
                desc: AppField(JsonUnesc(IniRead(path, sec, "desc", ""))),
                hint: AppField(JsonUnesc(IniRead(path, sec, "hint", "")))})
        }
    } catch as e {
        TT_Log("apps.ini не читается (" e.Message "), беру заводской список")
        return CloneApps(DEFAULT_APPS)
    }
    if (out.Length = 0)
        return CloneApps(DEFAULT_APPS)
    TT_Log("загружено профилей приложений: " out.Length)
    return out
}

SaveApps(list) {
    path := APPS_INI
    try FileDelete(path)
    IniWrite(list.Length, path, "apps", "count")
    for i, a in list {
        sec := "app" i
        IniWrite(JsonEsc(a.proc), path, sec, "proc")
        ; "-" вместо пустой строки: пустое значение в INI неотличимо от
        ; отсутствующего ключа (та же грабля, что с клавишами действий)
        IniWrite(a.title = "" ? "-" : JsonEsc(a.title), path, sec, "title")
        IniWrite(JsonEsc(a.name), path, sec, "name")
        IniWrite(a.desc = "" ? "-" : JsonEsc(a.desc), path, sec, "desc")
        IniWrite(a.hint = "" ? "-" : JsonEsc(a.hint), path, sec, "hint")
    }
}

; "-" из файла означает пустое поле
AppField(v) => (v = "-" ? "" : v)

; Ищет профиль по окну. Записи с заголовком проверяются первыми: иначе общий
; "chrome.exe" перехватывал бы Jira и Confluence.
MatchApp(proc, title) {
    if (proc = "")
        return ""
    for pass in [1, 2] {
        for a in APPS {
            hasTitle := a.title != ""
            if ((pass = 1) != hasTitle)
                continue                                  ; 1-й проход - только записи с заголовком
            if (a.proc = "" || !(proc = a.proc))          ; сравнение без учёта регистра
                continue
            if (hasTitle && !InStr(title, a.title))
                continue
            return a
        }
    }
    return ""
}

; Возвращает профиль окна-источника; незнакомое приложение заводит сам
ResolveApp(hwnd) {
    global APPS
    if !(hwnd && WinExist("ahk_id " hwnd))
        return ""
    proc := "", title := ""
    try proc := WinGetProcessName("ahk_id " hwnd)
    try title := WinGetTitle("ahk_id " hwnd)
    if (proc = "")
        return ""
    if (found := MatchApp(proc, title))
        return found
    ; новое приложение: нейтральный профиль сразу, описание - в фоне
    entry := {proc: proc, title: "", name: proc, hint: "",
        desc: "Добавлено автоматически" (title != "" ? " (окно: " SubStr(title, 1, 60) ")" : "")}
    APPS.Push(entry)
    try SaveApps(APPS)
    TT_Log("новое приложение: " proc " - профиль нейтральный, запрашиваю описание")
    try RequestAppDescription(entry, title)
    return entry
}

AppProfilingOn() {
    return CfgFlag(cfg, "app_profiling") = "1"
}

; Готовая добавка к системному промпту (пустая строка = поправки нет)
AppHint(hwnd) {
    a := ResolveApp(hwnd)
    return IsObject(a) ? a.hint : ""
}

; ---------------------------------------------------------------------------
; Фоновый запрос описания приложения
; ---------------------------------------------------------------------------

; Промпт намеренно жёсткий: без догадок и с обязательным UNKNOWN, если модель
; не уверена. Пустой hint безопаснее выдуманного - он просто ничего не меняет.
APP_DESC_PROMPT := "
(
You identify Windows desktop applications by their executable file name and window title.

If you do not reliably know this specific application, output exactly one word: UNKNOWN
Output UNKNOWN also when the executable name is generic, ambiguous, or could belong to several different products. Never guess, never invent a product.

Otherwise output exactly two lines and nothing else:
DESC: <one sentence in Russian: what this application is and what kind of text a user would select in it>
HINT: <one or two sentences in English, addressed to a text-processing assistant: how to treat text taken from this application - register, what must be preserved verbatim, what to avoid>

The HINT must be specific to this application. If you cannot write a HINT that is more useful than no instruction at all, output UNKNOWN instead.
)"

RequestAppDescription(entry, sampleTitle) {
    static seq := 0
    if (cfg.api_key = "")
        return
    if !AppProfilingOn()
        return
    payload := "Executable: " entry.proc "`nWindow title: " (sampleTitle != "" ? sampleTitle : "(none)")
    if (MsgBox(Loc("apps.profile.ask", "Отправить провайдеру сведения о приложении, чтобы подобрать поправку к промпту?")
            . "`n`n" payload, "TextTools", "YesNo Icon? 262144") != "Yes")
        return
    seq += 1
    tag := "TextTools_app_" seq "_" A_TickCount
    reqF := A_Temp "\" tag "_req.json"
    respF := A_Temp "\" tag "_resp.json"
    curlF := A_Temp "\" tag "_curl.cfg"
    cmdF := A_Temp "\" tag ".cmd"
    markF := A_Temp "\" tag "_done.txt"

    user := payload
    body := '{"model":"' JsonEsc(cfg.model) '","messages":['
        . '{"role":"system","content":"' JsonEsc(APP_DESC_PROMPT) '"},'
        . '{"role":"user","content":"' JsonEsc(user) '"}'
        . '],"stream":false}'
    f := FileOpen(reqF, "w", "UTF-8-RAW")
    f.Write(body)
    f.Close()
    try ValidateCurlValue("URL", RTrim(cfg.base_url, "/") "/chat/completions")
    try ValidateCurlValue("API key", cfg.api_key)
    curlCfg := 'url = "' CurlQuote(RTrim(cfg.base_url, "/") "/chat/completions") '"`n'
        . "silent`nshow-error`nmax-time = 60`n"
        . 'header = "Content-Type: application/json; charset=utf-8"`n'
        . (cfg.api_key != "" ? 'header = "Authorization: Bearer ' cfg.api_key '"`n' : "")
        . CurlProxyCfg(RTrim(cfg.base_url, "/") "/chat/completions", EffectiveProxy(cfg))
        . 'data-binary = "@' StrReplace(reqF, "\", "\\") '"`n'
        . 'output = "' StrReplace(respF, "\", "\\") '"`n'
    f := FileOpen(curlF, "w", "UTF-8-RAW")
    f.Write(curlCfg)
    f.Close()
    ; .cmd-обёртка вместо голого curl: маркер появляется в любом случае, и по
    ; нему видно, что файл ответа дописан целиком, а не читается на середине
    f := FileOpen(cmdF, "w", "UTF-8-RAW")
    f.Write("@echo off`r`n" Format('"{1}" -K "{2}"`r`necho done> "{3}"`r`n', CurlExe(), curlF, markF))
    f.Close()
    Run(cmdF, , "Hide")
    SetTimer(CheckAppDescription.Bind(entry, respF, markF, [reqF, curlF, cmdF], A_TickCount), 1200)
}

CheckAppDescription(entry, respF, markF, tmpFiles, started) {
    global APPS
    if (A_TickCount - started > 90000) {
        SetTimer(, 0)
        TT_Log("описание для " entry.proc " не получено (таймаут)")
        CleanupAppTmp(tmpFiles, respF, markF)
        return
    }
    if !FileExist(markF)
        return
    SetTimer(, 0)
    raw := ""
    try raw := FileRead(respF, "UTF-8")
    CleanupAppTmp(tmpFiles, respF, markF)
    if (raw = "") {
        TT_Log("описание для " entry.proc ": пустой ответ")
        return
    }
    ; фоновый запрос идет мимо CallApiMsgs, но платный - учитываем здесь
    u := ExtractUsage(raw)
    if IsObject(u)
        UsageRecord(cfg.model, u, "описание приложения")
    text := ""
    try text := Trim(JsonUnesc(ExtractContent(raw)), " `t`r`n")
    catch as e {
        TT_Log("описание для " entry.proc ": ответ не разобран (" e.Message ")")
        return
    }
    if (text = "" || InStr(text, "UNKNOWN") = 1) {
        TT_Log("модель не знает " entry.proc " - профиль остаётся нейтральным")
        return
    }
    desc := "", hint := ""
    for line in StrSplit(StrReplace(text, "`r", ""), "`n") {
        line := Trim(line)
        if (SubStr(line, 1, 5) = "DESC:")
            desc := Trim(SubStr(line, 6))
        else if (SubStr(line, 1, 5) = "HINT:")
            hint := Trim(SubStr(line, 6))
    }
    if (desc = "" && hint = "") {
        TT_Log("описание для " entry.proc ": формат не распознан, профиль нейтральный")
        return
    }
    if (desc != "")
        entry.desc := desc
    entry.hint := hint
    try SaveApps(APPS)
    TT_Log("описание получено: " entry.proc (hint = "" ? " (без поправки к промпту)" : ""))
}

CleanupAppTmp(tmpFiles, respF, markF) {
    for p in tmpFiles
        try FileDelete(p)
    try FileDelete(respF)
    try FileDelete(markF)
}
