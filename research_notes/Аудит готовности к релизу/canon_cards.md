# Канон и карточки TextTools на 2026-11-13

Проверено только по файлам, которые называют канон и ответ MCP, плюс два файла правил, git-состояние этих путей и снимок платной карты. Serena и Repository retrieval не вызывались. Сборка карты, локального графа и эмбеддингов не запускалась. В вызовы не передавались project, repo_path, slug и id. Токен в эту заметку не копировался.

## Что содержат architecture/project.yaml, docs/features-index.yaml и docs/module-coupling.yaml, и совпадает ли index с id и title?

### Takeaway

В рабочем дереве на 2026-11-13 `architecture/project.yaml` содержит `index: ready`, а `docs/features-index.yaml` содержит одну карточку с id `llm-chat` и title `OpenAI-compatible chat`, строкой `current` и одной журнальной строкой, в которой есть `Why`. Это совпадает с правилом «ready только когда в списке уже есть id и title». Портал при этом отвечает `index: pending`: три файла карточек не закоммичены и в `main` их нет.

### Cited Findings

- `architecture/project.yaml` целиком состоит из четырёх строк: `slug: TextTools`, `index: ready`, `doc_roots:` и единственный корень `README.md`. Ключей `zones`, `ignore`, `profile`, `features` в файле нет. — [architecture/project.yaml](d:/workspace/02-personal/TextTools/architecture/project.yaml)
- `docs/features-index.yaml` содержит ровно одну запись `features`: `id: llm-chat`, `title: OpenAI-compatible chat`, `readme: docs/modules/data.md`, `classes` из двух путей. Ключа `public` нет. — [docs/features-index.yaml](d:/workspace/02-personal/TextTools/docs/features-index.yaml)
- Классы карточки: `data/src/main/kotlin/dev/belyaevad/texttools/data/Llm.kt` и `app/src/main/kotlin/dev/belyaevad/texttools/app/AppGraph.kt`. Все три пути карточки (readme и оба класса) на диске есть: `Test-Path` вернул True. — [docs/features-index.yaml](d:/workspace/02-personal/TextTools/docs/features-index.yaml)
- Строка current, дословно: «LlmClient posts one non-streaming completion to the provider chat/completions path with a bearer header. The direct client is used unless a proxy is configured and the base URL is not loopback, in which case the proxy client is used. Both clients wait 180 seconds for response bytes. An empty body, empty content, or a non-success status fails the call. Token counts come from prompt_tokens and completion_tokens. When error reports are on, a failed call, including a socket timeout, is sent to GlitchTip.» — [docs/features-index.yaml](d:/workspace/02-personal/TextTools/docs/features-index.yaml)
- Журнальная строка одна, в двойных кавычках, и содержит `Why`: «2026-11-11 fix: Model HTTP clients wait 180 seconds to read a chat completion. Why: OkHttp's 10 second read timeout aborted a live OpenAI completion and opened TEXTTOOLS-4. Now: both clients use a 180 second read timeout, and a timeout is still reported when error reports are on.» Вида `add` нет. Строк `digest` нет. — [docs/features-index.yaml](d:/workspace/02-personal/TextTools/docs/features-index.yaml)
- `docs/module-coupling.yaml` целиком: `edges: []`. Пар `from`/`to` нет. — [docs/module-coupling.yaml](d:/workspace/02-personal/TextTools/docs/module-coupling.yaml)
- Правило карточек: `index: ready` ставится только когда в списке уже есть id и title; пока списка нет, оставлять `pending`. Готовая карточка — это id, title, строка current и журнальная строка с `Why`. Соседи пишутся в `docs/module-coupling.yaml` как рёбра `from`/`to` между id карточек. — [.cursor/rules/texttools-architecture-cards.mdc](d:/workspace/02-personal/TextTools/.cursor/rules/texttools-architecture-cards.mdc)
- `git status -sb` показывает `?? architecture/`, `?? docs/features-index.yaml`, `?? docs/module-coupling.yaml`. `git ls-files` по этим путям пуст. `git log` по ним пуст. `git check-ignore` эти три пути не пометил. Ветка: строка статуса `## main...origin/main` без счётчика ahead/behind. HEAD: `b28e67f9986510195549dfe544838fd0475843a6`, дата `2026-11-11 00:46:01 +0700`, сообщение «Place the menu and the progress window on the input window, and show token cost while a request runs.» — [git, рабочее дерево TextTools, 2026-11-13](d:/workspace/02-personal/TextTools)
- В том же `git status -sb` есть другие незакоммиченные правки продукта, среди них `M app/src/main/kotlin/dev/belyaevad/texttools/app/AppGraph.kt` и `M docs/modules/data.md`. `Llm.kt` в этом списке не было. Полный перечень грязных файлов в эту заметку не переписывался. — [git, рабочее дерево TextTools, 2026-11-13](d:/workspace/02-personal/TextTools)
- `README.md` (единственный `doc_roots`) описывает трей, хоткей, сетку действий, пятнадцать заводских действий, историю, статистику, редактор действий, настройки API, языков и интерфейса, окно результата, установщик и отчёты об ошибках. Версия в README: 1.2. — [README.md](d:/workspace/02-personal/TextTools/README.md)

### Inferences

- Локальный флаг `ready` согласован с наличием id и title. Ответ портала `pending` согласован с тем, что этих файлов нет ни в одном коммите: правило прямо говорит, что портал читает свой клон, а не рабочее дерево.
- Условие «карточка готова» для `llm-chat` в рабочем дереве выполнено: есть id, title, current и журнал с `Why`. Уплотнение журнала не требуется: не-current и не-digest строка одна, дата 2026-11-11, до 2026-11-13 два дня, порог правила — больше 7 строк за день или минимум 3 строки при возрасте старшей даты 7 дней.
- Пустой `edges: []` согласован с одной карточкой: второго id, на который можно сослаться, в индексе нет.
- Одна карточка про HTTP-клиент чата не описывает поведение из README и из сообщения HEAD (меню на окне ввода, стоимость токенов). Это дыра покрытия, а не отсутствие id/title у той записи, которая уже написана.

### Gaps

- Тело `docs/modules/data.md` и `Llm.kt` не читалось. Совпадение строки current с кодом и с текстом readme карточки — unknown.
- `docs/README.md`, на который ссылается README, не читался. Полный список модульных заметок — unknown.
- Какие из остальных грязных файлов меняют поведение так, что по правилу карточек им нужна была бы своя журнальная строка, в этом проходе не устанавливалось.

## Есть ли файлы канона и правила карточек, какие редакции они объявляют, и есть ли вторая копия канона?

### Takeaway

Оба файла на диске есть. Канон объявляет `Id: canon:TextTools` и `Edition: e09349e08db6`. Правило карточек объявляет `cards-rule: 2026-10-27b` и само говорит, что оно не канон. В `.cursor`, `architecture`, `docs` и `README.md` второй копии `canon:TextTools` нет. Оба файла правил в git не входят: `.gitignore` строка 12 игнорирует `.cursor/rules/*`. Короткая заметка MCP называет тот же id и другую редакцию `ae0e314247a1`; полного текста этой редакции в репозитории нет.

### Cited Findings

- Файл `.cursor/rules/texttools-architecture-canon.mdc` существует, `alwaysApply: true`. Строка редакции в теле: «Id: canon:TextTools. Edition: e09349e08db6.» То же издание указано в description фронтматтера. — [.cursor/rules/texttools-architecture-canon.mdc](d:/workspace/02-personal/TextTools/.cursor/rules/texttools-architecture-canon.mdc)
- В том же файле, раздел Scope: «Profile: generic. Card index: pending.» Строка MCP дословно: «MCP: выключены: Serena выключена». Строка Authorization — заглушка `Bearer <no token>`; живого токена в файле нет, и в эту заметку он не переносился. Id `llm-chat` в тексте канона нет. — [.cursor/rules/texttools-architecture-canon.mdc](d:/workspace/02-personal/TextTools/.cursor/rules/texttools-architecture-canon.mdc)
- Канон перечисляет платную карту как справочник из четырёх компонентов: 1 Application & UI Presentation; 2 Domain Models & Contracts; 3 Data Access & LLM Clients; 4 Platform & OS Integration. Дата снимка в тексте канона: `2026-11-11T00:46:01+07:00`. — [.cursor/rules/texttools-architecture-canon.mdc](d:/workspace/02-personal/TextTools/.cursor/rules/texttools-architecture-canon.mdc)
- Файл `.cursor/rules/texttools-architecture-cards.mdc` существует, `alwaysApply: true`. Первая содержательная строка правила: «English. Rule text.» Затем «cards-rule: 2026-10-27b». Файл прямо говорит: канон — отдельный файл, его не сливать сюда и его редакцию не править. — [.cursor/rules/texttools-architecture-cards.mdc](d:/workspace/02-personal/TextTools/.cursor/rules/texttools-architecture-cards.mdc)
- Каталог `.cursor/rules` содержит четыре файла: `texttools-architecture-canon.mdc`, `texttools-architecture-cards.mdc`, `texttools-kotlin.mdc`, `texttools-release.mdc`. Поиск строк `Id: canon:`, `cards-rule:`, `ae0e314247a1`, `e09349e08db6` по `.cursor`, `architecture`, `docs` и `README.md` нашёл издание `e09349e08db6` и `cards-rule: 2026-10-27b` только в двух файлах правил архитектуры. `ae0e314247a1` в этих каталогах не встретился. — [поиск по .cursor, architecture, docs, README.md, 2026-11-13](d:/workspace/02-personal/TextTools/.cursor/rules)
- `git check-ignore -v` для обоих файлов правил: `.gitignore:12:.cursor/rules/*`. `git ls-files` по ним пуст. `git log` по ним пуст. — [git check-ignore, 2026-11-13](d:/workspace/02-personal/TextTools/.gitignore)
- Заметка подключения MCP-неймспейса `project-0-TextTools-Architecture` в этой сессии говорит: идентификатор `canon:TextTools`, редакция `ae0e314247a1`, и что полный канон в эту заметку не вшит. — [project-0-TextTools-Architecture](https://anal.6wd.ru/architecture/mcp)
- Повторный `rg` по всему дереву с `--no-ignore` завершился кодом 1 и пустым выводом. Это противоречит уже найденным совпадениям в `.cursor/rules`, поэтому пустой прогон не считается доказательством отсутствия копий. — [rg, 2026-11-13](d:/workspace/02-personal/TextTools)

### Inferences

- В просмотренных каталогах репозитория конкурирующей второй копии `canon:TextTools` нет. Правило карточек само отделено от канона и редакцию `e09349e08db6` не дублирует.
- `texttools-kotlin.mdc` и `texttools-release.mdc` лежат рядом, но искомых строк канона в поиске по `.cursor` не дали. Это другие правила, не вторая редакция `canon:TextTools`. Их полный текст в этом проходе не читался.
- Расхождение `e09349e08db6` (файл на диске) и `ae0e314247a1` (короткая заметка сервера) — два объявленных номера одной id. Тело серверной редакции не скачивалось, поэтому отличие текста за пределами номера редакции — unknown. Канон требует при отличии редакции заменить сохранённую копию; эта замена не делалась, потому что аудит только читает.
- Строка канона «Card index: pending» совпадает с ответом портала и расходится с незакоммиченным `index: ready`. Правило карточек запрещает править канон ради такого флага: живой флаг — это `architecture/project.yaml` и ответ `architecture_context`.

### Gaps

- Полный текст редакции `ae0e314247a1` — unknown. Запрос `GET https://anal.6wd.ru/architecture/v1/canon` не выполнялся.
- Обход всего дерева надёжным поиском не завершён: второй `rg` дал пустой вывод вопреки уже найденным файлам. Вне `.cursor`, `architecture`, `docs` и `README.md` наличие ещё одной копии `canon:TextTools` — unknown.
- Полный текст `texttools-kotlin.mdc` и `texttools-release.mdc` не читался.

## Что вернули architecture_context, index_status и architecture_map?

### Takeaway

Снимок платной карты свежий: `freshness` равен `current`, `commits_since` равен 0, вершина `2026-11-11T00:46:01+07:00`. Фраза «Устаревшие данные» к этому снимку не относится. `architecture_context` при этом возвращает индекс карточек `pending` и `must=write-cards`. `index_status` возвращает другой индекс, граф символов, со статусом `ready`.

### Cited Findings

- `architecture_context` вызван без `id`. Ответ: `slug` `TextTools`, `id` пустой, `index` `pending`, `source` `local-cards`, `limited` `index-pending`, `must` `write-cards`, `paths` только `README.md`, `neighbors` пусто, `zones` пусто, `public` пусто, `invariants` пусто, `tests` пусто, `neighbors_truncated` false. `card_duty.done` false. В `card_duty.files` названы `architecture/project.yaml`, `docs/features-index.yaml`, `docs/module-coupling.yaml`. Текст остановки: «Ход не закончен, пока в docs/features-index.yaml нет записи features с id и title.» Та же фраза есть в файле канона. — [architecture_context](https://anal.6wd.ru/architecture/mcp); [.cursor/rules/texttools-architecture-canon.mdc](d:/workspace/02-personal/TextTools/.cursor/rules/texttools-architecture-canon.mdc)
- `index_status` вызван с `diagnostics: summary`, без аргумента project. Ответ: `project` `opt-arch-memory-repos-TextTools`, `root_path` `/opt/arch-memory/repos/TextTools`, `status` `ready`, `nodes` 1695, `edges` 7595, `indexed_at` `2026-11-11T09:33:24Z`. — [index_status](https://anal.6wd.ru/architecture/mcp)
- Тот же ответ: `parse_partial` — один файл `gradlew`, диапазон `180-180`, count 1, truncated false. `parse_unusable` count 0. `skipped` count 0. `not_indexed`: каталог `.git` (dirs_count 1); files_count 28, truncated true, files_omitted 23. Показаны пять путей с причиной `ignored-suffix`: `app/packaging/icon.ico`, `app/packaging/icon.png`, `docs/assets/screenshots/en/menu.png`, `docs/assets/screenshots/en/result.png`, `docs/assets/screenshots/en/settings-api.png`. Примечание ответа: это исключение по правилам ignore, не ошибка. — [index_status](https://anal.6wd.ru/architecture/mcp)
- Тот же ответ: `doc_links.status` `ok`, `mentions` 80, неразрешённые `ambiguous` 3, `below_bar_tier` 2, `external` 5, `graph_gap` 7, `missing` 9. `cross_repo.status` `never_run`. `watch.watched` false, причина `watcher_disabled`. Подсказка ответа упоминает повторный прогон индекса; прогон не запускался. — [index_status](https://anal.6wd.ru/architecture/mcp)
- `architecture_map` вызван без `component`, `model` и `built_at`. Активный снимок: `freshness` `current`, `stale_reason` пустой, `commits_since` 0, `snapshot_commit_at` и `tip_commit_at` оба `2026-11-11T00:46:01+07:00`, `built_at` `2026-11-11T15:19:00Z`, `incomplete` false, `failed_blocks` 0, модель `google/gemini-3.8-flash`. Компоненты: id `1` Application & UI Presentation, `2` Domain Models & Contracts, `3` Data Access & LLM Clients, `4` Platform & OS Integration. Имена совпадают со списком в файле канона. — [architecture_map](https://anal.6wd.ru/architecture/mcp); [.cursor/rules/texttools-architecture-canon.mdc](d:/workspace/02-personal/TextTools/.cursor/rules/texttools-architecture-canon.mdc)
- В том же ответе верхнее поле `relations` — пустой массив, а у активной версии поле `relations` равно 51. Вторая, неактивная версия: модель `deepseek/deepseek-v4.1-flash-1017`, `built_at` `2026-11-11T15:14:14Z`, `freshness` `untracked`, `commits_since` null, `active` false, `relations` 6. — [architecture_map](https://anal.6wd.ru/architecture/mcp)
- Дата HEAD `2026-11-11 00:46:01 +0700` совпадает с `tip_commit_at` карты. SHA в ответе карты нет. — [architecture_map](https://anal.6wd.ru/architecture/mcp); [git log -1, 2026-11-13](d:/workspace/02-personal/TextTools)

### Inferences

- `index: pending` у карточек и `status: ready` у графа символов — разные индексы. Готовый граф не закрывает остановку «в features-index нет id и title» на клоне портала.
- Карта описывает вершину `2026-11-11T00:46:01+07:00`, ту же дату, что у локального HEAD, и 0 коммитов после неё. Незакоммиченные файлы карточек в этот снимок не входят. Календарные два дня с 2026-11-11 по 2026-11-13 статусом карты не являются: статус — `current`.
- Пустой массив `relations` на верхнем уровне не читается как «связей нет»: рядом активная версия сообщает `relations: 51`. Что именно стоит за числом 51, ответ покомпонентно не раскрыл, потому что `component` не передавался.
- `indexed_at` графа `2026-11-11T09:33:24Z` позже момента HEAD `2026-11-11 00:46:01 +0700`. Граф собран по клону после этого коммита. Неотслеживаемые yaml-карточки в ответе `architecture_context` не видны, что согласуется с их отсутствием в git.

### Gaps

- Имена 23 опущенных `not_indexed` файлов — unknown: ответ summary их не перечислил. `diagnostics: full` не запрашивался.
- SHA коммита снимка карты — unknown: в ответе есть только метки времени.
- Состав 51 связи активной карты — unknown.
- Совпадает ли клон `/opt/arch-memory/repos/TextTools` с `origin/main` побайтно, помимо `commits_since: 0` и равных меток времени, — unknown.

## Какие разрывы делают релиз неготовым по канонам, а какие только расхождение документации?

### Takeaway

Неготовым релиз «по канонам» делает одно: коммит, на котором стоят `main`, `origin/main` и вершина карты, не содержит `docs/features-index.yaml` с id и title, поэтому `architecture_context` на 2026-11-13 держит `index: pending`, `must: write-cards` и `done: false`. Запись в рабочем дереве это условие для клона портала не закрывает. Остальное ниже — покрытие, пустые рёбра, расхождение номера редакции без сверки тела и частные сигналы графа.

### Cited Findings

- Остановка канона и ответа MCP одна: ход не закончен, пока в `docs/features-index.yaml` нет записи `features` с id и title. На клоне, который читает портал, `paths` равен только `README.md`, `card_duty.done` false. В рабочем дереве такая запись есть и не закоммичена. — [architecture_context](https://anal.6wd.ru/architecture/mcp); [docs/features-index.yaml](d:/workspace/02-personal/TextTools/docs/features-index.yaml); [git status, 2026-11-13](d:/workspace/02-personal/TextTools)
- Правило: пока коммит с этими файлами не дошёл до клона, `architecture_context` карточки не видит. Отдельный коммит только ради карточки правило не требует, но и не считает незакоммиченный файл видимым порталу. — [.cursor/rules/texttools-architecture-cards.mdc](d:/workspace/02-personal/TextTools/.cursor/rules/texttools-architecture-cards.mdc)
- Файлы канона и правила карточек есть на диске этой рабочей копии и помечены `alwaysApply: true`, но `.gitignore:12` прячет `.cursor/rules/*`, и в `git ls-files` их нет. — [.cursor/rules/texttools-architecture-canon.mdc](d:/workspace/02-personal/TextTools/.cursor/rules/texttools-architecture-canon.mdc); [git check-ignore, 2026-11-13](d:/workspace/02-personal/TextTools/.gitignore)
- Канон запрещает передавать `id`, пока индекс не ready и пока этот id не перечислен в тексте канона. Сейчас портал отвечает pending, а `llm-chat` в тексте редакции `e09349e08db6` не назван. — [.cursor/rules/texttools-architecture-canon.mdc](d:/workspace/02-personal/TextTools/.cursor/rules/texttools-architecture-canon.mdc); [architecture_context](https://anal.6wd.ru/architecture/mcp)
- Платная карта: `freshness` `current`, `commits_since` 0, `incomplete` false, `failed_blocks` 0, четыре компонента совпадают с текстом канона. — [architecture_map](https://anal.6wd.ru/architecture/mcp)
- Граф символов: `status` `ready`. Частичный разбор — одна строка `gradlew`. Неиндексированные картинки и `.git` помечены как исключение по правилам. `doc_links.status` `ok` при ненулевых неразрешённых счётчиках. `cross_repo` `never_run`. — [index_status](https://anal.6wd.ru/architecture/mcp)

### Inferences

- Блокер. Релиз с текущей вершины `main` не выполняет остановку карточек на том клоне, который читает портал. Чтобы портал увидел id и title, файлы `architecture/project.yaml`, `docs/features-index.yaml` и `docs/module-coupling.yaml` должны оказаться в коммите, и этот коммит должен дойти до клона. Пока этого нет, агент по канону не имеет права передавать `id`, а ответ остаётся `must=write-cards`.
- Блокер утверждения «в git-дереве релиза лежит его канон», и только его. Сами правила на диске этой копии есть и загружаются, но `origin/main` их не содержит из-за `.gitignore`. Это не замена блокера карточек: портал читает yaml-карточки, а не `.cursor/rules`.
- Не блокер релиза приложения, а незакрытая сверка канона. Номера `e09349e08db6` и `ae0e314247a1` расходятся. Пока полный текст `ae0e314247a1` не сравнен с файлом, нельзя утверждать, что сохранённая копия — единственная живая редакция, и нельзя утверждать, что тело правил уже другое.
- Не блокер, ограничение вызова. Даже после того как портал увидит `index: ready`, текущий текст канона не содержит id `llm-chat`, а правило карточек запрещает дописывать журнал в канон. Право передавать `id` этим файлом канона сегодня не дано. На остановку «есть ли в yaml id и title» это не влияет.
- Не блокер, ожидаемое расщепление флага. `index: ready` в незакоммиченном yaml верен относительно правила карточек. «Card index: pending» внутри файла канона — строка скачанной редакции; правило запрещает править канон, чтобы её переписать. Живое состояние портала на сегодня — pending.
- Не блокер, покрытие. Журнал `llm-chat` фиксирует таймаут чтения 180 секунд и содержит `Why`. Поведения из README и из сообщения HEAD отдельными карточками не записаны. Порог остановки — одна запись с id и title, и в рабочем дереве она есть. Пустой список рёбер при одной карточке правилу соседей не противоречит.
- Не блокер, сигналы графа и карты. Карта `current`. Граф `ready`. Строка `gradlew`, игнорируемые картинки, ненулевые неразрешённые doc_links при статусе `ok`, `cross_repo: never_run` и выключенный watcher в тексте канона порогом релиза не названы. Watcher и повторную сборку индекса этот аудит не включал.

### Gaps

- Нужно ли продукту по замыслу владельца иметь карточку на каждое поведение README, а не только на `llm-chat`, из прочитанных файлов не следует. Правило говорит «одна фича — одна карточка» и «новую карточку открывать только для новой фичи», но списка обязательных фич не даёт.
- Должны ли gitignored-правила входить в тег релиза, канон отдельной фразой не говорит. Факт отсутствия их в `main` установлен, требуемость для установщика — unknown.
- Отличие тела редакции `ae0e314247a1` от файла `e09349e08db6` — unknown.
