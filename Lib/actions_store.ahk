; TextTools - хранилище пользовательских действий и быстрых уточнений (actions.ini).
; Подключается из TextTools.ahk, отдельно не запускается.
;
; Заводской набор живёт в коде (DEFAULT_ACTIONS / DEFAULT_QUICK в actions.ahk),
; пользовательский - в actions.ini рядом со скриптом. Файл появляется при первом
; запуске и дальше является источником правды; кнопка "Сбросить к заводским" в
; редакторе возвращает набор из кода.
;
; Многострочные промпты хранятся экранированными (\n) теми же JsonEsc/JsonUnesc,
; что и запросы к API: INI построчный и сырые переводы строк не переживёт.

; Потолок на число читаемых секций (MAX_ACTIONS) задан в точке входа: этот файл
; подключается после автозапуска, а LoadActionSets() зовётся уже в нём.

CloneActions(list) {
    out := []
    for a in list
        out.Push({id: a.HasProp("id") ? a.id : "", name: a.name, icon: a.icon,
            prefix: a.prefix, task: a.task, lang: a.lang, ru: a.ru, win: a.win,
            key: a.HasProp("key") ? a.key : "",
            model: a.HasProp("model") ? a.model : "",
            role: a.HasProp("role") ? a.role : ""})
    return out
}

; "-" из файла и всё, чего нет в пуле, означает "без клавиши"
NormKey(k) {
    k := StrLower(Trim(k))
    for pk in MENU_KEYS
        if (k = pk)
            return k
    return ""
}

; "-" в файле = пустое значение: пустая строка в INI неотличима от
; отсутствующего ключа (см. TEXTTOOLS_RUNBOOK §5 п. 23)
NormDash(m) {
    m := Trim(m)
    return (m = "-") ? "" : m
}

; Первая свободная клавиша пула - для нового действия в редакторе
FreeMenuKey(list) {
    for k in MENU_KEYS {
        taken := false
        for a in list
            if (a.HasProp("key") && a.key = k)
                taken := true
        if !taken
            return k
    }
    return ""
}

; Кто держит клавишу пула. exceptRow (1-based) не считается: это текущая карточка.
FindMenuKeyOwner(list, key, exceptRow := 0) {
    if (key = "")
        return 0
    for i, a in list
        if (i != exceptRow && a.HasProp("key") && a.key = key)
            return i
    return 0
}

; Уступание занятой клавиши: swap отдаёт владельцу incomingOldKey, take снимает.
; Текущую карточку не трогает - её ключ записывает вызывающий при сохранении.
YieldMenuKey(list, occupantRow, incomingOldKey, mode) {
    if (occupantRow < 1 || occupantRow > list.Length)
        return false
    if (mode = "swap")
        list[occupantRow].key := incomingOldKey
    else if (mode = "take")
        list[occupantRow].key := ""
    else
        return false
    return true
}

CloneQuick(list) {
    out := []
    for q in list
        out.Push({label: q.label, instr: q.instr})
    return out
}

; Читает наборы из INI. Любая проблема - откат на заводские, без падения.
LoadActionSets() {
    path := ACTIONS_INI
    if !FileExist(path) {
        TT_Log("actions.ini не найден, раскладываю заводской набор")
        acts := CloneActions(DEFAULT_ACTIONS)
        try ApplyFactoryRoles(acts, cfg.base_url)
        try EnsureModelsForActions(acts)
        ; try: раскладка идёт из автозапуска ДО SetupTray и регистрации хоткеев,
        ; и невозможность записи (папка только для чтения, файл занят) не должна
        ; оставлять процесс без трея и без хоткея
        try SaveActionSets(acts, DEFAULT_QUICK)
        return {actions: acts, quick: CloneQuick(DEFAULT_QUICK)}
    }
    acts := [], quicks := []
    try {
        ; Секции читаются подряд до первой отсутствующей, а не по полю count:
        ; испорченный count (например 500000) заставлял бы крутить IniRead
        ; полмиллиона раз - скрипт вис бы на старте, без меню и хоткея.
        ; Потолок MAX_ACTIONS - страховка от файла с осмысленным, но абсурдным
        ; числом секций.
        loop MAX_ACTIONS {
            sec := "action" A_Index
            name := IniRead(path, sec, "name", "")
            task := IniRead(path, sec, "task", "")
            if (name = "")
                break                                      ; секции кончились
            if (task = "")
                continue                                   ; битая запись - пропускаем
            ; Файл старого формата без key: клавиша берётся по позиции, как
            ; было раньше, - иначе после обновления все акселераторы пропали бы
            acts.Push({
                ; id есть только у заводских действий - по нему берётся
                ; переведённое название; у пользовательских он пуст
                id: NormDash(IniRead(path, sec, "id", "-")),
                name: JsonUnesc(name),
                icon: JsonUnesc(IniRead(path, sec, "icon", "")),
                prefix: JsonUnesc(IniRead(path, sec, "prefix", "")),
                task: JsonUnesc(task),
                lang: IniRead(path, sec, "lang", "auto"),
                ru: IniRead(path, sec, "ru", "1") = "1",
                win: IniRead(path, sec, "win", "0") = "1",
                key: NormKey(IniRead(path, sec, "key",
                    A_Index <= MENU_KEYS.Length ? MENU_KEYS[A_Index] : "")),
                ; "-" и отсутствие ключа = модель по умолчанию из настроек
                model: NormDash(IniRead(path, sec, "model", "-"))})
        }
        loop MAX_ACTIONS {
            sec := "quick" A_Index
            label := IniRead(path, sec, "label", "")
            instr := IniRead(path, sec, "instr", "")
            if (label = "")
                break
            if (instr = "")
                continue
            quicks.Push({label: JsonUnesc(label), instr: JsonUnesc(instr)})
        }
    } catch as e {
        TT_Log("actions.ini не читается (" e.Message "), беру заводской набор")
        return {actions: CloneActions(DEFAULT_ACTIONS), quick: CloneQuick(DEFAULT_QUICK)}
    }
    if (acts.Length = 0) {
        TT_Log("в actions.ini нет ни одного действия, беру заводские")
        acts := CloneActions(DEFAULT_ACTIONS)
    }
    ; Осознанно пустой набор уточнений отличается от битого файла по [quick] count:
    ; count=0 пишет сам редактор, отсутствие ключа - старый или испорченный файл.
    ; Иначе удаление всех уточнений откатывалось бы заводским набором при рестарте.
    if (quicks.Length = 0 && Trim(IniRead(path, "quick", "count", "")) != "0")
        quicks := CloneQuick(DEFAULT_QUICK)
    TT_Log("загружено действий: " acts.Length ", быстрых уточнений: " quicks.Length)
    return {actions: acts, quick: quicks}
}

; Пишет наборы в INI. Файл пересоздаётся целиком: иначе от удалённых действий
; остались бы секции, и следующая загрузка подняла бы их обратно.
SaveActionSets(acts, quicks) {
    path := ACTIONS_INI
    try FileDelete(path)
    IniWrite(acts.Length, path, "actions", "count")
    for i, a in acts {
        sec := "action" i
        IniWrite((a.HasProp("id") && a.id != "") ? a.id : "-", path, sec, "id")
        IniWrite(JsonEsc(a.name), path, sec, "name")
        IniWrite(JsonEsc(a.icon), path, sec, "icon")
        IniWrite(JsonEsc(a.prefix), path, sec, "prefix")
        ; "-" вместо пустой строки: пустое значение в INI неотличимо от
        ; отсутствующего ключа, и действие без клавиши снова получило бы её
        IniWrite(a.key = "" ? "-" : a.key, path, sec, "key")
        IniWrite((a.HasProp("model") && a.model != "") ? a.model : "-", path, sec, "model")
        IniWrite(a.lang, path, sec, "lang")
        IniWrite(a.ru ? "1" : "0", path, sec, "ru")
        IniWrite(a.win ? "1" : "0", path, sec, "win")
        IniWrite(JsonEsc(a.task), path, sec, "task")
    }
    IniWrite(quicks.Length, path, "quick", "count")
    for i, q in quicks {
        sec := "quick" i
        IniWrite(JsonEsc(q.label), path, sec, "label")
        IniWrite(JsonEsc(q.instr), path, sec, "instr")
    }
    TT_Log("actions.ini сохранён: действий " acts.Length ", уточнений " quicks.Length)
}
