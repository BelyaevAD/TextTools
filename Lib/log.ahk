; TextTools - Лог, обработка фатальных ошибок и индикация прогресса у трея.
; Подключается из TextTools.ahk, отдельно не запускается.

TT_Log(msg) {
    try {
        if FileExist(LOGF) {
            sz := 0
            try sz := FileGetSize(LOGF)
            if (sz > 1048576) {
                bak := LOGF ".old"
                try FileDelete(bak)
                try FileMove(LOGF, bak)
            }
        }
        FileAppend(FormatTime(, "yyyy-MM-dd HH:mm:ss") "  " msg "`n", LOGF, "UTF-8")
    }
}

ErrorLogger(e, mode) {
    extra := ""
    try extra := (e.Extra != "" ? " | " e.Extra : "") " @ " e.File ":" e.Line
    TT_Log("ОШИБКА (" mode "): " e.Message extra)
    TrayTip(Loc("err.logged", "Ошибка (записана в лог): ") e.Message, "TextTools")
    ; mode = "ExitApp" означает, что процесс сейчас умрет: без рестарта хоткей
    ; пропадает до конца дня и пользователь этого не замечает. Guard по аптайму,
    ; чтобы не зациклиться на ошибке старта. #SingleInstance Force сам снимет
    ; старый экземпляр, если тот все-таки выжил.
    fatal := (mode = "ExitApp") || InStr(e.Message, "Invalid memory")
        || InStr(e.Message, "System exception")
    if (fatal && A_TickCount - gStartTick > 20000) {
        TT_Log("критическая ошибка, запускаю новый экземпляр")
        try Run(Format('"{}" "{}"', A_AhkPath, A_ScriptFullPath))
    }
    return 1
}

ExitLogger(reason, code) {
    TT_Log("выход: " reason " (код " code ")")
}

Flash(msg) {
    ToolTip(msg)
    SetTimer((*) => ToolTip(), -1600)
}

; Плашка с бегущим прогресс-баром у трея: видно, что запрос обрабатывается.
; Вторая строка - цифры (объем запроса, токены ответа) и секундомер: запрос
; обычно укладывается в пару секунд, но видно, что именно сейчас происходит.
BusyShow(text) {
    global gBusyGui, gBusyText, gBusyInfo, gBusyDetail, gBusyStart
    SetTimer(BusyHide, 0)          ; снять отложенное скрытие от прошлого запроса
    BusyHide()
    gBusyDetail := ""
    gBusyStart := A_TickCount
    g := Gui("-Caption +AlwaysOnTop +ToolWindow +Border")
    g.SetFont("s10", "Segoe UI")
    g.MarginX := 12
    g.MarginY := 10
    gBusyText := g.Add("Text", "w300", text)
    pb := g.Add("Progress", "w300 h12 +0x8 -Smooth")
    SendMessage(0x40A, 1, 40, pb.Hwnd)  ; PBM_SETMARQUEE: включить бегущую анимацию
    g.SetFont("s8", "Segoe UI")
    gBusyInfo := g.Add("Text", "w300 cGray", Format(Loc("busy.sec", "{:.1f} с"), 0))
    g.Show("Hide")
    g.GetPos(,, &w, &h)
    MonitorGetWorkArea(MonitorGetPrimary(), &l, &t, &r, &b)
    g.Show(Format("x{} y{} NoActivate", r - w - 12, b - h - 12))
    gBusyGui := g
    SetTimer(BusyTick, 100)
}

BusyHide() {
    global gBusyGui, gBusyText, gBusyInfo, gBusyDetail
    SetTimer(BusyTick, 0)
    if gBusyGui {
        try gBusyGui.Destroy()
        gBusyGui := ""
        gBusyText := ""
        gBusyInfo := ""
        gBusyDetail := ""
    }
}

; Обновляет строку статуса на плашке, если она показана
BusyUpdate(text) {
    global gBusyGui, gBusyText
    if (gBusyGui && gBusyText) {
        try gBusyText.Value := text
    }
}

; Цифры под прогресс-баром; секундомер дописывается тиком
BusyDetail(text) {
    global gBusyDetail
    gBusyDetail := text
    BusyTick()
}

BusyTick() {
    global gBusyGui, gBusyInfo, gBusyDetail, gBusyStart
    if !(gBusyGui && gBusyInfo) {
        SetTimer(BusyTick, 0)
        return
    }
    el := Format(Loc("busy.sec", "{:.1f} с"), (A_TickCount - gBusyStart) / 1000)
    try gBusyInfo.Value := (gBusyDetail != "" ? gBusyDetail " · " el : el)
}

; Итог остается на плашке пару секунд: иначе цифры исчезают раньше, чем их
; успеваешь прочитать. Секундомер останавливаем на последнем отрисованном.
BusyDone(text) {
    global gBusyGui, gBusyText
    if !gBusyGui
        return
    SetTimer(BusyTick, 0)
    if gBusyText
        try gBusyText.Value := text
    SetTimer(BusyHide, -2000)
}
