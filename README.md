# TextTools

**Язык:** Русский | [English](README.en.md)

[![License: MIT](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)
[![AutoHotkey](https://img.shields.io/badge/AutoHotkey-v2-blue.svg)](https://www.autohotkey.com/)
[![Site](https://img.shields.io/badge/site-belyaevad.github.io-0f8f73.svg)](https://belyaevad.github.io/TextTools/)

Обработка выделенного текста через LLM на Windows: выделили фрагмент → хоткей → сетка действий (вычитать, переписать, суммаризировать, таблица, разбор кода и др.) → результат заменяет выделение или открывается в окне с Markdown.

**Сайт:** [belyaevad.github.io/TextTools](https://belyaevad.github.io/TextTools/) · [English site](https://belyaevad.github.io/TextTools/en/)

## Скриншоты

| Меню действий | Настройки API |
|---|---|
| ![Сетка действий](docs/assets/screenshots/ru/01-action-menu.png) | ![Настройки API](docs/assets/screenshots/ru/02-settings-api.png) |

| Редактор действий | Редактор промпта |
|---|---|
| ![Редактор действий](docs/assets/screenshots/ru/04-actions-editor.png) | ![Редактор промпта](docs/assets/screenshots/ru/05-prompt-editor.png) |

| Интерфейс | Окно результата |
|---|---|
| ![Настройки UI](docs/assets/screenshots/ru/03-settings-ui.png) | ![Результат](docs/assets/screenshots/ru/06-result-window.png) |

Написан на **AutoHotkey v2**. HTTP к API идёт через системный `curl.exe` (стабильнее COM/WinHTTP в этом сценарии).

## Возможности

- Сетка действий с акселераторами клавиш и произвольной инструкцией
- Повтор последнего действия (`Ctrl+Shift+Space` по умолчанию)
- Редактор кнопок и промптов, профили приложений-источников
- Любой OpenAI-совместимый endpoint (OpenRouter, OpenAI, Anthropic-прокси, локальный Ollama, …)
- Модель по умолчанию и модель на уровне действия
- Учёт токенов и оценка стоимости (каталог OpenRouter)
- Локализация интерфейса пакетами `lang/*.ini`

## Требования

- Windows 10/11
- [AutoHotkey v2](https://www.autohotkey.com/download/) (проверено на 2.0.26)
- Системный `curl.exe`
- API-ключ выбранного провайдера (или локальный Ollama)

## Установка

```powershell
winget install -e --id AutoHotkey.AutoHotkey
git clone https://github.com/BelyaevAD/TextTools.git
cd TextTools
powershell -ExecutionPolicy Bypass -File .\install.ps1
```

Либо скачайте source zip с [Releases](https://github.com/BelyaevAD/TextTools/releases), распакуйте и запустите `install.ps1`.

Скрипт копирует файлы в `%LOCALAPPDATA%\Scripts\TextTools\`, регистрирует автозапуск и стартует приложение. В трее откройте **Настройки**, укажите Base URL и API key (шаблон — [`TextTools.ini.example`](TextTools.ini.example)).

Запуск без install: дважды щёлкните `TextTools.ahk` при установленном AHK v2 (состояние всё равно пишется рядом со скриптом).

## Использование

| Действие | По умолчанию |
|---|---|
| Меню действий | `Ctrl+Alt+Space` |
| Повтор последнего действия | `Ctrl+Shift+Space` |
| Результат в окне (не вставка) | **Shift** + клик / клавиша действия |

Хоткеи меняются в настройках. `Ctrl+Space` часто занят IME или другими программами с клавиатурными хуками — factory поэтому использует `Ctrl+Alt+Space`.

Подробнее: [docs/USER_GUIDE.md](docs/USER_GUIDE.md), типичные сбои: [docs/TROUBLESHOOTING.md](docs/TROUBLESHOOTING.md).

## Конфиденциальность

- API-ключ хранится только локально в `TextTools.ini` (файл в `.gitignore`)
- Текст уходит к выбранному вами провайдеру только когда вы запускаете действие
- История результатов на диске содержит обработанные тексты — не коммитьте её и чистите при работе с чувствительными данными

## Credits

Идея системной LLM-обработки выделенного текста вдохновлена проектом [Writing Tools](https://github.com/theJayTea/WritingTools) (theJayTea). Код TextTools и тексты промптов в этом репозитории — самостоятельные.

Runtime: [AutoHotkey](https://www.autohotkey.com/) (Lexikos и сообщество).

## Лицензия

[MIT](LICENSE) — Copyright (c) 2026 BelyaevAD
