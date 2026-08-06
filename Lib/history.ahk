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
    if !FileExist(lastF) {
        Flash(Loc("msg.no_results", "Результатов еще не было"))
        return
    }
    ShowResult("Последний результат", FileRead(lastF, "UTF-8"))
}

; usage - объект {prompt, completion} из ответа API либо "" (провайдер его не вернул)
HistoryAdd(action, sel, result, model := "", usage := "") {
    global gHistory
    tok := IsObject(usage) ? usage.prompt + usage.completion : 0
    cost := IsObject(usage) && model != "" ? CostOf(model, usage.prompt, usage.completion) : -1
    gHistory.InsertAt(1, {ts: FormatTime(, "yyyy-MM-dd HH:mm"), action: action, sel: sel,
        result: result, model: model, tok: tok, cost: cost})
    while (gHistory.Length > 20)
        gHistory.RemoveAt(gHistory.Length)
    SaveHistory()
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
        out.Push({ts: f[1], action: JsonUnesc(f[2]), model: JsonUnesc(f[3]),
            tok: Integer(f[4]), cost: f[5] + 0,
            sel: JsonUnesc(f[6]), result: JsonUnesc(f[7])})
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
        ShowResult("История", "# История пуста`n`n"
            . "Здесь копятся последние 20 операций: исходный текст, результат, "
            . "модель и стоимость.`n`n"
            . "Записей пока нет — после этого запуска ни одно действие не выполнялось. "
            . "Обработайте любой текст, и он появится здесь.`n`n"
            . "История хранится в файле TextTools_history.txt рядом со скриптом "
            . "и переживает перезапуск (трей → «Открыть папку скрипта»).")
        return
    }
    snap := gHistory.Clone()
    hg := Gui("+AlwaysOnTop", "TextTools - " Loc("hist.title", "история"))
    hg.SetFont("s10", "Segoe UI")
    lv := hg.Add("ListView", "x10 y10 w960 r16 -Multi",
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
            ShowResult(rec.action " - оригинал", rec.sel)
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
UsageRecord(model, u, label := "") {
    line := FormatTime(, "yyyy-MM-dd HH:mm:ss") "|" (label != "" ? label : "запрос")
        . "|" model "|" u.prompt "|" u.completion "`n"
    try FileAppend(line, USAGEF, "UTF-8")
}

; Токены и деньги по моделям. Цены - каталог OpenRouter, см. Lib/prices.ahk
UsageReport() {
    txt := ""
    try txt := FileRead(USAGEF, "UTF-8")
    if (Trim(txt) = "")
        return "`n## Расход токенов`n`nПока пусто: расход пишется начиная с версии, где появился этот отчет.`n"
    agg := Map()
    for line in StrSplit(txt, "`n") {
        f := StrSplit(Trim(line, " `r`t"), "|")
        if (f.Length < 5)
            continue
        model := f[3]
        if !agg.Has(model)
            agg[model] := {n: 0, p: 0, c: 0}
        rec := agg[model]
        rec.n += 1
        rec.p += Integer(f[4])
        rec.c += Integer(f[5])
    }
    if (agg.Count = 0)
        return "`n## Расход токенов`n`nВ файле расхода нет разбираемых строк.`n"
    PricesRefresh()                  ; каталог тянется не чаще раза в сутки
    rows := []
    for model, rec in agg
        rows.Push({model: model, n: rec.n, p: rec.p, c: rec.c, cost: CostOf(model, rec.p, rec.c)})
    i := 2                            ; сортировка по суммарным токенам, по убыванию
    while (i <= rows.Length) {
        j := i
        while (j > 1 && (rows[j].p + rows[j].c) > (rows[j - 1].p + rows[j - 1].c)) {
            tmp := rows[j], rows[j] := rows[j - 1], rows[j - 1] := tmp
            j--
        }
        i++
    }
    md := "`n## Расход токенов по моделям`n`n"
        . "| Модель | Запросов | Промпт | Ответ | Всего | Стоимость |`n|---|---:|---:|---:|---:|---:|`n"
    grand := 0.0, unknown := false, tn := 0, tp := 0, tc := 0
    for r in rows {
        if (r.cost >= 0)
            grand += r.cost
        else
            unknown := true
        tn += r.n, tp += r.p, tc += r.c
        md .= "| " r.model " | " r.n " | " NumFmt(r.p) " | " NumFmt(r.c) " | " NumFmt(r.p + r.c)
            . " | " (r.cost >= 0 ? MoneyFmt(r.cost) : "цена неизвестна") " |`n"
    }
    md .= "| **Итого** | **" tn "** | **" NumFmt(tp) "** | **" NumFmt(tc) "** | **" NumFmt(tp + tc)
        . "** | **" MoneyFmt(grand) (unknown ? " +" : "") "** |`n`n"
    stamp := PricesStamp()
    md .= "> Стоимость - **оценка**. Токены точные: их возвращает само API в поле usage."
        . " Цены берутся из каталога OpenRouter (/api/v1/models), потому что у OpenAI"
        . " публичного прайс-эндпоинта нет."
        . (stamp != "" ? " Каталог загружен " stamp ", обновляется не чаще раза в сутки." : "")
        . " Со счётом провайдера возможны расхождения.`n"
    if unknown
        md .= "`n> Для части моделей цены в каталоге не нашлось - они не вошли в итог (отмечен плюсом).`n"
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
    if (counts.Count = 0 && !FileExist(USAGEF)) {
        Flash("В логе еще нет действий")
        return
    }
    rows := []
    for name, n in counts
        rows.Push({name: name, n: n})
    i := 2
    while (i <= rows.Length) {
        j := i
        while (j > 1 && rows[j].n > rows[j - 1].n) {
            tmp := rows[j]
            rows[j] := rows[j - 1]
            rows[j - 1] := tmp
            j--
        }
        i++
    }
    md := "# Статистика использования`n`n## Запуски действий`n`nПо логу TextTools.log.`n`n"
        . "| Действие | Запусков |`n|---|---:|`n"
    for r in rows
        md .= "| " r.name " | " r.n " |`n"
    md .= UsageReport()
    ShowResult("Статистика", md)
}
