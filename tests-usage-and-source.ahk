#Requires AutoHotkey v2.0
#SingleInstance Off
; Регрессионная проверка учета расхода и трекера окна-источника: разбор usage
; из ответа API, цены из каталога, отчет по моделям, окно статистики и отбор
; окон для вызова меню из трея. Сеть нужна только для проверки цен.
; Отчет пишется в файл, а не в stdout: AHK - GUI-процесс, и запись в
; перенаправленный поток подвешивает его наглухо.
REPORT := A_Temp "\tt_us_report.txt"
try FileDelete(REPORT)
OnError((e, mode) => (FileAppend("ОШИБКА: " e.Message " | " e.Extra " @ " e.File ":" e.Line "`n", REPORT, "UTF-8"), ExitApp(9)))
LOGF := A_Temp "\tt_us.log"
INI := A_Temp "\tt_us_cfg.ini"
ACTIONS_INI := A_Temp "\tt_us_actions.ini"
APPS_INI := A_Temp "\tt_us_apps.ini"
USAGEF := A_Temp "\tt_us_usage.log"
PRICESF := A_Temp "\tt_us_prices.json"
HISTF := A_Temp "\tt_us_history.txt"
LANGS_INI := A_Temp "\tt_us_langs.ini"
MAX_ACTIONS := 200
try FileDelete(USAGEF)
try FileDelete(HISTF)

#Include Lib/prompts.ahk
#Include Lib/actions.ahk
#Include Lib/apps.ahk
#Include Lib/langs.ahk

cfg := {base_url: "https://api.openai.com/v1", api_key: "", model: "gpt-4.1-mini",
        models: "gpt-4o-mini, gpt-4.1, gpt-4o-mini, gpt-4.1-mini", models_known: "gpt-4o", ui_lang: "ru",
        hotkey: "^Space", hotkey_repeat: "^+Space", markdown: "0"}
gEditorGui := "", gMenuEdit := "", gMenuFirstBtn := "", gMenuGui := "", gBusy := false
gBusyGui := "", gBusyText := "", gBusyInfo := "", gBusyDetail := "", gBusyStart := 0
gHistory := [], gPrevWin := 0, gForeWin := 0, gSel := "", gLastUsage := "", gCallLabel := "", gUiStrings := Map()
try FileDelete(LANGS_INI)
LANGS := LoadLangs()
ACTIONS := DEFAULT_ACTIONS, QUICK_REFINE := DEFAULT_QUICK, APPS := DEFAULT_APPS
Say(s) => FileAppend(s "`n", REPORT, "UTF-8")
fails := 0
Check(ok, msg) {
    global fails
    if !ok
        fails++
    Say((ok ? "OK   " : "FAIL ") msg)
}

; --- разбор usage из ответа, как его отдает OpenAI ---
sample := '{"id":"chatcmpl-x","choices":[{"message":{"role":"assistant","content":"привет"}}]'
    . ',"usage":{"prompt_tokens":1234,"completion_tokens":56,"total_tokens":1290}}'
u := ExtractUsage(sample)
Check(IsObject(u) && u.prompt = 1234 && u.completion = 56, "usage разобран: промпт " (IsObject(u) ? u.prompt : "-")
    ", ответ " (IsObject(u) ? u.completion : "-"))
Check(ExtractUsage('{"choices":[]}') = "", "ответ без usage не ломает разбор")
Check(IsObject(ExtractContent(sample)) = false && ExtractContent(sample) = "привет", "content по-прежнему достается")

; --- форматирование ---
Check(NumFmt(1234567) = "1 234 567", "разряды: " NumFmt(1234567))
Check(NumFmt(42) = "42", "короткое число без разделителя")
Check(MoneyFmt(0.0000512) = "$0.00005", "мелкая сумма не схлопывается: " MoneyFmt(0.0000512))
Check(MoneyFmt(2.5) = "$2.50", "крупная сумма: " MoneyFmt(2.5))
Check(MoneyFmt(-1) = "—", "неизвестная цена помечается прочерком")
Check(EstTokens("") = 0, "оценка пустой строки - ноль")
lat := EstTokens("abcdefgh")            ; 8 латинских -> ~2
cyr := EstTokens("абвгдежз")            ; 8 кириллицы -> ~4
Check(lat = 2 && cyr = 4, "оценка токенов: латиница " lat ", кириллица " cyr)

; --- цены из каталога (нужна сеть) ---
net := PricesRefresh(true)
if net {
    p := PriceOf("openai/gpt-4.1-mini")
    Check(IsObject(p) && p.prompt > 0 && p.completion > p.prompt,
        "цена gpt-4.1-mini получена: промпт " p.prompt ", ответ " p.completion)
    Check(PriceOf("openai/такой-модели-нет") = "", "неизвестная модель дает пустую цену")
    Check(PriceSlug("gpt-4.1-mini", "https://api.openai.com/v1") = "openai/gpt-4.1-mini",
        "слаг для прямого OpenAI собирается с вендором")
    Check(PriceSlug("anthropic/claude-haiku-4.5", "https://openrouter.ai/api/v1") = "anthropic/claude-haiku-4.5",
        "готовый слаг не портится")
    c := CostOf("gpt-4.1-mini", 1000000, 0)
    Check(c > 0.1 && c < 5, "миллион токенов промпта стоит " MoneyFmt(c) " - в разумных пределах")
    Check(PricesStamp() != "", "у кэша цен есть отметка времени: " PricesStamp())
} else {
    Say("SKIP цены: каталог не скачался (нет сети?)")
}

; --- отчет по моделям на синтетическом файле расхода ---
UsageRecord("gpt-4.1-mini", {prompt: 1000, completion: 200}, "Переписать")
UsageRecord("gpt-4.1-mini", {prompt: 500, completion: 100}, "уточнение")
UsageRecord("gpt-4o-mini", {prompt: 300, completion: 50}, "тест API")
rep := UsageReport()
Check(InStr(rep, "gpt-4.1-mini") && InStr(rep, "gpt-4o-mini"), "в отчете обе модели")
Check(InStr(rep, "1 500"), "токены промпта просуммированы по модели (1 500)")
Check(InStr(rep, "Итого"), "есть строка итога")
if net
    Check(InStr(rep, "$"), "стоимость посчитана")

; --- окно статистики должно реально открываться ---
try FileDelete(LOGF)
FileAppend("[x] действие: Переписать, выделено 10 символов`n", LOGF, "UTF-8")
ShowStats()
found := 0
for hw in WinGetList() {
    t := ""
    try t := WinGetTitle("ahk_id " hw)
    if InStr(t, "Статистика")
        found++
}
Check(found > 0, "окно статистики открылось")

; --- каталог: контекст модели рядом с ценой ---
if net {
    Check(ContextOf("openai/gpt-4.1-mini") > 100000,
        "контекст модели читается из каталога: " NumFmt(ContextOf("openai/gpt-4.1-mini")))
    Check(ContextOf("openai/gpt-4o-mini") = 128000,
        "контекст берётся у нужной модели, а не у соседней по файлу")
    Check(ContextOf("нет-такой-модели") = 0, "неизвестной модели контекст не выдумывается")
}

; порядок в списке моделей: отмеченные наверх, внутри группы по алфавиту
mr := [{id: "zeta", on: true}, {id: "alpha", on: false}, {id: "beta", on: true}]
i := 2
while (i <= mr.Length) {
    j := i
    while (j > 1 && ModelLess(mr[j], mr[j - 1])) {
        t := mr[j], mr[j] := mr[j - 1], mr[j - 1] := t
        j--
    }
    i++
}
Check(mr[1].id = "beta" && mr[2].id = "zeta" && mr[3].id = "alpha",
    "отмеченные модели наверху, внутри группы по алфавиту")

; сортировка по цене - числовая, а не по тексту: "$10.00" не должен встать
; раньше "$2.00", а строки без цены обязаны уходить вниз в обе стороны
SortBy(list, col, desc) {
    i := 2
    while (i <= list.Length) {
        j := i
        while (j > 1 && ModelLessBy(list[j], list[j - 1], col, desc)) {
            t := list[j], list[j] := list[j - 1], list[j - 1] := t
            j--
        }
        i++
    }
    return list
}
IdList(list) {
    s := ""
    for x in list
        s .= (s = "" ? "" : ",") x.id
    return s
}
priced := [{id: "дорогая", on: false, ctx: 8000, pin: 10.0, pout: 30.0},
           {id: "дешёвая", on: false, ctx: 128000, pin: 2.0, pout: 6.0},
           {id: "средняя", on: true, ctx: 400000, pin: 2.5, pout: 8.0},
           {id: "безцены", on: true, ctx: 0, pin: -1, pout: -1}]
SortBy(priced, 3, false)
Check(IdList(priced) = "дешёвая,средняя,дорогая,безцены",
    "по цене возрастанию: " IdList(priced))
SortBy(priced, 3, true)
Check(IdList(priced) = "дорогая,средняя,дешёвая,безцены",
    "по цене убыванию, строки без цены остались внизу: " IdList(priced))
SortBy(priced, 2, true)
Check(IdList(priced) = "средняя,дешёвая,дорогая,безцены",
    "по контексту убыванию: " IdList(priced))
Check(ModelLessBy({id: "a", ctx: 0, pin: 2.0, pout: 1}, {id: "b", ctx: 0, pin: 10.0, pout: 1}, 3, false),
    "2 дешевле 10 (а текстом было бы наоборот)")

; --- рекомендуемые модели по провайдеру ---
Check(ProviderOf("https://api.openai.com/v1") = "openai", "OpenAI опознан по адресу")
Check(ProviderOf("https://openrouter.ai/api/v1") = "openrouter", "OpenRouter опознан")
Check(ProviderOf("https://api.anthropic.com/v1") = "anthropic", "Anthropic опознан")
Check(ProviderOf("http://127.0.0.1:11434/v1") = "ollama", "локальная Ollama опознана по порту")
Check(ProviderOf("http://localhost:11434/v1") = "ollama", "Ollama опознана по localhost")
Check(ProviderOf("https://api.example.com/v1") = "", "чужой адрес не опознаётся, а не угадывается")
recOa := RecommendedFor("https://api.openai.com/v1")
Check(recOa.Length = 4, "для OpenAI ровно четыре модели: " recOa.Length)
uniq := Map()
for r in recOa
    uniq[r.id] := true
Check(uniq.Count = recOa.Length, "рекомендации не дублируются между ролями")
roles := Map()
for r in recOa
    roles[r.role] := true
Check(roles.Count = recOa.Length, "у каждой рекомендации своя роль")
Check(RecommendedFor("https://api.example.com/v1").Length = 0,
    "неопознанному провайдеру ничего не рекомендуется")
badRec := "", emptyRec := ""
for prov in ["https://api.openai.com/v1", "https://openrouter.ai/api/v1", "https://api.anthropic.com/v1",
             "https://generativelanguage.googleapis.com/v1beta/openai", "https://api.deepseek.com/v1",
             "https://api.mistral.ai/v1", "https://api.x.ai/v1", "http://localhost:11434/v1"] {
    for r in RecommendedFor(prov) {
        ; batch - отложенное исполнение, интерактивно не работает вообще
        if InStr(r.id, ":batch")
            badRec .= " " r.id
        if (r.id = "" || r.role = "")
            emptyRec .= " [" r.id "/" r.role "]"
    }
}
Check(badRec = "", "batch-вариантов в рекомендациях нет" (badRec = "" ? "" : ":" badRec))
Check(emptyRec = "", "у каждой рекомендации есть и имя, и роль" emptyRec)
; reasoning-модели OpenAI (o-серия, *-pro) для замены текста под курсором не годятся
oaBad := ""
for r in RecommendedFor("https://api.openai.com/v1")
    if (SubStr(r.id, 1, 1) = "o" || InStr(r.id, "-pro"))
        oaBad .= " " r.id
Check(oaBad = "", "в наборе OpenAI нет reasoning-моделей" (oaBad = "" ? "" : ":" oaBad))

; модель по умолчанию не предлагается второй раз как переопределение действия
dupDefault := false
for m in ExtraModels()
    if (m = cfg.model)
        dupDefault := true
Check(!dupDefault, "модель по умолчанию не дублируется в списке переопределений")

; --- модель на уровне действия ---
ex := ExtraModels()
Check(ex.Length = 2, "список дополнительных моделей: дубли и модель по умолчанию отброшены (" ex.Length ")")
Check(ex[1] = "gpt-4o-mini" && ex[2] = "gpt-4.1", "порядок и разбор списка сохранены")
Check(ActionCfg({name: "без своей модели"}).model = "gpt-4.1-mini", "действие без модели берет модель по умолчанию")
Check(ActionCfg({name: "своя", model: "gpt-4.1"}).model = "gpt-4.1", "своя модель действия перекрывает настройки")
Check(ActionCfg({name: "своя", model: "gpt-4.1"}).api_key = cfg.api_key, "ключ и адрес наследуются")

; модель должна пережить запись и чтение actions.ini
saveActs := CloneActions(DEFAULT_ACTIONS)
saveActs[1].model := "gpt-4.1"
saveActs[2].model := ""
SaveActionSets(saveActs, DEFAULT_QUICK)
reloaded := LoadActionSets()
Check(reloaded.actions[1].model = "gpt-4.1", "модель действия пережила запись/чтение actions.ini")
Check(reloaded.actions[2].model = "", "пустая модель читается как «по умолчанию», а не как «-»")
try FileDelete(ACTIONS_INI)

; --- история хранит модель и расход ---
gHistory := []
HistoryAdd("Переписать", "исходник", "результат", "gpt-4.1-mini", {prompt: 1000, completion: 200})
HistoryAdd("Без расхода", "исходник", "результат")
rec := gHistory[2]
Check(rec.model = "gpt-4.1-mini" && rec.tok = 1200, "в истории записаны модель и токены")
if net
    Check(rec.cost > 0, "в истории посчитана стоимость: " MoneyFmt(rec.cost))
Check(gHistory[1].tok = 0 && gHistory[1].cost = -1, "запись без расхода не ломает историю")

; история обязана пережить перезапуск - её стирал каждый деплой
HistoryAdd("С разделителем", "текст с | трубой`nи переводом строки", "ответ", "gpt-4.1-mini",
    {prompt: 10, completion: 5})
restored := LoadHistory()
Check(restored.Length = 3, "история прочитана с диска: записей " restored.Length)
Check(restored[1].sel = "текст с | трубой`nи переводом строки",
    "разделитель и перевод строки пережили запись/чтение")
Check(restored[2].model = "" && restored[2].tok = 0, "запись без модели восстановилась корректно")
Check(restored[3].model = "gpt-4.1-mini" && restored[3].tok = 1200, "модель и токены восстановились")
Check(Abs(restored[3].cost - gHistory[3].cost) < 0.0000001, "стоимость восстановилась без потери точности")
Check(HTime(FormatTime(, "yyyy-MM-dd") " 09:05") = "09:05", "сегодняшнее время без даты")
Check(HTime("2026-01-02 09:05") = "02.01 09:05", "прошлое время с датой")
ShowHistory()
foundH := 0
for hw in WinGetList() {
    t := ""
    try t := WinGetTitle("ahk_id " hw)
    if InStr(t, "история")
        foundH++
}
Check(foundH > 0, "окно истории открылось")

; пустая история обязана открывать окно, а не отвечать всплывающей подсказкой:
; подсказку дважды приняли за неработающий пункт меню
gHistory := []
try FileDelete(HISTF)
ShowHistory()
foundE := 0
for hw in WinGetList() {
    t := ""
    try t := WinGetTitle("ahk_id " hw)
    if InStr(t, "История")
        foundE++
}
Check(foundE > 0, "пустая история тоже открывает окно с объяснением")

; --- языки ответа ---
Check(LANGS.Length = DEFAULT_LANGS.Length, "заводской список языков разложен: " LANGS.Length)
Check(EnabledLangs().Length = 2, "по умолчанию включены два языка: " EnabledLangs().Length)
Check(InStr(LangBlock("auto"), "language of the input"), "auto даёт блок «как в исходном тексте»")
Check(InStr(LangBlock("de"), "ALWAYS German"), "код языка превращается в английское имя для промпта")
Check(InStr(LangBlock("xx-неизвестный"), "xx-неизвестный"),
    "язык, удалённый из списка, не подменяется молча другим")
Check(LangLabel("ru") = "Русский" && LangLabel("auto") = "", "подпись языка для интерфейса")
; свой язык должен пережить запись и чтение
langsSave := CloneLangs(LANGS)
langsSave.Push({code: "pt-BR", name: "Português (BR)", prompt: "Brazilian Portuguese", on: true})
SaveLangs(langsSave)
langsBack := LoadLangs()
Check(langsBack.Length = langsSave.Length, "свой язык пережил запись/чтение langs.ini")
Check(langsBack[langsBack.Length].prompt = "Brazilian Portuguese", "имя для промпта сохранилось")
Check(langsBack[langsBack.Length].on, "галочка сохранилась")
try FileDelete(LANGS_INI)

; --- список моделей и локализация ---
Check(JoinList(SplitList(" a , b ,, c ")) = "a, b, c", "разбор и сборка списка через запятую")
Check(ModelIsUseless("text-embedding-3-small") && !ModelIsUseless("gpt-4.1"),
    "служебные модели отсеиваются, обычные нет")
; ответ /v1/models в том виде, как его отдаёт OpenAI
modelsResp := '{"object":"list","data":['
    . '{"id":"gpt-4.1","object":"model","owned_by":"openai"},'
    . '{"id":"text-embedding-3-small","object":"model","owned_by":"openai"},'
    . '{"id":"whisper-1","object":"model","owned_by":"openai"},'
    . '{"id":"gpt-4.1","object":"model","owned_by":"openai"},'
    . '{"id":"gpt-5.6-luna","object":"model","owned_by":"openai"}]}'
mIds := ParseModelIds(modelsResp)
Check(mIds.Length = 2, "из ответа взяты только текстовые модели: " mIds.Length)
Check(mIds[1] = "gpt-4.1" && mIds[2] = "gpt-5.6-luna", "дубли убраны, список отсортирован")
Check(ParseModelIds('{"data":[]}').Length = 0, "пустой список не ломает разбор")
srt := ["gpt-5", "aaa", "gpt-4.1"]
SortStrings(srt)
Check(srt[1] = "aaa" && srt[3] = "gpt-5", "сортировка списка моделей")
Check(Loc("нет.такого.ключа", "запасной текст") = "запасной текст",
    "непереведённый ключ показывает русский текст, а не пустоту")
gUiStrings := Map("btn.save", "Save")
Check(Loc("btn.save", "Сохранить") = "Save", "пакет перекрывает текст из кода")
gUiStrings := Map()
Check(ActionName({id: "proofread", name: "Вычитать"}) = "Вычитать",
    "без пакета заводское действие названо по-русски")
gUiStrings := Map("action.proofread", "Proofread")
Check(ActionName({id: "proofread", name: "Вычитать"}) = "Proofread", "пакет переводит заводское действие")
Check(ActionName({id: "", name: "Моя кнопка"}) = "Моя кнопка",
    "пользовательское действие не трогаем: у него нет id")
gUiStrings := Map()
packs := UiLangPacks()
Check(packs.Length >= 1 && packs[1].code = "ru", "русский всегда первым в списке пакетов")
Check(packs.Length >= 2, "английский пакет найден в папке lang")
enName := ""
for pk in packs
    if (pk.code = "en")
        enName := pk.name
Check(enName = "English", "имя пакета прочитано из [meta]: «" enName "»")
; пакет лежит в UTF-8; через IniRead Win32 разбирал его как ANSI, и в меню
; вместо «…» лезло «вЂ¦» - проверяем именно небуквенные символы
packEn := LoadUiLang("en")
Check(packEn.Count > 50, "английский пакет загружен: строк " packEn.Count)
Check(packEn.Has("tray.history") && packEn["tray.history"] = "History…",
    "многоточие не испорчено: «" (packEn.Has("tray.history") ? packEn["tray.history"] : "-") "»")
Check(packEn.Has("menu.editor") && InStr(packEn["menu.editor"], "⚙"),
    "значок шестерёнки не испорчен")
mojibake := 0
for k, v in packEn
    if (InStr(v, "вЂ") || InStr(v, "вљ") || InStr(v, "Ð"))
        mojibake++
Check(mojibake = 0, "во всём пакете нет козябриков")

; --- трекер окна-источника (вызов меню из трея) ---
; Свое видимое окно трекер обязан отбраковать: иначе после закрытия окна
; результата источником запомнится наше же окно, а не окно с текстом.
own := Gui("+AlwaysOnTop", "проба своего окна")
own.Add("Text", , "тест")
own.Show("NoActivate x0 y0 w200 h80")
Check(IsOwnWin(own.Hwnd), "свое окно распознано как свое")
own.Destroy()
tray := WinExist("ahk_class Shell_TrayWnd")
Check(tray && IsShellWin(tray), "панель задач распознана как оболочка")

; Суть починки трея: когда активно НЕ окно пользователя (из трея это оболочка,
; после окна результата - наше собственное), источник берется из памяти трекера,
; и трекер эту память своим окном не перетирает.
own2 := Gui("+AlwaysOnTop", "свое активное окно")
own2.Add("Text", , "тест")
own2.Show("x0 y0 w200 h80")
WinActivate("ahk_id " own2.Hwnd)
try WinWaitActive("ahk_id " own2.Hwnd, , 2)
gForeWin := tray                       ; заведомо не наше окно в памяти трекера
; проверки имеют смысл только если Windows реально отдала фокус нашему окну;
; в headless-прогоне активным может остаться чужое окно - тогда честнее
; пропустить, чем показать красный тест из-за окружения
if WinActive("ahk_id " own2.Hwnd) {
    Check(SourceWin() = tray, "при активном своем окне источник берется из памяти")
    ForeWatch()
    Check(gForeWin = tray, "трекер не перетер память своим окном")
} else {
    Say("SKIP трекер: наше окно не получило фокус")
}
own2.Destroy()
Check(FocusSource(0) = false, "несуществующее окно не активируется")

try FileDelete(USAGEF)
try FileDelete(PRICESF)
Say(fails = 0 ? "`nВСЕ ПРОВЕРКИ ПРОШЛИ" : "`nПРОВАЛОВ: " fails)
ExitApp(fails = 0 ? 0 : 1)

#Include Lib/log.ahk
#Include Lib/i18n.ahk
#Include Lib/langs_store.ahk
#Include Lib/config.ahk
#Include Lib/api.ahk
#Include Lib/prices.ahk
#Include Lib/actions_store.ahk
#Include Lib/apps_store.ahk
#Include Lib/history.ahk
#Include Lib/ui_result.ahk
#Include Lib/markdown.ahk
#Include Lib/hotkeys.ahk
#Include Lib/ui_menu.ahk
#Include Lib/action_run.ahk
#Include Lib/clipboard.ahk
#Include Lib/ui_settings.ahk
#Include Lib/recommend.ahk
#Include Lib/ui_actions_editor.ahk
