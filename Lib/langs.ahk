; TextTools - Заводской список языков ответа (данные, не код).
; Подключается из TextTools.ahk ДО автозапуска, отдельно не запускается.
;
; code   - код языка, он же значение поля lang у действия ("auto" зарезервирован)
; name   - как язык подписан в интерфейсе
; prompt - как язык называется в промпте (по-английски): подставляется в
;          LANG_FIXED, поэтому модель понимает его однозначно
; on     - показывать ли язык в выпадающем списке карточки действия

DEFAULT_LANGS := [
    {code: "ru", name: "Русский",     prompt: "Russian",              on: true},
    {code: "en", name: "English",     prompt: "English",              on: true},
    {code: "de", name: "Deutsch",     prompt: "German",               on: false},
    {code: "fr", name: "Français",    prompt: "French",               on: false},
    {code: "es", name: "Español",     prompt: "Spanish",              on: false},
    {code: "it", name: "Italiano",    prompt: "Italian",              on: false},
    {code: "pt", name: "Português",   prompt: "Portuguese",           on: false},
    {code: "pl", name: "Polski",      prompt: "Polish",               on: false},
    {code: "tr", name: "Türkçe",      prompt: "Turkish",              on: false},
    {code: "uk", name: "Українська",  prompt: "Ukrainian",            on: false},
    {code: "kk", name: "Қазақша",     prompt: "Kazakh",               on: false},
    {code: "sr", name: "Srpski",      prompt: "Serbian",              on: false},
    {code: "zh", name: "中文",         prompt: "Simplified Chinese",   on: false},
    {code: "ja", name: "日本語",       prompt: "Japanese",             on: false},
    {code: "ko", name: "한국어",       prompt: "Korean",               on: false},
    {code: "ar", name: "العربية",      prompt: "Arabic",               on: false},
    {code: "he", name: "עברית",        prompt: "Hebrew",               on: false},
    {code: "hi", name: "हिन्दी",         prompt: "Hindi",                on: false}]
