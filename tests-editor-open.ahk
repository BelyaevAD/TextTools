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
