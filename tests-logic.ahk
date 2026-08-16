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
Check(ok, msg) {
    global fails
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
Check(InStr(badU, "pre") && InStr(badU, "post") && !InStr(badU, "Error"), "битый \\uXXXX не бросает")
emoji := JsonUnesc("\uD83D\uDE00")
Check(StrLen(emoji) = 2 || Ord(emoji) > 0xFFFF || Ord(emoji) = 0x1F600, "суррогатная пара \\uD83D\\uDE00")

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

try {
    ValidateCurlValue("API key", "abc`"def")
    Check(false, "ValidateCurlValue должен бросать на кавычке")
} catch {
    Check(true, "ValidateCurlValue отклоняет кавычку в API key")
}

Check(NormalizeProxy("") = "", "NormalizeProxy пустой")
Check(NormalizeProxy("127.0.0.1:7890") = "http://127.0.0.1:7890", "NormalizeProxy без схемы → http://")
Check(NormalizeProxy("socks5h://127.0.0.1:1080") = "socks5h://127.0.0.1:1080", "NormalizeProxy socks5h без изменений")
Check(NormalizeProxy("http://user:pass@host:3000") = "http://user:pass@host:3000", "NormalizeProxy http с логином")
try {
    NormalizeProxy("ftp://host:21")
    Check(false, "NormalizeProxy должен бросать на чужой схеме")
} catch {
    Check(true, "NormalizeProxy отклоняет ftp://")
}
Check(UrlIsLoopback("http://127.0.0.1:11434/v1/chat/completions"), "UrlIsLoopback 127.0.0.1")
Check(CurlProxyCfg("https://openrouter.ai/api/v1/models", "") = "", "CurlProxyCfg без прокси пустой")
Check(CurlProxyCfg("http://127.0.0.1:11434/v1/chat/completions", "http://user:pass@host:3000") = "", "localhost не идёт через прокси")
pxBlock := CurlProxyCfg("https://openrouter.ai/api/v1/models", "user:pass@host:3000")
Check(InStr(pxBlock, "proxy = ") && InStr(pxBlock, "http://user:pass@host:3000") && InStr(pxBlock, "noproxy"), "CurlProxyCfg пишет proxy и noproxy")
Check(EffectiveProxy({proxy: "http://h:1", proxy_on: "0"}) = "", "EffectiveProxy выключен галочкой")
Check(EffectiveProxy({proxy: "http://h:1", proxy_on: "1"}) = "http://h:1", "EffectiveProxy включён")
Check(EffectiveProxy({proxy: "http://h:1"}) = "http://h:1", "EffectiveProxy без proxy_on — URL действует")

Say(fails = 0 ? "`nВСЕ ПРОВЕРКИ ПРОШЛИ" : "`nПРОВАЛОВ: " fails)
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
