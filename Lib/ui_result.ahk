; TextTools - Окно результата: Markdown-рендер, быстрые уточнения, follow-up-диалог.
; Подключается из TextTools.ahk, отдельно не запускается.

; model - модель, которой получен текст: уточнения должны идти той же,
; иначе дешёвое действие внезапно продолжилось бы дорогой моделью из настроек
ShowResult(title, text, convo := 0, model := "") {
    g := Gui("+AlwaysOnTop", "TextTools - " title)
    g.SetFont("s10", "Segoe UI")
    hasConvo := IsObject(convo)
    disp := StrReplace(StrReplace(text, "`r`n", "`n"), "`n", "`r`n")

    ; Пробуем отрендерить Markdown во встроенном браузерном контроле; при любой
    ; ошибке откатываемся на простое текстовое поле.
    ; ВНИМАНИЕ: это единственный COM в скрипте, и он уже ронял процесс
    ; аппаратным исключением внутри Navigate (лог 2026-07-30 22:43,
    ; 0xC0000094) - такое не ловится try/catch. Если падения повторятся,
    ; выключить рендер: Настройки -> снять галку (ui/markdown=0 в INI).
    rendered := false
    ax := "", wbRef := ""
    if cfg.markdown {
        try {
            ax := g.Add("ActiveX", "x10 y10 w780 h470", "Shell.Explorer")
            wb := ax.Value
            wb.Silent := true
            wb.Navigate("about:blank")
            t0 := A_TickCount
            while (wb.ReadyState < 4 && A_TickCount - t0 < 3000)
                Sleep 20
            doc := wb.Document
            doc.open()
            doc.write(MdToHtml(text))
            doc.close()
            wbRef := wb
            rendered := true
        } catch as e {
            TT_Log("рендер Markdown не удался (" e.Message "), показываю текстом")
            try {
                if ax
                    ax.Visible := false
            }
        }
    }

    ed := g.Add("Edit", "x10 y10 w780 h470 Multi ReadOnly VScroll" (rendered ? " Hidden" : ""), disp)

    yRow := 488
    eAsk := "", bAsk := ""
    if hasConvo {
        ; ряд быстрых уточнений: тот же диалог, инструкция задана заранее
        g.SetFont("s9", "Segoe UI")
        g.Add("Text", "x10 y" (yRow + 6), Loc("res.quick", "Быстро:"))
        first := true
        for qr in QUICK_REFINE {
            chip := g.Add("Button", (first ? "x68 y" yRow " h26" : "x+6 yp h26"), qr.label)
            chip.OnEvent("Click", QuickRefine.Bind(qr))
            first := false
        }
        g.SetFont("s10", "Segoe UI")
        yRow += 34

        g.Add("Text", "x10 y" (yRow + 4), Loc("res.refine", "Уточнить:"))
        eAsk := g.Add("Edit", "x85 y" yRow " w567 h26", "")
        bAsk := g.Add("Button", "x+8 y" (yRow - 1) " w130 Default", Loc("res.send", "Отправить"))
        yRow += 37
    }

    bCopy := g.Add("Button", "x10 y" yRow " w130", Loc("btn.copy", "Копировать"))
    bPaste := g.Add("Button", "x+8 w160", Loc("res.paste", "Вставить в окно"))
    bView := rendered ? g.Add("Button", "x+8 w130", Loc("res.source", "Исходник")) : ""
    bClose := g.Add("Button", "x+8 w100", Loc("btn.close", "Закрыть"))

    bCopy.OnEvent("Click", (*) => (A_Clipboard := text, Flash(Loc("msg.copied", "Скопировано"))))
    bPaste.OnEvent("Click", (*) => (g.Destroy(), PasteResult(text)))
    bClose.OnEvent("Click", (*) => g.Destroy())
    if rendered
        bView.OnEvent("Click", ToggleSrc)
    if hasConvo
        bAsk.OnEvent("Click", DoRefine)
    g.OnEvent("Escape", (*) => g.Destroy())
    ; без обработчика Close крестик окна только прячет Gui: скрытые окна
    ; вместе с ActiveX-рендером копились бы до конца сессии
    g.OnEvent("Close", (*) => g.Destroy())
    g.Show()
    TT_Log("окно результата показано" (rendered ? " (Markdown)" : " (текст)"))

    ToggleSrc(*) {
        if ed.Visible {
            ed.Visible := false
            ax.Visible := true
            bView.Text := Loc("res.source", "Исходник")
        } else {
            ax.Visible := false
            ed.Visible := true
            bView.Text := Loc("res.markup", "Разметка")
        }
    }

    ; Follow-up: уточнение уходит следующим ходом того же диалога,
    ; результат обновляется в этом же окне
    DoRefine(*) {
        q := ""
        try q := Trim(eAsk.Value)
        if (q = "")
            return
        RefineWith(q)
    }

    ; Кнопка быстрого уточнения - та же механика с готовой инструкцией
    QuickRefine(qr, *) {
        RefineWith(qr.instr, qr.label)
    }

    RefineWith(q, label := "") {
        global gBusy, gCallLabel
        if gBusy {
            Flash(Loc("msg.busy", "Запрос уже выполняется"))
            return
        }
        hw := g.Hwnd                     ; окно можно закрыть, пока ждем ответ
        gCallLabel := (label != "" ? "уточнение: " label : "уточнение")
        try bAsk.Enabled := false
        BusyShow("⏳ " (label != "" ? label : Loc("res.refine_busy", "уточнение")) "…")
        TT_Log("уточнение" (label != "" ? " (" label ")" : "") ", диалог " convo.Length " сообщений")
        convo.Push({role: "user", content: q})
        r := "", err := ""
        rc := (model != "" && model != cfg.model)
            ? {base_url: cfg.base_url, api_key: cfg.api_key, proxy: CfgFlag(cfg, "proxy", ""), model: model} : cfg
        gBusy := true
        try r := CallApiMsgs(rc, TrimConvo(convo))
        catch as e
            err := e.Message
        finally
            gBusy := false
        BusyHide()
        alive := WinExist("ahk_id " hw)
        if alive
            try bAsk.Enabled := true
        if err {
            convo.Pop()
            TT_Log("сбой уточнения: " err)
            MsgBox(Loc("msg.request_failed", "Ошибка запроса:") "`n`n" err, "TextTools", "Iconx 262144")
            return
        }
        ; сначала сохраняем ответ, и только потом трогаем контролы: окно могли
        ; закрыть во время запроса, обращение к мертвому контролу убьет поток
        r := Trim(r, " `t`r`n")
        convo.Push({role: "assistant", content: r})
        text := r
        SaveLastResult(r)
        HistoryAdd(title " (" (label != "" ? label : "уточнение") ")", q, r, rc.model, gLastUsage)
        TT_Log("уточнение ok, ответ " StrLen(r) " символов")
        if !alive {
            TT_Log("окно уточнения закрыто, результат сохранен в историю")
            return
        }
        try {
            eAsk.Value := ""
            ed.Value := StrReplace(StrReplace(r, "`r`n", "`n"), "`n", "`r`n")
        }
        if rendered {
            try {
                d := wbRef.Document
                d.open()
                d.write(MdToHtml(r))
                d.close()
            } catch {
                rendered := false
                try ax.Visible := false
                try ed.Visible := true
            }
        }
    }
}

; Ограничение хвоста диалога: system + последние ~8 сообщений.
; После system первым обязан идти user - иначе строгие провайдеры отвечают 400.
TrimConvo(msgs) {
    if (msgs.Length <= 9)
        return msgs
    out := [msgs[1]]
    i := msgs.Length - 7
    if (msgs[i].role != "user")
        i++
    while (i <= msgs.Length) {
        out.Push(msgs[i])
        i++
    }
    return out
}
