; TextTools - Окно-сетка действий: показ, раскладка, закрытие по потере фокуса.
; Подключается из TextTools.ahk, отдельно не запускается.

ShowMenu() {
    global gSel, gPrevWin, gMenuGui
    if gBusy
        return
    MenuClose()
    ; из трея активна оболочка, поэтому источник берем у трекера и возвращаем
    ; ему фокус - иначе Ctrl+C уходит в никуда (см. hotkeys.ahk)
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
    gMenuGui := BuildMenu()
    PositionAndShow(gMenuGui)
    ; фокус на первую кнопку: цифры/буквы работают как акселераторы,
    ; для ввода инструкции - Tab или клик в поле
    if gMenuFirstBtn
        try gMenuFirstBtn.Focus()
    SetTimer(MenuWatch, 300)
}

; Закрывает меню, когда фокус ушел на другое окно (замена перехвата WM_ACTIVATE)
MenuWatch() {
    global gMenuGui
    if !gMenuGui {
        SetTimer(MenuWatch, 0)
        return
    }
    active := 0
    try active := WinActive("ahk_id " gMenuGui.Hwnd)
    if !active
        MenuClose()
}

BuildMenu() {
    global gMenuEdit, gMenuFirstBtn
    g := Gui("-Caption +AlwaysOnTop +ToolWindow +Border")
    g.SetFont("s10", "Segoe UI")

    bw := 150, bh := 34, gap := 7
    totalW := 3 * bw + 2 * gap

    g.Add("Text", "x10 y13", Loc("menu.instruction", "Инструкция:"))
    eCustom := g.Add("Edit", Format("x95 y10 w{} h26", 10 + totalW - 95), "")
    gMenuEdit := eCustom

    gMenuFirstBtn := ""
    for i, a in ACTIONS {
        col := Mod(i - 1, 3)
        row := (i - 1) // 3
        opts := Format("x{} y{} w{} h{}", 10 + col * (bw + gap), 46 + row * (bh + gap), bw, bh)
        key := (a.HasProp("key") && a.key != "") ? StrUpper(a.key) " " : ""
        label := key (a.icon != "" ? a.icon "  " : "") ActionName(a)
        btn := g.Add("Button", opts, label)
        btn.OnEvent("Click", MenuBtnClick.Bind(a, eCustom))
        if (i = 1)
            gMenuFirstBtn := btn
    }

    ; Нижняя строка: подсказка слева, компактный вход в редактор справа.
    ; Кнопка намеренно мельче командных, чтобы не соперничать с ними взглядом.
    rows := Ceil(ACTIONS.Length / 3)
    footY := 46 + rows * (bh + gap) + 2
    edW := 150
    g.SetFont("s9 c707070")
    g.Add("Text", Format("x10 y{} w{}", footY, totalW - edW - 10),
        Loc("menu.hint", "Клавиша на кнопке запускает действие. Shift — показать результат в окне, не заменяя выделение."))
    bEditor := g.Add("Button", Format("x{} y{} w{} h24", 10 + totalW - edW, footY - 3, edW),
        Loc("menu.editor", "⚙  Редактор действий"))
    g.SetFont("s10")
    bEditor.OnEvent("Click", (*) => (MenuClose(), ShowActionsEditor()))

    bDef := g.Add("Button", "x0 y0 w0 h0 Default", "")
    bDef.OnEvent("Click", CustomGo.Bind(eCustom))

    g.OnEvent("Escape", MenuClose)
    g.OnEvent("Close", MenuClose)
    return g
}

; Текст из поля "Инструкция" уходит дополнительным указанием к ЛЮБОЙ кнопке.
; Shift при клике - показать результат в окне вместо замены выделения.
MenuBtnClick(a, eCustom, *) {
    extra := ""
    try extra := Trim(eCustom.Value)
    RunAction(a, extra, GetKeyState("Shift", "P"))
}

CustomGo(eCustom, *) {
    txt := ""
    try txt := Trim(eCustom.Value)
    if (txt = "")
        return
    RunAction(CUSTOM_ACTION, txt, GetKeyState("Shift", "P"))
}

MenuClose(*) {
    global gMenuGui
    SetTimer(MenuWatch, 0)
    if gMenuGui {
        try gMenuGui.Destroy()
        gMenuGui := ""
    }
}

PositionAndShow(g) {
    x := 0, y := 0
    got := false
    try got := CaretGetPos(&x, &y)
    if (!got || (x <= 0 && y <= 0))
        MouseGetPos(&x, &y)
    else
        y += 24
    g.Show("Hide")
    g.GetPos(,, &w, &h)
    loop MonitorGetCount() {
        MonitorGetWorkArea(A_Index, &l, &t, &r, &b)
        if (x >= l && x < r && y >= t && y < b) {
            x := Max(Min(x, r - w - 6), l + 6)
            y := Max(Min(y, b - h - 6), t + 6)
            break
        }
    }
    g.Show(Format("x{} y{}", x, y))
}
