; TextTools - вкладка API окна настроек. Подключается из TextTools.ahk.
;
; Координаты абсолютные, а не накопительные (y+N): Tab3 обрезает содержимое, и
; уехавшая за край кнопка просто не видна - так однажды потерялась «Загрузить у
; провайдера». Нижний край содержимого - 536 при клиентской границе вкладки ~567.

SettingsApiTab(sg) {
    sg.Add("Text", "x25 y44 w590",
        Loc("set.baseurl", "Base URL (OpenAI-совместимый API; выберите из списка или впишите свой):"))
    ; ComboBox, а не DropDownList: адрес берут из списка популярных, но любой
    ; свой endpoint (локальный прокси, самохост) остаётся вписываемым руками.
    ; Читать значение надо через .Text - .Value у ComboBox это номер строки.
    urlItems := []
    for p in ProviderUrls()
        urlItems.Push(p.url)
    eUrl := sg.Add("ComboBox", "x25 y62 w590 r10", urlItems)
    eUrl.Text := cfg.base_url
    sg.Add("Text", "x25 y92 w590", Loc("set.apikey", "API key:"))
    eKey := sg.Add("Edit", "x25 y110 w590 h24 -Multi Password", cfg.api_key)
    proxyOn := (CfgFlag(cfg, "proxy_on", "") = "1")
        || (CfgFlag(cfg, "proxy_on", "") = "" && Trim(CfgFlag(cfg, "proxy", "")) != "")
    cbProxy := sg.Add("CheckBox", "x25 y142 w100 h24" (proxyOn ? " Checked" : ""),
        Loc("set.proxy", "Прокси"))
    eProxy := sg.Add("Edit", "x130 y142 w485 h24 -Multi", CfgFlag(cfg, "proxy", ""))
    eProxy.Enabled := proxyOn
    sg.SetFont("s9 c707070")
    sg.Add("Text", "x130 y174 w485 h20",
        Loc("set.proxy.hint", "http://user:pass@host:port  ·  socks5h://host:port"))
    sg.SetFont("s10")
    sg.Add("Text", "x25 y202 w590 h20", Loc("set.model", "Модель по умолчанию:"))
    eModel := sg.Add("Edit", "x25 y222 w590 h24 -Multi", cfg.model)
    sg.Add("Text", "x25 y254 w590 h20",
        Loc("set.models", "Модели, доступные действиям (отмеченные видны в карточке действия):"))
    ; NoSort: встроенная сортировка по клику на заголовок переставляет СТРОКИ, не
    ; трогая modelRows, а они сопоставлены ПО ИНДЕКСУ (SyncChecks, DoMakeDefault).
    ; Своя сортировка по числам живёт в OnColClick - событие ColClick NoSort не снимает.
    ; Высота на 8 строк: со списком в три строки выбирать модель было невозможно.
    lvM := sg.Add("ListView", "x25 y274 w590 h168 Checked -Multi NoSort",
        [Loc("set.col.model", "Модель"), Loc("set.col.ctx", "Контекст"),
         Loc("set.col.in", "$/1M вх"), Loc("set.col.out", "$/1M вых")])
    lvM.ModifyCol(1, 300), lvM.ModifyCol(2, 90), lvM.ModifyCol(3, 80), lvM.ModifyCol(4, 85)
    bLoad := sg.Add("Button", "x25 y450 w200 h28", Loc("set.load", "Загрузить у провайдера"))
    bRecommend := sg.Add("Button", "x233 y450 w210 h28",
        Loc("set.recommend", "Рекомендуемые модели"))
    bTest := sg.Add("Button", "x451 y450 w164 h28", Loc("set.test", "Тест API"))
    bDefault := sg.Add("Button", "x25 y482 w240 h28",
        Loc("set.make_default", "Сделать моделью по умолчанию"))
    bRetarget := sg.Add("Button", "x273 y482 w342 h28",
        Loc("set.retarget", "Перенастроить действия под провайдера"))
    sg.SetFont("s9 c707070")
    sg.Add("Text", "x25 y516 w590 h20",
        Loc("set.models.hint", "Контекст и цены — каталог OpenRouter. Двойной клик по строке тоже делает модель основной."))
    sg.SetFont("s10")
    return {eUrl: eUrl, eKey: eKey, cbProxy: cbProxy, eProxy: eProxy, eModel: eModel, lvM: lvM,
        bLoad: bLoad, bRecommend: bRecommend, bTest: bTest, bDefault: bDefault, bRetarget: bRetarget}
}
