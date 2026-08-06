; TextTools - ЗАВОДСКОЙ список приложений-источников текста.
; Подключается из TextTools.ahk, отдельно не запускается.
;
; Рабочий список лежит в apps.ini и правится через редактор (вкладка
; «Приложения»). Здесь - значения по умолчанию и цель кнопки «Сбросить».
;
; Поля записи:
;   proc  - имя процесса окна, где нажат хоткей (без учёта регистра). Пусто -
;           запись выключена
;   title - подстрока заголовка окна; пусто = любой заголовок. Записи с
;           заголовком проверяются ПЕРВЫМИ, поэтому «chrome + Jira» выигрывает
;           у общего «chrome»
;   name  - как показывать в редакторе и логе
;   desc  - описание для человека; на промпт не влияет
;   hint  - 1-2 предложения на английском, дописываются к системному промпту.
;           Пусто = обрабатывать как обычно, без поправки на приложение
;
; Factory set covers common desktop apps; users extend it in the Apps editor.

DEFAULT_APPS := [
    ; --- браузер: сначала конкретные площадки, потом общий случай ---
    {proc: "chrome.exe", title: "Jira", name: "Jira (в браузере)",
     desc: "Задачи и требования в Jira: описания, комментарии, критерии приёмки.",
     hint: "The text comes from a Jira issue: keep issue keys (like ABC-123), status names and field labels exactly as written."},
    {proc: "chrome.exe", title: "Confluence", name: "Confluence (в браузере)",
     desc: "Страницы вики: спецификации, регламенты, протоколы встреч.",
     hint: "The text comes from a Confluence page: preserve headings, lists and table structure so the result can be pasted back as a document fragment."},
    {proc: "chrome.exe", title: "Битрикс24", name: "Битрикс24 (в браузере)",
     desc: "Портал Битрикс24: задачи, CRM, комментарии, бизнес-процессы.",
     hint: "The text comes from Bitrix24: keep entity names, field labels and workflow status names exactly as written, they are part of the system's configuration."},
    {proc: "chrome.exe", title: "", name: "Google Chrome",
     desc: "Браузер: веб-страницы, документация, почта, внутренние порталы.",
     hint: "The text was selected on a web page and may contain stray interface labels or navigation fragments; ignore them and work only with the meaningful content."},

    ; --- редакторы кода и IDE ---
    {proc: "Cursor.exe", title: "", name: "Cursor",
     desc: "Редактор кода: исходники проекта, конфиги, Markdown-документация.",
     hint: "The text comes from a code editor: treat it as source code or a technical file, never translate identifiers, and keep formatting and indentation byte-for-byte."},
    {proc: "idea64.exe", title: "", name: "IntelliJ IDEA",
     desc: "IDE: код, конфигурация, комментарии.",
     hint: "The text comes from an IDE: treat it as source code, never translate identifiers, and keep formatting and indentation byte-for-byte."},
    {proc: "phpstorm64.exe", title: "", name: "PhpStorm",
     desc: "IDE для PHP: код Bitrix24-проекта, конфиги.",
     hint: "The text comes from an IDE: treat it as source code, never translate identifiers, and keep formatting and indentation byte-for-byte."},
    {proc: "pycharm64.exe", title: "", name: "PyCharm",
     desc: "IDE для Python: скрипты, конфиги.",
     hint: "The text comes from an IDE: treat it as source code, never translate identifiers, and keep formatting and indentation byte-for-byte."},

    ; --- переписка ---
    {proc: "OUTLOOK.EXE", title: "", name: "Outlook",
     desc: "Рабочая почта: письма коллегам, бизнесу, вендорам.",
     hint: "The text is business email: keep the formal register, and leave greetings, sign-offs and addressee names in place unless asked otherwise."},
    {proc: "Telegram.exe", title: "", name: "Telegram",
     desc: "Рабочие и личные чаты, обсуждения задач.",
     hint: "The text comes from a work chat: keep it short and direct in an informal but professional register, without letter-style greetings."},
    {proc: "claude.exe", title: "", name: "Claude (десктоп)",
     desc: "Диалог с ИИ-ассистентом: запросы и его ответы.",
     hint: "The text comes from a chat with an AI assistant and may already be model output; do not add meta-commentary about the conversation itself."},

    ; --- документы и таблицы ---
    {proc: "WINWORD.EXE", title: "", name: "Microsoft Word",
     desc: "Документы: регламенты, спецификации, договоры.",
     hint: "The text comes from a Word document: preserve paragraph structure, lists and numbering so the result can replace the fragment in place."},
    {proc: "EXCEL.EXE", title: "", name: "Microsoft Excel",
     desc: "Таблицы: реестры, выгрузки, расчёты.",
     hint: "The text comes from spreadsheet cells separated by tabs: keep the column structure and never alter numbers, dates or codes."},

    ; --- базы данных и API ---
    {proc: "datagrip64.exe", title: "", name: "DataGrip",
     desc: "SQL-клиент: запросы, схемы, результаты выборок.",
     hint: "The text comes from a database tool: it is SQL or query output, so never change table, column or identifier names."},
    {proc: "dbeaver.exe", title: "", name: "DBeaver",
     desc: "SQL-клиент: запросы, схемы, результаты выборок.",
     hint: "The text comes from a database tool: it is SQL or query output, so never change table, column or identifier names."},
    {proc: "heidisql.exe", title: "", name: "HeidiSQL",
     desc: "SQL-клиент для MySQL/MariaDB.",
     hint: "The text comes from a database tool: it is SQL or query output, so never change table, column or identifier names."},
    {proc: "Postman.exe", title: "", name: "Postman",
     desc: "API-клиент: запросы, заголовки, JSON-ответы.",
     hint: "The text comes from an API client: it is JSON, headers or endpoint definitions, so keep the structure syntactically valid and field names untouched."},
    {proc: "Insomnia.exe", title: "", name: "Insomnia",
     desc: "API-клиент: запросы и ответы.",
     hint: "The text comes from an API client: it is JSON, headers or endpoint definitions, so keep the structure syntactically valid and field names untouched."},

    ; --- терминалы и простой текст ---
    {proc: "WindowsTerminal.exe", title: "", name: "Windows Terminal",
     desc: "Консоль: команды, вывод, логи.",
     hint: "The text comes from a terminal: it is commands, log lines or program output, so keep commands, paths and log lines byte-for-byte."},
    {proc: "putty.exe", title: "", name: "PuTTY",
     desc: "SSH-сессия: вывод команд на серверах.",
     hint: "The text comes from a terminal: it is commands, log lines or program output, so keep commands, paths and log lines byte-for-byte."},
    {proc: "notepad.exe", title: "", name: "Блокнот",
     desc: "Черновики и временные заметки.",
     hint: ""},
    {proc: "notepad++.exe", title: "", name: "Notepad++",
     desc: "Текстовый редактор: черновики, логи, конфиги.",
     hint: ""},

    ; --- прочее ---
    {proc: "drawio.exe", title: "", name: "draw.io",
     desc: "Редактор схем: подписи узлов и связей.",
     hint: "The text comes from a diagram editor: these are short node and edge labels, so keep them terse and do not turn them into sentences."},
]
