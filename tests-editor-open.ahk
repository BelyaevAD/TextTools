#Requires AutoHotkey v2.0
#SingleInstance Off
; Регрессионная проверка: редактор действительно открывается и все три вкладки
; заполнены. Именно этого не хватило, когда локальная apps затенила глобальный APPS.
; Отчет пишется в файл, а не в stdout: AHK - GUI-процесс, и запись в
; перенаправленный поток подвешивает его наглухо при запуске из скрипта.
REPORT := A_Temp "\tt_eo_report.txt"
try FileDelete(REPORT)
OnError((e, mode) => (FileAppend("ОШИБКА: " e.Message " | " e.Extra " @ " e.File ":" e.Line "`n", REPORT, "UTF-8"), ExitApp(9)))
LOGF := A_Temp "\tt_eo.log"
INI := A_Temp "\tt_eo_cfg.ini"
ACTIONS_INI := A_Temp "\tt_eo_actions.ini"
APPS_INI := A_Temp "\tt_eo_apps.ini"
USAGEF := A_Temp "\tt_eo_usage.log"
PRICESF := A_Temp "\tt_eo_prices.json"
HISTF := A_Temp "\tt_eo_history.txt"
LANGS_INI := A_Temp "\tt_eo_langs.ini"
MAX_ACTIONS := 200
try FileDelete(ACTIONS_INI)
try FileDelete(APPS_INI)

#Include Lib/prompts.ahk
#Include Lib/actions.ahk
#Include Lib/apps.ahk
#Include Lib/langs.ahk

cfg := {base_url: "http://127.0.0.1:9", api_key: "", model: "test",
        models: "модель-подешевле, модель-подороже", models_known: "", ui_lang: "ru", hotkey: "^Space",
        hotkey_repeat: "^+Space", markdown: "1"}
gEditorGui := "", gMenuEdit := "", gMenuFirstBtn := "", gMenuGui := "", gBusy := false
gBusyGui := "", gBusyText := "", gBusyInfo := "", gBusyDetail := "", gBusyStart := 0
gHistory := [], gPrevWin := 0, gForeWin := 0, gSel := "", gSettingsGui := ""
gLastUsage := "", gCallLabel := "", gLastAction := "", gLastInstr := "", gRestoreTimer := ""
gHotkeyCurrent := "", gHotkeyRepeatCurrent := "", gUiStrings := Map()
try FileDelete(LANGS_INI)
LANGS := LoadLangs()
sets := LoadActionSets()
ACTIONS := sets.actions
QUICK_REFINE := sets.quick
APPS := LoadApps()
Say(s) => FileAppend(s "`n", REPORT, "UTF-8")

Say("наборы загружены: действий " ACTIONS.Length ", уточнений " QUICK_REFINE.Length ", приложений " APPS.Length)
ShowActionsEditor()
if !(IsObject(gEditorGui)) {
    Say("FAIL окно редактора не создано")
    ExitApp(1)
}
Say("OK   редактор открылся без ошибок")
lists := []
for c in gEditorGui
    if (c.Type = "ListView")
        lists.Push(c.GetCount())
Say(lists.Length = 3 ? "OK   три вкладки: строк " lists[1] " / " lists[2] " / " lists[3]
    : "FAIL списков " lists.Length)
Say(lists.Length = 3 && lists[1] = ACTIONS.Length && lists[2] = QUICK_REFINE.Length && lists[3] = APPS.Length
    ? "OK   все три списка заполнены данными" : "FAIL наполнение")
gEditorGui.GetPos(, , &w, &h)
over := 0
for c in gEditorGui {
    c.GetPos(&cx, &cy, &cw, &ch)
    if (cx + cw > w - 8 || cy + ch > h - 8)
        over++
}
Say(over = 0 ? "OK   контролы не выходят за край (окно " w "x" h ")" : "FAIL за краем: " over)

; карточка действия и карточка приложения тоже должны открываться
EditActionDialog(gEditorGui, ACTIONS[1], ACTIONS, 1, (*) => 0)
Say("OK   карточка действия открылась")
EditAppDialog(gEditorGui, APPS[1], (*) => 0)
Say("OK   карточка приложения открылась")

; Окно настроек: его отсутствие в тесте и пропустило дефект, когда локальная
; modelRows затенила функцию ModelRows() и настройки перестали открываться.
ShowSettings()
if !IsObject(gSettingsGui) {
    Say("FAIL окно настроек не создано")
    ExitApp(1)
}
Say("OK   настройки открылись без ошибок")
setLists := [], setTabs := 0
for c in gSettingsGui {
    if (c.Type = "ListView")
        setLists.Push(c.GetCount())
    if (InStr(c.Type, "Tab") = 1)
        setTabs++
}
Say(setTabs = 1 ? "OK   вкладки настроек созданы" : "FAIL вкладок " setTabs)
Say(setLists.Length = 2 ? "OK   списки моделей и языков: строк " setLists[1] " / " setLists[2]
    : "FAIL списков в настройках " setLists.Length)
Say(setLists.Length = 2 && setLists[2] = LANGS.Length
    ? "OK   список языков заполнен" : "FAIL языков в списке")

; Кнопки на вкладках обязаны помещаться в саму вкладку: уехавшую за нижний край
; «Загрузить у провайдера» пользователь просто не видел и решил, что функции нет.
tx := 0, ty := 0, tw := 0, th := 0
for c in gSettingsGui
    if (InStr(c.Type, "Tab") = 1)
        c.GetPos(&tx, &ty, &tw, &th)
clipped := 0, onPage := 0
for c in gSettingsGui {
    if (c.Type != "Button")
        continue
    c.GetPos(&bx, &by, &bw, &bh)
    if (by >= ty + th)                       ; кнопки под вкладкой (Сохранить/Отмена)
        continue
    onPage++
    if (by + bh > ty + th - 4 || bx + bw > tx + tw - 4)
        clipped++
}
Say(clipped = 0 ? "OK   кнопки вкладок помещаются (" onPage " шт., вкладка " tw "x" th ")"
    : "FAIL кнопок за краем вкладки: " clipped " из " onPage)
; 6 кнопок внутри вкладок: «Загрузить у провайдера», «Рекомендуемые модели»,
; «Тест API», «Сделать моделью по умолчанию» на API и «Добавить свой…» +
; «Удалить» на языках. Ручного добавления и удаления моделей нет намеренно -
; список приходит от провайдера целиком.
Say(onPage = 6 ? "OK   кнопки вкладок ровно те, что нужны (" onPage ")"
    : "FAIL кнопок на вкладках всего " onPage)
try FileDelete(ACTIONS_INI)
try FileDelete(APPS_INI)
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
; набор включений держим полным, как в самой программе: неполный набор даёт
; ошибку загрузки на первой же ссылке в другой файл, и тест молча зависает
#Include Lib/hotkeys.ahk
#Include Lib/ui_menu.ahk
#Include Lib/action_run.ahk
#Include Lib/clipboard.ahk
#Include Lib/ui_settings.ahk
#Include Lib/recommend.ahk
#Include Lib/ui_actions_editor.ahk
