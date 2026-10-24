#Requires AutoHotkey v2.0
#SingleInstance Off
; Чистая логика без GUI — гейт CI. Отчёт в файл, не в stdout (AHK GUI hang).
; Набор #Include обязан совпадать с TextTools.ahk: неполный набор даёт молчаливое зависание.
REPORT := A_Temp "\tt_logic_report.txt"
try FileDelete(REPORT)
OnError((e, mode) => (FileAppend("ОШИБКА: " e.Message " | " e.Extra " @ " e.File ":" e.Line "`n", REPORT, "UTF-8"), ExitApp(9)))
LOGF := A_Temp "\tt_logic.log"
INI := A_Temp "\tt_logic_cfg.ini"
ACTIONS_INI := A_Temp "\tt_logic_actions.ini"
APPS_INI := A_Temp "\tt_logic_apps.ini"
USAGEF := A_Temp "\tt_logic_usage.log"
PRICESF := A_Temp "\tt_logic_prices.json"
HISTF := A_Temp "\tt_logic_history.txt"
LANGS_INI := A_Temp "\tt_logic_langs.ini"
MAX_ACTIONS := 200
gStartTick := A_TickCount
gUiStrings := Map()
gBusy := false
gBusyGui := "", gBusyText := "", gBusyInfo := "", gBusyDetail := "", gBusyStart := 0
gHistory := [], gPrevWin := 0, gForeWin := 0, gSel := "", gSettingsGui := ""
gLastUsage := "", gCallLabel := "", gLastAction := "", gLastInstr := "", gRestoreTimer := ""
gHotkeyCurrent := "", gHotkeyRepeatCurrent := "", gEditorGui := "", gMenuEdit := "", gMenuFirstBtn := "", gMenuGui := ""
gClipWarned := false
gPriceIndex := ""
gTakenKeyHwnd := 0
try FileDelete(HISTF)
try FileDelete(ACTIONS_INI)
try FileDelete(APPS_INI)

#Include Lib/prompts.ahk
#Include Lib/actions.ahk
#Include Lib/apps.ahk
#Include Lib/langs.ahk

cfg := CfgDefaults()
cfg.base_url := "https://openrouter.ai/api/v1"
cfg.model := "anthropic/claude-haiku-4.5"
cfg.ui_lang := "ru"
LANGS := LoadLangs()
ACTIONS := CloneActions(DEFAULT_ACTIONS)
QUICK_REFINE := CloneQuick(DEFAULT_QUICK)
APPS := CloneApps(DEFAULT_APPS)

Say(s) => FileAppend(s "`n", REPORT, "UTF-8")
fails := 0
checks := 0
Check(ok, msg) {
    global fails, checks
    checks++
    if !ok
        fails++
    Say((ok ? "OK   " : "FAIL ") msg)
}

Check(JsonEsc("a`"b") = "a\`"b", "JsonEsc экранирует кавычку")
Check(JsonEsc("a\b") = "a\\b", "JsonEsc экранирует обратный слэш")
Check(JsonUnesc("a\nb") = "a`nb", "JsonUnesc \\n")
Check(JsonUnesc("x\by") = "x" Chr(8) "y", "JsonUnesc \\b")
Check(JsonUnesc("x\fy") = "x" Chr(12) "y", "JsonUnesc \\f")
badU := JsonUnesc("pre\uZZZZpost")
; Не «не бросает», а «текст остался ровно тем, что был»: прежняя проверка
; !InStr(badU, "Error") была истинной при любом результате.
Check(badU = "pre\uZZZZpost", "битый \\uXXXX остаётся текстом как есть: «" badU "»")
emoji := JsonUnesc("\uD83D\uDE00")
Check(Ord(emoji) = 0x1F600, "суррогатная пара склеена в код 0x1F600, получено " Ord(emoji))
Check(StrLen(emoji) = 2, "в UTF-16 это ровно два кодовых блока, получено " StrLen(emoji))

sample := '{"id":"x","content":"WRONG","choices":[{"message":{"role":"assistant","content":"RIGHT"}}]}'
Check(JsonUnesc(ExtractContent(sample)) = "RIGHT", "ExtractContent берёт choices[0].message.content")
u := ExtractUsage('{"usage":{"prompt_tokens":10,"completion_tokens":2}}')
Check(IsObject(u) && u.prompt = 10 && u.completion = 2, "ExtractUsage")
Check(ExtractErrorMessage('{"error":{"message":"nope"}}') = "nope", "ExtractErrorMessage")

html := MdToHtml("# Hi`n`n**bold**")
Check(InStr(html, "<h1>") && InStr(html, "<b>"), "MdToHtml заголовки и жирный")

sys := {role: "system", content: "sys"}
msgs := [sys]
i := 1
while (i <= 12) {
    msgs.Push({role: "user", content: "u" i})
    msgs.Push({role: "assistant", content: "a" i})
    i++
}
trimmed := TrimConvo(msgs)
Check(trimmed.Length <= 9 && trimmed[1].role = "system" && trimmed[2].role = "user", "TrimConvo: system + хвост, user после system")

a := DEFAULT_ACTIONS[1]
sysInstr := BuildInstruction(a, "from chrome")
Check(InStr(sysInstr, "SOURCE APPLICATION: from chrome") && InStr(sysInstr, a.task), "BuildInstruction вставляет hint и TASK")

APPS := [{proc: "chrome.exe", title: "Jira", name: "Jira", desc: "", hint: "jira-hint"},
         {proc: "chrome.exe", title: "", name: "Chrome", desc: "", hint: "chrome-hint"}]
hit := MatchApp("chrome.exe", "ACME - Jira - ticket")
Check(IsObject(hit) && hit.name = "Jira", "MatchApp: запись с заголовком раньше общего процесса")
hit2 := MatchApp("chrome.exe", "Google")
Check(IsObject(hit2) && hit2.name = "Chrome", "MatchApp: общий процесс без заголовка")
APPS := CloneApps(DEFAULT_APPS)

FileAppend("2026-01-01|bad|m|not-a-number|-1|sel|res`n2026-01-02|ok|m|3|-1|hello|world`n", HISTF, "UTF-8")
h := LoadHistory()
Check(h.Length >= 1, "LoadHistory на битой строке не падает: записей " h.Length)
try FileDelete(HISTF)

orActs := CloneActions(DEFAULT_ACTIONS)
ApplyFactoryRoles(orActs, "https://openrouter.ai/api/v1")
proof := "", keys := "", rew := ""
for x in orActs {
    if (x.id = "proofread")
        proof := x.model
    if (x.id = "keypoints")
        keys := x.model
    if (x.id = "rewrite")
        rew := x.model
}
Check(proof = "qwen/qwen3.7-flash", "роль mech на OpenRouter: " proof)
Check(keys = "anthropic/claude-sonnet-5", "роль deep на OpenRouter: " keys)
Check(rew = "", "действие без роли остаётся на модели по умолчанию")
Check(!InStr(proof, "gpt-4.1") && !InStr(keys, "gpt-4.1"), "заводские роли не оставляют голые слаги OpenAI на OpenRouter")

xActs := CloneActions(DEFAULT_ACTIONS)
ApplyFactoryRoles(xActs, "https://api.x.ai/v1")
xProof := ""
for x in xActs
    if (x.id = "proofread")
        xProof := x.model
Check(xProof = "grok-4.3", "неполная сетка xAI: mech откатывается на main: " xProof)

hdr := A_Temp "\tt_logic_hdr.txt"
try FileDelete(hdr)
FileAppend("HTTP/1.1 301 Moved`r`nLocation: /`r`n`r`nHTTP/1.1 401 Unauthorized`r`n", hdr, "UTF-8")
Check(ParseHttpStatus(hdr) = 401, "ParseHttpStatus берёт последний статус после редиректа")
try FileDelete(hdr)
Check(InStr(HttpStatusText(401), "401") && InStr(HttpStatusText(429), "429"), "HttpStatusText 401/429")
Check(InStr(HttpStatusText(503), "503"), "HttpStatusText 5xx")

srt := ["gpt-5", "aaa", "gpt-4.1"]
SortBy(srt, (a, b) => StrCompare(a, b) < 0)
Check(srt[1] = "aaa" && srt[3] = "gpt-5", "SortBy строк")
emptyLast := [{id: "z", v: 0}, {id: "a", v: 5}, {id: "m", v: 0}]
SortBy(emptyLast, (a, b) => (a.v > 0) != (b.v > 0) ? (a.v > 0) : StrCompare(a.id, b.id) < 0)
Check(emptyLast[1].id = "a" && emptyLast[3].v = 0, "SortBy: строки без данных внизу")

; Проверяем не «бросило что-нибудь», а ЧТО именно: сообщение обязано называть
; поле, иначе пользователь не узнает, где именно у него кавычка.
cvQuote := ""
try {
    ValidateCurlValue("API key", "abc`"def")
} catch as e {
    cvQuote := e.Message
}
Check(InStr(cvQuote, "API key"), "ValidateCurlValue называет поле с кавычкой: «" cvQuote "»")
cvNl := ""
try {
    ValidateCurlValue("URL", "http://x`ny")
} catch as e {
    cvNl := e.Message
}
Check(InStr(cvNl, "URL"), "ValidateCurlValue называет поле с переводом строки: «" cvNl "»")
cvOk := true
try {
    ValidateCurlValue("proxy", "")
    ValidateCurlValue("proxy", "http://host:3128")
} catch {
    cvOk := false
}
Check(cvOk, "ValidateCurlValue пропускает пустое и нормальное значение")
Check(CurlQuote('a"b\c') = 'a\"b\\c', "CurlQuote экранирует кавычку и слэш: " CurlQuote('a"b\c'))

Check(NormalizeProxy("") = "", "NormalizeProxy пустой")
Check(NormalizeProxy("127.0.0.1:7890") = "http://127.0.0.1:7890", "NormalizeProxy без схемы → http://")
Check(NormalizeProxy("socks5h://127.0.0.1:1080") = "socks5h://127.0.0.1:1080", "NormalizeProxy socks5h без изменений")
Check(NormalizeProxy("http://user:pass@host:3000") = "http://user:pass@host:3000", "NormalizeProxy http с логином")
; Та же правка: сообщение обязано перечислять рабочие схемы, иначе из него
; нельзя понять, что писать в поле прокси.
npMsg := ""
try {
    NormalizeProxy("ftp://host:21")
} catch as e {
    npMsg := e.Message
}
Check(InStr(npMsg, "http://") && InStr(npMsg, "socks5"),
    "NormalizeProxy объясняет, какие схемы годятся: «" npMsg "»")
Check(NormalizeProxy('"127.0.0.1:7890"') = "http://127.0.0.1:7890",
    "NormalizeProxy снимает обрамляющие кавычки: " NormalizeProxy('"127.0.0.1:7890"'))
Check(NormalizeProxy("  socks4://h:1  ") = "socks4://h:1", "NormalizeProxy обрезает пробелы")
Check(UrlIsLoopback("http://127.0.0.1:11434/v1/chat/completions"), "UrlIsLoopback 127.0.0.1")
Check(CurlProxyCfg("https://openrouter.ai/api/v1/models", "") = "", "CurlProxyCfg без прокси пустой")
Check(CurlProxyCfg("http://127.0.0.1:11434/v1/chat/completions", "http://user:pass@host:3000") = "", "localhost не идёт через прокси")
pxBlock := CurlProxyCfg("https://openrouter.ai/api/v1/models", "user:pass@host:3000")
Check(InStr(pxBlock, "proxy = ") && InStr(pxBlock, "http://user:pass@host:3000") && InStr(pxBlock, "noproxy"), "CurlProxyCfg пишет proxy и noproxy")
Check(EffectiveProxy({proxy: "http://h:1", proxy_on: "0"}) = "", "EffectiveProxy выключен галочкой")
Check(EffectiveProxy({proxy: "http://h:1", proxy_on: "1"}) = "http://h:1", "EffectiveProxy включён")
Check(EffectiveProxy({proxy: "http://h:1"}) = "http://h:1", "EffectiveProxy без proxy_on — URL действует")

ks := CloneActions(DEFAULT_ACTIONS)
proofIdx := 0, promptIdx := 0
for i, x in ks {
    if (x.id = "proofread")
        proofIdx := i
    if (x.id = "prompt")
        promptIdx := i
}
Check(proofIdx && promptIdx && ks[proofIdx].key = "1" && ks[promptIdx].key = "6",
    "заводские клавиши: Вычитать=1, Промпт=6")
Check(FindMenuKeyOwner(ks, "1", promptIdx) = proofIdx, "FindMenuKeyOwner: 1 держит Вычитать")
Check(FindMenuKeyOwner(ks, "1", proofIdx) = 0, "FindMenuKeyOwner: exceptRow не считает себя")
Check(FindMenuKeyOwner(ks, "", 0) = 0, "FindMenuKeyOwner: пустая клавиша ничья")
oldPromptKey := ks[promptIdx].key
Check(YieldMenuKey(ks, proofIdx, oldPromptKey, "swap"), "YieldMenuKey swap")
Check(ks[proofIdx].key = oldPromptKey, "swap: Вычитать получает прежнюю клавишу Промпта")
ks[promptIdx].key := "1"
Check(FindMenuKeyOwner(ks, "1", promptIdx) = 0, "после swap клавиша 1 свободна для Промпта")
ks2 := CloneActions(DEFAULT_ACTIONS)
Check(YieldMenuKey(ks2, proofIdx, ks2[promptIdx].key, "take"), "YieldMenuKey take")
Check(ks2[proofIdx].key = "", "take: Вычитать остаётся без клавиши")
Check(!YieldMenuKey(ks2, 0, "1", "take"), "YieldMenuKey: несуществующий ряд")
Check(!YieldMenuKey(ks2, proofIdx, "1", "nope"), "YieldMenuKey: неизвестный mode")

; ===========================================================================
; Markdown -> HTML: до этого проверялись только заголовок и жирный, а окно
; результата показывает ещё таблицы, списки, блоки кода и ссылки.
; ===========================================================================
BT := Chr(96)
FENCE3 := BT BT BT

mdTab := MdToHtml("| a | b |`n|---|---|`n| 1 | 2 |")
Check(InStr(mdTab, "<table>") && InStr(mdTab, "</table>"), "MdToHtml: таблица обрамлена <table>")
Check(InStr(mdTab, "<th>a</th>") && InStr(mdTab, "<th>b</th>"), "MdToHtml: шапка таблицы в <th>")
Check(InStr(mdTab, "<td>1</td>") && InStr(mdTab, "<td>2</td>"), "MdToHtml: тело таблицы в <td>")
mdNoTab := MdToHtml("| a | b |`n| 1 | 2 |")
Check(!InStr(mdNoTab, "<table>"), "MdToHtml: без строки-разделителя это не таблица, а абзац")

mdCode := MdToHtml(FENCE3 "`n" "**не жирный** < >" "`n" FENCE3)
Check(InStr(mdCode, "<pre>") && InStr(mdCode, "</pre>"), "MdToHtml: блок кода обрамлён <pre>")
Check(InStr(mdCode, "&lt;") && InStr(mdCode, "&gt;"), "MdToHtml: внутри блока кода угловые скобки экранированы")
Check(!InStr(mdCode, "<b>"), "MdToHtml: внутри блока кода разметка не применяется")
Check(InStr(MdToHtml(FENCE3 "`n" "abc"), "</pre>"), "MdToHtml: незакрытый блок кода всё равно закрывается")

mdList := MdToHtml("- раз`n- два`n`n1. один`n2. два")
Check(InStr(mdList, "<ul>") && InStr(mdList, "</ul>"), "MdToHtml: маркированный список")
Check(InStr(mdList, "<ol>") && InStr(mdList, "</ol>"), "MdToHtml: нумерованный список")
Check(InStr(mdList, "<li>раз</li>") && InStr(mdList, "<li>один</li>"), "MdToHtml: пункты списков")
Check(InStr(mdList, "</ul>") < InStr(mdList, "<ol>"), "MdToHtml: список закрывается перед началом другого")
Check(InStr(MdToHtml("текст`n`n---`n`nещё"), "<hr>"), "MdToHtml: горизонтальная линия")

mdInl := MdToHtml("Ссылка [текст](http://example.test) и " BT "код" BT " и *курсив* и <тег>")
Check(InStr(mdInl, "<code>код</code>"), "MdInline: обратные кавычки дают <code>")
Check(InStr(mdInl, "<i>курсив</i>"), "MdInline: звёздочки дают <i>")
Check(InStr(mdInl, "&lt;тег&gt;"), "MdInline: угловые скобки экранируются")
Check(!InStr(mdInl, "http://example.test"), "MdInline: от ссылки остаётся только текст, адрес убирается")
Check(InStr(MdToHtml("__жирный__"), "<b>жирный</b>"), "MdInline: двойное подчёркивание тоже жирный")

Check(HtmlEsc("a & <b> &amp;") = "a &amp; &lt;b&gt; &amp;amp;", "HtmlEsc экранирует & первым: " HtmlEsc("a & <b> &amp;"))
Check(IsTableSep("|---|:--:|"), "IsTableSep: выравнивание двоеточиями")
Check(!IsTableSep("| a |"), "IsTableSep: без дефиса не разделитель")
Check(!IsTableSep(""), "IsTableSep: пустая строка не разделитель")
Check(!IsTableSep("|--x--|"), "IsTableSep: посторонний символ не разделитель")
Check(MdTableRow("| a | b |", "td") = "<tr><td>a</td><td>b</td></tr>`n", "MdTableRow собирает строку таблицы")

; ===========================================================================
; JSON: разбор ответа провайдера. Это единственный путь данных из сети внутрь.
; ===========================================================================
Check(JsonEsc(Chr(1) "a" A_Tab "b") = "\u0001a\tb", "JsonEsc прячет управляющие символы: " JsonEsc(Chr(1) "a" A_Tab "b"))
Check(JsonEsc("a`r`nb") = "a\nb", "JsonEsc сводит CRLF к \\n")
rtStr := "слэш \ кавычка " Chr(34) " таб" A_Tab "конец"
Check(JsonUnesc(JsonEsc(rtStr)) = rtStr, "JsonEsc/JsonUnesc: круговой обход без потерь")
Check(JsonUnesc("a\rb") = "ab", "JsonUnesc выбрасывает \\r")
Check(IsHex4("00AF") && IsHex4("ffff"), "IsHex4 принимает четыре шестнадцатеричные цифры")
Check(!IsHex4("00A") && !IsHex4("00AG") && !IsHex4("00AFF"), "IsHex4 отвергает короткое, длинное и нецифру")

escResp := '{"choices":[{"message":{"content":"скажи \"да\" ровно"}}]}'
Check(JsonUnesc(ExtractContent(escResp)) = 'скажи "да" ровно',
    "ExtractContent: экранированная кавычка не обрезает content: «" JsonUnesc(ExtractContent(escResp)) "»")
Check(JsonUnesc(ExtractContent('{"content":"запасной путь"}')) = "запасной путь",
    "ExtractContent: без choices берётся первый content")
ecMsg := ""
try {
    ExtractContent('{"error":{"message":"лимит исчерпан"}}')
} catch as e {
    ecMsg := e.Message
}
Check(ecMsg = "лимит исчерпан", "ExtractContent бросает именно текст ошибки из тела: «" ecMsg "»")
ecNo := ""
try {
    ExtractContent('{"object":"list"}')
} catch as e {
    ecNo := e.Message
}
Check(InStr(ecNo, "content"), "ExtractContent без content объясняет, чего не нашёл: «" ecNo "»")

uPart := ExtractUsage('{"usage":{"prompt_tokens":7}}')
Check(IsObject(uPart) && uPart.prompt = 7 && uPart.completion = 0, "ExtractUsage: половина полей - остальное ноль")
Check(ExtractUsage('{"usage":{}}') = "", "ExtractUsage: usage без чисел - пусто")
Check(ExtractErrorMessage('{"ok":1}') = "", "ExtractErrorMessage: нет error - пусто")
Check(JsonIntAfter('{"n": 42}', '"n"', 1) = 42, "JsonIntAfter читает число после ключа")
Check(JsonIntAfter('{"n": null}', '"n"', 1) = -1, "JsonIntAfter: null - минус один, а не цифра из соседнего поля")
Check(JsonIntAfter('{"m": 1}', '"n"', 1) = -1, "JsonIntAfter: нет ключа - минус один")
Check(JsonStringAfter('{"a":"x","b":"y"}', '"b"') = "y", "JsonStringAfter берёт значение нужного ключа")
Check(JsonStringAfter('{"a":5}', '"a"') = "", "JsonStringAfter: не строковое значение - пусто")

Check(ModelIsUseless("text-embedding-3-small") && ModelIsUseless("WHISPER-1") && ModelIsUseless("gpt-4o-audio"),
    "ModelIsUseless отсеивает служебные модели (регистр не важен)")
Check(!ModelIsUseless("gpt-4.1") && !ModelIsUseless("claude-sonnet-5"), "ModelIsUseless не трогает обычные модели")
mResp := '{"object":"list","data":['
    . '{"id":"b-model","object":"model"},'
    . '{"id":"text-embedding-3-small","object":"model"},'
    . '{"id":"b-model","object":"model"},'
    . '{"id":"a-model","object":"model"}]}'
mIds := ParseModelIds(mResp)
Check(mIds.Length = 2, "ParseModelIds: служебные отброшены, дубли убраны (" mIds.Length ")")
Check(mIds.Length = 2 && mIds[1] = "a-model" && mIds[2] = "b-model", "ParseModelIds сортирует список")
Check(ParseModelIds('{"data":[]}').Length = 0, "ParseModelIds: пустой список не ломает разбор")
Check(ParseModelIds("не json вовсе").Length = 0, "ParseModelIds на мусоре не падает")
Check(BodyText([{role: "user", content: "раз"}, {role: "assistant", content: "два"}]) = "раз`nдва`n",
    "BodyText склеивает содержимое сообщений")

; ===========================================================================
; TrimConvo: после system первым обязан идти user. Нечётная граница - тот самый
; случай, когда на месте user оказывается assistant и строгий провайдер даёт 400.
; ===========================================================================
oddConvo := [{role: "system", content: "s"}]
ocK := 1
while (ocK <= 23) {
    oddConvo.Push({role: (Mod(ocK, 2) = 1 ? "user" : "assistant"), content: "m" ocK})
    ocK++
}
Check(oddConvo.Length = 24, "подготовка диалога из 24 сообщений: " oddConvo.Length)
ocTrim := TrimConvo(oddConvo)
Check(ocTrim.Length = 8, "TrimConvo: при нечётной границе хвост короче на одно (" ocTrim.Length ")")
Check(ocTrim.Length >= 2 && ocTrim[1].role = "system" && ocTrim[2].role = "user",
    "TrimConvo: после system идёт user, а не assistant")
Check(TrimConvo([{role: "system"}, {role: "user"}]).Length = 2, "TrimConvo короткий диалог не режет")

; ===========================================================================
; HTTP: статусы и прокси
; ===========================================================================
Check(InStr(HttpStatusText(402), "402"), "HttpStatusText 402")
Check(InStr(HttpStatusText(404), "404"), "HttpStatusText 404")
Check(HttpStatusText(418) = "HTTP 418", "HttpStatusText: прочий 4xx как есть: " HttpStatusText(418))
Check(HttpStatusText(200) = "", "HttpStatusText: успешный статус текста не даёт")
Check(HttpStatusText(0) = "", "HttpStatusText: нулевой статус текста не даёт")
noHdr := A_Temp "\tt_logic_no_such_hdr.txt"
try FileDelete(noHdr)
Check(ParseHttpStatus(noHdr) = 0, "ParseHttpStatus: файла нет - ноль, без падения")
Check(UrlIsLoopback("http://[::1]:11434/v1"), "UrlIsLoopback [::1]")
Check(UrlIsLoopback("http://LOCALHOST:1234"), "UrlIsLoopback localhost без учёта регистра")
Check(!UrlIsLoopback("https://openrouter.ai/api/v1"), "UrlIsLoopback: внешний адрес не локальный")

cfg.proxy := "http://cfg-proxy:3128"
cfg.proxy_on := "1"
Check(ResolveProxy({}) = "http://cfg-proxy:3128", "ResolveProxy без opts берёт прокси из конфига: " ResolveProxy({}))
Check(ResolveProxy({proxy: "http://opts-proxy:9"}) = "http://opts-proxy:9", "ResolveProxy: opts главнее конфига")
Check(ResolveProxy({proxy: ""}) = "", "ResolveProxy: пустой proxy в opts - это явное «напрямую»")
cfg.proxy_on := "0"
Check(ResolveProxy({}) = "", "ResolveProxy: снятая галочка отключает прокси из конфига")
cfg.proxy := ""

; ===========================================================================
; Конфиг
; ===========================================================================
Check(CfgFlag({app_profiling: "1"}, "app_profiling") = "1", "CfgFlag читает поле")
Check(CfgFlag({}, "app_profiling") = "0", "CfgFlag: нет поля - умолчание 0")
Check(CfgFlag({}, "proxy", "") = "", "CfgFlag: своё умолчание")
Check(CfgFlag("", "proxy", "нет") = "нет", "CfgFlag на не-объекте отдаёт умолчание")
Check(JoinList(SplitList("a`nb,`r`nc")) = "a, b, c", "SplitList считает перевод строки запятой: " JoinList(SplitList("a`nb,`r`nc")))
Check(SplitList("").Length = 0, "SplitList пустой строки - пустой список")
Check(JoinList([]) = "", "JoinList пустого списка - пустая строка")

cfg.models := "mdl-1, mdl-2, mdl-1, " cfg.model
exm := ExtraModels()
Check(exm.Length = 2, "ExtraModels: дубли и модель по умолчанию отброшены (" exm.Length ")")
Check(exm.Length = 2 && exm[1] = "mdl-1" && exm[2] = "mdl-2", "ExtraModels сохраняет порядок списка")
cfg.models := ""

; ActionCfg на конфиге БЕЗ поля proxy_on: правило EffectiveProxy - «нет поля,
; значит URL действует», и у действия со своей моделью оно обязано быть то же.
cfgSaved := cfg
cfg := {base_url: "https://openrouter.ai/api/v1", api_key: "k", model: "default-model",
        models: "", models_known: "", proxy: "http://px-from-cfg:3128"}
acOwn := ActionCfg({name: "своя модель", model: "other-model"})
Check(EffectiveProxy(acOwn) = EffectiveProxy(cfg),
    "ActionCfg без proxy_on не отключает прокси: «" EffectiveProxy(acOwn) "» против «" EffectiveProxy(cfg) "»")
Check(EffectiveProxy(acOwn) = "http://px-from-cfg:3128", "прокси доезжает до конфига действия со своей моделью")
Check(acOwn.model = "other-model" && acOwn.api_key = "k" && acOwn.base_url = cfg.base_url,
    "ActionCfg переносит модель, ключ и адрес")
Check(ActionCfg({name: "без своей модели"}).model = "default-model", "действие без модели берёт модель по умолчанию")
Check(ActionCfg({name: "своя, но та же", model: "default-model"}).model = "default-model",
    "модель, равная умолчанию, не создаёт отдельный конфиг")
cfg := cfgSaved

; ===========================================================================
; actions.ini
; ===========================================================================
Check(NormDash("-") = "" && NormDash("  x  ") = "x", "NormDash разворачивает «-» в пустую строку")
Check(NormKey(" 1 ") = "1" && NormKey("Q") = "q", "NormKey приводит клавишу к пулу")
Check(NormKey("-") = "" && NormKey("z") = "" && NormKey("") = "", "NormKey: «-» и клавиша вне пула - без клавиши")
fkOne := [{key: MENU_KEYS[1]}]
Check(FreeMenuKey(fkOne) = MENU_KEYS[2], "FreeMenuKey отдаёт первую незанятую: " FreeMenuKey(fkOne))
Check(FreeMenuKey([]) = MENU_KEYS[1], "FreeMenuKey на пустом списке отдаёт первую клавишу пула")
Check(FreeMenuKey(CloneActions(DEFAULT_ACTIONS)) = "",
    "FreeMenuKey: заводской набор занимает весь пул - свободных нет")

rtActs := CloneActions(DEFAULT_ACTIONS)
rtActs[1].task := "line one`nline two"
rtActs[1].key := ""
rtActs[1].model := ""
try FileDelete(ACTIONS_INI)
SaveActionSets(rtActs, DEFAULT_QUICK)
rtBack := LoadActionSets()
Check(rtBack.actions.Length = rtActs.Length, "actions.ini: число действий сохранилось (" rtBack.actions.Length ")")
Check(rtBack.actions.Length >= 1 && rtBack.actions[1].task = "line one`nline two",
    "actions.ini: многострочный промпт пережил запись и чтение")
Check(rtBack.actions.Length >= 1 && rtBack.actions[1].key = "", "actions.ini: действие без клавиши не получает её обратно")
Check(rtBack.actions.Length >= 1 && rtBack.actions[1].model = "", "actions.ini: пустая модель читается как «по умолчанию»")
Check(rtBack.actions.Length >= 1 && rtBack.actions[1].id = "proofread", "actions.ini: id заводского действия сохранился")
Check(rtBack.quick.Length = DEFAULT_QUICK.Length, "actions.ini: уточнения сохранились (" rtBack.quick.Length ")")

; Осознанно пустой набор уточнений: [quick] count=0 пишет сам редактор, и этот
; ноль обязан пережить рестарт. Отсутствие ключа - старый файл, тогда заводские.
try FileDelete(ACTIONS_INI)
IniWrite("-", ACTIONS_INI, "action1", "id")
IniWrite("Probe", ACTIONS_INI, "action1", "name")
IniWrite("Do something", ACTIONS_INI, "action1", "task")
IniWrite("auto", ACTIONS_INI, "action1", "lang")
IniWrite("1", ACTIONS_INI, "action1", "ru")
IniWrite("0", ACTIONS_INI, "action1", "win")
IniWrite("-", ACTIONS_INI, "action1", "key")
IniWrite("-", ACTIONS_INI, "action1", "model")
IniWrite("0", ACTIONS_INI, "quick", "count")
q0 := LoadActionSets()
Check(q0.actions.Length = 1 && q0.actions[1].name = "Probe", "LoadActionSets прочитал единственное действие")
Check(q0.actions.Length = 1 && q0.actions[1].id = "" && q0.actions[1].key = "" && q0.actions[1].model = "",
    "actions.ini: «-» разворачивается в пустые поля")
Check(q0.quick.Length = 0, "[quick] count=0 - осознанно пустой набор, заводской не возвращается (" q0.quick.Length ")")
try IniDelete(ACTIONS_INI, "quick", "count")
qN := LoadActionSets()
Check(qN.quick.Length = DEFAULT_QUICK.Length,
    "без ключа [quick] count набор уточнений восстанавливается заводским (" qN.quick.Length ")")
try FileDelete(ACTIONS_INI)

; ===========================================================================
; Профили приложений
; ===========================================================================
Check(AppField("-") = "" && AppField("x") = "x", "AppField разворачивает «-»")
APPS := [{proc: "CHROME.EXE", title: "", name: "Chrome", desc: "", hint: "chrome-hint"}]
mcCase := MatchApp("chrome.exe", "что угодно")
Check(IsObject(mcCase) && mcCase.name = "Chrome", "MatchApp: регистр имени процесса не важен")
Check(MatchApp("", "Jira") = "", "MatchApp: пустое имя процесса ничего не находит")
Check(MatchApp("other.exe", "Jira") = "", "MatchApp: чужой процесс не подхватывается")
APPS := [{proc: "chrome.exe", title: "Jira", name: "Jira", desc: "", hint: "h1"}]
mcTitle := MatchApp("chrome.exe", "ACME - Jira")
Check(IsObject(mcTitle) && mcTitle.name = "Jira", "MatchApp: запись с заголовком срабатывает на совпадение")
Check(MatchApp("chrome.exe", "ACME - Gmail") = "", "MatchApp: запись с заголовком не срабатывает на чужой заголовок")
APPS := CloneApps(DEFAULT_APPS)
cfg.app_profiling := "0"
Check(!AppProfilingOn(), "профилирование приложений по умолчанию выключено")
cfg.app_profiling := "1"
Check(AppProfilingOn(), "профилирование включается флагом")
cfg.app_profiling := "0"

; ===========================================================================
; Рекомендуемые модели
; ===========================================================================
; Выпадающий список адресов и детектор провайдера обязаны знать одно и то же:
; адрес, выбранный мышью, но не опознанный ProviderOf, молча лишает заводские
; действия ролевых моделей и делает кнопку рекомендаций бесполезной.
puSeen := Map(), puBad := "", puNoRec := ""
for pu in ProviderUrls() {
    if (ProviderOf(pu.url) != pu.key)
        puBad .= " " pu.url "->" ProviderOf(pu.url) "(ждали " pu.key ")"
    if puSeen.Has(pu.url)
        puBad .= " дубль:" pu.url
    puSeen[pu.url] := true
    if (RecommendedFor(pu.url).Length = 0)
        puNoRec .= " " pu.key
}
Check(ProviderUrls().Length >= 6, "адресов провайдеров в списке: " ProviderUrls().Length)
Check(puBad = "", puBad = "" ? "каждый адрес из списка опознаётся как заявленный провайдер"
    : "расхождение списка и детектора:" puBad)
Check(puNoRec = "", puNoRec = "" ? "у каждого адреса из списка есть рекомендации"
    : "без рекомендаций:" puNoRec)
Check(CfgDefaults().HasProp("models_url"), "в конфиге есть поле models_url: адрес, которому принадлежит список моделей")

Check(ProviderOf("https://api.openai.com/v1") = "openai", "ProviderOf OpenAI")
Check(ProviderOf("http://localhost:11434/v1") = "ollama", "ProviderOf локальная Ollama")
Check(ProviderOf("https://api.example.com/v1") = "", "ProviderOf: чужой адрес не угадывается")
Check(ProviderName("openai") = "OpenAI" && ProviderName("чего-то") = "чего-то",
    "ProviderName: известное имя переводится, неизвестное отдаётся как есть")
PROVIDERS := ["https://api.openai.com/v1", "https://openrouter.ai/api/v1", "https://api.anthropic.com/v1",
    "https://generativelanguage.googleapis.com/v1beta/openai", "https://api.deepseek.com/v1",
    "https://api.mistral.ai/v1", "https://api.x.ai/v1", "http://localhost:11434/v1"]
recBatch := "", recEmpty := "", recDup := "", recNoMain := ""
for prov in PROVIDERS {
    seenId := Map(), seenKey := Map(), hasMain := false
    for r in RecommendedFor(prov) {
        if InStr(r.id, ":batch")
            recBatch .= " " r.id
        if (r.id = "" || r.key = "" || r.role = "")
            recEmpty .= " [" prov " " r.id "/" r.key "]"
        if (seenId.Has(r.id) || seenKey.Has(r.key))
            recDup .= " " prov "/" r.id
        seenId[r.id] := true, seenKey[r.key] := true
        if (r.key = "main")
            hasMain := true
    }
    if !hasMain
        recNoMain .= " " prov
}
Check(recBatch = "", "в рекомендациях нет batch-вариантов" (recBatch = "" ? "" : ":" recBatch))
Check(recEmpty = "", "у каждой рекомендации есть id, ключ роли и подпись" recEmpty)
Check(recDup = "", "в наборе провайдера нет дублей ни по id, ни по роли" recDup)
Check(recNoMain = "", "у каждого провайдера есть роль main - на неё откатывается RoleModel" recNoMain)
Check(RecommendedFor("https://api.example.com/v1").Length = 0, "неопознанному провайдеру ничего не рекомендуется")
oaReason := ""
for r in RecommendedFor("https://api.openai.com/v1")
    if (SubStr(r.id, 1, 1) = "o" || InStr(r.id, "-pro"))
        oaReason .= " " r.id
Check(oaReason = "", "в наборе OpenAI нет reasoning-моделей" (oaReason = "" ? "" : ":" oaReason))
Check(RoleModel("", "https://api.openai.com/v1") = "", "RoleModel без роли - пусто")
Check(RoleModel("mech", "https://api.openai.com/v1") = "gpt-4.1-nano", "RoleModel берёт модель своей роли: " RoleModel("mech", "https://api.openai.com/v1"))
Check(RoleModel("alt", "https://api.deepseek.com/v1") = "deepseek-chat",
    "RoleModel: нет роли alt - откат на main: " RoleModel("alt", "https://api.deepseek.com/v1"))
Check(RoleModel("mech", "https://api.example.com/v1") = "", "RoleModel: неопознанный провайдер - пусто")

; Копия заводского действия (id пуст) - своя модель, и «Перенастроить действия»
; её не переписывает: иначе ручной выбор пропадал бы при каждой смене провайдера.
dupActs := CloneActions(DEFAULT_ACTIONS)
dupCopy := CloneActions([dupActs[1]])[1]
dupCopy.id := ""
dupCopy.name .= " (копия)"
dupCopy.model := "my-own-model"
dupActs.InsertAt(2, dupCopy)
ApplyFactoryRoles(dupActs, "https://openrouter.ai/api/v1")
Check(dupActs[2].id = "" && dupActs[2].model = "my-own-model",
    "ApplyFactoryRoles не трогает копию заводского действия: «" dupActs[2].model "»")
Check(dupActs[1].model = "qwen/qwen3.7-flash",
    "а самому заводскому действию роль назначается: «" dupActs[1].model "»")

emSavedModels := cfg.models, emSavedKnown := cfg.models_known
cfg.models := "", cfg.models_known := ""
EnsureModelsForActions([{model: "mdl-a"}, {model: ""}, {model: "mdl-a"}, {model: "mdl-b"}])
Check(cfg.models = "mdl-a, mdl-b", "EnsureModelsForActions добавляет модели действий без дублей: «" cfg.models "»")
Check(cfg.models_known = cfg.models, "models_known заполняется при первом заходе")
cfg.models := emSavedModels, cfg.models_known := emSavedKnown

; ===========================================================================
; Языки ответа
; ===========================================================================
Check(LANGS.Length = DEFAULT_LANGS.Length, "заводской список языков разложен: " LANGS.Length)
Check(EnabledLangs().Length = 2, "по умолчанию включены два языка: " EnabledLangs().Length)
Check(InStr(LangBlock("auto"), "language of the input"), "LangBlock auto: «как в исходном тексте»")
Check(LangBlock("") = LangBlock("auto"), "LangBlock: пустой код равен auto")
Check(InStr(LangBlock("de"), "German"), "LangBlock: код языка превращается в английское имя")
Check(InStr(LangBlock("zz-нет-такого"), "zz-нет-такого"), "LangBlock: удалённый язык не подменяется другим")
Check(LangLabel("auto") = "" && LangLabel("") = "", "LangLabel: у auto подписи нет")
Check(LangLabel("ru") = "Русский", "LangLabel: подпись языка для интерфейса: " LangLabel("ru"))
Check(LangLabel("zz-нет-такого") = "zz-нет-такого", "LangLabel: незнакомый код показывается как есть")
Check(IsObject(LangByCode("ru")) && LangByCode("ru").code = "ru", "LangByCode находит язык")
Check(LangByCode("zz") = "", "LangByCode: нет языка - пусто")
Check(CloneLangs(LANGS).Length = LANGS.Length, "CloneLangs копирует весь список")
Check(StyleBlock("ru") = BLOCK_STYLE_RU, "StyleBlock: русский блок стиля")
Check(StyleBlock("en") = BLOCK_STYLE_EN, "StyleBlock: английский блок стиля")
Check(StyleBlock("auto") = BLOCK_STYLE_AUTO && StyleBlock("de") = BLOCK_STYLE_AUTO,
    "StyleBlock: для прочих языков общий блок")

; ===========================================================================
; Системный промпт
; ===========================================================================
biActs := CloneActions(DEFAULT_ACTIONS)
biRu := biActs[1]
biInstr := BuildInstruction(biRu)
Check(InStr(biInstr, BLOCK_STYLE_AUTO), "BuildInstruction: при ru=true добавляется блок стиля")
Check(!InStr(biInstr, "SOURCE APPLICATION"), "BuildInstruction: без hint строки SOURCE APPLICATION нет")
Check(InStr(biInstr, BLOCK_CONTEXT) && InStr(biInstr, BLOCK_FIDELITY) && InStr(biInstr, BLOCK_OUTPUT),
    "BuildInstruction: контекст, точность и формат вывода на месте")
Check(InStr(biInstr, biRu.task) && InStr(biInstr, biRu.task) < InStr(biInstr, BLOCK_BREVITY),
    "BuildInstruction: правило краткости идёт ПОСЛЕ задачи")
biEn := ""
for biA in biActs
    if (biA.id = "to_en")
        biEn := biA
Check(IsObject(biEn), "в заводском наборе есть действие to_en")
Check(IsObject(biEn) && !InStr(BuildInstruction(biEn), BLOCK_STYLE_EN),
    "BuildInstruction: при ru=false блока стиля нет")
Check(IsObject(biEn) && InStr(BuildInstruction(biEn), "ALWAYS English"),
    "BuildInstruction: язык действия попадает в блок LANGUAGE")

; ===========================================================================
; Цены и форматирование
; ===========================================================================
Check(NumFmt(1234567) = "1 234 567", "NumFmt разряды: " NumFmt(1234567))
Check(NumFmt(1000) = "1 000", "NumFmt ровно тысяча: " NumFmt(1000))
Check(NumFmt(999) = "999", "NumFmt три цифры без разделителя")
Check(NumFmt(0) = "0", "NumFmt ноль")
Check(MoneyFmt(-1) = "—", "MoneyFmt: неизвестная цена - прочерк")
Check(MoneyFmt(2.5) = "$2.50", "MoneyFmt от доллара - два знака: " MoneyFmt(2.5))
Check(MoneyFmt(0.5) = "$0.500", "MoneyFmt от цента - три знака: " MoneyFmt(0.5))
Check(MoneyFmt(0.0000512) = "$0.00005", "MoneyFmt мелкая сумма не схлопывается: " MoneyFmt(0.0000512))
Check(MoneyFmt(0) = "$0.00000", "MoneyFmt: ноль - это не прочерк: " MoneyFmt(0))
Check(EstTokens("") = 0, "EstTokens пустой строки - ноль")
Check(EstTokens("abcdefgh") = 2 && EstTokens("абвгдежз") = 4,
    "EstTokens: латиница " EstTokens("abcdefgh") ", кириллица " EstTokens("абвгдежз"))
Check(PriceSlug("gpt-4.1-mini", "https://api.openai.com/v1") = "openai/gpt-4.1-mini", "PriceSlug: вендор для прямого OpenAI")
Check(PriceSlug("claude-sonnet-5", "https://api.anthropic.com/v1") = "anthropic/claude-sonnet-5", "PriceSlug: вендор Anthropic")
Check(PriceSlug("anthropic/claude-haiku-4.5", "https://openrouter.ai/api/v1") = "anthropic/claude-haiku-4.5",
    "PriceSlug: готовый слаг не портится")
Check(PriceSlug("qwen3:4b", "http://127.0.0.1:11434/v1") = "qwen3:4b", "PriceSlug: локальная модель без вендора")
Check(JsonStrAfter('{"pricing":{"prompt":"0.5","completion":"1"}}', '"prompt"', 1, 100) = "0.5",
    "JsonStrAfter достаёт строковое значение")
Check(JsonStrAfter('{"a":1}', '"prompt"', 1, 100) = "", "JsonStrAfter: нет ключа - пусто")
Check(JsonStrAfter('{"prompt":5}', '"prompt"', 1, 100) = "", "JsonStrAfter: не строковое значение - пусто")

; Синтетический каталог: запись с нормальным context_length доказывает, что
; фикстура вообще разобрана, запись с null - что цифра из соседнего поля
; ("tokenizer":"Qwen3") не подхватывается.
try FileDelete(PRICESF)
PriceCacheReset()
pxF := FileOpen(PRICESF, "w", "UTF-8")
pxF.Write('{"data":['
    . '{"id":"probe/with-ctx","context_length":128000,"tokenizer":"Probe9",'
    . '"pricing":{"prompt":"0.000001","completion":"0.000002"}},'
    . '{"id":"probe/null-ctx","context_length":null,"tokenizer":"Qwen3",'
    . '"pricing":{"prompt":"0.000003","completion":"0.000004"}}]}')
pxF.Close()
PriceCacheReset()
Check(ContextOf("probe/with-ctx") = 128000, "ContextOf читает контекст из каталога: " ContextOf("probe/with-ctx"))
Check(ContextOf("probe/null-ctx") = 0,
    "ContextOf: context_length=null не подхватывает цифру из соседнего поля, получено " ContextOf("probe/null-ctx"))
pxPrice := PriceOf("probe/with-ctx")
Check(IsObject(pxPrice) && pxPrice.prompt > 0 && pxPrice.completion > pxPrice.prompt,
    "PriceOf разобрал цены синтетической записи")
Check(PriceOf("probe/нет-такой") = "", "PriceOf: неизвестная модель - пустая цена")
Check(PriceOf("") = "" && ContextOf("") = 0, "PriceOf/ContextOf на пустом слаге не ищут")
Check(ContextOf("probe/null-ctx", true) = 0, "ContextOf с reset сбрасывает кэш и отдаёт ноль")
PriceCacheReset()
Check(PricesStamp() != "", "у кэша цен есть отметка времени: " PricesStamp())

; ===========================================================================
; Расход токенов. "|" - разделитель полей, поэтому подпись и модель идут через
; HEsc: переименованное в "A|B" действие сдвигало поля, Integer() доставался
; текст модели и бросал исключение - окно статистики не открывалось вообще.
; PRICESF на месте, поэтому PricesRefresh() внутри отчёта в сеть не идёт.
; ===========================================================================
try FileDelete(USAGEF)
Check(InStr(UsageReport(), "Пока пусто"), "UsageReport на пустом файле объясняет, что данных нет")
UsageRecord("mdl-a", {prompt: 1000, completion: 200}, "A|B")
UsageRecord("mdl-a", {prompt: 500, completion: 100}, "C|D")
UsageRecord("mdl|pipe", {prompt: 7, completion: 3}, "обычная подпись")
FileAppend("2020-01-01 00:00:00|A|B|mdl-old|9|1`n", USAGEF, "UTF-8")
usRep := UsageReport()
Check(InStr(usRep, "mdl-a"), "UsageReport: модель с «|» в подписи попала в отчёт")
Check(InStr(usRep, "mdl|pipe"), "UsageReport: «|» в имени модели восстановлено из HEsc")
Check(!InStr(usRep, "mdl-old"), "UsageReport пропускает строку со сдвинутыми полями, а не падает на Integer()")
Check(InStr(usRep, "1 500"), "UsageReport суммирует промпт по модели (1 500)")
Check(InStr(usRep, NumFmt(1800)), "UsageReport считает «всего» по модели (" NumFmt(1800) ")")
Check(InStr(usRep, "Итого"), "UsageReport: строка итога есть")
Check(InStr(usRep, "цена неизвестна"), "UsageReport: без цены в каталоге модель помечена, а не посчитана нулём")
try FileDelete(USAGEF)
FileAppend("мусор без разделителей`nи ещё строка`n", USAGEF, "UTF-8")
Check(InStr(UsageReport(), "нет разбираемых строк"), "UsageReport на нечитаемом файле объясняет это, а не падает")
try FileDelete(USAGEF)

; Битый прокси из INI не должен ронять поток: PricesRefresh() зовут окно
; настроек и статистика - ровно те два места, где этот прокси и исправляют.
try FileDelete(PRICESF)
PriceCacheReset()
cfg.proxy := "socks://1.2.3.4:1080"
cfg.proxy_on := "1"
pxThrew := false, pxRes := "не вызывалось"
try {
    pxRes := PricesRefresh(true)
} catch as e {
    pxThrew := true
    pxRes := e.Message
}
Check(!pxThrew, "PricesRefresh не выпускает исключение битого прокси наружу: " pxRes)
Check(pxRes = false, "PricesRefresh без кэша и с битым прокси возвращает false, получено " (pxRes = "" ? "«»" : pxRes))
cfg.proxy := "", cfg.proxy_on := "0"
PriceCacheReset()

; ===========================================================================
; История
; ===========================================================================
Check(HistTrim("коротко", 100) = "коротко", "HistTrim короткую строку не трогает")
htLong := ""
loop 50
    htLong .= "0123456789"
Check(StrLen(HistTrim(htLong, 100)) = 101 && SubStr(HistTrim(htLong, 100), -1) = "…",
    "HistTrim режет по лимиту и ставит многоточие: " StrLen(HistTrim(htLong, 100)))
; Проверяем ОБА факта: экранированная форма появилась, сырого разделителя нет.
; В прежней записи обе половины сравнивали с "|", то есть условие было ложно всегда.
Check(InStr(HEsc("a|b"), "\u007C") && !InStr(HEsc("a|b"), "|"),
    "HEsc прячет разделитель полей: " HEsc("a|b"))
Check(JsonUnesc(HEsc("a|b")) = "a|b", "HEsc разворачивается обратно через JsonUnesc")
Check(HTime(FormatTime(, "yyyy-MM-dd") " 09:05") = "09:05", "HTime: сегодняшнее время без даты")
Check(HTime("2020-01-02 09:05") = "02.01 09:05", "HTime: прошлое время с датой: " HTime("2020-01-02 09:05"))
Check(Snip("строка`nвторая") = "строка вторая", "Snip склеивает перевод строки в пробел: " Snip("строка`nвторая"))
snLong := ""
loop 20
    snLong .= "abcde"
Check(StrLen(Snip(snLong)) = 61 && SubStr(Snip(snLong), -1) = "…", "Snip обрезает до 60 символов: " StrLen(Snip(snLong)))

gHistory := []
try FileDelete(HISTF)
HistoryAdd("Действие", "текст с | трубой`nи переводом строки", "ответ", "mdl-x", {prompt: 10, completion: 5})
HistoryAdd("Без расхода", "и", "о")
hBack := LoadHistory()
Check(hBack.Length = 2, "история прочитана с диска: записей " hBack.Length)
Check(hBack.Length = 2 && hBack[2].sel = "текст с | трубой`nи переводом строки",
    "история: труба и перевод строки пережили запись и чтение")
Check(hBack.Length = 2 && hBack[2].model = "mdl-x" && hBack[2].tok = 15,
    "история: модель и токены восстановились")
Check(hBack.Length = 2 && hBack[1].model = "" && hBack[1].tok = 0 && hBack[1].cost = -1,
    "история: запись без расхода не ломает чтение")
try FileDelete(HISTF)
FileAppend("2020-01-01|битая|m|не-число|-1|sel|res`n", HISTF, "UTF-8")
hBad := LoadHistory()
Check(hBad.Length = 1, "история: строка с нечисловыми токенами не теряется и не роняет загрузку (" hBad.Length ")")
Check(hBad.Length = 1 && hBad[1].tok = 0 && hBad[1].cost = -1,
    "история: нечисловые токены читаются как ноль")
try FileDelete(HISTF)
gHistory := []

; ===========================================================================
; Языковые пакеты. BOM снимается Chr(0xFEFF); у `uFEFF в наборе Trim оказывались
; буквы u, F и E, и значения с ними на краях теряли символы.
; ===========================================================================
pkPath := A_Temp "\tt_logic_pack.ini"
try FileDelete(pkPath)
pkF := FileOpen(pkPath, "w", "UTF-8")
pkF.Write("[meta]`nname=Probe`n`n[ui]`nk.u=menu`nk.f=OFF`nk.e=ONE`nk.ue=valueu`n"
    . "usage.title=Token usage`n; это комментарий`nk.empty=`n")
pkF.Close()
pk := ParsePack(pkPath)
Check(pk.Has("meta.name") && pk["meta.name"] = "Probe",
    "ParsePack читает [meta] name при наличии BOM: «" (pk.Has("meta.name") ? pk["meta.name"] : "-") "»")
Check(pk.Has("ui.k.u") && pk["ui.k.u"] = "menu",
    "ParsePack: значение, кончающееся на «u», не обрезается: «" (pk.Has("ui.k.u") ? pk["ui.k.u"] : "-") "»")
Check(pk.Has("ui.k.f") && pk["ui.k.f"] = "OFF",
    "ParsePack: значение на «F» не обрезается: «" (pk.Has("ui.k.f") ? pk["ui.k.f"] : "-") "»")
Check(pk.Has("ui.k.e") && pk["ui.k.e"] = "ONE",
    "ParsePack: значение на «E» не обрезается: «" (pk.Has("ui.k.e") ? pk["ui.k.e"] : "-") "»")
Check(pk.Has("ui.k.ue") && pk["ui.k.ue"] = "valueu",
    "ParsePack: «valueu» не теряет последнюю букву: «" (pk.Has("ui.k.ue") ? pk["ui.k.ue"] : "-") "»")
Check(pk.Has("ui.usage.title") && pk["ui.usage.title"] = "Token usage",
    "ParsePack: ключ, начинающийся на «u», не теряет первую букву (в en.ini это весь блок usage.*)")
Check(pk.Has("ui.k.empty") && pk["ui.k.empty"] = "", "ParsePack: ключ без значения читается с пустым значением")
Check(pk.Count = 7, "ParsePack: в пакете ровно семь ключей, комментарий пропущен (" pk.Count ")")
try FileDelete(pkPath)

pkNoBom := A_Temp "\tt_logic_pack_nobom.ini"
try FileDelete(pkNoBom)
pkF2 := FileOpen(pkNoBom, "w", "UTF-8-RAW")
pkF2.Write("[ui]`nk.a=значение`n")
pkF2.Close()
pk2 := ParsePack(pkNoBom)
Check(pk2.Has("ui.k.a") && pk2["ui.k.a"] = "значение",
    "ParsePack читает пакет без BOM: «" (pk2.Has("ui.k.a") ? pk2["ui.k.a"] : "-") "»")
try FileDelete(pkNoBom)
Check(ParsePack(A_Temp "\tt_logic_no_such_pack.ini").Count = 0, "ParsePack: нет файла - пустой Map, без падения")

; ===========================================================================
; Буфер обмена
; ===========================================================================
cfg.clip_wait := "0.05"
Check(ClipWaitTimeout() = 0.2, "ClipWaitTimeout: нижняя граница 0.2, получено " ClipWaitTimeout())
cfg.clip_wait := "99"
Check(ClipWaitTimeout() = 5, "ClipWaitTimeout: верхняя граница 5, получено " ClipWaitTimeout())
cfg.clip_wait := "не число"
Check(ClipWaitTimeout() = 0.8, "ClipWaitTimeout: мусор в настройке - умолчание 0.8, получено " ClipWaitTimeout())
cfg.clip_wait := "1.5"
Check(ClipWaitTimeout() = 1.5, "ClipWaitTimeout берёт значение из настроек, получено " ClipWaitTimeout())
cfg.clip_wait := "0.8"

; Предупреждение «в буфере картинка или файлы» различает пустой буфер и буфер
; с нетекстовым форматом по ЧИСЛУ форматов: у честно пустого их нет вовсе.
; Буфер пользователя сохраняем и возвращаем; занят другим приложением - SKIP.
clipSaved := "", clipTaken := false
try {
    clipSaved := ClipboardAll()
    clipTaken := true
} catch as e {
    Say("SKIP ClipHasData: буфер не удалось сохранить (" e.Message ")")
}
if clipTaken {
    try {
        A_Clipboard := "проба буфера"
        Sleep 60
        Check(ClipHasData(), "ClipHasData видит текст в буфере")
        A_Clipboard := ""
        Sleep 60
        Check(!ClipHasData(), "ClipHasData на честно пустом буфере ложного предупреждения не даёт")
    } catch as e {
        Say("SKIP ClipHasData: буфер занят другим приложением (" e.Message ")")
    }
    try A_Clipboard := clipSaved
}

; ===========================================================================
; NoSort у ВСЕХ ListView. Сортировку по клику на заголовок AHK делает сам, а
; строки сопоставлены с массивами ПО ИНДЕКСУ: один клик, и Удалить / Вверх /
; «Сделать основной» / «Открыть результат» срабатывают на другой записи и
; записывают это на диск. NoSort - внутренний флаг AHK, в стиле окна его не
; видно, а клик по заголовку без настоящего ввода не воспроизвести, поэтому
; инвариант проверяется по исходникам: новый ListView без NoSort уронит гейт.
; ===========================================================================
lvTotal := 0, lvBad := ""
loop files A_ScriptDir "\Lib\*.ahk" {
    lvTxt := ""
    try lvTxt := FileRead(A_LoopFileFullPath, "UTF-8")
    for lvLine in StrSplit(StrReplace(lvTxt, "`r", ""), "`n") {
        if !InStr(lvLine, 'Add("ListView"')
            continue
        lvTotal++
        if !InStr(lvLine, "NoSort")
            lvBad .= " " A_LoopFileName ": " Trim(lvLine)
    }
}
Check(lvTotal = 6, "ListView в Lib ровно шесть, файлы прочитаны (" lvTotal ")")
Check(lvBad = "", "у каждого ListView стоит NoSort" (lvBad = "" ? "" : ":" lvBad))

; ===========================================================================
; Локализация. Блок последний: он подменяет gUiStrings.
; ===========================================================================
Check(Loc("нет.такого.ключа", "запасной текст") = "запасной текст",
    "непереведённый ключ показывает русский текст, а не пустоту")
Check(ActionName({id: "proofread", name: "Вычитать"}) = "Вычитать",
    "без пакета заводское действие названо по-русски")
Check(ActionName({id: "", name: "Моя кнопка"}) = "Моя кнопка",
    "пользовательское действие не трогаем: у него нет id")
Check(ActionName({name: "Без поля id"}) = "Без поля id", "действие без поля id не роняет ActionName")
gUiStrings := Map("btn.save", "Save", "action.proofread", "Proofread")
Check(Loc("btn.save", "Сохранить") = "Save", "пакет перекрывает текст из кода")
Check(ActionName({id: "proofread", name: "Вычитать"}) = "Proofread", "пакет переводит заводское действие")
Check(ActionName({id: "", name: "Моя кнопка"}) = "Моя кнопка", "пакет не трогает пользовательское действие")
gUiStrings := Map()
Check(Loc("btn.save", "Сохранить") = "Сохранить", "gUiStrings сброшен обратно")

Say("`nпроверок: " checks ", провалов: " fails)
Say(fails = 0 ? "ВСЕ ПРОВЕРКИ ПРОШЛИ" : "ПРОВАЛОВ: " fails)
ExitApp(fails = 0 ? 0 : 1)

#Include Lib/log.ahk
#Include Lib/i18n.ahk
#Include Lib/config.ahk
#Include Lib/langs_store.ahk
#Include Lib/http.ahk
#Include Lib/actions_store.ahk
#Include Lib/apps_store.ahk
#Include Lib/hotkeys.ahk
#Include Lib/api.ahk
#Include Lib/clipboard.ahk
#Include Lib/action_run.ahk
#Include Lib/ui_menu.ahk
#Include Lib/ui_result.ahk
#Include Lib/ui_settings.ahk
#Include Lib/ui_settings_api.ahk
#Include Lib/ui_settings_langs.ahk
#Include Lib/ui_settings_ui.ahk
#Include Lib/recommend.ahk
#Include Lib/ui_actions_editor.ahk
#Include Lib/history.ahk
#Include Lib/prices.ahk
#Include Lib/markdown.ahk
