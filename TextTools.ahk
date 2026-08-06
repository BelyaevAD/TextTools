#Requires AutoHotkey v2.0
#SingleInstance Force
; ============================================================================
; TextTools - LLM actions on selected text (AutoHotkey v2)
;
; Entry point: directives, globals, autostart.
; Logic lives under Lib\ - see docs/USER_GUIDE.md.
;
; #Include в AHK v2 - текстовая вставка в точку директивы, поэтому файлы с
; ДАННЫМИ (присваивания верхнего уровня) подключаются ДО автозапуска, а файлы
; с определениями функций - после: их порядок значения не имеет.
; ============================================================================

CoordMode "Caret", "Screen"
CoordMode "Mouse", "Screen"
CoordMode "ToolTip", "Screen"

INI := A_ScriptDir "\TextTools.ini"
LOGF := A_ScriptDir "\TextTools.log"
ACTIONS_INI := A_ScriptDir "\actions.ini"
APPS_INI := A_ScriptDir "\apps.ini"
LANGS_INI := A_ScriptDir "\langs.ini"          ; языки ответа (галочки в настройках)
USAGEF := A_ScriptDir "\TextTools_usage.log"   ; расход токенов, строка на запрос
HISTF := A_ScriptDir "\TextTools_history.txt"  ; история результатов, переживает перезапуск
PRICESF := A_ScriptDir "\prices.json"          ; кэш каталога цен (OpenRouter)
MAX_ACTIONS := 200            ; потолок секций при чтении actions.ini (защита от битого файла)

; --- заводские наборы (должны быть определены до автозапуска) ---
#Include Lib\prompts.ahk
#Include Lib\actions.ahk
#Include Lib\apps.ahk
#Include Lib\langs.ahk

; ---------------------------------------------------------------------------
; Запуск
; ---------------------------------------------------------------------------

cfg := LoadCfg()
gUiStrings := LoadUiLang(cfg.ui_lang)   ; должен быть готов до первого окна
LANGS := LoadLangs()                    ; языки ответа
; рабочие наборы: actions.ini (правится редактором), при его отсутствии - заводские
sets := LoadActionSets()
ACTIONS := sets.actions
QUICK_REFINE := sets.quick
APPS := LoadApps()          ; профили приложений-источников
gEditorGui := ""
gSel := ""
gPrevWin := 0
gForeWin := 0                 ; последнее пользовательское окно (для вызова из трея)
gMenuGui := ""
gSettingsGui := ""
gBusy := false
gBusyGui := ""
gBusyText := ""
gBusyInfo := ""
gBusyDetail := ""
gBusyStart := 0
gLastUsage := ""              ; usage последнего ответа API
gCallLabel := ""              ; чем вызван запрос - попадает в файл расхода
gHotkeyCurrent := ""
gHotkeyRepeatCurrent := ""
gLastAction := ""
gLastInstr := ""
gHistory := LoadHistory()     ; деплой перезапускает процесс - история берётся с диска
gMenuEdit := ""
gMenuFirstBtn := ""
gRestoreTimer := ""
gStartTick := A_TickCount

OnError ErrorLogger
OnExit ExitLogger
TT_Log("=== старт, PID " ProcessExist() ", хоткей " cfg.hotkey " ===")

SetupTray()
; трекер окна-источника: при вызове меню из трея активной оказывается оболочка,
; и без запомненного окна Ctrl+C уходит в никуда (см. Lib\hotkeys.ahk)
SetTimer(ForeWatch, 400)
try ApplyHotkeyPair(cfg.hotkey, cfg.hotkey_repeat)
catch as e {
    TT_Log("хоткеи из конфига не приняты (" e.Message "), ставлю умолчания")
    try ApplyHotkeyPair("^!Space", "^+Space")
}

; акселераторы меню: активны только когда окно меню на переднем плане
; и фокус НЕ в поле "Инструкция" (там клавиши печатают текст)
HotIf(MenuKeysCtx)
for k in MENU_KEYS {
    Hotkey(k, MenuKeyPress.Bind(k, false))
    Hotkey("+" k, MenuKeyPress.Bind(k, true))    ; Shift - результат в окне
}
HotIf()

if (cfg.api_key = "") {
    TrayTip("Укажи API key в настройках (иконка в трее)", "TextTools")
    ShowSettings()
} else {
    TrayTip("Запущен. Хоткей меню: " cfg.hotkey, "TextTools")
}

; --- определения функций ---
#Include Lib\log.ahk
#Include Lib\i18n.ahk
#Include Lib\config.ahk
#Include Lib\langs_store.ahk
#Include Lib\actions_store.ahk
#Include Lib\apps_store.ahk
#Include Lib\hotkeys.ahk
#Include Lib\api.ahk
#Include Lib\clipboard.ahk
#Include Lib\action_run.ahk
#Include Lib\ui_menu.ahk
#Include Lib\ui_result.ahk
#Include Lib\ui_settings.ahk
#Include Lib\recommend.ahk
#Include Lib\ui_actions_editor.ahk
#Include Lib\history.ahk
#Include Lib\prices.ahk
#Include Lib\markdown.ahk
