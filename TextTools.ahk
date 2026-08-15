#Requires AutoHotkey v2.0
#SingleInstance Force
; ============================================================================
; TextTools - LLM actions on selected text (AutoHotkey v2)
;
; Entry point: directives, globals, autostart.
; Logic lives under Lib\ - see docs/USER_GUIDE.md.
; Homelab ops and invariants: TEXTTOOLS_RUNBOOK.md in 03-infrastructure.
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
LANGS_INI := A_ScriptDir "\langs.ini"
USAGEF := A_ScriptDir "\TextTools_usage.log"
HISTF := A_ScriptDir "\TextTools_history.txt"
PRICESF := A_ScriptDir "\prices.json"
MAX_ACTIONS := 200
gStartTick := A_TickCount

#Include Lib\prompts.ahk
#Include Lib\actions.ahk
#Include Lib\apps.ahk
#Include Lib\langs.ahk

; ---------------------------------------------------------------------------
; Запуск
; ---------------------------------------------------------------------------

OnError ErrorLogger
OnExit ExitLogger
CleanupTextToolsTemp()

cfg := LoadCfg()
gUiStrings := LoadUiLang(cfg.ui_lang)
LANGS := LoadLangs()
sets := LoadActionSets()
ACTIONS := sets.actions
QUICK_REFINE := sets.quick
APPS := LoadApps()
gEditorGui := ""
gSel := ""
gPrevWin := 0
gForeWin := 0
gMenuGui := ""
gSettingsGui := ""
gBusy := false
gBusyGui := ""
gBusyText := ""
gBusyInfo := ""
gBusyDetail := ""
gBusyStart := 0
gLastUsage := ""
gCallLabel := ""
gHotkeyCurrent := ""
gHotkeyRepeatCurrent := ""
gLastAction := ""
gLastInstr := ""
gHistory := LoadHistory()
gMenuEdit := ""
gMenuFirstBtn := ""
gRestoreTimer := ""
gClipWarned := false
gPriceIndex := ""

TT_Log("=== старт, PID " ProcessExist() ", хоткей " cfg.hotkey " ===")

SetupTray()
SetTimer(ForeWatch, 400)
try ApplyHotkeyPair(cfg.hotkey, cfg.hotkey_repeat)
catch as e {
    TT_Log("хоткеи из конфига не приняты (" e.Message "), ставлю умолчания")
    try ApplyHotkeyPair("^!Space", "^+Space")
}

HotIf(MenuKeysCtx)
for k in MENU_KEYS {
    Hotkey(k, MenuKeyPress.Bind(k, false))
    Hotkey("+" k, MenuKeyPress.Bind(k, true))
}
HotIf()

if (cfg.api_key = "") {
    TrayTip(Loc("tray.need_key", "Укажи API key в настройках (иконка в трее)"), "TextTools")
    ShowSettings()
} else {
    TrayTip(Format(Loc("tray.started", "Запущен. Хоткей меню: {1}"), cfg.hotkey), "TextTools")
}

#Include Lib\log.ahk
#Include Lib\i18n.ahk
#Include Lib\config.ahk
#Include Lib\langs_store.ahk
#Include Lib\http.ahk
#Include Lib\actions_store.ahk
#Include Lib\apps_store.ahk
#Include Lib\hotkeys.ahk
#Include Lib\api.ahk
#Include Lib\clipboard.ahk
#Include Lib\action_run.ahk
#Include Lib\ui_menu.ahk
#Include Lib\ui_result.ahk
#Include Lib\ui_settings.ahk
#Include Lib\ui_settings_api.ahk
#Include Lib\ui_settings_langs.ahk
#Include Lib\ui_settings_ui.ahk
#Include Lib\recommend.ahk
#Include Lib\ui_actions_editor.ahk
#Include Lib\history.ahk
#Include Lib\prices.ahk
#Include Lib\markdown.ahk
