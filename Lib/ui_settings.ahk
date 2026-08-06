; TextTools - Окно настроек поверх TextTools.ini и langs.ini.
; Подключается из TextTools.ahk, отдельно не запускается.
;
; Три вкладки: API (адрес, ключ, модели), Языки ответа, Интерфейс. Списки
; моделей и языков - ListView с галочками, а не строка через запятую: набор
; правится мышью, пополняется из провайдера и переживёт будущие настройки,
; где выбор из многих значений понадобится снова.

ShowSettings(*) {
    global gSettingsGui
    if gSettingsGui {
        try gSettingsGui.Destroy()
        gSettingsGui := ""
    }
    sg := Gui("+AlwaysOnTop", "TextTools - " Loc("set.title", "настройки"))
    gSettingsGui := sg
    sg.SetFont("s10", "Segoe UI")

    ; каталог цен нужен для колонок контекста и стоимости; тянется не чаще
    ; раза в сутки, поэтому на открытие окна это обычно чтение файла
    PricesRefresh()
    ; правим копии: отмена не должна оставлять следов
    modelRows := CollectModelRows()                  ; [{id, on}]
    starred := cfg.model                             ; какая строка помечена звёздочкой
    sortCol := 0, sortDesc := false                  ; сортировка кликом по заголовку
    langRows := CloneLangs(LANGS)

    tab := sg.Add("Tab3", "x10 y10 w620 h420", [Loc("set.tab.api", "API"), Loc("set.tab.langs", "Языки ответа"), Loc("set.tab.ui", "Интерфейс")])

    ; --- вкладка API ---
    ; Координаты явные, а не накопительные (y+N): при накоплении кнопки под
    ; списком уехали за нижний край вкладки и пользователь их просто не видел -
    ; загрузка моделей у провайдера выглядела как отсутствующая функция.
    ; Высота полей задана явно (h24). Без неё AHK подбирает её под текст, и
    ; длинный ключ превращал поле в трёхстрочное с полосой прокрутки - оно
    ; наезжало на подпись следующего поля.
    tab.UseTab(1)
    sg.Add("Text", "x25 y44 w590", Loc("set.baseurl", "Base URL (OpenAI-совместимый API):"))
    eUrl := sg.Add("Edit", "x25 y64 w590 h24 -Multi", cfg.base_url)
    sg.Add("Text", "x25 y96 w590", "API key:")
    eKey := sg.Add("Edit", "x25 y116 w590 h24 -Multi Password", cfg.api_key)
    sg.Add("Text", "x25 y148 w590", Loc("set.model", "Модель по умолчанию:"))
    eModel := sg.Add("Edit", "x25 y168 w590 h24 -Multi", cfg.model)
    sg.Add("Text", "x25 y200 w590",
        Loc("set.models", "Модели, доступные действиям (отмеченные видны в карточке действия):"))
    lvM := sg.Add("ListView", "x25 y220 w590 h118 Checked -Multi",
        [Loc("set.col.model", "Модель"), Loc("set.col.ctx", "Контекст"),
         Loc("set.col.in", "$/1M вх"), Loc("set.col.out", "$/1M вых")])
    lvM.ModifyCol(1, 300), lvM.ModifyCol(2, 90), lvM.ModifyCol(3, 80), lvM.ModifyCol(4, 85)
    bLoad := sg.Add("Button", "x25 y344 w200 h28", Loc("set.load", "Загрузить у провайдера"))
    bRecommend := sg.Add("Button", "x233 y344 w210 h28",
        Loc("set.recommend", "Рекомендуемые модели"))
    bTest := sg.Add("Button", "x451 y344 w164 h28", Loc("set.test", "Тест API"))
    bDefault := sg.Add("Button", "x25 y376 w240 h28",
        Loc("set.make_default", "Сделать моделью по умолчанию"))
    sg.SetFont("s9 c707070")
    sg.Add("Text", "x25 y408 w590",
        Loc("set.models.hint", "Контекст и цены — каталог OpenRouter. Двойной клик по строке тоже делает модель основной."))
    sg.SetFont("s10")

    ; --- вкладка языков ---
    tab.UseTab(2)
    sg.Add("Text", "x25 y44 w590",
        Loc("set.langs.hint", "Отмеченные языки попадают в список «Язык ответа» карточки действия."))
    lvL := sg.Add("ListView", "x25 y70 w590 h296 Checked -Multi",
        [Loc("set.col.lang", "Язык"), Loc("set.col.code", "Код"), Loc("set.col.prompt", "Как называется в промпте")])
    lvL.ModifyCol(1, 200), lvL.ModifyCol(2, 80), lvL.ModifyCol(3, 280)
    bAddL := sg.Add("Button", "x25 y374 w210 h28", Loc("set.add_own", "Добавить свой…"))
    bDelL := sg.Add("Button", "x243 y374 w150 h28", Loc("btn.delete", "Удалить"))

    ; --- вкладка интерфейса ---
    tab.UseTab(3)
    sg.Add("Text", "x25 y44", Loc("set.ui_lang", "Язык интерфейса:"))
    uiLangs := UiLangPacks()
    uiItems := []
    for p in uiLangs
        uiItems.Push(p.name " (" p.code ")")
    ddUi := sg.Add("DropDownList", "x25 y64 w280", uiItems)
    for i, p in uiLangs
        if (p.code = cfg.ui_lang)
            ddUi.Value := i
    if (!ddUi.Value && uiItems.Length)
        ddUi.Value := 1
    sg.SetFont("s9 c707070")
    ; ключ и текст держим в одной строке: сборщик lang/en.ini разбирает Loc(...)
    ; построчно и склейку через "." не увидит
    sg.Add("Text", "x25 y92 w590", Loc("set.ui_lang.hint", "Пакеты — в папке lang рядом со скриптом. Свой язык: скопировать en.ini, перевести значения, перезапустить."))
    sg.SetFont("s10")
    sg.Add("Text", "x25 y134", Loc("set.hotkey", "Хоткей меню (^ = Ctrl, ! = Alt, + = Shift, # = Win):"))
    eHk := sg.Add("Edit", "x25 y154 w280", cfg.hotkey)
    sg.Add("Text", "x25 y186", Loc("set.hotkey_repeat", "Хоткей повтора последнего действия:"))
    eHkR := sg.Add("Edit", "x25 y206 w280", cfg.hotkey_repeat)
    cbMd := sg.Add("CheckBox", "x25 y246 w590" (cfg.markdown ? " Checked" : ""),
        Loc("set.markdown", "Рендер Markdown в окне результата (снять, если окна роняют скрипт)"))
    sg.SetFont("s9 c707070")
    sg.Add("Text", "x25 y286 w590",
        "OpenRouter: https://openrouter.ai/api/v1     Ollama: http://localhost:11434/v1 (key: ollama)")
    sg.SetFont("s10")

    tab.UseTab()
    bSave := sg.Add("Button", "x10 y440 w160 Default", Loc("btn.save", "Сохранить"))
    sg.Add("Button", "x+8 w120", Loc("btn.cancel", "Отмена")).OnEvent("Click", (*) => sg.Destroy())

    FillModels()
    FillLangs()
    bSave.OnEvent("Click", DoSave)
    bTest.OnEvent("Click", DoTest)
    bLoad.OnEvent("Click", DoLoadModels)
    bRecommend.OnEvent("Click", DoRecommend)
    ; кнопка и двойной клик делают одно и то же: кнопка видна, клик быстрее
    bDefault.OnEvent("Click", DoMakeDefault)
    lvM.OnEvent("DoubleClick", DoMakeDefault)
    lvM.OnEvent("ColClick", OnColClick)
    ; звёздочка должна следовать за полем, если модель вписали руками
    eModel.OnEvent("Change", OnDefaultTyped)
    bAddL.OnEvent("Click", DoAddLang)
    bDelL.OnEvent("Click", DoDelLang)
    sg.OnEvent("Escape", (*) => sg.Destroy())
    sg.OnEvent("Close", (*) => sg.Destroy())
    sg.Show()

    ; --- списки ---

    ; Контекст и цены берутся из каталога цен, который и так качается ради
    ; статистики. Модели, которой в каталоге нет (локальная, свежая, через свой
    ; прокси), показываются с прочерками - это честнее пустых ячеек.
    FillModels() {
        lvM.Delete()
        cur := Trim(eModel.Value)
        for m in modelRows {
            ; звёздочка у модели по умолчанию: иначе среди полусотни строк
            ; непонятно, какая из них сейчас основная
            lvM.Add(m.on ? "Check" : "", (m.id = cur ? "★ " : "") m.id,
                m.ctx > 0 ? NumFmt(m.ctx) : "—",
                m.pin >= 0 ? MoneyFmt(m.pin) : "—",
                m.pout >= 0 ? MoneyFmt(m.pout) : "—")
        }
    }

    ; Клик по заголовку сортирует по этой колонке; повторный клик разворачивает.
    ; Группировка "отмеченные наверх" тут сознательно не действует: кликая по
    ; цене, ищут самую дешёвую вообще, а не самую дешёвую среди отмеченных.
    OnColClick(ctrl, info) {
        col := info
        if (col < 1 || col > 4)
            return
        sortDesc := (col = sortCol) ? !sortDesc : false
        sortCol := col
        SyncChecks()
        i := 2
        while (i <= modelRows.Length) {
            j := i
            while (j > 1 && ModelLessBy(modelRows[j], modelRows[j - 1], sortCol, sortDesc)) {
                t := modelRows[j], modelRows[j] := modelRows[j - 1], modelRows[j - 1] := t
                j--
            }
            i++
        }
        FillModels()
    }

    ; Отмечает четыре модели под роли этого инструмента. Провайдер берётся из
    ; поля адреса, а не из сохранённого конфига: пользователь мог только что
    ; вписать другой адрес и ещё не нажал «Сохранить».
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
            ; Модели нет в списке: либо список не загружали, либо у провайдера
            ; её действительно нет. Добавляем и говорим об этом прямо - молча
            ; проглотить значило бы оставить в наборе несуществующее имя.
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
            msg .= "`n`n" Loc("set.rec_missing",
                "Этих не было в списке — они добавлены, но у провайдера не проверены:") missing
        MsgBox(msg, "TextTools", "Iconi 262144")
    }

    ; Выделенная строка становится моделью по умолчанию
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

    ; Перерисовываем список, только когда звёздочка реально переезжает:
    ; на каждое нажатие клавиши перерисовывать полсотни строк незачем
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

    ; галочки читаем из ListView в момент сохранения: следить за каждым кликом
    ; через ItemCheck незачем, состояние и так лежит в контроле
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
        tmp := {base_url: Trim(eUrl.Value), api_key: Trim(eKey.Value), model: Trim(eModel.Value)}
        gBusy := true
        BusyShow("⏳ " Loc("set.loading_models", "запрашиваю список моделей…"))
        ids := "", err := ""
        try ids := FetchModelIds(tmp)
        catch as e
            err := e.Message
        BusyHide()
        gBusy := false
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
        ; Список пришёл от провайдера целиком, поэтому модели, которых там уже
        ; нет, убираем - кроме отмеченных: их держит какое-то действие, и
        ; молча выкинуть их значит незаметно сменить модель у кнопки.
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
        ; Loc() держим в одной строке: сборщик пакета разбирает файлы построчно
        ; и склейку через "." не увидит - ключ молча выпал бы из перевода
        msg := Loc("set.models_loaded", "У провайдера моделей: {1}. Новых: {2}, убрано исчезнувших: {3}.")
        MsgBox(Format(msg, ids.Length, added, dropped) "`n`n"
            . Loc("set.models_check", "Отметьте галочками те, которые нужны действиям."),
            "TextTools", "Iconi 262144")
    }

    ; Отмеченные наверх, дальше по алфавиту. Пересортировка только при открытии
    ; окна и после загрузки списка: если бы строка прыгала в момент клика по
    ; галочке, отметить несколько моделей подряд было бы невозможно.
    SortModelRows() {
        i := 2
        while (i <= modelRows.Length) {
            j := i
            while (j > 1 && ModelLess(modelRows[j], modelRows[j - 1])) {
                tmp2 := modelRows[j], modelRows[j] := modelRows[j - 1], modelRows[j - 1] := tmp2
                j--
            }
            i++
        }
    }

    DoAddLang(*) {
        SyncChecks()
        ib := InputBox("Код языка (например de, pt-BR):", "TextTools - новый язык", "w420 h130")
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
        ib2 := InputBox("Как подписать в интерфейсе:", "TextTools - новый язык", "w420 h130", code)
        if (ib2.Result != "OK")
            return
        name := Trim(ib2.Value) != "" ? Trim(ib2.Value) : code
        ib3 := InputBox("Как язык называется по-английски (уйдёт в промпт модели):",
            "TextTools - новый язык", "w420 h130", name)
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
            try ApplyHotkeyPair(cfg.hotkey, cfg.hotkey_repeat)   ; откат к рабочей паре
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
        cfg.model := Trim(eModel.Value)
        cfg.models := JoinList(picked)
        cfg.models_known := JoinList(all)      ; чтобы не ходить к провайдеру каждый раз
        cfg.hotkey := newHk
        cfg.hotkey_repeat := newHkR
        cfg.markdown := cbMd.Value ? "1" : "0"
        uiOld := cfg.ui_lang
        if (ddUi.Value && ddUi.Value <= uiLangs.Length)
            cfg.ui_lang := uiLangs[ddUi.Value].code
        SaveCfg(cfg)
        LANGS := langRows
        SaveLangs(LANGS)
        A_IconTip := "TextTools - обработка текста через LLM (" cfg.hotkey ")"
        TT_Log("настройки сохранены, хоткей " newHk ", модель " cfg.model
            . ", моделей в списке " picked.Length ", языков включено " CountOn(langRows))
        sg.Destroy()
        if (cfg.ui_lang != uiOld) {
            TT_Log("сменён язык интерфейса: " uiOld " -> " cfg.ui_lang ", перезапуск")
            Reload()
            return
        }
        TrayTip("Сохранено. Хоткей меню: " newHk, "TextTools")
    }

    DoTest(*) {
        global gBusy, gCallLabel
        if gBusy {
            Flash(Loc("msg.busy", "Запрос уже выполняется"))
            return
        }
        tmp := {base_url: Trim(eUrl.Value), api_key: Trim(eKey.Value), model: Trim(eModel.Value)}
        gCallLabel := "тест API"
        gBusy := true
        BusyShow("⏳ проверяю подключение…")
        r := "", err := ""
        try r := CallApiCfg(tmp, "You are a connectivity test.", "Reply with exactly one word: OK")
        catch as e
            err := e.Message
        BusyHide()
        gBusy := false
        if err
            MsgBox("Ошибка:`n`n" err, "TextTools", "Iconx 262144")
        else
            MsgBox("Соединение работает. Ответ модели:`n`n" r, "TextTools", "Iconi 262144")
    }
}

; ИМЯ НЕ ДОЛЖНО СОВПАДАТЬ с переменной modelRows: имена в AHK
; регистронезависимы, и локальная modelRows затеняет эту функцию (§5 п. 39).
; Строки списка моделей: отмеченные (cfg.models) плюс всё, что когда-то
; загружали у провайдера (cfg.models_known) - иначе после сохранения список
; схлопывался бы до отмеченных и выбирать было бы не из чего
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
    ; отмеченные наверх - при открытии окна, а не в момент клика по галочке
    i := 2
    while (i <= rows.Length) {
        j := i
        while (j > 1 && ModelLess(rows[j], rows[j - 1])) {
            tmp := rows[j], rows[j] := rows[j - 1], rows[j - 1] := tmp
            j--
        }
        i++
    }
    return rows
}

; Числа из каталога кладём в саму строку: по ним потом сортируем. Сортировать
; по тому, что показано, нельзя - "$10.00" текстом встаёт раньше "$2.00", а
; "1 047 576" с разделителями не число вообще.
EnrichModel(m) {
    slug := PriceSlug(m.id, cfg.base_url)
    pr := PriceOf(slug)
    m.ctx := ContextOf(slug)                          ; 0 = неизвестен
    m.pin := IsObject(pr) ? pr.prompt * 1000000 : -1  ; -1 = цены нет в каталоге
    m.pout := IsObject(pr) ? pr.completion * 1000000 : -1
    return m
}

; Порядок строк списка моделей: сначала отмеченные, внутри группы - по алфавиту
ModelLess(a, b) {
    if (a.on != b.on)
        return a.on
    return StrCompare(a.id, b.id) < 0
}

; Сравнение по колонке для сортировки кликом по заголовку.
; Строки без данных каталога всегда внизу - и по возрастанию, и по убыванию:
; иначе клик по "$/1M вх" забивал бы верх таблицы прочерками.
ModelLessBy(a, b, col, desc) {
    if (col = 1)
        return desc ? StrCompare(a.id, b.id) > 0 : StrCompare(a.id, b.id) < 0
    av := col = 2 ? a.ctx : col = 3 ? a.pin : a.pout
    bv := col = 2 ? b.ctx : col = 3 ? b.pin : b.pout
    aKnown := av > 0, bKnown := bv > 0
    if (aKnown != bKnown)
        return aKnown
    if (!aKnown)
        return StrCompare(a.id, b.id) < 0             ; among unknowns - по имени
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
