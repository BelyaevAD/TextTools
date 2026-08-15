; TextTools - Чтение выделения и вставка результата; отложенное восстановление буфера.
; Подключается из TextTools.ahk, отдельно не запускается.

ClipWaitTimeout() {
    t := 0.8
    if (IsObject(cfg) && cfg.HasProp("clip_wait") && cfg.clip_wait != "")
        try t := cfg.clip_wait + 0
    if (t < 0.2)
        t := 0.2
    if (t > 5)
        t := 5
    return t
}

GetSelection() {
    global gClipWarned
    CancelRestore()
    saved := A_Clipboard
    if (saved = "" && !gClipWarned) {
        gClipWarned := true
        Flash(Loc("clip.nontext", "В буфере нет текста (картинка или файлы). TextTools сохраняет только текст — нетекстовое содержимое буфера будет потеряно."))
    }
    A_Clipboard := ""
    Send "^c"
    ok := ClipWait(ClipWaitTimeout())
    text := ok ? A_Clipboard : ""
    A_Clipboard := saved
    return text
}

PasteResult(t, convo := 0, model := "") {
    global gRestoreTimer
    CancelRestore()
    if !(gPrevWin && WinExist("ahk_id " gPrevWin)) {
        TT_Log("вставка: исходное окно закрыто, показываю в окне")
        ShowResult(Loc("clip.result", "Результат"), t, convo, model)
        return
    }
    saved := A_Clipboard
    A_Clipboard := t
    WinActivate("ahk_id " gPrevWin)
    ok := 0
    try ok := WinWaitActive("ahk_id " gPrevWin, , 2)
    if !ok {
        TT_Log("вставка: не удалось активировать окно " gPrevWin ", показываю в окне")
        A_Clipboard := saved
        ShowResult(Loc("clip.result", "Результат"), t, convo, model)
        return
    }
    Sleep 150
    Send "^v"
    TT_Log("вставка отправлена в окно " gPrevWin)
    gRestoreTimer := RestoreClip.Bind(saved, t)
    SetTimer(gRestoreTimer, -3000)
}

CancelRestore() {
    global gRestoreTimer
    if gRestoreTimer {
        try SetTimer(gRestoreTimer, 0)
        gRestoreTimer := ""
    }
}

RestoreClip(saved, pasted) {
    global gRestoreTimer
    gRestoreTimer := ""
    if (A_Clipboard = pasted)
        A_Clipboard := saved
}
