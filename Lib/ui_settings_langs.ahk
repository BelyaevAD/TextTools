; TextTools - вкладка языков ответа окна настроек. Подключается из TextTools.ahk.

SettingsLangsTab(sg) {
    sg.Add("Text", "x25 y44 w590 h20",
        Loc("set.langs.hint", "Отмеченные языки попадают в список «Язык ответа» карточки действия."))
    lvL := sg.Add("ListView", "x25 y70 w590 h318 Checked -Multi",
        [Loc("set.col.lang", "Язык"), Loc("set.col.code", "Код"), Loc("set.col.prompt", "Как называется в промпте")])
    lvL.ModifyCol(1, 200), lvL.ModifyCol(2, 80), lvL.ModifyCol(3, 280)
    bAddL := sg.Add("Button", "x25 y396 w210 h28", Loc("set.add_own", "Добавить свой…"))
    bDelL := sg.Add("Button", "x243 y396 w150 h28", Loc("btn.delete", "Удалить"))
    return {lvL: lvL, bAddL: bAddL, bDelL: bDelL}
}
