; TextTools - Сборка системного промпта и выполнение действия целиком.
; Подключается из TextTools.ahk, отдельно не запускается.

; srcHint - поправка от приложения-источника (Lib/apps_store.ahk); пустая
; строка означает «обрабатывать как обычно»
BuildInstruction(a, srcHint := "") {
    parts := []
    parts.Push(LangBlock(a.lang))
    parts.Push(BLOCK_CONTEXT)
    if (srcHint != "")
        parts.Push("SOURCE APPLICATION: " srcHint)
    parts.Push(BLOCK_FIDELITY)
    if a.ru
        parts.Push(BLOCK_RUSTYLE)
    parts.Push(a.task)
    ; краткость идёт ПОСЛЕ задачи: правило про воду должно перебивать соблазн
    ; развернуть ответ, заданный самим TASK-блоком
    parts.Push(BLOCK_BREVITY)
    parts.Push(BLOCK_OUTPUT)
    out := ""
    for i, p in parts
        out .= (i > 1 ? "`n`n" : "") p
    return out
}

RunAction(a, customText := "", forceWindow := false) {
    global gBusy, gLastAction, gLastInstr, gCallLabel
    ; меню может быть открыто, пока идет уточнение в окне результата
    if gBusy {
        Flash(Loc("msg.busy", "Запрос уже выполняется"))
        return
    }
    MenuClose()
    ; сразу возвращаем фокус исходному окну: пока фокус наш, Windows разрешает
    ; его передать; после долгого запроса это право можно и не получить
    if (gPrevWin && WinExist("ahk_id " gPrevWin))
        try WinActivate("ahk_id " gPrevWin)
    if (cfg.api_key = "") {
        ShowSettings()
        return
    }
    gLastAction := a
    gLastInstr := customText
    gCallLabel := a.name              ; попадет в файл расхода токенов
    gBusy := true
    ac := ActionCfg(a)                ; своя модель действия перекрывает настройки
    TT_Log("действие: " a.name (customText != "" ? " (+инструкция)" : "")
        . (forceWindow ? " (Shift: в окно)" : "") ", выделено " StrLen(gSel) " символов"
        . ", модель " ac.model)
    BusyShow("⏳ " ActionName(a) ": " Loc("run.preparing", "готовлю запрос…"))
    result := "", err := "", instr := "", user := ""
    try {
        instr := BuildInstruction(a, AppHint(gPrevWin))
        user := a.prefix gSel
        if (customText != "") {
            if (a.name = "Custom")
                user .= "`n`nThe change to make: " customText
            else
                user .= "`n`nAdditional instruction from the user (follow it within the task above): " customText
        }
        result := CallApiCfg(ac, instr, user)
    } catch as e {
        err := e.Message
    }
    gBusy := false
    if err {
        BusyHide()
        TT_Log("сбой: " err)
        MsgBox(Loc("msg.request_failed", "Ошибка запроса:") "`n`n" err, "TextTools", "Iconx 262144")
        return
    }
    ; плашка с итоговыми цифрами гаснет сама через пару секунд
    BusyDone("✅ " ActionName(a) ": " Loc("run.done", "готово"))
    result := Trim(result, " `t`r`n")
    TT_Log("ok, ответ " StrLen(result) " символов")
    SaveLastResult(result)
    HistoryAdd(ActionName(a), gSel, result, ac.model, gLastUsage)
    convo := [{role: "system", content: instr}, {role: "user", content: user}, {role: "assistant", content: result}]
    ; модель тащим дальше: уточнения в окне должны идти той же моделью
    if (a.win || forceWindow)
        ShowResult(ActionName(a), result, convo, ac.model)
    else
        PasteResult(result, convo, ac.model)   ; при неудачной вставке уйдет в окно
    TT_Log("действие завершено")
}
