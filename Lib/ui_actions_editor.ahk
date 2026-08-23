; TextTools - редактор действий: состав кнопок меню и быстрых уточнений,
; их промпты и режимы. Подключается из TextTools.ahk, отдельно не запускается.
;
; Правки ведутся на КОПИИ наборов и применяются только по «Сохранить»: закрытие
; окна крестиком или Esc ничего не меняет. После сохранения перестраивать ничего
; не нужно - меню и окно результата читают ACTIONS/QUICK_REFINE при каждом показе.

ShowActionsEditor(*) {
    global gEditorGui
    if IsSet(gEditorGui) && gEditorGui {
        try {
            gEditorGui.Show()
            return
        }
    }
    acts := CloneActions(ACTIONS)              ; рабочая копия
    quicks := CloneQuick(QUICK_REFINE)
    ; ВАЖНО: имя рабочей копии не должно отличаться от глобального APPS только
    ; регистром - в AHK v2 имена нечувствительны к регистру, и присваивание
    ; сделало бы локальной саму глобальную переменную (см. TEXTTOOLS_RUNBOOK §5 п.27)
    appList := CloneApps(APPS)

    g := Gui("+Resize", "TextTools - " Loc("ed.title", "редактор действий"))
    gEditorGui := g
    g.SetFont("s10", "Segoe UI")
    tab := g.Add("Tab3", "x10 y10 w760 h420", [
        Loc("ed.tab.actions", "Кнопки меню"),
        Loc("ed.tab.quick", "Быстрые уточнения"),
        Loc("ed.tab.apps", "Приложения")])

    ; --- вкладка 1: действия ---
    tab.UseTab(1)
    lvA := g.Add("ListView", "x25 y45 w560 r16 -Multi",
        [Loc("ed.col.key", "Клавиша"), Loc("ed.col.name", "Название"),
         Loc("ed.col.result", "Результат"), Loc("ed.col.lang", "Язык"),
         Loc("ed.col.model", "Модель")])
    bAAdd := g.Add("Button", "x595 y45 w160", Loc("ed.add", "Добавить"))
    bAEdit := g.Add("Button", "x595 y+6 w160", Loc("ed.edit", "Изменить…"))
    bADup := g.Add("Button", "x595 y+6 w160", Loc("ed.dup", "Дублировать"))
    bADel := g.Add("Button", "x595 y+6 w160", Loc("btn.delete", "Удалить"))
    g.Add("Text", "x595 y+14 w160", Loc("ed.grid_order", "Порядок в сетке:"))
    bAUp := g.Add("Button", "x595 y+4 w77", Loc("ed.up", "Вверх"))
    bADown := g.Add("Button", "x+6 yp w77", Loc("ed.down", "Вниз"))

    ; --- вкладка 2: быстрые уточнения ---
    tab.UseTab(2)
    lvQ := g.Add("ListView", "x25 y45 w560 r16 -Multi",
        [Loc("ed.col.name", "Название"), Loc("ed.col.instr", "Инструкция модели")])
    bQAdd := g.Add("Button", "x595 y45 w160", Loc("ed.add", "Добавить"))
    bQEdit := g.Add("Button", "x595 y+6 w160", Loc("ed.edit", "Изменить…"))
    bQDel := g.Add("Button", "x595 y+6 w160", Loc("btn.delete", "Удалить"))
    g.Add("Text", "x595 y+14 w160", Loc("ed.row_order", "Порядок в ряду:"))
    bQUp := g.Add("Button", "x595 y+4 w77", Loc("ed.up", "Вверх"))
    bQDown := g.Add("Button", "x+6 yp w77", Loc("ed.down", "Вниз"))

    ; --- вкладка 3: приложения-источники ---
    tab.UseTab(3)
    lvP := g.Add("ListView", "x25 y45 w560 r16 -Multi",
        [Loc("ed.col.proc", "Процесс"), Loc("ed.col.title_has", "Заголовок содержит"),
         Loc("ed.col.name", "Название"), Loc("ed.col.prompt_fix", "Поправка к промпту")])
    bPAdd := g.Add("Button", "x595 y45 w160", Loc("ed.add", "Добавить"))
    bPEdit := g.Add("Button", "x595 y+6 w160", Loc("ed.edit", "Изменить…"))
    bPDel := g.Add("Button", "x595 y+6 w160", Loc("btn.delete", "Удалить"))
    g.Add("Text", "x595 y+14 w160", Loc("ed.app_order", "Порядок роли не играет:"))
    g.SetFont("s9 c707070")
    g.Add("Text", "x595 y+2 w160",
        Loc("ed.app_order.hint", "записи с заголовком проверяются первыми, поэтому Jira выигрывает у общего Chrome"))
    g.SetFont("s10")

    tab.UseTab()
    g.SetFont("s9 c707070")
    hintText := Loc("ed.hint", "Клавиша выбирается в карточке из {1} возможных; занятую можно обменять или забрать. Порядок в списке задаёт только раскладку сетки. Промпт пишется на английском: так модель точнее следует инструкции.")
    hint := g.Add("Text", "x10 y440 w760", StrReplace(hintText, "{1}", MENU_KEYS.Length))
    g.SetFont("s10")
    bSave := g.Add("Button", "x10 y470 w150 Default", Loc("btn.save", "Сохранить"))
    bCancel := g.Add("Button", "x+8 w120", Loc("btn.cancel", "Отмена"))
    bReset := g.Add("Button", "x+8 w200", Loc("ed.reset", "Сбросить к заводским"))

    FillA(), FillQ(), FillP()

    bAAdd.OnEvent("Click", (*) => EditActionRow(0))
    bAEdit.OnEvent("Click", (*) => EditActionRow(lvA.GetNext(0)))
    lvA.OnEvent("DoubleClick", (*) => EditActionRow(lvA.GetNext(0)))
    bADup.OnEvent("Click", DupA)
    bADel.OnEvent("Click", DelA)
    bAUp.OnEvent("Click", (*) => MoveRow(acts, lvA, -1, FillA))
    bADown.OnEvent("Click", (*) => MoveRow(acts, lvA, 1, FillA))

    bQAdd.OnEvent("Click", (*) => EditQuickRow(0))
    bQEdit.OnEvent("Click", (*) => EditQuickRow(lvQ.GetNext(0)))
    lvQ.OnEvent("DoubleClick", (*) => EditQuickRow(lvQ.GetNext(0)))
    bQDel.OnEvent("Click", DelQ)
    bQUp.OnEvent("Click", (*) => MoveRow(quicks, lvQ, -1, FillQ))
    bQDown.OnEvent("Click", (*) => MoveRow(quicks, lvQ, 1, FillQ))

    bPAdd.OnEvent("Click", (*) => EditAppRow(0))
    bPEdit.OnEvent("Click", (*) => EditAppRow(lvP.GetNext(0)))
    lvP.OnEvent("DoubleClick", (*) => EditAppRow(lvP.GetNext(0)))
    bPDel.OnEvent("Click", DelP)

    bSave.OnEvent("Click", DoSaveSets)
    bCancel.OnEvent("Click", (*) => CloseEditor())
    bReset.OnEvent("Click", DoReset)
    g.OnEvent("Escape", (*) => CloseEditor())
    g.OnEvent("Close", (*) => CloseEditor())
    g.Show()

    FillA() {
        sel := lvA.GetNext(0)
        lvA.Delete()
        for i, a in acts
            lvA.Add(, a.key != "" ? StrUpper(a.key) : "—",
                (a.icon != "" ? a.icon " " : "") ActionName(a),
                a.win ? Loc("ed.result.window", "в окне") : Loc("ed.result.replace", "заменить выделение"),
                (a.lang = "" || a.lang = "auto") ? Loc("act.lang.auto.short", "как в тексте")
                    : LangLabel(a.lang),
                (a.HasProp("model") && a.model != "") ? a.model : Loc("ed.model.default", "по умолчанию"))
        lvA.ModifyCol(1, 60), lvA.ModifyCol(2, 170), lvA.ModifyCol(3, 140)
        lvA.ModifyCol(4, 90), lvA.ModifyCol(5, 95)
        if (sel && sel <= acts.Length)
            lvA.Modify(sel, "Select Focus")
    }

    FillQ() {
        sel := lvQ.GetNext(0)
        lvQ.Delete()
        for q in quicks
            lvQ.Add(, q.label, Snip(q.instr))
        lvQ.ModifyCol(1, 150), lvQ.ModifyCol(2, 400)
        if (sel && sel <= quicks.Length)
            lvQ.Modify(sel, "Select Focus")
    }

    ; row = 0 -> добавление нового
    EditActionRow(row) {
        src := row ? acts[row]
            ; id пуст: он есть только у заводских действий, по нему берётся перевод
            : {id: "", name: "", icon: "", prefix: "", task: "", lang: "auto", ru: true,
               win: false, key: FreeMenuKey(acts), model: ""}
        EditActionDialog(g, src, acts, row, (res) => (row ? acts[row] := res : acts.Push(res), FillA()))
    }

    EditQuickRow(row) {
        src := row ? quicks[row] : {label: "", instr: ""}
        EditQuickDialog(g, src, (res) => (row ? quicks[row] := res : quicks.Push(res), FillQ()))
    }

    FillP() {
        sel := lvP.GetNext(0)
        lvP.Delete()
        for a in appList
            lvP.Add(, a.proc, a.title != "" ? a.title : "—", a.name,
                a.hint != "" ? Snip(a.hint) : Loc("ed.no_hint", "— без поправки —"))
        lvP.ModifyCol(1, 130), lvP.ModifyCol(2, 120), lvP.ModifyCol(3, 130), lvP.ModifyCol(4, 160)
        if (sel && sel <= appList.Length)
            lvP.Modify(sel, "Select Focus")
    }

    EditAppRow(row) {
        src := row ? appList[row] : {proc: "", title: "", name: "", desc: "", hint: ""}
        EditAppDialog(g, src, (res) => (row ? appList[row] := res : appList.Push(res), FillP()))
    }

    DelP(*) {
        row := lvP.GetNext(0)
        if !row
            return
        if (MsgBox(Format(Loc("ed.del_app", "Удалить профиль «{1}»?`nПриложение снова заведётся автоматически при следующем использовании."), appList[row].name),
            "TextTools", "YesNo Icon? 262144") = "Yes") {
            appList.RemoveAt(row)
            FillP()
        }
    }

    DupA(*) {
        row := lvA.GetNext(0)
        if !row
            return
        copy := CloneActions([acts[row]])[1]
        copy.name .= Loc("ed.copy_suffix", " (копия)")
        copy.key := FreeMenuKey(acts)           ; клавиша занята оригиналом
        acts.InsertAt(row + 1, copy)
        FillA()
    }

    DelA(*) {
        row := lvA.GetNext(0)
        if !row
            return
        if (acts.Length = 1) {
            MsgBox(Loc("ed.last_action", "Нельзя удалить последнее действие: меню осталось бы пустым."), "TextTools", "Icon! 262144")
            return
        }
        if (MsgBox(Format(Loc("ed.del_action", "Удалить действие «{1}»?"), acts[row].name), "TextTools", "YesNo Icon? 262144") = "Yes") {
            acts.RemoveAt(row)
            FillA()
        }
    }

    DelQ(*) {
        row := lvQ.GetNext(0)
        if !row
            return
        quicks.RemoveAt(row)
        FillQ()
    }

    DoSaveSets(*) {
        global ACTIONS, QUICK_REFINE, APPS
        try {
            SaveActionSets(acts, quicks)
            SaveApps(appList)
        } catch as e {
            MsgBox(Loc("ed.save_fail", "Не удалось сохранить настройки:") "`n`n" e.Message, "TextTools", "Iconx 262144")
            return
        }
        ACTIONS := acts
        QUICK_REFINE := quicks
        APPS := appList
        CloseEditor()
        TrayTip(Format(Loc("ed.saved", "Сохранено: действий {1}, уточнений {2}, приложений {3}"), ACTIONS.Length, QUICK_REFINE.Length, APPS.Length), "TextTools")
    }

    DoReset(*) {
        if (MsgBox(Loc("ed.reset_ask", "Вернуть заводские наборы кнопок, уточнений и приложений?`nТекущие изменения будут потеряны."), "TextTools", "YesNo Icon! 262144") != "Yes")
            return
        acts := CloneActions(DEFAULT_ACTIONS)
        try ApplyFactoryRoles(acts, cfg.base_url)
        try EnsureModelsForActions(acts)
        quicks := CloneQuick(DEFAULT_QUICK)
        appList := CloneApps(DEFAULT_APPS)
        FillA(), FillQ(), FillP()
    }

    CloseEditor() {
        global gEditorGui
        gEditorGui := ""
        g.Destroy()
    }
}

; Перемещение строки списка на delta позиций (общая для обеих вкладок)
MoveRow(list, lv, delta, refill) {
    row := lv.GetNext(0)
    if (!row || row + delta < 1 || row + delta > list.Length)
        return
    item := list.RemoveAt(row)
    list.InsertAt(row + delta, item)
    refill()
    lv.Modify(row + delta, "Select Focus Vis")
}

; Модальный диалог правки действия. onSave получает готовый объект.
; allActs/selfRow нужны, чтобы поймать клавишу, занятую другим действием.
EditActionDialog(parent, a, allActs, selfRow, onSave) {
    d := Gui("+Owner" parent.Hwnd, a.name = ""
        ? Loc("ed.act.new", "Новое действие")
        : Loc("ed.act.edit", "Действие:") " " ActionName(a))
    d.SetFont("s10", "Segoe UI")
    parent.Opt("+Disabled")

    d.Add("Text", "x10 y12 w110", Loc("ed.field.name", "Название:"))
    eName := d.Add("Edit", "x125 y10 w250", a.name)
    d.Add("Text", "x385 y12 w60", Loc("ed.icon", "Значок:"))
    eIcon := d.Add("Edit", "x445 y10 w60", a.icon)

    ; клавиша выбирается из пула: произвольную нельзя - хоткеи пула
    ; зарегистрированы один раз на старте
    d.Add("Text", "x520 y12 w60", Loc("ed.field.key", "Клавиша:"))
    keyItems := [Loc("ed.key.none", "— нет —")]
    for k in MENU_KEYS
        keyItems.Push(StrUpper(k))
    ddKey := d.Add("DropDownList", "x585 y9 w50", keyItems)
    ddKey.Value := 1
    for i, k in MENU_KEYS
        if (a.HasProp("key") && a.key = k)
            ddKey.Value := i + 1

    d.Add("Text", "x10 y46 w110", Loc("ed.field.result", "Результат:"))
    rbReplace := d.Add("Radio", "x125 y46" (a.win ? "" : " Checked"), Loc("ed.result.replace", "заменить выделение"))
    rbWindow := d.Add("Radio", "x+14 yp" (a.win ? " Checked" : ""), Loc("ed.result.open", "открыть в окне"))

    ; Языки берутся из настроек (галочки), а не зашиты в код. Язык, назначенный
    ; раньше и позже убранный из списка, добавляется обратно - иначе он молча
    ; сменился бы на «как в исходном тексте» при первом же сохранении.
    d.Add("Text", "x10 y78 w110", Loc("act.lang", "Язык ответа:"))
    langCodes := [""]
    langItems := [Loc("act.lang.auto", "как в исходном тексте")]
    for lg in EnabledLangs() {
        langCodes.Push(lg.code)
        langItems.Push(lg.name)
    }
    if (a.lang != "" && a.lang != "auto") {
        known := false
        for c in langCodes
            if (c = a.lang)
                known := true
        if !known {
            langCodes.Push(a.lang)
            langItems.Push(LangLabel(a.lang) " " Loc("act.lang.off", "(нет в списке)"))
        }
    }
    ddLang := d.Add("DropDownList", "x125 y75 w180", langItems)
    ddLang.Value := 1
    for i, c in langCodes
        if (c != "" && c = a.lang)
            ddLang.Value := i
    cbRu := d.Add("CheckBox", "x320 y78" (a.ru ? " Checked" : ""),
        Loc("act.langstyle", "правила языка по умолчанию"))

    ; Модель действия. Первый пункт - следовать настройкам, а не фиксировать имя:
    ; тогда смена модели по умолчанию подхватится всеми обычными действиями.
    curModel := (a.HasProp("model") && a.model != "") ? a.model : ""
    modelItems := [Loc("ed.model.default_of", "по умолчанию") " (" cfg.model ")"]
    modelList := ExtraModels()
    ; модель, заданная раньше и убранная из настроек, не должна молча потеряться
    if (curModel != "") {
        known := false
        for m in modelList
            if (m = curModel)
                known := true
        if !known
            modelList.Push(curModel)
    }
    for m in modelList
        modelItems.Push(m)
    d.Add("Text", "x10 y112 w110", Loc("ed.field.model", "Модель:"))
    ddModel := d.Add("DropDownList", "x125 y109 w250", modelItems)
    ddModel.Value := 1
    for i, m in modelList
        if (m = curModel)
            ddModel.Value := i + 1
    d.SetFont("s9 c707070")
    d.Add("Text", "x385 y113 w250", modelList.Length
        ? Loc("ed.model.hint", "список моделей - в настройках")
        : Loc("ed.model.hint_empty", "добавь модели в настройках, чтобы выбирать"))
    d.SetFont("s10")

    d.Add("Text", "x10 y146 w110", Loc("ed.field.prefix", "Префикс:"))
    ePrefix := d.Add("Edit", "x125 y144 w510", StrReplace(a.prefix, "`n", "\n"))
    d.SetFont("s9 c707070")
    d.Add("Text", "x125 y+2 w510", Loc("ed.prefix.hint", "Уходит перед выделенным текстом. \n - перевод строки."))
    d.SetFont("s10")

    d.Add("Text", "x10 y+10 w620", Loc("ed.field.prompt", "Промпт (что модель должна сделать; пишется на английском):"))
    eTask := d.Add("Edit", "x10 y+4 w625 r14 Multi WantTab", a.task)

    bOk := d.Add("Button", "x10 y+10 w150 Default", Loc("btn.save", "Сохранить"))
    d.Add("Button", "x+8 w120", Loc("btn.cancel", "Отмена")).OnEvent("Click", (*) => Close())
    bOk.OnEvent("Click", Save)
    d.OnEvent("Escape", (*) => Close())
    d.OnEvent("Close", (*) => Close())
    d.Show()

    Save(*) {
        name := Trim(eName.Value), task := Trim(eTask.Value)
        if (name = "") {
            MsgBox(Loc("ed.name_empty", "Название не может быть пустым."), "TextTools", "Icon! 262144")
            return
        }
        if (task = "") {
            MsgBox(Loc("ed.prompt_empty", "Промпт не может быть пустым: без него действие ничего не сделает."),
                "TextTools", "Icon! 262144")
            return
        }
        key := ddKey.Value > 1 ? MENU_KEYS[ddKey.Value - 1] : ""
        occupant := FindMenuKeyOwner(allActs, key, selfRow)
        if (occupant) {
            oldKey := a.HasProp("key") ? a.key : ""
            choice := AskTakenKey(d, key, ActionName(allActs[occupant]), oldKey)
            if (choice = "cancel")
                return
            YieldMenuKey(allActs, occupant, oldKey, choice)
        }
        saved := CloneActions([a])[1]
        saved.name := name
        saved.icon := Trim(eIcon.Value)
        saved.prefix := StrReplace(ePrefix.Value, "\n", "`n")
        saved.task := task
        saved.lang := (ddLang.Value > 1 && ddLang.Value <= langCodes.Length)
            ? langCodes[ddLang.Value] : "auto"
        saved.ru := cbRu.Value ? true : false
        saved.win := rbWindow.Value ? true : false
        saved.key := key
        saved.model := ddModel.Value > 1 ? modelList[ddModel.Value - 1] : ""
        onSave(saved)
        Close()
    }

    Close() {
        parent.Opt("-Disabled")
        d.Destroy()
        try parent.Show()          ; вернуть фокус родителю
    }
}

; Конфликт клавиши: native MsgBox даёт только Да/Нет/Отмена без своих подписей.
; Возвращает "cancel" | "swap" | "take". Swap без своей клавиши недоступен.
AskTakenKey(owner, key, otherName, oldKey) {
    global gTakenKeyHwnd
    choice := "cancel"
    closed := false
    g := Gui("+Owner" owner.Hwnd " -MinimizeBox -MaximizeBox", "TextTools")
    hwnd := g.Hwnd
    gTakenKeyHwnd := hwnd
    g.SetFont("s10", "Segoe UI")
    owner.Opt("+Disabled")

    g.SetFont("s18")
    g.Add("Text", "x16 y14 w36 h36", "⚠")
    g.SetFont("s10", "Segoe UI")
    shown := StrUpper(key)
    body := Format(Loc("ed.key_taken", "Клавиша {1} уже у «{2}»."), shown, otherName)
    body .= "`n" Format(Loc("ed.key_taken.body", "Это действие получит {1}."), shown)
    if (oldKey != "") {
        body .= "`n`n" Format(Loc("ed.key_taken.swap_to", "Обменять: «{1}» получит {2}."), otherName, StrUpper(oldKey))
        body .= "`n" Format(Loc("ed.key_taken.take_to", "Забрать: «{1}» останется без клавиши."), otherName)
    } else {
        body .= "`n`n" Format(Loc("ed.key_taken.take_to", "Забрать: «{1}» останется без клавиши."), otherName)
    }
    g.Add("Text", "x56 y16 w360 h110", body)

    canSwap := oldKey != ""
    g.Add("Button", "x16 y140 w100", Loc("btn.cancel", "Отмена")).OnEvent("Click", (*) => Finish("cancel"))
    bSwap := g.Add("Button", "x+8 w110" (canSwap ? " Default" : ""), Loc("ed.key_conflict.swap", "Обменять"))
    bTake := g.Add("Button", "x+8 w110" (canSwap ? "" : " Default"), Loc("ed.key_conflict.take", "Забрать"))
    if !canSwap
        bSwap.Enabled := false
    bSwap.OnEvent("Click", (*) => Finish("swap"))
    bTake.OnEvent("Click", (*) => Finish("take"))
    g.OnEvent("Escape", (*) => Finish("cancel"))
    g.OnEvent("Close", (*) => Finish("cancel"))
    g.Show()
    WinWaitClose("ahk_id " hwnd)
    gTakenKeyHwnd := 0
    try owner.Opt("-Disabled")
    try owner.Show()
    return choice

    Finish(c) {
        if closed
            return
        closed := true
        if (c = "swap" && oldKey = "")
            c := "cancel"
        choice := c
        try g.Destroy()
    }
}

EditAppDialog(parent, a, onSave) {
    d := Gui("+Owner" parent.Hwnd, a.proc = "" ? Loc("ed.app.new", "Новое приложение") : Loc("ed.app.edit", "Приложение: ") a.name)
    d.SetFont("s10", "Segoe UI")
    parent.Opt("+Disabled")

    d.Add("Text", "x10 y12 w150", Loc("ed.app.proc", "Процесс (exe):"))
    eProc := d.Add("Edit", "x165 y10 w200", a.proc)
    d.Add("Text", "x385 y12 w130", Loc("ed.app.title", "Заголовок содержит:"))
    eTitle := d.Add("Edit", "x520 y10 w200", a.title)
    d.SetFont("s9 c707070")
    d.Add("Text", "x165 y+2 w555", Loc("ed.app.title.hint", "Заголовок необязателен. Он нужен, чтобы отличать площадки внутри одного приложения (Jira и Confluence в браузере); записи с заголовком проверяются первыми."))
    d.SetFont("s10")

    d.Add("Text", "x10 y+12 w150", Loc("ed.field.name", "Название:"))
    eName := d.Add("Edit", "x165 yp-2 w350", a.name)

    d.Add("Text", "x10 y+12 w150", Loc("ed.app.desc", "Описание (для себя):"))
    eDesc := d.Add("Edit", "x165 yp-2 w555", a.desc)

    d.Add("Text", "x10 y+14 w710", Loc("ed.app.hint", "Поправка к промпту (1-2 предложения на английском; пусто — обрабатывать как обычно):"))
    eHint := d.Add("Edit", "x10 y+4 w710 r5 Multi WantTab", a.hint)

    d.Add("Button", "x10 y+10 w150 Default", Loc("btn.save", "Сохранить")).OnEvent("Click", Save)
    d.Add("Button", "x+8 w120", Loc("btn.cancel", "Отмена")).OnEvent("Click", (*) => Close())
    d.OnEvent("Escape", (*) => Close())
    d.OnEvent("Close", (*) => Close())
    d.Show()

    Save(*) {
        proc := Trim(eProc.Value)
        if (proc = "") {
            MsgBox(Loc("ed.app.proc_req", "Имя процесса обязательно: по нему приложение и определяется."),
                "TextTools", "Icon! 262144")
            return
        }
        name := Trim(eName.Value)
        onSave({proc: proc, title: Trim(eTitle.Value), name: name != "" ? name : proc,
            desc: Trim(eDesc.Value), hint: Trim(eHint.Value)})
        Close()
    }

    Close() {
        parent.Opt("-Disabled")
        d.Destroy()
        try parent.Show()
    }
}

EditQuickDialog(parent, q, onSave) {
    d := Gui("+Owner" parent.Hwnd, q.label = "" ? Loc("ed.quick.new", "Новое уточнение") : Loc("ed.quick.edit", "Уточнение: ") q.label)
    d.SetFont("s10", "Segoe UI")
    parent.Opt("+Disabled")

    d.Add("Text", "x10 y12 w110", Loc("ed.field.name", "Название:"))
    eLabel := d.Add("Edit", "x125 y10 w250", q.label)
    d.SetFont("s9 c707070")
    d.Add("Text", "x125 y+2 w400", Loc("ed.quick.name.hint", "Короткое - это подпись на кнопке в окне результата."))
    d.SetFont("s10")

    d.Add("Text", "x10 y+12 w620", Loc("ed.quick.instr", "Инструкция модели (на английском):"))
    eInstr := d.Add("Edit", "x10 y+4 w520 r8 Multi WantTab", q.instr)

    d.Add("Button", "x10 y+10 w150 Default", Loc("btn.save", "Сохранить")).OnEvent("Click", Save)
    d.Add("Button", "x+8 w120", Loc("btn.cancel", "Отмена")).OnEvent("Click", (*) => Close())
    d.OnEvent("Escape", (*) => Close())
    d.OnEvent("Close", (*) => Close())
    d.Show()

    Save(*) {
        label := Trim(eLabel.Value), instr := Trim(eInstr.Value)
        if (label = "" || instr = "") {
            MsgBox(Loc("ed.quick.required", "Название и инструкция должны быть заполнены."), "TextTools", "Icon! 262144")
            return
        }
        onSave({label: label, instr: instr})
        Close()
    }

    Close() {
        parent.Opt("-Disabled")
        d.Destroy()
        try parent.Show()
    }
}
