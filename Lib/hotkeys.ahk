; TextTools - Хоткеи (пара применяется атомарно), повтор последнего действия,
; клавиши-акселераторы меню и меню в трее.
; Подключается из TextTools.ahk, отдельно не запускается.

ApplyHotkeyPair(hk, hkRepeat) {
    global gHotkeyCurrent, gHotkeyRepeatCurrent
    if gHotkeyCurrent {
        try Hotkey(gHotkeyCurrent, "Off")
        gHotkeyCurrent := ""
    }
    if gHotkeyRepeatCurrent {
        try Hotkey(gHotkeyRepeatCurrent, "Off")
        gHotkeyRepeatCurrent := ""
    }
    Hotkey(hk, MenuHotkeyHandler, "On")
    gHotkeyCurrent := hk
    if (hkRepeat != "" && hkRepeat != hk) {
        Hotkey(hkRepeat, RepeatLast, "On")
        gHotkeyRepeatCurrent := hkRepeat
    }
}

MenuHotkeyHandler(*) {
    ShowMenu()
}

; --- окно-источник текста -----------------------------------------------
; По хоткею активное окно и есть источник, но при вызове из трея активной
; оказывается оболочка Windows, а Ctrl+C уходит в никуда - меню молча
; отвечало "нет выделенного текста". Поэтому последнее пользовательское окно
; запоминается фоновым трекером.

ForeWatch() {
    global gForeWin
    id := 0
    try id := WinGetID("A")
    if (!id || IsOwnWin(id) || IsShellWin(id))
        return
    gForeWin := id
}

; Свой PID берем у ProcessExist(): главное окно скрипта скрыто, и WinGetPID по
; A_ScriptHwnd молча не находит его без DetectHiddenWindows - трекер тогда
; считал бы окна самого TextTools окном-источником.
IsOwnWin(id) {
    static myPid := ProcessExist()
    pid := 0
    try pid := WinGetPID("ahk_id " id)
    return (pid && myPid && pid = myPid)
}

; Панель задач, рабочий стол, выпадающие меню и оболочка Windows 11
IsShellWin(id) {
    cls := ""
    try cls := WinGetClass("ahk_id " id)
    for c in ["Shell_TrayWnd", "Shell_SecondaryTrayWnd", "Progman", "WorkerW", "#32768"
        , "Windows.UI.Core.CoreWindow", "XamlExplorerHostIslandWindow", "Xaml_WindowedPopupClass"] {
        if (cls = c)
            return true
    }
    return false
}

SourceWin() {
    global gForeWin
    id := 0
    try id := WinGetID("A")
    if (id && !IsOwnWin(id) && !IsShellWin(id))
        return id
    return gForeWin
}

; Вернуть фокус источнику перед Ctrl+C: из трея фокус у оболочки
FocusSource(id) {
    if !(id && WinExist("ahk_id " id))
        return false
    if WinActive("ahk_id " id)
        return true
    try WinActivate("ahk_id " id)
    ok := 0
    try ok := WinWaitActive("ahk_id " id, , 1)
    return ok ? true : false
}

; Повтор последнего действия на новом выделении, минуя меню
RepeatLast(*) {
    global gSel, gPrevWin
    if gBusy
        return
    if !IsObject(gLastAction) {
        Flash(Loc("msg.nothing_repeat", "Повторять нечего - действий еще не было"))
        return
    }
    MenuClose()
    gPrevWin := SourceWin()
    if !FocusSource(gPrevWin) {
        Flash(Loc("msg.no_source_win", "Не нашлось окна с текстом"))
        return
    }
    gSel := GetSelection()
    if (gSel = "") {
        Flash(Loc("msg.no_selection", "Нет выделенного текста"))
        return
    }
    RunAction(gLastAction, gLastInstr)
}

; Контекст клавиш-акселераторов меню
MenuKeysCtx(*) {
    global gMenuGui, gMenuEdit
    if !gMenuGui
        return false
    ok := false
    try ok := WinActive("ahk_id " gMenuGui.Hwnd) && (gMenuGui.FocusedCtrl != gMenuEdit)
    return ok
}

; Клавиши пула регистрируются один раз на старте, а действие ищется по своему
; полю key: иначе правка клавиши в редакторе требовала бы перерегистрации
; хоткеев на лету - лишний риск ради того же результата.
MenuKeyPress(key, forceWindow, *) {
    global gMenuEdit
    extra := ""
    try extra := Trim(gMenuEdit.Value)
    for a in ACTIONS {
        if (a.HasProp("key") && a.key = key) {
            RunAction(a, extra, forceWindow)
            return
        }
    }
}

SetupTray() {
    A_IconTip := "TextTools - обработка текста через LLM (" cfg.hotkey ")"
    A_TrayMenu.Delete()
    ; Пункта "Меню обработки" здесь намеренно нет: чтобы добраться до трея, окно
    ; с выделенным текстом придется покинуть, а часть приложений при потере
    ; фокуса снимает выделение - Ctrl+C вернул бы пустоту или чужой текст.
    ; Меню вызывается только хоткеем, не отрывая рук от текста.
    A_TrayMenu.Add(Loc("tray.history", "История…"), ShowHistory)
    A_TrayMenu.Add(Loc("tray.last", "Показать последний результат"), ShowLastResult)
    A_TrayMenu.Add(Loc("tray.stats", "Статистика использования"), ShowStats)
    A_TrayMenu.Add()
    A_TrayMenu.Add(Loc("tray.editor", "Редактор действий…"), ShowActionsEditor)
    A_TrayMenu.Add(Loc("tray.settings", "Настройки"), ShowSettings)
    A_TrayMenu.Add()
    A_TrayMenu.Add(Loc("tray.log", "Открыть лог"), (*) => Run(LOGF))
    A_TrayMenu.Add(Loc("tray.folder", "Открыть папку скрипта"), (*) => Run(A_ScriptDir))
    A_TrayMenu.Add(Loc("tray.restart", "Перезапустить"), (*) => Reload())
    A_TrayMenu.Add(Loc("tray.exit", "Выход"), (*) => ExitApp())
    A_TrayMenu.Default := Loc("tray.settings", "Настройки")
}
