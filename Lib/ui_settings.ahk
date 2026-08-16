; TextTools - Окно настроек поверх TextTools.ini и langs.ini.
; Вкладки собраны в ui_settings_api.ahk / _langs.ahk / _ui.ahk.
; Подключается из TextTools.ahk, отдельно не запускается.

ShowSettings(*) {
    global gSettingsGui, ACTIONS
    if gSettingsGui {
        try gSettingsGui.Destroy()
        gSettingsGui := ""
    }
    sg := Gui("+AlwaysOnTop", "TextTools - " Loc("set.title", "настройки"))
    gSettingsGui := sg
    sg.SetFont("s10", "Segoe UI")
    PricesRefresh()
    modelRows := CollectModelRows()
    starred := cfg.model
    sortCol := 0, sortDesc := false
    langRows := CloneLangs(LANGS)

    tab := sg.Add("Tab3", "x10 y10 w620 h470", [Loc("set.tab.api", "API"), Loc("set.tab.langs", "Языки ответа"), Loc("set.tab.ui", "Интерфейс")])
    tab.UseTab(1)
    api := SettingsApiTab(sg)
    tab.UseTab(2)
    langTab := SettingsLangsTab(sg)
    tab.UseTab(3)
    ui := SettingsUiTab(sg)
    tab.UseTab()
    bSave := sg.Add("Button", "x10 y490 w160 Default", Loc("btn.save", "Сохранить"))
    sg.Add("Button", "x+8 w120", Loc("btn.cancel", "Отмена")).OnEvent("Click", (*) => sg.Destroy())

    eUrl := api.eUrl, eKey := api.eKey, cbProxy := api.cbProxy, eProxy := api.eProxy, eModel := api.eModel, lvM := api.lvM
    lvL := langTab.lvL
    ddUi := ui.ddUi, uiLangs := ui.uiLangs, eHk := ui.eHk, eHkR := ui.eHkR
    cbMd := ui.cbMd, cbProf := ui.cbProf, eClip := ui.eClip

    FillModels()
    FillLangs()
    cbProxy.OnEvent("Click", (*) => (eProxy.Enabled := !!cbProxy.Value))
    bSave.OnEvent("Click", DoSave)
    api.bTest.OnEvent("Click", DoTest)
    api.bLoad.OnEvent("Click", DoLoadModels)
    api.bRecommend.OnEvent("Click", DoRecommend)
    api.bDefault.OnEvent("Click", DoMakeDefault)
    api.bRetarget.OnEvent("Click", DoRetarget)
    lvM.OnEvent("DoubleClick", DoMakeDefault)
    lvM.OnEvent("ColClick", OnColClick)
    eModel.OnEvent("Change", OnDefaultTyped)
    langTab.bAddL.OnEvent("Click", DoAddLang)
    langTab.bDelL.OnEvent("Click", DoDelLang)
    sg.OnEvent("Escape", (*) => sg.Destroy())
    sg.OnEvent("Close", (*) => sg.Destroy())
    sg.Show()

    FillModels() {
        lvM.Delete()
        cur := Trim(eModel.Value)
        for m in modelRows {
            lvM.Add(m.on ? "Check" : "", (m.id = cur ? "★ " : "") m.id,
                m.ctx > 0 ? NumFmt(m.ctx) : "—",
                m.pin >= 0 ? MoneyFmt(m.pin) : "—",
                m.pout >= 0 ? MoneyFmt(m.pout) : "—")
        }
    }

    OnColClick(ctrl, info) {
        col := info
        if (col < 1 || col > 4)
            return
        sortDesc := (col = sortCol) ? !sortDesc : false
        sortCol := col
        SyncChecks()
        SortBy(modelRows, (a, b) => ModelLessBy(a, b, sortCol, sortDesc))
        FillModels()
    }

    DoRecommend(*) {
        SyncChecks()
        url := Trim(eUrl.Value)
        rec := RecommendedFor(url)
        if !rec.Length {
            MsgBox(Loc("set.rec_unknown", "Провайдер по адресу не опознан, рекомендовать нечего.")
                . "`n`n" url, "TextTools", "Icon! 262144")
            return
        }
        lines := "", missing := ""
        for r in rec {
            found := false
            for m in modelRows {
                if (m.id = r.id) {
                    m.on := true
                    found := true
                }
            }
            if !found {
                modelRows.Push(EnrichModel({id: r.id, on: true}))
                missing .= "`n  • " r.id
            }
            lines .= "`n  • " r.id " — " r.role
        }
        SortModelRows()
        FillModels()
        msg := Format(Loc("set.rec_done", "Отмечено моделей для {1}: {2}."),
            ProviderName(ProviderOf(url)), rec.Length) "`n" lines
        if (missing != "")
            msg .= "`n`n" Loc("set.rec_missing", "Этих не было в списке — они добавлены, но у провайдера не проверены:") missing
        MsgBox(msg, "TextTools", "Iconi 262144")
    }

    DoRetarget(*) {
        global ACTIONS
        url := Trim(eUrl.Value)
        ApplyFactoryRoles(ACTIONS, url)
        EnsureModelsForActions(ACTIONS)
        try SaveActionSets(ACTIONS, QUICK_REFINE)
        SyncChecks()
        for a in ACTIONS {
            if (a.HasProp("model") && a.model != "") {
                found := false
                for m in modelRows
                    if (m.id = a.model)
                        found := true, m.on := true
                if !found
                    modelRows.Push(EnrichModel({id: a.model, on: true}))
            }
        }
        SortModelRows()
        FillModels()
        MsgBox(Loc("set.retarget.done", "Заводские действия получили модели выбранного провайдера. Имена в actions.ini обновлены."),
            "TextTools", "Iconi 262144")
    }

    DoMakeDefault(*) {
        row := lvM.GetNext(0)
        if !row {
            Flash(Loc("set.pick_row", "Сначала выберите модель в списке"))
            return
        }
        SyncChecks()
        if (row > modelRows.Length)
            return
        eModel.Value := modelRows[row].id
        starred := modelRows[row].id
        FillModels()
        lvM.Modify(row, "Select Focus")
        Flash(Loc("set.default_set", "Модель по умолчанию: ") modelRows[row].id)
    }

    OnDefaultTyped(*) {
        cur := Trim(eModel.Value)
        if (cur = starred)
            return
        starred := cur
        SyncChecks()
        FillModels()
    }

    FillLangs() {
        lvL.Delete()
        for lg in langRows
            lvL.Add(lg.on ? "Check" : "", lg.name, lg.code, lg.prompt)
    }

    SyncChecks() {
        loop modelRows.Length
            modelRows[A_Index].on := lvM.GetNext(A_Index - 1, "Checked") = A_Index
        loop langRows.Length
            langRows[A_Index].on := lvL.GetNext(A_Index - 1, "Checked") = A_Index
    }

    DoLoadModels(*) {
        global gBusy
        if gBusy {
            Flash(Loc("msg.busy", "Запрос уже выполняется"))
            return
        }
        SyncChecks()
        tmp := {base_url: Trim(eUrl.Value), api_key: Trim(eKey.Value),
            proxy: Trim(eProxy.Value), proxy_on: cbProxy.Value ? "1" : "0",
            model: Trim(eModel.Value)}
        try {
            if (tmp.proxy_on = "1")
                tmp.proxy := NormalizeProxy(tmp.proxy)
        } catch as e {
            MsgBox(Loc("set.proxy.bad", "Некорректный прокси:") "`n" e.Message, "TextTools", "Iconx 262144")
            return
        }
        gBusy := true
        BusyShow("⏳ " Loc("set.loading_models", "запрашиваю список моделей…"))
        ids := "", err := ""
        try ids := FetchModelIds(tmp)
        catch as e
            err := e.Message
        finally
            gBusy := false
        BusyHide()
        if err {
            MsgBox(Loc("set.models_failed", "Не удалось получить список моделей:") "`n`n" err, "TextTools", "Iconx 262144")
            return
        }
        added := 0
        for id in ids {
            known := false
            for m in modelRows
                if (m.id = id)
                    known := true
            if !known {
                modelRows.Push(EnrichModel({id: id, on: false}))
                added++
            }
        }
        dropped := 0
        i := modelRows.Length
        while (i >= 1) {
            still := false
            for id in ids
                if (modelRows[i].id = id)
                    still := true
            if (!still && !modelRows[i].on) {
                modelRows.RemoveAt(i)
                dropped++
            }
            i--
        }
        SortModelRows()
        FillModels()
        msg := Loc("set.models_loaded", "У провайдера моделей: {1}. Новых: {2}, убрано исчезнувших: {3}.")
        MsgBox(Format(msg, ids.Length, added, dropped) "`n`n"
            . Loc("set.models_check", "Отметьте галочками те, которые нужны действиям."),
            "TextTools", "Iconi 262144")
    }

    SortModelRows() {
        SortBy(modelRows, ModelLess)
    }

    DoAddLang(*) {
        SyncChecks()
        ib := InputBox(Loc("set.lang_code", "Код языка (например de, pt-BR):"), Loc("set.lang_new", "TextTools - новый язык"), "w420 h130")
        if (ib.Result != "OK")
            return
        code := Trim(ib.Value)
        if (code = "")
            return
        for lg in langRows {
            if (lg.code = code) {
                MsgBox(Loc("set.lang_dup", "Язык с таким кодом уже есть."), "TextTools", "Icon! 262144")
                return
            }
        }
        ib2 := InputBox(Loc("set.lang_label", "Как подписать в интерфейсе:"), Loc("set.lang_new", "TextTools - новый язык"), "w420 h130", code)
        if (ib2.Result != "OK")
            return
        name := Trim(ib2.Value) != "" ? Trim(ib2.Value) : code
        ib3 := InputBox(Loc("set.lang_prompt", "Как язык называется по-английски (уйдёт в промпт модели):"),
            Loc("set.lang_new", "TextTools - новый язык"), "w420 h130", name)
        if (ib3.Result != "OK")
            return
        prompt := Trim(ib3.Value) != "" ? Trim(ib3.Value) : name
        langRows.Push({code: code, name: name, prompt: prompt, on: true})
        FillLangs()
    }

    DoDelLang(*) {
        SyncChecks()
        row := lvL.GetNext(0)
        if !row
            return
        if (langRows.Length <= 1) {
            MsgBox(Loc("set.lang_last", "Нельзя удалить последний язык."), "TextTools", "Icon! 262144")
            return
        }
        langRows.RemoveAt(row)
        FillLangs()
    }

    DoSave(*) {
        global LANGS
        SyncChecks()
        newHk := Trim(eHk.Value)
        newHkR := Trim(eHkR.Value)
        if (newHk = "") {
            MsgBox(Loc("set.hotkey_empty", "Хоткей меню не может быть пустым"), "TextTools", "Iconx 262144")
            return
        }
        if (newHkR != "" && newHkR = newHk) {
            MsgBox(Loc("set.hotkey_same", "Хоткей меню и хоткей повтора должны отличаться"), "TextTools", "Iconx 262144")
            return
        }
        try {
            ApplyHotkeyPair(newHk, newHkR)
        } catch as e {
            MsgBox(Loc("set.hotkey_bad", "Некорректный хоткей:") "`n" e.Message, "TextTools", "Iconx 262144")
            try ApplyHotkeyPair(cfg.hotkey, cfg.hotkey_repeat)
            return
        }
        picked := []
        for m in modelRows
            if m.on
                picked.Push(m.id)
        all := []
        for m in modelRows
            all.Push(m.id)
        cfg.base_url := Trim(eUrl.Value)
        cfg.api_key := Trim(eKey.Value)
        cfg.proxy_on := cbProxy.Value ? "1" : "0"
        try cfg.proxy := (cfg.proxy_on = "1")
            ? NormalizeProxy(Trim(eProxy.Value))
            : Trim(eProxy.Value)
        catch as e {
            MsgBox(Loc("set.proxy.bad", "Некорректный прокси:") "`n" e.Message, "TextTools", "Iconx 262144")
            return
        }
        cfg.model := Trim(eModel.Value)
        cfg.models := JoinList(picked)
        cfg.models_known := JoinList(all)
        cfg.hotkey := newHk
        cfg.hotkey_repeat := newHkR
        cfg.markdown := cbMd.Value ? "1" : "0"
        cfg.app_profiling := cbProf.Value ? "1" : "0"
        cw := Trim(eClip.Value)
        cfg.clip_wait := (cw != "" ? cw : "0.8")
        uiOld := cfg.ui_lang
        if (ddUi.Value && ddUi.Value <= uiLangs.Length)
            cfg.ui_lang := uiLangs[ddUi.Value].code
        SaveCfg(cfg)
        LANGS := langRows
        SaveLangs(LANGS)
        A_IconTip := Format(Loc("tray.tip", "TextTools - обработка текста через LLM ({1})"), cfg.hotkey)
        TT_Log("настройки сохранены, хоткей " newHk ", модель " cfg.model
            . ", моделей в списке " picked.Length ", языков включено " CountOn(langRows))
        sg.Destroy()
        if (cfg.ui_lang != uiOld) {
            TT_Log("сменён язык интерфейса: " uiOld " -> " cfg.ui_lang ", перезапуск")
            Reload()
            return
        }
        TrayTip(Format(Loc("set.saved", "Сохранено. Хоткей меню: {1}"), newHk), "TextTools")
    }

    DoTest(*) {
        global gBusy, gCallLabel
        if gBusy {
            Flash(Loc("msg.busy", "Запрос уже выполняется"))
            return
        }
        tmp := {base_url: Trim(eUrl.Value), api_key: Trim(eKey.Value),
            proxy: Trim(eProxy.Value), proxy_on: cbProxy.Value ? "1" : "0",
            model: Trim(eModel.Value)}
        try {
            if (tmp.proxy_on = "1")
                tmp.proxy := NormalizeProxy(tmp.proxy)
        } catch as e {
            MsgBox(Loc("set.proxy.bad", "Некорректный прокси:") "`n" e.Message, "TextTools", "Iconx 262144")
            return
        }
        gCallLabel := "тест API"
        gBusy := true
        BusyShow("⏳ " Loc("set.testing", "проверяю подключение…"))
        r := "", err := ""
        try r := CallApiCfg(tmp, "You are a connectivity test.", "Reply with exactly one word: OK")
        catch as e
            err := e.Message
        finally
            gBusy := false
        BusyHide()
        if err
            MsgBox(Loc("set.test_fail", "Ошибка:") "`n`n" err, "TextTools", "Iconx 262144")
        else
            MsgBox(Loc("set.test_ok", "Соединение работает. Ответ модели:") "`n`n" r, "TextTools", "Iconi 262144")
    }
}

CollectModelRows() {
    rows := [], seen := Map()
    Add(id, on) {
        id := Trim(id)
        if (id = "" || seen.Has(id))
            return
        seen[id] := true
        rows.Push(EnrichModel({id: id, on: on}))
    }
    for id in SplitList(cfg.models)
        Add(id, true)
    for id in SplitList(cfg.models_known)
        Add(id, false)
    SortBy(rows, ModelLess)
    return rows
}

EnrichModel(m) {
    slug := PriceSlug(m.id, cfg.base_url)
    pr := PriceOf(slug)
    m.ctx := ContextOf(slug)
    m.pin := IsObject(pr) ? pr.prompt * 1000000 : -1
    m.pout := IsObject(pr) ? pr.completion * 1000000 : -1
    return m
}

ModelLess(a, b) {
    if (a.on != b.on)
        return a.on
    return StrCompare(a.id, b.id) < 0
}

ModelLessBy(a, b, col, desc) {
    if (col = 1)
        return desc ? StrCompare(a.id, b.id) > 0 : StrCompare(a.id, b.id) < 0
    av := col = 2 ? a.ctx : col = 3 ? a.pin : a.pout
    bv := col = 2 ? b.ctx : col = 3 ? b.pin : b.pout
    aKnown := av > 0, bKnown := bv > 0
    if (aKnown != bKnown)
        return aKnown
    if (!aKnown)
        return StrCompare(a.id, b.id) < 0
    if (av = bv)
        return StrCompare(a.id, b.id) < 0
    return desc ? (av > bv) : (av < bv)
}

CountOn(list) {
    n := 0
    for x in list
        if x.on
            n++
    return n
}
