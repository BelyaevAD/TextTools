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

; Есть ли в буфере хоть какой-то формат: для картинки и файлов A_Clipboard пуст,
; а для честно пустого буфера форматов нет вовсе - терять в нем нечего
ClipHasData() {
    n := 0
    try n := DllCall("CountClipboardFormats", "Int")
    return n > 0
}

; Буфером владеет вся система: любое обращение может упасть, если его держит
; открытым другое приложение (RDP, синхронизаторы буфера, Excel). Без защиты
; поток умирал молча, а буфер пользователя оставался стёртым.
GetSelection() {
    global gClipWarned
    CancelRestore()
    saved := ""
    try saved := A_Clipboard
    catch as e
        TT_Log("буфер обмена не прочитан (" e.Message ")")
    if (saved = "" && !gClipWarned && ClipHasData()) {
        gClipWarned := true
        Flash(Loc("clip.nontext", "В буфере нет текста (картинка или файлы). TextTools сохраняет только текст — нетекстовое содержимое буфера будет потеряно."))
    }
    ; очистка обязательна: ClipWait вернётся немедленно, если в буфере остались
    ; прежние данные, и действие ушло бы работать с ними вместо выделения
    try A_Clipboard := ""
    catch as e {
        TT_Log("буфер обмена занят другим приложением (" e.Message "), выделение не прочитано")
        return ""
    }
    ok := 0
    try {
        Send "^c"
        ok := ClipWait(ClipWaitTimeout())
    } catch as e
        TT_Log("копирование выделения не удалось (" e.Message ")")
    text := ""
    if ok {
        try text := A_Clipboard
    }
    try A_Clipboard := saved
    catch as e
        TT_Log("буфер обмена не восстановлен (" e.Message ")")
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
    saved := ""
    try saved := A_Clipboard
    catch as e
        TT_Log("вставка: прежний буфер не прочитан (" e.Message ")")
    ; без текста в буфере вставлять нечего, и результат нельзя терять молча
    try A_Clipboard := t
    catch as e {
        TT_Log("вставка: буфер недоступен (" e.Message "), показываю в окне")
        ShowResult(Loc("clip.result", "Результат"), t, convo, model)
        return
    }
    WinActivate("ahk_id " gPrevWin)
    ok := 0
    try ok := WinWaitActive("ahk_id " gPrevWin, , 2)
    if !ok {
        TT_Log("вставка: не удалось активировать окно " gPrevWin ", показываю в окне")
        try A_Clipboard := saved
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
    cur := ""
    try cur := A_Clipboard
    catch as e {
        TT_Log("буфер не восстановлен: не читается (" e.Message ")")
        return
    }
    if (cur = pasted)
        try A_Clipboard := saved
}
