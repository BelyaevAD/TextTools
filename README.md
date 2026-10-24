# TextTools

**Язык:** Русский | [English](README.en.md)

[![License: MIT](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)
[![AutoHotkey](https://img.shields.io/badge/AutoHotkey-v2-blue.svg)](https://www.autohotkey.com/)
[![Site](https://img.shields.io/badge/site-belyaevad.github.io-0f8f73.svg)](https://belyaevad.github.io/TextTools/)

Небольшой скрипт на AutoHotkey v2. Выделяешь текст в любом окне Windows, жмёшь хоткей и выбираешь, что сделать через LLM: вычитать, переписать, сжать, собрать таблицу, разобрать кусок кода и т.д. Результат либо встаёт вместо выделения, либо открывается в отдельном окне.

**Сайт:** [belyaevad.github.io/TextTools](https://belyaevad.github.io/TextTools/) · [English](https://belyaevad.github.io/TextTools/en/)

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

| Клавиша занята |
|---|
| ![Конфликт клавиши](docs/assets/screenshots/ru/07-key-conflict.png) |

HTTP к API идёт через системный `curl.exe` — так стабильнее, чем COM/WinHTTP внутри процесса AHK.

## Что умеет

- Сетка действий с горячими клавишами и полем для своей инструкции
- Повтор последнего действия (`Ctrl+Shift+Space` по умолчанию)
- Редактор кнопок и промптов, профили «откуда текст»; занятую клавишу действия можно **обменять** или **забрать**
- Любой OpenAI-совместимый endpoint (OpenRouter, OpenAI, прокси Anthropic, локальный Ollama…)
- Опциональный HTTP/HTTPS/SOCKS-прокси (галочка на вкладке API; `http://user:pass@host:port` или `socks5h://…`)
- Модель по умолчанию и отдельно на действие
- Язык ответа на действие: 18 языков в списке, отмеченные попадают в карточку действия
- История последних 20 операций, последний результат и отчёт по расходу — из трея
- Учёт токенов и оценка стоимости: токены точные (API возвращает их в `usage`), а цены берутся из каталога OpenRouter даже когда провайдер другой — поэтому деньги это оценка, а не биллинг
- Язык интерфейса через `lang/*.ini` (русский встроен в код, `ru.ini` не нужен)

## Требования

- Windows 10/11
- [AutoHotkey v2](https://www.autohotkey.com/download/) (проверял на 2.0.26)
- Системный `curl.exe`
- API-ключ провайдера (или локальный Ollama). Адрес выбирается из выпадающего списка восьми провайдеров либо вписывается свой

## Установка

```powershell
winget install -e --id AutoHotkey.AutoHotkey
git clone https://github.com/BelyaevAD/TextTools.git
cd TextTools
powershell -ExecutionPolicy Bypass -File .\install.ps1
```

Или скачай source zip с [Releases](https://github.com/BelyaevAD/TextTools/releases), распакуй и запусти `install.ps1`.

Скрипт копирует файлы в `%LOCALAPPDATA%\Scripts\TextTools\`, вешает автозапуск и стартует приложение. В трее открой **Настройки**, укажи Base URL и API key (шаблон — [`TextTools.ini.example`](TextTools.ini.example)).

Без install: дважды щёлкни `TextTools.ahk`, если AHK v2 уже стоит (состояние всё равно пишется рядом со скриптом).

Удалить: `powershell -ExecutionPolicy Bypass -File .\uninstall.ps1` — снимает автозапуск и удаляет `%LOCALAPPDATA%\Scripts\TextTools` целиком, вместе с настройками, историей и логом. Сам AutoHotkey остаётся.

## Использование

| Действие | По умолчанию |
|---|---|
| Меню действий | `Ctrl+Alt+Space` |
| Повтор последнего | `Ctrl+Shift+Space` |
| Результат в окне (без вставки) | **Shift** + клик / клавиша действия |

Хоткеи меняются в настройках. `Ctrl+Space` часто уже занят IME или другими хуками — поэтому по умолчанию `Ctrl+Alt+Space`.

Подробнее: [docs/USER_GUIDE.md](docs/USER_GUIDE.md). Типичные сбои: [docs/TROUBLESHOOTING.md](docs/TROUBLESHOOTING.md).

## Конфиденциальность

- API-ключ только локально в `TextTools.ini` (файл в `.gitignore`)
- URL прокси с логином/паролем — тот же секрет, что и ключ; не коммитить
- Текст уходит к выбранному провайдеру, когда ты запускаешь действие
- Профилирование новых приложений (имя exe и заголовок окна) **выключено по умолчанию**. Если включить галочку в настройках, перед отправкой показывается, что уйдёт к модели
- История на диске хранит обработанные тексты — не коммить её; из трея можно очистить историю и лог. Последний результат лежит ещё и в `TextTools_last.txt`, его «Очистить историю» не удаляет

## Credits

Идея системной LLM-обработки выделенного текста навеяна [Writing Tools](https://github.com/theJayTea/WritingTools) (theJayTea). Код и тексты промптов в этом репозитории — свои.

Runtime: [AutoHotkey](https://www.autohotkey.com/) (Lexikos и сообщество).

## Лицензия

[MIT](LICENSE) — Copyright (c) 2026 BelyaevAD
