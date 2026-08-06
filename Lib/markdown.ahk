; TextTools - Конвертер Markdown -> HTML для окна результата (чистые функции).
; Подключается из TextTools.ahk, отдельно не запускается.

HtmlEsc(s) {
    s := StrReplace(s, "&", "&amp;")
    s := StrReplace(s, "<", "&lt;")
    s := StrReplace(s, ">", "&gt;")
    return s
}

; инлайновая разметка: `код`, **жирный**, __жирный__, *курсив*, [текст](url)
MdInline(s) {
    static BT := Chr(96)
    s := HtmlEsc(s)
    s := RegExReplace(s, BT "([^" BT "]+)" BT, "<code>$1</code>")
    s := RegExReplace(s, "\*\*([^*]+)\*\*", "<b>$1</b>")
    s := RegExReplace(s, "__([^_]+)__", "<b>$1</b>")
    s := RegExReplace(s, "(?<!\*)\*([^*]+)\*(?!\*)", "<i>$1</i>")
    s := RegExReplace(s, "\[([^\]]+)\]\(([^)]+)\)", "$1")
    return s
}

; строка-разделитель шапки таблицы: только | - : и пробелы, есть хотя бы один -
IsTableSep(line) {
    t := Trim(line)
    if (t = "" || !InStr(t, "-"))
        return false
    loop parse t {
        c := A_LoopField
        if (c != "|" && c != "-" && c != ":" && c != " ")
            return false
    }
    return true
}

MdTableRow(line, tag) {
    t := Trim(line, " `t")
    t := Trim(t, "|")
    out := "<tr>"
    for cell in StrSplit(t, "|")
        out .= "<" tag ">" MdInline(Trim(cell)) "</" tag ">"
    return out "</tr>`n"
}

MdToHtml(md) {
    static BT := Chr(96)
    static FENCE := BT BT BT
    head := '
(
<!DOCTYPE html><html><head><meta http-equiv="X-UA-Compatible" content="IE=edge"><meta charset="utf-8"><style>
body{font-family:"Segoe UI",Arial,sans-serif;font-size:14px;color:#1b1b1b;margin:12px;}
table{border-collapse:collapse;margin:8px 0;}
th,td{border:1px solid #b5b5b5;padding:4px 9px;text-align:left;vertical-align:top;}
th{background:#efefef;}
code{background:#f2f2f2;padding:1px 4px;font-family:Consolas,monospace;font-size:13px;}
pre{background:#f2f2f2;padding:8px;font-family:Consolas,monospace;font-size:13px;white-space:pre-wrap;}
h1{font-size:20px;margin:10px 0 6px;}h2{font-size:18px;margin:10px 0 6px;}h3{font-size:16px;margin:8px 0 4px;}
h4,h5,h6{font-size:14px;margin:8px 0 4px;}
ul,ol{margin:4px 0 6px 26px;padding:0;}
li{margin:2px 0;}
p{margin:6px 0;}
hr{border:0;border-top:1px solid #ccc;margin:10px 0;}
</style></head><body>
)'
    md := StrReplace(md, "`r`n", "`n")
    md := StrReplace(md, "`r", "`n")
    lines := StrSplit(md, "`n")
    body := ""
    inCode := false
    listTag := ""
    n := lines.Length
    i := 1
    while (i <= n) {
        line := lines[i]
        t := Trim(line, " `t")

        ; блок кода ```
        if (SubStr(t, 1, 3) = FENCE) {
            if listTag {
                body .= "</" listTag ">`n"
                listTag := ""
            }
            body .= inCode ? "</pre>`n" : "<pre>"
            inCode := !inCode
            i++
            continue
        }
        if inCode {
            body .= HtmlEsc(line) "`n"
            i++
            continue
        }
        if (t = "") {
            if listTag {
                body .= "</" listTag ">`n"
                listTag := ""
            }
            i++
            continue
        }
        ; таблица: строка с | и следом строка-разделитель
        if (SubStr(t, 1, 1) = "|" && i < n && IsTableSep(lines[i + 1])) {
            if listTag {
                body .= "</" listTag ">`n"
                listTag := ""
            }
            body .= "<table>`n" MdTableRow(line, "th")
            i += 2
            while (i <= n && SubStr(Trim(lines[i], " `t"), 1, 1) = "|") {
                body .= MdTableRow(lines[i], "td")
                i++
            }
            body .= "</table>`n"
            continue
        }
        ; заголовок
        if RegExMatch(t, "^(#{1,6})\s+(.+)$", &m) {
            if listTag {
                body .= "</" listTag ">`n"
                listTag := ""
            }
            lvl := StrLen(m[1])
            body .= "<h" lvl ">" MdInline(m[2]) "</h" lvl ">`n"
            i++
            continue
        }
        ; горизонтальная линия
        if RegExMatch(t, "^(-{3,}|\*{3,})$") {
            if listTag {
                body .= "</" listTag ">`n"
                listTag := ""
            }
            body .= "<hr>`n"
            i++
            continue
        }
        ; маркированный список
        if RegExMatch(t, "^[-*+]\s+(.+)$", &m) {
            if (listTag != "ul") {
                if listTag
                    body .= "</" listTag ">`n"
                body .= "<ul>`n"
                listTag := "ul"
            }
            body .= "<li>" MdInline(m[1]) "</li>`n"
            i++
            continue
        }
        ; нумерованный список
        if RegExMatch(t, "^\d+[.)]\s+(.+)$", &m) {
            if (listTag != "ol") {
                if listTag
                    body .= "</" listTag ">`n"
                body .= "<ol>`n"
                listTag := "ol"
            }
            body .= "<li>" MdInline(m[1]) "</li>`n"
            i++
            continue
        }
        ; обычный абзац
        if listTag {
            body .= "</" listTag ">`n"
            listTag := ""
        }
        body .= "<p>" MdInline(t) "</p>`n"
        i++
    }
    if inCode
        body .= "</pre>`n"
    if listTag
        body .= "</" listTag ">`n"
    return head body "</body></html>"
}
