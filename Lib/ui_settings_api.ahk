; TextTools - вкладка API окна настроек. Подключается из TextTools.ahk.

SettingsApiTab(sg) {
    sg.Add("Text", "x25 y44 w590", Loc("set.baseurl", "Base URL (OpenAI-совместимый API):"))
    eUrl := sg.Add("Edit", "x25 y62 w590 h24 -Multi", cfg.base_url)
    sg.Add("Text", "x25 y90 w590", Loc("set.apikey", "API key:"))
    eKey := sg.Add("Edit", "x25 y108 w590 h24 -Multi Password", cfg.api_key)
    sg.Add("Text", "x25 y136 w590", Loc("set.proxy", "Прокси (необязательно):"))
    eProxy := sg.Add("Edit", "x25 y154 w590 h24 -Multi", CfgFlag(cfg, "proxy", ""))
    sg.SetFont("s9 c707070")
    sg.Add("Text", "x25 y180 w590",
        Loc("set.proxy.hint", "http://user:pass@host:port · https://host:port · socks5h://host:port. Пусто — напрямую. Ollama на localhost прокси не использует."))
    sg.SetFont("s10")
    sg.Add("Text", "x25 y204 w590", Loc("set.model", "Модель по умолчанию:"))
    eModel := sg.Add("Edit", "x25 y222 w590 h24 -Multi", cfg.model)
    sg.Add("Text", "x25 y250 w590",
        Loc("set.models", "Модели, доступные действиям (отмеченные видны в карточке действия):"))
    lvM := sg.Add("ListView", "x25 y268 w590 h80 Checked -Multi",
        [Loc("set.col.model", "Модель"), Loc("set.col.ctx", "Контекст"),
         Loc("set.col.in", "$/1M вх"), Loc("set.col.out", "$/1M вых")])
    lvM.ModifyCol(1, 300), lvM.ModifyCol(2, 90), lvM.ModifyCol(3, 80), lvM.ModifyCol(4, 85)
    bLoad := sg.Add("Button", "x25 y354 w200 h28", Loc("set.load", "Загрузить у провайдера"))
    bRecommend := sg.Add("Button", "x233 y354 w210 h28",
        Loc("set.recommend", "Рекомендуемые модели"))
    bTest := sg.Add("Button", "x451 y354 w164 h28", Loc("set.test", "Тест API"))
    bDefault := sg.Add("Button", "x25 y386 w240 h28",
        Loc("set.make_default", "Сделать моделью по умолчанию"))
    bRetarget := sg.Add("Button", "x273 y386 w342 h28",
        Loc("set.retarget", "Перенастроить действия под провайдера"))
    sg.SetFont("s9 c707070")
    sg.Add("Text", "x25 y420 w590",
        Loc("set.models.hint", "Контекст и цены — каталог OpenRouter. Двойной клик по строке тоже делает модель основной."))
    sg.SetFont("s10")
    return {eUrl: eUrl, eKey: eKey, eProxy: eProxy, eModel: eModel, lvM: lvM, bLoad: bLoad,
        bRecommend: bRecommend, bTest: bTest, bDefault: bDefault, bRetarget: bRetarget}
}
