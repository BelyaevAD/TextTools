; TextTools - вкладка API окна настроек. Подключается из TextTools.ahk.

SettingsApiTab(sg) {
    sg.Add("Text", "x25 y44 w590", Loc("set.baseurl", "Base URL (OpenAI-совместимый API):"))
    eUrl := sg.Add("Edit", "x25 y64 w590 h24 -Multi", cfg.base_url)
    sg.Add("Text", "x25 y96 w590", Loc("set.apikey", "API key:"))
    eKey := sg.Add("Edit", "x25 y116 w590 h24 -Multi Password", cfg.api_key)
    sg.Add("Text", "x25 y148 w590", Loc("set.model", "Модель по умолчанию:"))
    eModel := sg.Add("Edit", "x25 y168 w590 h24 -Multi", cfg.model)
    sg.Add("Text", "x25 y200 w590",
        Loc("set.models", "Модели, доступные действиям (отмеченные видны в карточке действия):"))
    lvM := sg.Add("ListView", "x25 y220 w590 h100 Checked -Multi",
        [Loc("set.col.model", "Модель"), Loc("set.col.ctx", "Контекст"),
         Loc("set.col.in", "$/1M вх"), Loc("set.col.out", "$/1M вых")])
    lvM.ModifyCol(1, 300), lvM.ModifyCol(2, 90), lvM.ModifyCol(3, 80), lvM.ModifyCol(4, 85)
    bLoad := sg.Add("Button", "x25 y328 w200 h28", Loc("set.load", "Загрузить у провайдера"))
    bRecommend := sg.Add("Button", "x233 y328 w210 h28",
        Loc("set.recommend", "Рекомендуемые модели"))
    bTest := sg.Add("Button", "x451 y328 w164 h28", Loc("set.test", "Тест API"))
    bDefault := sg.Add("Button", "x25 y360 w240 h28",
        Loc("set.make_default", "Сделать моделью по умолчанию"))
    bRetarget := sg.Add("Button", "x273 y360 w342 h28",
        Loc("set.retarget", "Перенастроить действия под провайдера"))
    sg.SetFont("s9 c707070")
    sg.Add("Text", "x25 y396 w590",
        Loc("set.models.hint", "Контекст и цены — каталог OpenRouter. Двойной клик по строке тоже делает модель основной."))
    sg.SetFont("s10")
    return {eUrl: eUrl, eKey: eKey, eModel: eModel, lvM: lvM, bLoad: bLoad,
        bRecommend: bRecommend, bTest: bTest, bDefault: bDefault, bRetarget: bRetarget}
}
