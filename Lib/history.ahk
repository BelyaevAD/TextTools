; TextTools - История результатов, последний результат и статистика использования по логу.
; Подключается из TextTools.ahk, отдельно не запускается.

; Последний результат всегда дублируется в файл: если процесс упадет
; вместе с окном результата, текст можно достать через меню в трее
SaveLastResult(t) {
    lastF := A_ScriptDir "\TextTools_last.txt"
    try FileDelete(lastF)
    try FileAppend(t, lastF, "UTF-8")
}

ShowLastResult(*) {
    lastF := A_ScriptDir "\TextTools_last.txt"
    ; пункт трея, который может ничего не показать, отвечает ОКНОМ: ToolTip у
    ; трея неотличим от «пункт меню не работает» (та же грабля, что с историей)
    if !FileExist(lastF) {
        ShowResult(Loc("hist.last", "Последний результат"), "# " Loc("hist.no_last.title", "Результатов ещё не было") "`n`n"
            . Loc("hist.no_last.body", "Текст последнего ответа модели дублируется в файл TextTools_last.txt рядом со скриптом: его можно достать, даже если окно результата закрылось вместе с процессом.`n`nФайла пока нет — после этого запуска ни одно действие не выполнялось. Выделите текст, нажмите хоткей меню и выберите действие."))
        return
    }
    ShowResult(Loc("hist.last", "Последний результат"), FileRead(lastF, "UTF-8"))
}

; usage - объект {prompt, completion} из ответа API либо "" (провайдер его не вернул)
HistoryAdd(action, sel, result, model := "", usage := "") {
    global gHistory
    tok := IsObject(usage) ? usage.prompt + usage.completion : 0
    cost := IsObject(usage) && model != "" ? CostOf(model, usage.prompt, usage.completion) : -1
    gHistory.InsertAt(1, {ts: FormatTime(, "yyyy-MM-dd HH:mm"), action: action, sel: HistTrim(sel),
        result: HistTrim(result), model: model, tok: tok, cost: cost})
    while (gHistory.Length > 20)
        gHistory.RemoveAt(gHistory.Length)
    SaveHistory()
}

HistTrim(s, n := 8000) {
    if (StrLen(s) <= n)
        return s
    return SubStr(s, 1, n) "…"
}

; Разделитель полей - "|", поэтому он экранируется отдельно от JsonEsc;
; JsonUnesc понимает \uXXXX, так что обратное преобразование уже есть.
HEsc(s) {
    return StrReplace(JsonEsc(s), "|", "\u007C")
}

; История переживает перезапуск: она живёт в памяти, а деплой перезапускает
; процесс - без файла каждое обновление стирало бы её подчистую.
SaveHistory() {
    global gHistory
    out := ""
    for r in gHistory {
        out .= r.ts "|" HEsc(r.action) "|" HEsc(r.model) "|" r.tok "|"
            . (r.cost >= 0 ? Format("{:.8f}", r.cost) : "-1") "|"
            . HEsc(r.sel) "|" HEsc(r.result) "`n"
    }
    try {
        f := FileOpen(HISTF, "w", "UTF-8")
        f.Write(out)
        f.Close()
    } catch as e {
        TT_Log("история не сохранена (" e.Message ")")
    }
}

LoadHistory() {
    out := []
    if !FileExist(HISTF)
        return out
    txt := ""
    try txt := FileRead(HISTF, "UTF-8")
    catch as e {
        TT_Log("история не читается (" e.Message ")")
        return out
    }
    for line in StrSplit(txt, "`n") {
        f := StrSplit(Trim(line, " `r`t"), "|")
        if (f.Length < 7)
            continue
        tok := 0, cost := -1
        try tok := Integer(f[4])
        try cost := f[5] + 0
        try out.Push({ts: f[1], action: JsonUnesc(f[2]), model: JsonUnesc(f[3]),
            tok: tok, cost: cost, sel: JsonUnesc(f[6]), result: JsonUnesc(f[7])})
        catch as e
            TT_Log("история: пропуск битой строки (" e.Message ")")
        if (out.Length >= 20)
            break
    }
    TT_Log("история загружена: записей " out.Length)
    return out
}

; Сегодняшнее время без даты, прошлое - с датой
HTime(ts) {
    if (SubStr(ts, 1, 10) = FormatTime(, "yyyy-MM-dd"))
        return SubStr(ts, 12)
    return SubStr(ts, 9, 2) "." SubStr(ts, 6, 2) " " SubStr(ts, 12)
}

Snip(s) {
    s := Trim(StrReplace(StrReplace(s, "`r", " "), "`n", " "))
    return (StrLen(s) > 60) ? SubStr(s, 1, 60) "…" : s
}

ShowHistory(*) {
    global gHistory
    ; пустая история - нормальное состояние, но подсказкой ToolTip она
    ; неотличима от "пункт меню не работает" (пожаловались дважды),
    ; поэтому отвечаем окном
    if (gHistory.Length = 0)
        gHistory := LoadHistory()          ; вдруг файл появился после старта
    if (gHistory.Length = 0) {
        ShowResult(Loc("hist.title", "История"), "# " Loc("hist.empty.title", "История пуста") "`n`n"
            . Loc("hist.empty.body", "Здесь копятся последние 20 операций: исходный текст, результат, модель и стоимость.`n`nЗаписей пока нет — после этого запуска ни одно действие не выполнялось. Обработайте любой текст, и он появится здесь.`n`nИстория хранится в файле TextTools_history.txt рядом со скриптом и переживает перезапуск (трей → «Открыть папку скрипта»)."))
        return
    }
    snap := gHistory.Clone()
    hg := Gui("+AlwaysOnTop", "TextTools - " Loc("hist.title", "История"))
    hg.SetFont("s10", "Segoe UI")
    ; NoSort: строки сопоставлены со snap по индексу (Sel), а колонки показывают
    ; форматированный текст - встроенная сортировка открывала бы чужую запись
    lv := hg.Add("ListView", "x10 y10 w960 r16 -Multi NoSort",
        [Loc("hist.col.time", "Время"), Loc("hist.col.action", "Действие"), Loc("set.col.model", "Модель"), Loc("hist.col.tokens", "Токены"), Loc("hist.col.cost", "Стоимость"), Loc("hist.col.source", "Исходник"), Loc("hist.col.result", "Результат")])
    for rec in snap {
        model := rec.HasProp("model") && rec.model != "" ? rec.model : "—"
        tok := rec.HasProp("tok") && rec.tok ? NumFmt(rec.tok) : "—"
        ; цена неизвестна и расход неизвестен - разные вещи, но обе дают прочерк
        cost := rec.HasProp("cost") && rec.cost >= 0 ? MoneyFmt(rec.cost) : "—"
        lv.Add(, HTime(rec.HasProp("ts") ? rec.ts : ""), rec.action, model, tok, cost,
            Snip(rec.sel), Snip(rec.result))
    }
    lv.ModifyCol(1, 55)
    lv.ModifyCol(2, 150)
    lv.ModifyCol(3, 125)
    lv.ModifyCol(4, 70)
    lv.ModifyCol(5, 80)
    lv.ModifyCol(6, 235)
    lv.ModifyCol(7, 235)
    lv.Modify(1, "Select Focus")
    bRes := hg.Add("Button", "x10 w170", Loc("hist.open_result", "Открыть результат"))
    bOrig := hg.Add("Button", "x+8 w170", Loc("hist.open_source", "Открыть оригинал"))
    bCp := hg.Add("Button", "x+8 w180", Loc("hist.copy", "Копировать результат"))
    bCl := hg.Add("Button", "x+8 w100", Loc("btn.close", "Закрыть"))

    Sel() {
        row := lv.GetNext(0)
        return row ? snap[row] : ""
    }
    OpenRes(*) {
        rec := Sel()
        if IsObject(rec)
            ShowResult(rec.action, rec.result)
    }
    OpenOrig(*) {
        rec := Sel()
        if IsObject(rec)
            ShowResult(rec.action " - " Loc("hist.orig_suffix", "оригинал"), rec.sel)
    }
    CopyRes(*) {
        rec := Sel()
        if IsObject(rec) {
            A_Clipboard := rec.result
            Flash(Loc("msg.copied", "Скопировано"))
        }
    }
    bRes.OnEvent("Click", OpenRes)
    lv.OnEvent("DoubleClick", OpenRes)
    bOrig.OnEvent("Click", OpenOrig)
    bCp.OnEvent("Click", CopyRes)
    bCl.OnEvent("Click", (*) => hg.Destroy())
    hg.OnEvent("Escape", (*) => hg.Destroy())
    hg.OnEvent("Close", (*) => hg.Destroy())
    hg.Show()
}

; Расход пишем отдельным файлом: по TextTools.log его не восстановить, а для
; отчета нужны модель и токены, а не текст запроса. Строка на запрос.
; Разделитель полей - "|", поэтому подпись и модель идут через HEsc: действие,
; переименованное в "A|B", сдвигало поля, Integer() доставался текст модели и
; бросал исключение - окно статистики не открывалось вообще.
UsageRecord(model, u, label := "") {
    line := FormatTime(, "yyyy-MM-dd HH:mm:ss") "|" HEsc(label != "" ? label : "запрос")
        . "|" HEsc(model) "|" u.prompt "|" u.completion "`n"
    try FileAppend(line, USAGEF, "UTF-8")
}

; Токены и деньги по моделям. Цены - каталог OpenRouter, см. Lib/prices.ahk
UsageReport() {
    txt := ""
    try txt := FileRead(USAGEF, "UTF-8")
    if (Trim(txt) = "")
        return "`n## " Loc("usage.title", "Расход токенов") "`n`n"
            . Loc("usage.empty", "Пока пусто: расход пишется начиная с версии, где появился этот отчет.") "`n"
    agg := Map()
    for line in StrSplit(txt, "`n") {
        f := StrSplit(Trim(line, " `r`t"), "|")
        if (f.Length < 5)
            continue
        ; в строке старого файла "|" из имени действия сдвинул поля, и на месте
        ; чисел стоит текст: такую строку пропускаем, а не роняем весь отчёт
        pt := 0, ct := 0
        try {
            pt := Integer(f[4])
            ct := Integer(f[5])
        } catch {
            continue
        }
        model := JsonUnesc(f[3])       ; в старых строках модель писалась без HEsc
        if !agg.Has(model)
            agg[model] := {n: 0, p: 0, c: 0}
        rec := agg[model]
        rec.n += 1
        rec.p += pt
        rec.c += ct
    }
    if (agg.Count = 0)
        return "`n## " Loc("usage.title", "Расход токенов") "`n`n"
            . Loc("usage.unparsed", "В файле расхода нет разбираемых строк.") "`n"
    PricesRefresh()                  ; каталог тянется не чаще раза в сутки
    rows := []
    for model, rec in agg
        rows.Push({model: model, n: rec.n, p: rec.p, c: rec.c, cost: CostOf(model, rec.p, rec.c)})
    i := 2                            ; сортировка по суммарным токенам, по убыванию
    SortBy(rows, (a, b) => (a.p + a.c) > (b.p + b.c))
    md := "`n## " Loc("usage.by_model", "Расход токенов по моделям") "`n`n"
        . "| " Loc("set.col.model", "Модель")
        . " | " Loc("usage.col.calls", "Запросов")
        . " | " Loc("usage.col.prompt", "Промпт")
        . " | " Loc("usage.col.completion", "Ответ")
        . " | " Loc("usage.col.total", "Всего")
        . " | " Loc("usage.col.cost", "Стоимость") " |`n|---|---:|---:|---:|---:|---:|`n"
    grand := 0.0, unknown := false, tn := 0, tp := 0, tc := 0
    for r in rows {
        if (r.cost >= 0)
            grand += r.cost
        else
            unknown := true
        tn += r.n, tp += r.p, tc += r.c
        md .= "| " r.model " | " r.n " | " NumFmt(r.p) " | " NumFmt(r.c) " | " NumFmt(r.p + r.c)
            . " | " (r.cost >= 0 ? MoneyFmt(r.cost) : Loc("usage.no_price", "цена неизвестна")) " |`n"
    }
    md .= "| **" Loc("usage.total", "Итого") "** | **" tn "** | **" NumFmt(tp) "** | **" NumFmt(tc) "** | **" NumFmt(tp + tc)
        . "** | **" MoneyFmt(grand) (unknown ? " +" : "") "** |`n`n"
    stamp := PricesStamp()
    md .= "> " Loc("usage.note.est", "Стоимость - **оценка**. Токены точные: их возвращает само API в поле usage.")
        . " " Loc("usage.note.catalog", "Цены берутся из каталога OpenRouter (/api/v1/models), потому что у OpenAI публичного прайс-эндпоинта нет.")
        . (stamp != "" ? " " Format(Loc("usage.note.stamp", "Каталог загружен {1}, обновляется не чаще раза в сутки."), stamp) : "")
        . " " Loc("usage.note.diff", "Со счётом провайдера возможны расхождения.") "`n"
    if unknown
        md .= "`n> " Loc("usage.note.unknown", "Для части моделей цены в каталоге не нашлось - они не вошли в итог (отмечен плюсом).") "`n"
    return md
}

; Статистика нажатий кнопок по логу (для решения, какие кнопки мертвы)
ShowStats(*) {
    txtLog := ""
    try txtLog := FileRead(LOGF, "UTF-8")
    counts := Map()
    for line in StrSplit(txtLog, "`n") {
        p := InStr(line, "действие: ")
        if !p
            continue
        rest := SubStr(line, p + StrLen("действие: "))
        name := Trim(StrSplit(rest, ",")[1])
        name := StrReplace(name, " (custom)")
        name := StrReplace(name, " (+инструкция)")
        counts[name] := counts.Get(name, 0) + 1
    }
    ; тот же пункт трея: пусто - нормальное состояние, но отвечать надо окном,
    ; а не подсказкой у трея, которую не отличить от «ничего не произошло»
    if (counts.Count = 0 && !FileExist(USAGEF)) {
        ShowResult(Loc("hist.stats", "Статистика"), "# " Loc("hist.no_actions", "В логе еще нет действий") "`n`n"
            . Loc("hist.no_actions.body", "Статистика считается по двум файлам рядом со скриптом: TextTools.log — сколько раз запускалось каждое действие, TextTools_usage.log — сколько токенов и денег ушло на каждую модель.`n`nНи одного запуска пока не записано. Обработайте любой текст, и цифры появятся здесь."))
        return
    }
    rows := []
    for name, n in counts
        rows.Push({name: name, n: n})
    SortBy(rows, (a, b) => a.n > b.n)
    md := "# " Loc("hist.stats.heading", "Статистика использования") "`n`n## " Loc("hist.runs", "Запуски действий") "`n`n" Loc("hist.by_log", "По логу TextTools.log.") "`n`n"
        . "| " Loc("hist.col.action", "Действие") " | " Loc("hist.runs_n", "Запусков") " |`n|---|---:|`n"
    for r in rows
        md .= "| " r.name " | " r.n " |`n"
    md .= UsageReport()
    ShowResult(Loc("hist.stats", "Статистика"), md)
}

ClearHistory(*) {
    global gHistory
    gHistory := []
    try FileDelete(HISTF)
    ShowResult(Loc("hist.title", "История"), "# " Loc("hist.cleared.title", "История очищена") "`n`n"
        . Loc("hist.cleared.body", "Файл TextTools_history.txt удалён. Новые операции снова появятся здесь."))
}

ClearLog(*) {
    try FileDelete(LOGF)
    try FileDelete(LOGF ".old")
    ShowResult(Loc("log.title", "лог"), "# " Loc("log.cleared.title", "Лог очищен") "`n`n"
        . Loc("log.cleared.body", "TextTools.log удалён. Новые события начнут писаться с этого момента."))
}
