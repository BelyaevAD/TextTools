#Requires AutoHotkey v2.0
#SingleInstance Off
; Capture product screenshots → docs/assets/screenshots/
; Run from repo root (no stdout redirect):
;   AutoHotkey64.exe capture-screenshots.ahk

lang := (A_Args.Length >= 1) ? StrLower(A_Args[1]) : "ru"
if (lang != "en" && lang != "ru")
    lang := "ru"
OutDir := A_ScriptDir "\docs\assets\screenshots\" lang
DirCreate(OutDir)
PsCap := A_ScriptDir "\tools\capture-window.ps1"
Report := OutDir "\_capture-report.txt"
try FileDelete(Report)
OnError((e, mode) => (FileAppend("FAIL " e.Message " | " e.Extra " @ " e.File ":" e.Line "`n", Report, "UTF-8"), ExitApp(9)))

LOGF := A_Temp "\tt_shot.log"
INI := A_Temp "\tt_shot.ini"
ACTIONS_INI := A_Temp "\tt_shot_actions.ini"
APPS_INI := A_Temp "\tt_shot_apps.ini"
USAGEF := A_Temp "\tt_shot_usage.log"
PRICESF := A_Temp "\tt_shot_prices.json"
HISTF := A_Temp "\tt_shot_history.txt"
LANGS_INI := A_Temp "\tt_shot_langs.ini"
MAX_ACTIONS := 200
try FileDelete(ACTIONS_INI)
try FileDelete(APPS_INI)
try FileDelete(LANGS_INI)
try FileAppend("{`"data`":[]}", PRICESF, "UTF-8")

#Include Lib/prompts.ahk
#Include Lib/actions.ahk
#Include Lib/apps.ahk
#Include Lib/langs.ahk

cfg := {
    base_url: "https://openrouter.ai/api/v1",
    api_key: "sk-or-v1-demo-key-not-real",
    model: "anthropic/claude-haiku-4.5",
    models: "anthropic/claude-haiku-4.5, openai/gpt-4.1-mini, google/gemini-2.5-flash",
    models_known: "anthropic/claude-haiku-4.5, openai/gpt-4.1-mini, google/gemini-2.5-flash, openai/gpt-4.1",
    ui_lang: lang,
    hotkey: "^!Space",
    hotkey_repeat: "^+Space",
    markdown: "0"
}
gEditorGui := "", gMenuEdit := "", gMenuFirstBtn := "", gMenuGui := "", gBusy := false
gBusyGui := "", gBusyText := "", gBusyInfo := "", gBusyDetail := "", gBusyStart := 0
gHistory := [], gPrevWin := 0, gForeWin := 0
gSel := "The quick brown fox jumps over the lazy dog.`r`nWe need clearer requirements for the auth flow."
gSettingsGui := ""
gLastUsage := "", gCallLabel := "", gLastAction := "", gLastInstr := "", gRestoreTimer := ""
gHotkeyCurrent := "", gHotkeyRepeatCurrent := "", gUiStrings := Map()
gStartTick := A_TickCount

LANGS := LoadLangs()
sets := LoadActionSets()
ACTIONS := sets.actions
QUICK_REFINE := sets.quick
APPS := LoadApps()

Say(s) => FileAppend(s "`n", Report, "UTF-8")

Shot(hwnd, name) {
    global OutDir, PsCap
    path := OutDir "\" name ".png"
    try FileDelete(path)
    try WinActivate("ahk_id " hwnd)
    Sleep 300
    cmd := Format(
        'powershell.exe -NoProfile -ExecutionPolicy Bypass -File "{1}" -Hwnd {2} -Path "{3}"',
        PsCap, Integer(hwnd), path)
    exitCode := RunWait(cmd, OutDir, "Hide")
    if (exitCode != 0 || !FileExist(path))
        throw Error("capture failed: " name " exit=" exitCode)
    Say("OK  " name ".png  " FileGetSize(path) " bytes")
}

CenterGui(g) {
    g.GetPos(, , &w, &h)
    g.Move(Max(40, (A_ScreenWidth - w) // 2), Max(40, (A_ScreenHeight - h) // 2))
}

gUiStrings := LoadUiLang(lang)
Say("start lang=" lang " strings=" gUiStrings.Count " ACTIONS=" ACTIONS.Length)

gMenuGui := BuildMenu()
try gMenuEdit.Value := "Make it shorter and keep the technical meaning"
CenterGui(gMenuGui)
gMenuGui.Show()
if gMenuFirstBtn
    try gMenuFirstBtn.Focus()
Sleep 450
Shot(gMenuGui.Hwnd, "01-action-menu")
MenuClose()

ShowSettings()
Sleep 500
Shot(gSettingsGui.Hwnd, "02-settings-api")
for c in gSettingsGui {
    if (InStr(c.Type, "Tab") = 1) {
        c.Value := 3
        break
    }
}
Sleep 350
Shot(gSettingsGui.Hwnd, "03-settings-ui")
try gSettingsGui.Destroy()
gSettingsGui := ""

ShowActionsEditor()
Sleep 500
Shot(gEditorGui.Hwnd, "04-actions-editor")

EditActionDialog(gEditorGui, ACTIONS[1], ACTIONS, 1, (*) => 0)
Sleep 500
dlgHwnd := WinExist("Действие:")
if !dlgHwnd
    dlgHwnd := WinExist("Action:")
if !dlgHwnd
    throw Error("action dialog not found")
Shot(dlgHwnd, "05-prompt-editor")
try WinClose("ahk_id " dlgHwnd)
Sleep 200
try gEditorGui.Destroy()
gEditorGui := ""

sampleMd :=
(
"# Auth flow notes

- Keep password reset out of the login modal.
- Add rate limits on `/otp/verify`.
- Prefer magic-link for first-time desktop users.

| Step | Owner | Status |
| --- | --- | --- |
| Spec | Product | Done |
| API | Backend | In progress |
| UI | Frontend | Todo |
"
)
ShowResult("Summary", sampleMd, [{role: "user", content: "summarize"}, {role: "assistant", content: sampleMd}], cfg.model)
Sleep 600
resHwnd := WinExist("TextTools - Summary")
if !resHwnd
    throw Error("result window not found")
Shot(resHwnd, "06-result-window")
try WinClose("ahk_id " resHwnd)

; Close leftover GUIs so the process can exit.
DetectHiddenWindows true
for hwnd in WinGetList("ahk_class AutoHotkeyGUI ahk_pid " ProcessExist()) {
    try WinClose("ahk_id " hwnd)
}
Say("DONE")
ExitApp(0)

#Include Lib/log.ahk
#Include Lib/i18n.ahk
#Include Lib/langs_store.ahk
#Include Lib/config.ahk
#Include Lib/api.ahk
#Include Lib/prices.ahk
#Include Lib/actions_store.ahk
#Include Lib/apps_store.ahk
#Include Lib/history.ahk
#Include Lib/ui_result.ahk
#Include Lib/markdown.ahk
#Include Lib/hotkeys.ahk
#Include Lib/ui_menu.ahk
#Include Lib/action_run.ahk
#Include Lib/clipboard.ahk
#Include Lib/ui_settings.ahk
#Include Lib/recommend.ahk
#Include Lib/ui_actions_editor.ahk
