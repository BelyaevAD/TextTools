; TextTools - хранилище языков ответа (langs.ini).
; Подключается из TextTools.ahk, отдельно не запускается.
;
; Заводской список живёт в Lib/langs.ahk, пользовательский - в langs.ini рядом
; со скриптом. Пользователь отмечает галочками, какие языки видны в карточке
; действия, и может добавить свой (например региональный вариант).

CloneLangs(list) {
    out := []
    for lg in list
        out.Push({code: lg.code, name: lg.name, prompt: lg.prompt, on: lg.on})
    return out
}

LoadLangs() {
    if !FileExist(LANGS_INI) {
        TT_Log("langs.ini не найден, раскладываю заводской список")
        try SaveLangs(DEFAULT_LANGS)   ; автозапуск: сбой записи не должен сорвать старт
        return CloneLangs(DEFAULT_LANGS)
    }
    out := []
    try {
        loop MAX_ACTIONS {                       ; секции подряд до первой пустой
            sec := "lang" A_Index
            code := Trim(IniRead(LANGS_INI, sec, "code", ""))
            if (code = "")
                break
            name := JsonUnesc(IniRead(LANGS_INI, sec, "name", code))
            out.Push({code: code, name: name,
                prompt: JsonUnesc(IniRead(LANGS_INI, sec, "prompt", name)),
                on: IniRead(LANGS_INI, sec, "on", "0") = "1"})
        }
    } catch as e {
        TT_Log("langs.ini не читается (" e.Message "), беру заводской список")
        return CloneLangs(DEFAULT_LANGS)
    }
    if (out.Length = 0) {
        TT_Log("в langs.ini нет языков, беру заводские")
        return CloneLangs(DEFAULT_LANGS)
    }
    TT_Log("загружено языков: " out.Length)
    return out
}

SaveLangs(list) {
    try FileDelete(LANGS_INI)
    for i, lg in list {
        sec := "lang" i
        IniWrite(lg.code, LANGS_INI, sec, "code")
        IniWrite(JsonEsc(lg.name), LANGS_INI, sec, "name")
        IniWrite(JsonEsc(lg.prompt), LANGS_INI, sec, "prompt")
        IniWrite(lg.on ? "1" : "0", LANGS_INI, sec, "on")
    }
    TT_Log("langs.ini сохранён: языков " list.Length)
}

; Только отмеченные - из них строится выпадающий список карточки действия
EnabledLangs() {
    out := []
    for lg in LANGS
        if lg.on
            out.Push(lg)
    return out
}

LangByCode(code) {
    for lg in LANGS
        if (lg.code = code)
            return lg
    return ""
}

; Подпись языка для интерфейса; незнакомый код показываем как есть,
; чтобы действие с языком, убранным из списка, не выглядело сломанным
LangLabel(code) {
    if (code = "" || code = "auto")
        return ""
    lg := LangByCode(code)
    return IsObject(lg) ? lg.name : code
}

; Блок LANGUAGE для системного промпта
LangBlock(code) {
    if (code = "" || code = "auto")
        return LANG_AUTO
    lg := LangByCode(code)
    ; язык мог быть удалён из списка уже после того, как его назначили действию:
    ; тогда берём код как есть - это лучше, чем молча ответить на другом языке
    name := IsObject(lg) ? lg.prompt : code
    return Format(LANG_FIXED, name)
}
