; TextTools - Чтение выделения и вставка результата; отложенное восстановление буфера.
; Подключается из TextTools.ahk, отдельно не запускается.

GetSelection() {
    ; отложенное восстановление буфера от прошлой вставки может выстрелить
    ; посреди ClipWait и подсунуть старый текст вместо нового выделения
    CancelRestore()
    ; сохраняем только текст буфера: бинарный ClipboardAll на экзотических
    ; форматах буфера подозревается в порче памяти процесса
    saved := A_Clipboard
    A_Clipboard := ""
    Send "^c"
    ok := ClipWait(0.8)
    text := ok ? A_Clipboard : ""
    A_Clipboard := saved
    return text
}

PasteResult(t, convo := 0, model := "") {
    global gRestoreTimer
    CancelRestore()
    if !(gPrevWin && WinExist("ahk_id " gPrevWin)) {
        ; окно, где выделяли текст, уже закрыто - вставлять некуда, показываем результат
        TT_Log("вставка: исходное окно закрыто, показываю в окне")
        ShowResult("Результат", t, convo, model)
        return
    }
    saved := A_Clipboard
    A_Clipboard := t
    WinActivate("ahk_id " gPrevWin)
    ok := 0
    try ok := WinWaitActive("ahk_id " gPrevWin, , 2)
    if !ok {
        ; Windows не отдал фокус окну - не шлем ^v вслепую, показываем результат
        TT_Log("вставка: не удалось активировать окно " gPrevWin ", показываю в окне")
        A_Clipboard := saved
        ShowResult("Результат", t, convo, model)
        return
    }
    ; Определить, есть ли под курсором поле ввода, надежного способа нет:
    ; ControlGetFocus пуст и для Chromium/Electron с активным полем ввода
    ; (проверено 2026-07-30 на chrome/claude/Cursor), поэтому автооткат в окно
    ; ломал бы замену везде. Осознанный выбор: шлем ^v, а для нередактируемых
    ; мест у пользователя есть Shift - показать результат в окне.
    Sleep 150
    Send "^v"
    TT_Log("вставка отправлена в окно " gPrevWin)
    ; буфер возвращаем с запасом: медленные приложения читают его не сразу
    gRestoreTimer := RestoreClip.Bind(saved, t)
    SetTimer(gRestoreTimer, -3000)
}

; Снять отложенное восстановление буфера, если оно еще не отработало
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
    ; возвращаем прежний буфер только если в нем все еще наш результат:
    ; иначе затрем то, что пользователь скопировал за эти секунды
    if (A_Clipboard = pasted)
        A_Clipboard := saved
}

; convo: массив {role, content} для follow-up-доработки; 0 = без доработки
