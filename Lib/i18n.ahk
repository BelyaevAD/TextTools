; TextTools - Локализация интерфейса.
; Подключается из TextTools.ahk, отдельно не запускается.
;
; Устройство намеренно простое, чтобы языковой пакет мог сделать кто угодно:
;   lang\<код>.ini   [meta] name=<как подписать язык>
;                    [ui]   ключ=перевод
; Русский текст лежит прямо в коде вторым аргументом L(), поэтому пакет
; переводит СКОЛЬКО СМОЖЕТ: непереведённый ключ показывается по-русски, а не
; пустой строкой и не техническим именем. Отсюда же следует, что ru.ini не
; нужен - русский и есть база.
;
; Ключи не менять без нужды: они и есть контракт с пакетами сообщества.

; Пакеты читаются НЕ через IniRead: Win32-функции INI понимают Unicode только в
; UTF-16, а UTF-8 молча разбирают как ANSI - в меню лезли "вЂ¦" вместо "…".
; Свой разбор принимает UTF-8 с BOM и без, то есть ровно то, что сохранит любой
; редактор у автора пакета.
ParsePack(path) {
    out := Map()
    txt := ""
    try txt := FileRead(path, "UTF-8")
    catch as e {
        TT_Log("языковой пакет " path " не читается (" e.Message ")")
        return out
    }
    sec := ""
    for line in StrSplit(txt, "`n") {
        line := Trim(line, " `r`t`uFEFF")
        if (line = "" || SubStr(line, 1, 1) = ";")
            continue
        if (SubStr(line, 1, 1) = "[") {
            sec := StrLower(Trim(line, "[]"))
            continue
        }
        p := InStr(line, "=")
        if (p < 2)
            continue
        key := Trim(SubStr(line, 1, p - 1))
        if (key != "")
            out[sec "." key] := JsonUnesc(Trim(SubStr(line, p + 1)))
    }
    return out
}

; Пакеты, найденные в lang\ и lang\user\: [{code, name}]. Русский всегда первым.
; Пакет в lang\user с тем же кодом перекрывает поставленный.
UiLangPacks() {
    out := [{code: "ru", name: Loc("lang.ru", "Русский")}]
    seen := Map("ru", true)
    AddDir(dir) {
        if !DirExist(dir)
            return
        loop files dir "\*.ini" {
            code := StrReplace(A_LoopFileName, ".ini")
            if seen.Has(code)
                continue
            seen[code] := true
            pack := ParsePack(A_LoopFileFullPath)
            out.Push({code: code, name: pack.Has("meta.name") ? pack["meta.name"] : code})
        }
    }
    AddDir(A_ScriptDir "\lang\user")
    AddDir(A_ScriptDir "\lang")
    return out
}

LoadUiLang(code) {
    m := Map()
    if (code = "" || code = "ru")
        return m
    path := A_ScriptDir "\lang\user\" code ".ini"
    if !FileExist(path)
        path := A_ScriptDir "\lang\" code ".ini"
    if !FileExist(path) {
        TT_Log("языковой пакет " code " не найден, интерфейс остаётся русским")
        return m
    }
    for key, val in ParsePack(path) {
        if (SubStr(key, 1, 3) = "ui.")
            m[SubStr(key, 4)] := val
    }
    TT_Log("языковой пакет " code ": строк " m.Count)
    return m
}

; Перевод по ключу; ru - текст по умолчанию, он же русская версия
Loc(key, ru) {
    global gUiStrings              ; без объявления имя было бы локальным и всегда пустым
    if (IsSet(gUiStrings) && IsObject(gUiStrings) && gUiStrings.Has(key))
        return gUiStrings[key]
    return ru
}

; Название заводского действия на языке интерфейса. У пользовательских действий
; поля id нет - они показываются ровно так, как их назвали.
ActionName(a) {
    if (a.HasProp("id") && a.id != "")
        return Loc("action." a.id, a.name)
    return a.name
}
