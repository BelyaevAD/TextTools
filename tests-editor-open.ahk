#Requires AutoHotkey v2.0
#SingleInstance Off
; Регрессионная проверка: редактор действительно открывается и все три вкладки
; заполнены. Именно этого не хватило, когда локальная apps затенила глобальный APPS.
; Отчет пишется в файл, а не в stdout: AHK - GUI-процесс, и запись в
; перенаправленный поток подвешивает его наглухо при запуске из скрипта.
REPORT := A_Temp "\tt_eo_report.txt"
try FileDelete(REPORT)
OnError((e, mode) => (FileAppend("ОШИБКА: " e.Message " | " e.Extra " @ " e.File ":" e.Line "`n", REPORT, "UTF-8"), ExitApp(9)))
LOGF := A_Temp "\tt_eo.log"
INI := A_Temp "\tt_eo_cfg.ini"
ACTIONS_INI := A_Temp "\tt_eo_actions.ini"
APPS_INI := A_Temp "\tt_eo_apps.ini"
USAGEF := A_Temp "\tt_eo_usage.log"
PRICESF := A_Temp "\tt_eo_prices.json"
HISTF := A_Temp "\tt_eo_history.txt"
LANGS_INI := A_Temp "\tt_eo_langs.ini"
MAX_ACTIONS := 200
try FileDelete(ACTIONS_INI)
try FileDelete(APPS_INI)

#Include Lib/prompts.ahk
#Include Lib/actions.ahk
#Include Lib/apps.ahk
#Include Lib/langs.ahk

cfg := {base_url: "http://127.0.0.1:9", api_key: "", model: "test",
        models: "модель-подешевле, модель-подороже", models_known: "", ui_lang: "ru", hotkey: "^Space",
        hotkey_repeat: "^+Space", markdown: "1", app_profiling: "0", clip_wait: "0.8"}
gEditorGui := "", gMenuEdit := "", gMenuFirstBtn := "", gMenuGui := "", gBusy := false
gBusyGui := "", gBusyText := "", gBusyInfo := "", gBusyDetail := "", gBusyStart := 0
gHistory := [], gPrevWin := 0, gForeWin := 0, gSel := "", gSettingsGui := ""
gLastUsage := "", gCallLabel := "", gLastAction := "", gLastInstr := "", gRestoreTimer := ""
gHotkeyCurrent := "", gHotkeyRepeatCurrent := "", gUiStrings := Map()
gClipWarned := false, gPriceIndex := "", gStartTick := A_TickCount, gTakenKeyHwnd := 0
try FileDelete(LANGS_INI)
LANGS := LoadLangs()
sets := LoadActionSets()
ACTIONS := sets.actions
QUICK_REFINE := sets.quick
APPS := LoadApps()
Say(s) => FileAppend(s "`n", REPORT, "UTF-8")
fails := 0
Check(ok, msg) {
    global fails
    if !ok
        fails++
    Say((ok ? "OK   " : "FAIL ") msg)
}

Say("наборы загружены: действий " ACTIONS.Length ", уточнений " QUICK_REFINE.Length ", приложений " APPS.Length)
ShowActionsEditor()
if !(IsObject(gEditorGui)) {
    Check(false, "окно редактора не создано")
    Say("SKIP остаток проверок редактора")
} else {
    Check(true, "редактор открылся без ошибок")
    lists := []
    for c in gEditorGui
        if (c.Type = "ListView")
            lists.Push(c.GetCount())
    Check(lists.Length = 3, lists.Length = 3 ? "три вкладки: строк " lists[1] " / " lists[2] " / " lists[3]
        : "FAIL списков " lists.Length)
    Check(lists.Length = 3 && lists[1] = ACTIONS.Length && lists[2] = QUICK_REFINE.Length && lists[3] = APPS.Length
        , "все три списка заполнены данными")
    gEditorGui.GetPos(, , &w, &h)
    over := 0
    for c in gEditorGui {
        c.GetPos(&cx, &cy, &cw, &ch)
        if (cx + cw > w - 8 || cy + ch > h - 8)
            over++
    }
    Check(over = 0, over = 0 ? "контролы не выходят за край (окно " w "x" h ")" : "за краем: " over)

    ; Карточки авторазмерные: разросшееся поле не вылезает за край окна, оно
    ; наезжает на соседний контрол (так однажды уехала подпись под API key).
    ; Поэтому проверяем пересечение прямоугольников, а не выход за границу.
    ; Описание берём заведомо длинное: у незнакомых приложений его пишет модель.
    snapA := SnapWins()
    EditActionDialog(gEditorGui, ACTIONS[1], ACTIONS, 1, (*) => 0)
    dAct := NewDialogSince(snapA)
    Check(IsObject(dAct), IsObject(dAct) ? "карточка действия открылась" : "карточка действия не создана")
    if IsObject(dAct) {
        ovA := OverlapCount(dAct)
        Check(ovA = 0, ovA = 0 ? "контролы карточки действия не наезжают друг на друга"
            : "в карточке действия пересечений: " ovA)
    }

    longApp := CloneApps([APPS[1]])[1]
    longApp.desc := "Описание, какое модель пишет для незнакомого приложения: "
        . "клиент для работы с задачами и документами, используется командой "
        . "аналитики и разработки, содержит тексты требований и переписку."
    snapP := SnapWins()
    EditAppDialog(gEditorGui, longApp, (*) => 0)
    dApp := NewDialogSince(snapP)
    Check(IsObject(dApp), IsObject(dApp) ? "карточка приложения открылась" : "карточка приложения не создана")
    if IsObject(dApp) {
        ovP := OverlapCount(dApp)
        Check(ovP = 0, ovP = 0 ? "контролы карточки приложения не наезжают (длинное описание)"
            : "в карточке приложения пересечений: " ovP)
    }

    ; --- ряд N списка = запись N набора --------------------------------------
    ; Это и есть инвариант, который ломала сортировка по клику на заголовок:
    ; Изменить / Удалить / Вверх / Вниз берут запись по НОМЕРУ РЯДА, а AHK по
    ; умолчанию переставляет только строки, не трогая acts/quicks/appList, -
    ; и редактор начинал править чужую запись и сохранял это на диск.
    ;
    ; Сам клик по заголовку здесь не подделывается: NoSort - внутренний флаг AHK,
    ; в стиле окна его не видно (это не LVS_NOSORTHEADER), а синтетический
    ; WM_NOTIFY/LVN_COLUMNCLICK проверял бы нашу подделку, а не поведение AHK, и
    ; при ошибке в ней тест прошёл бы вхолостую. Наличие NoSort у всех шести
    ; списков проверяется по исходникам в tests-logic.ahk; здесь - следствие,
    ; на которое опираются обработчики кнопок.
    edLv := []
    for c in gEditorGui
        if (c.Type = "ListView")
            edLv.Push(c)
    Check(edLv.Length = 3, "в редакторе ровно три списка: " edLv.Length)
    if (edLv.Length = 3) {
        aMapBad := ""
        for i, a in ACTIONS {
            aWant := a.key != "" ? StrUpper(a.key) : "—"
            aGot := edLv[1].GetText(i, 1)
            if (aGot != aWant)
                aMapBad .= " ряд " i ": «" aGot "» вместо «" aWant "»"
        }
        Check(aMapBad = "", "действия: ряд N отвечает ACTIONS[N] по клавише" aMapBad)
        aNameBad := ""
        for i, a in ACTIONS
            if !InStr(edLv[1].GetText(i, 2), ActionName(a))
                aNameBad .= " ряд " i ": «" edLv[1].GetText(i, 2) "» без «" ActionName(a) "»"
        Check(aNameBad = "", "действия: ряд N отвечает ACTIONS[N] и по названию" aNameBad)
        qMapBad := ""
        for i, q in QUICK_REFINE
            if (edLv[2].GetText(i, 1) != q.label)
                qMapBad .= " ряд " i ": «" edLv[2].GetText(i, 1) "» вместо «" q.label "»"
        Check(qMapBad = "", "уточнения: ряд N отвечает QUICK_REFINE[N]" qMapBad)
        pMapBad := ""
        for i, p in APPS
            if (edLv[3].GetText(i, 1) != p.proc)
                pMapBad .= " ряд " i ": «" edLv[3].GetText(i, 1) "» вместо «" p.proc "»"
        Check(pMapBad = "", "приложения: ряд N отвечает APPS[N] по имени процесса" pMapBad)
    }

    ; --- модель по умолчанию в карточке действия (ADR 032) -------------------
    ; У действия, чья model равна cfg.model, список моделей не должен показывать
    ; её отдельной строкой, а выбор обязан стоять на «по умолчанию»: именно из
    ; Value = 1 DoSave получает model := "" - по ADR 032 умолчание хранится
    ; пустой строкой, а не снимком текущего значения. Нажать «Сохранить» отсюда
    ; нечем (обработчик - замыкание внутри диалога), поэтому проверяется Value,
    ; из которого запись и выводится.
    aDef := CloneActions([ACTIONS[1]])[1]
    aDef.model := cfg.model
    snapM := SnapWins()
    EditActionDialog(gEditorGui, aDef, ACTIONS, 1, (*) => 0)
    dMod := NewDialogSince(snapM)
    Check(IsObject(dMod), IsObject(dMod) ? "карточка действия с моделью по умолчанию открылась"
        : "карточка с моделью по умолчанию не создана")
    if IsObject(dMod) {
        ddModelItems := "", ddModelVal := -1
        for c in dMod {
            ; Тип выпадающего списка в AHK v2 называется "DDL": по строке
            ; "DropDownList" фильтр не совпадал никогда и проверка шла вхолостую.
            if (c.Type != "DDL")
                continue
            ddItems := ControlGetItems(c)
            if (ddItems.Length && InStr(ddItems[1], Loc("ed.model.default_of", "по умолчанию"))) {
                ddModelItems := ddItems
                ddModelVal := c.Value
            }
        }
        Check(IsObject(ddModelItems), "список моделей в карточке найден по строке «по умолчанию»")
        dupModel := 0
        if IsObject(ddModelItems)
            for it in ddModelItems
                if (it = cfg.model)
                    dupModel++
        Check(IsObject(ddModelItems) && dupModel = 0,
            "модель по умолчанию не дублируется отдельной строкой списка (найдено " dupModel ")")
        Check(ddModelVal = 1,
            "выбрана строка «по умолчанию», значит DoSave запишет model = «» (Value = " ddModelVal ")")
        Check(IsObject(ddModelItems) && ddModelItems.Length = 1 + ExtraModels().Length,
            "в списке ровно «по умолчанию» плюс дополнительные модели ("
            . (IsObject(ddModelItems) ? ddModelItems.Length : -1) " при " ExtraModels().Length " дополнительных)")
    }
}

ShowSettings()
if !IsObject(gSettingsGui) {
    Check(false, "окно настроек не создано")
    Say("SKIP остаток проверок настроек")
} else {
    Check(true, "настройки открылись без ошибок")
    setLists := [], setTabs := 0
    for c in gSettingsGui {
        if (c.Type = "ListView")
            setLists.Push(c.GetCount())
        if (InStr(c.Type, "Tab") = 1)
            setTabs++
    }
    Check(setTabs = 1, setTabs = 1 ? "вкладки настроек созданы" : "вкладок " setTabs)
    Check(setLists.Length = 2, setLists.Length = 2 ? "списки моделей и языков: строк " setLists[1] " / " setLists[2]
        : "списков в настройках " setLists.Length)
    Check(setLists.Length = 2 && setLists[2] = LANGS.Length, "список языков заполнен")
    ; Список моделей должен показывать 6-7 строк: с высотой на три строки
    ; выбирать модель было невозможно. Считаем по фактической высоте контрола.
    lvModels := 0
    for c in gSettingsGui {
        if (c.Type != "ListView")
            continue
        lvModels := c
        break
    }
    if IsObject(lvModels) {
        lvModels.GetPos(, , , &lvH)
        ; шапка ~20 px, строка ~17 px
        rowsVisible := Floor((lvH - 20) / 17)
        Check(rowsVisible >= 6, rowsVisible >= 6
            ? "в списке моделей помещается строк: " rowsVisible " (высота " lvH ")"
            : "список моделей показывает всего " rowsVisible " строк (высота " lvH ")")
    } else {
        Check(false, "список моделей не найден в окне настроек")
    }

    ; Адрес провайдера - редактируемый ComboBox: выбор из популярных плюс свой.
    urlCombo := 0
    for c in gSettingsGui {
        if (c.Type != "ComboBox")
            continue
        urlCombo := c
        break
    }
    Check(IsObject(urlCombo), IsObject(urlCombo) ? "адрес провайдера - выпадающий список"
        : "ComboBox адреса не найден")
    if IsObject(urlCombo) {
        comboItems := ControlGetItems(urlCombo)
        Check(comboItems.Length = ProviderUrls().Length,
            "в списке адресов столько же строк, сколько в ProviderUrls (" comboItems.Length ")")
        Check(comboItems.Length && comboItems[1] = ProviderUrls()[1].url,
            "первый адрес списка совпадает с ProviderUrls")
        ; .Text, а не .Value: у ComboBox Value - это номер строки, и чтение
        ; через Value вернуло бы 0 для адреса, вписанного руками
        Check(urlCombo.Text = cfg.base_url, "в поле адреса стоит текущий base_url: " urlCombo.Text)
    }


    tx := 0, ty := 0, tw := 0, th := 0
    for c in gSettingsGui
        if (InStr(c.Type, "Tab") = 1)
            c.GetPos(&tx, &ty, &tw, &th)
    clipped := 0, onPage := 0
    for c in gSettingsGui {
        if (c.Type != "Button")
            continue
        c.GetPos(&bx, &by, &bw, &bh)
        if (by >= ty + th)
            continue
        onPage++
        if (by + bh > ty + th - 4 || bx + bw > tx + tw - 4)
            clipped++
    }
    Check(clipped = 0, clipped = 0 ? "кнопки вкладок помещаются (" onPage " шт., вкладка " tw "x" th ")"
        : "кнопок за краем вкладки: " clipped " из " onPage)
    ; 7 кнопок внутри вкладок: Load / Recommend / Test / Default / Retarget на API
    ; и «Добавить свой…» + «Удалить» на языках.
    Check(onPage = 7, onPage = 7 ? "кнопки вкладок ровно те, что нужны (" onPage ")"
        : "кнопок на вкладках всего " onPage)

    ; Ряд N = язык N: SyncChecks переписывает langRows[A_Index].on по номеру ряда,
    ; а «Удалить» снимает langRows[row]. Переставь строки сортировкой - и галочки
    ; лягут на другие языки, а удалится не тот.
    setLv := []
    for c in gSettingsGui
        if (c.Type = "ListView")
            setLv.Push(c)
    Check(setLv.Length = 2, "в настройках ровно два списка: " setLv.Length)
    if (setLv.Length = 2) {
        lgMapBad := ""
        for i, lg in LANGS {
            lgGot := setLv[2].GetText(i, 2)
            if (lgGot != lg.code)
                lgMapBad .= " ряд " i ": «" lgGot "» вместо «" lg.code "»"
        }
        Check(lgMapBad = "", "языки: ряд N отвечает LANGS[N] по коду" lgMapBad)
        lgNameBad := ""
        for i, lg in LANGS
            if (setLv[2].GetText(i, 1) != lg.name)
                lgNameBad .= " ряд " i ": «" setLv[2].GetText(i, 1) "» вместо «" lg.name "»"
        Check(lgNameBad = "", "языки: ряд N отвечает LANGS[N] и по названию" lgNameBad)
        lgChecked := 0, lgRow := 0
        loop {
            lgRow := setLv[2].GetNext(lgRow, "Checked")
            if !lgRow
                break
            lgChecked++
        }
        Check(lgChecked = CountOn(LANGS),
            "галочек в списке столько же, сколько включённых языков (" lgChecked " и " CountOn(LANGS) ")")
    }
}
try FileDelete(ACTIONS_INI)
try FileDelete(APPS_INI)
Say(fails = 0 ? "`nВСЕ ПРОВЕРКИ ПРОШЛИ" : "`nПРОВАЛОВ: " fails)
ExitApp(fails = 0 ? 0 : 1)
#Include Lib/log.ahk
#Include Lib/i18n.ahk
#Include Lib/langs_store.ahk
#Include Lib/config.ahk
#Include Lib/http.ahk
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
#Include Lib/ui_settings_api.ahk
#Include Lib/ui_settings_langs.ahk
#Include Lib/ui_settings_ui.ahk
#Include Lib/recommend.ahk
#Include Lib/ui_actions_editor.ahk

; --- геометрия диалогов -----------------------------------------------------

SnapWins() {
    m := Map()
    for hw in WinGetList()
        m[hw] := true
    return m
}

; Первое наше окно, которого не было в снимке
NewDialogSince(before) {
    for hw in WinGetList() {
        if before.Has(hw)
            continue
        g := 0
        try g := GuiFromHwnd(hw)
        if IsObject(g)
            return g
    }
    return 0
}

; Пары пересекающихся контролов. Контейнеры пропускаем: GroupBox и вкладки
; по устройству лежат под своим содержимым.
OverlapCount(g) {
    rects := []
    for c in g {
        if (c.Type = "GroupBox" || InStr(c.Type, "Tab") = 1)
            continue
        c.GetPos(&x, &y, &cw, &ch)
        if (cw <= 0 || ch <= 0)
            continue
        rects.Push({x: x, y: y, r: x + cw, b: y + ch, t: c.Type})
    }
    n := 0, i := 1
    while (i < rects.Length) {
        j := i + 1
        while (j <= rects.Length) {
            a := rects[i], z := rects[j]
            if (a.x < z.r && z.x < a.r && a.y < z.b && z.y < a.b)
                n++
            j++
        }
        i++
    }
    return n
}
