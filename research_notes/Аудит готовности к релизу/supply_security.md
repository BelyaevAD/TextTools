# Состав поставки TextTools и секреты (2026-11-13)

Дата среза: 2026-11-13. Репозиторий: `d:\workspace\02-personal\TextTools`. Сборка карты и сборка установщика не запускались. Значения токенов, паролей и ключей в эту заметку не копировались. `architecture_context` вызван один раз, без `id`, без project, repo_path и slug. Serena и Repository retrieval не вызывались.

Ответ MCP: `index: pending`, `paths: [README.md]`, `must: write-cards`, `source: local-cards`. Карточки в этом ходе не писались: задание разрешает запись только в этот файл.

## Что входит в артефакт релиза и что git игнорирует, но оставляет в дереве?

### Takeaway
Публикуемые файлы по skill релиза — свежий Windows MSI, Linux DEB и DMG только если его собрал Mac-скрипт; в git они не хранятся. Тег указывает на коммит с changelog и README, поэтому исходники тега — это отслеживаемые файлы, а не `dist/`.

### Cited Findings
- Skill требует чистую пересборку: Windows `./gradlew clean :app:packageMsi` (JDK 21, WiX 3), Linux — копия дерева в Ubuntu без `build`, `.gradle`, `dist`, `tmp`, `.git` и `./gradlew clean :app:packageDeb`, macOS — `sh packaging/macos/package-dmg.sh`. Если DMG не собирался, в заметках релиза его не ссылают. В `dist/` копируют новый msi и deb; эти файлы и DMG, только когда скрипт его создал, — единственные release assets. — [SKILL.md](file:///d:/workspace/02-personal/TextTools/.cursor/skills/texttools-release/SKILL.md)
- README: установщики собираются на своей ОС и в git не хранятся; команды `packageMsi`, `packageDmg`, `packageDeb`. Ручной workflow может собрать те же пакеты. Версия приложения в README — 1.2. — [README.md](file:///d:/workspace/02-personal/TextTools/README.md)
- `compose.desktop.application.nativeDistributions`: форматы Msi, Dmg, Deb; `packageName` TextTools; `packageVersion` 1.2.0; модули JRE `java.sql`, `java.net.http`, `java.management`, `jdk.unsupported`; иконки Windows и Linux. MSI дополнительно передаёт в jpackage `LICENSE` и URL `https://github.com/BelyaevAD/TextTools` (about, help, update). В каталог ресурсов jpackage копируются только `packaging/windows/main.wxs` и файлы `*.wxl`. В task нет копирования `.cursor/`, `.env`, `config/sentry.properties`, `sonar-project.properties`, `TextTools.ini`. — [app/build.gradle.kts](file:///d:/workspace/02-personal/TextTools/app/build.gradle.kts)
- Установщик включает JRE через jpackage. Native image и Kotlin/Native в эту линию не входят. — [docs/adr/S5-jvm-installers.md](file:///d:/workspace/02-personal/TextTools/docs/adr/S5-jvm-installers.md)
- Задачи упаковки модуля app: `packageMsi`, `packageDmg`, `packageDeb`. — [docs/modules/app.md](file:///d:/workspace/02-personal/TextTools/docs/modules/app.md)
- Changelog v1.2 (2026-11-12): загрузка этой версии — `TextTools-1.2.0.msi`. DEB и DMG названы как сборки на своих системах, без фразы «приложены к GitHub release». У v1.0 (2026-11-09) MSI и `texttools_1.0.0_amd64.deb` названы приложенными к GitHub release; DMG — сборка на Mac. — [CHANGELOG.md](file:///d:/workspace/02-personal/TextTools/CHANGELOG.md)
- Правило релиза: публиковать только файлы, которые дала пересборка; `gh-pages` (`index.html` и `en/`) — отдельный коммит, не `main`. — [.cursor/rules/texttools-release.mdc](file:///d:/workspace/02-personal/TextTools/.cursor/rules/texttools-release.mdc)
- CI `check` на push и pull_request гоняет `./gradlew check dokkaGenerate` и пакеты не собирает. Job `package` только при `workflow_dispatch`: `packageMsi` / `packageDmg` / `packageDeb` с `actions/checkout@v4` и upload `app/build/compose/binaries/**/*`. Это шире одного файла установщика: в artifact Actions попадает всё дерево binaries. — [.github/workflows/ci.yml](file:///d:/workspace/02-personal/TextTools/.github/workflows/ci.yml)
- `.gitignore` игнорирует `.gradle/`, `build/`, `**/build/`, `.kotlin/`, `*.db`, `texttools.db`, `.idea/`, `*.iml`, `.vscode/`, `.cursor/*` (кроме механизма rules/skills ниже), `config/sentry.properties`, `tmp/`, `.scannerwork/`, `sonar-project.properties`, `dist/`. — [.gitignore](file:///d:/workspace/02-personal/TextTools/.gitignore)
- Исключения `.cursor`: игнорируется содержимое `.cursor`, затем снова игнорируется всё в `.cursor/rules/`, кроме `.cursor/rules/texttools-release.mdc`; `.cursor/skills/**` из игнора вынут. Skill прямо говорит держать `.cursor/` игнорируемым, кроме этого skill и `texttools-release.mdc`. — [.gitignore](file:///d:/workspace/02-personal/TextTools/.gitignore); [SKILL.md](file:///d:/workspace/02-personal/TextTools/.cursor/skills/texttools-release/SKILL.md)
- `git ls-files -- .cursor` на 2026-11-13 показал ровно два пути: `.cursor/rules/texttools-release.mdc` и `.cursor/skills/texttools-release/SKILL.md`.
- `git check-ignore -v` подтвердил игнор: `.cursor/mcp.json` (правило `.cursor/*`), `config/sentry.properties`, `sonar-project.properties`, `dist/`, `.gradle/`.
- На диске есть каталоги `dist/`, `.gradle/`, `build/`. В git они не входят. Содержимое `dist/` не перечислялось.
- В `.gitignore` нет шаблонов `TextTools.ini`, `.env`, `.env.*`. Файлов `.env`, `.env.example`, `.env.local`, `TextTools.ini` на диске нет (`Test-Path` = False).
- Отслеживаемые примеры конфигурации: `TextTools.ini.example`, `config/sentry.properties.example`. `gradle.properties` тоже отслеживается и содержит только флаги JVM, parallel, стиль Kotlin и режим Dokka. — [gradle.properties](file:///d:/workspace/02-personal/TextTools/gradle.properties)

### Inferences
- Артефакт, который skill считает релизом, — бинарный установщик, а не архив исходников. Тег всё равно публикует снимок отслеживаемых файлов: GitHub отдаёт коммит тега, даже если в текст релиза вписан только MSI.
- Job `package` выкладывает каталог `app/build/compose/binaries/**/*`, то есть образ приложения и установщик вместе. Это artifact Actions по ручному запуску, не список `dist/` из skill.
- Копия дерева для Ubuntu по skill выкидывает только `build`, `.gradle`, `dist`, `tmp`, `.git`. Локальные `.cursor/mcp.json` и `config/sentry.properties` в этот список исключений не входят и попали бы на сборочную машину вместе с копией. В inputs `packageDeb` эти пути не указаны.

### Gaps
- Фактический список файлов на GitHub Release для тега v1.2 не запрашивался. Неизвестно, приложен ли к нему автоматический source zip и есть ли там DEB или DMG.
- Точный список файлов внутри уже собранного MSI/DEB/DMG и внутри `dist/` не снимался: сборка не запускалась.
- Каталог `gh-pages` и снимки `docs/assets/screenshots/en/` на видимые ключи, пароль прокси и DSN не просматривались. Skill и правило релиза запрещают публиковать такой кадр, но проверка пикселей не делалась. — [SKILL.md](file:///d:/workspace/02-personal/TextTools/.cursor/skills/texttools-release/SKILL.md)

## Отслеживаются ли в git учётные данные, токены, .env и сырой Bearer?

### Takeaway
В индексе git сырого Bearer, непустого API-ключа и непустого DSN нет. Оба живых секрета лежат в существующих локальных файлах, которые gitignore уже исключает: `.cursor/mcp.json` и `config/sentry.properties`.

### Cited Findings
- `.cursor/mcp.json` — на диске: да. В git: нет. `git ls-files` его не показал, `git log -- .cursor/mcp.json` пуст, `git check-ignore` сработал на `.cursor/*`. В файле два сервера MCP. У Architecture заголовок `Authorization` — сырой Bearer, не подстановка `${env:...}` и не пустая заглушка. У Repository retrieval в URL есть непрозрачный сегмент пути. Значения не записывались. Вид секрета: сырой Bearer; отдельно — непрозрачный идентификатор в URL MCP.
- `config/sentry.properties` — на диске: да. В git: нет (gitignore). Ключ `dsn`: значение непустое и не совпало с пустым или заглушечным шаблоном. Вид секрета: DSN Sentry. Ключи `environment` и `traces-sample-rate` тоже непустые; их значения не читались, к секретам не относились. Сетевая проверка DSN не делалась.
- `config/sentry.properties.example` — на диске: да. В git: да. `dsn` пустой, `environment` пустой, `traces-sample-rate` равен 0. Комментарий: приложение DSN не поставляет. — [config/sentry.properties.example](file:///d:/workspace/02-personal/TextTools/config/sentry.properties.example)
- `.env`, `.env.example`, `.env.local` — на диске: нет. В git: нет. Вида секрета нет.
- `TextTools.ini` — на диске: нет. В git: нет. Шаблона в `.gitignore` нет.
- `TextTools.ini.example` — на диске: да. В git: да. `api_key` пустой, `proxy` пустой. `base_url` — публичный адрес OpenRouter `https://openrouter.ai/api/v1`. В комментарии показана форма прокси `user:pass`, без заполненного пароля. Файл относится к раскладке рядом с `TextTools.ahk`. — [TextTools.ini.example](file:///d:/workspace/02-personal/TextTools/TextTools.ini.example)
- `sonar-project.properties` — на диске: да. В git: нет (gitignore). Среди ключей есть `sonar.projectKey`, `sonar.projectName`, `sonar.projectVersion`, `sonar.host.url`, кодировка, sources, tests, exclusions. Ключей токена, логина или пароля в файле нет. `sonar.host.url` непустой; значение не записывалось. Вид: локальные настройки сканера, не API-токен. Приватность URL: unknown.
- `gradle.properties` — в git: да. Секретов в ключах нет. — [gradle.properties](file:///d:/workspace/02-personal/TextTools/gradle.properties)
- `git grep` по отслеживаемым файлам с шаблоном длинного Bearer, приватного ключа, `AKIA`, `ghp_` и `github_pat_` дал один путь: `app/src/test/kotlin/dev/belyaevad/texttools/app/SentrySetupTest.kt`. Это тест затирания: в строке фиктивный Bearer и фиктивный userinfo прокси на `proxy.example`, ожидание — заменённый текст. Вид: синтетическая фикстура, не живой ключ. Отдельный тест пишет DSN `https://public@example/1`. — [SentrySetupTest.kt](file:///d:/workspace/02-personal/TextTools/app/src/test/kotlin/dev/belyaevad/texttools/app/SentrySetupTest.kt)
- Тот же `git grep` по хостам `dify.6wd.ru` и `anal.6wd.ru` в отслеживаемых файлах совпадений не дал.
- README: ключ API и прокси остаются в секретном хранилище ОС и не пишутся в репозиторий и в SQLite. DSN заполняется копией `config/sentry.properties.example` в `config/sentry.properties` или в `%LOCALAPPDATA%\TextTools\sentry.properties`; эту копию коммитить нельзя. Оба переключателя Sentry по умолчанию выключены. — [README.md](file:///d:/workspace/02-personal/TextTools/README.md)
- Код читает `sentry.properties` рядом с рабочим каталогом, затем у родителя, затем в каталоге данных. В поставку DSN не вшивается этим файлом-примером. — [SentrySetup.kt](file:///d:/workspace/02-personal/TextTools/app/src/main/kotlin/dev/belyaevad/texttools/app/SentrySetup.kt); [docs/dev/sentry.md](file:///d:/workspace/02-personal/TextTools/docs/dev/sentry.md)
- Зависимость `libs.sentry` есть в модуле app, то есть библиотека Sentry входит в приложение. Это SDK, не DSN. — [app/build.gradle.kts](file:///d:/workspace/02-personal/TextTools/app/build.gradle.kts)

### Inferences
- Живой Bearer и непустой DSN в текущий тег и в чистый `actions/checkout` не попадают: оба файла игнорируются, в истории пути `mcp.json` коммитов нет.
- Сканер по слову Bearer на отслеживаемом дереве увидит тест `SentrySetupTest.kt`. Это ожидаемая фикстура затирания, не второе место хранения боевого токена из `mcp.json`.
- Пустой `api_key` в отслеживаемом INI-примере и пустой `dsn` в отслеживаемом примере Sentry в поставку секрета не кладут.

### Gaps
- История всех блобов на совпадение с уже найденным Bearer не сканировалась: такой поиск вывел бы значение. Проверен путь `.cursor/mcp.json` (`git log` пуст) и текущее отслеживаемое дерево (`git grep`, одно тестовое совпадение).
- Непустое `environment` в локальном `config/sentry.properties` не классифицировано: значение не читалось.
- Живость DSN по сети не проверялась. Непустое значение может быть и нерабочей строкой, если она не попала в узкий шаблон заглушки.

## Что исключают ignore в architecture/project.yaml и .codeboardingignore?

### Takeaway
В рабочем дереве у `architecture/project.yaml` ключа `ignore` нет, каталога `.codeboarding` и файла `.codeboarding/.codeboardingignore` нет. Списка исключений карты, с которым можно сверить «что не должно попасть в карту продукта», в репозитории нет.

### Cited Findings
- Файл `architecture/project.yaml` на диске есть и целиком состоит из `slug: TextTools`, `index: ready`, `doc_roots: [README.md]`. Ключа `ignore` нет. — [architecture/project.yaml](file:///d:/workspace/02-personal/TextTools/architecture/project.yaml)
- `git ls-files` с pathspec `architecture/project.yaml` этот путь не напечатал. Файл в индексе не состоит. В `.gitignore` отдельного правила на `architecture/` нет; `git check-ignore` для него не запускался.
- `Test-Path` для `.codeboarding` и `.codeboarding/.codeboardingignore`: False. Поиск `**/.codeboardingignore` и `.codeboarding/**` дал 0 файлов.
- MCP `architecture_context` в этот же день вернул `index: pending`, `paths: [README.md]`, пустые zones и neighbors, `limited: index-pending`. Это не список ignore. Карта (`architecture_map`) не запрашивалась, сборка карты не запускалась.
- Канон репозитория (уже в правилах, не отдельный вызов): из индекса агент не берёт `vendor/`, `node_modules/`, `bitrix/`; деревья «не продукт» задаёт `ignore` в `architecture/project.yaml`; вырезание из платной карты и графа — `.codeboarding/.codeboardingignore`. Каталогов `vendor`, `node_modules`, `bitrix` в корне нет (`Test-Path` = False).
- `.gitignore` при этом исключает артефакты сборки, `dist/`, базы `*.db`, IDE, большую часть `.cursor/`, `config/sentry.properties` и `sonar-project.properties`. Эти правила — git, не карта. — [.gitignore](file:///d:/workspace/02-personal/TextTools/.gitignore)

### Inferences
- Сверять нечего: оба файла исключений карты отсутствуют, поэтому они не воспроизводят ни запрет канона на `vendor/`, `node_modules/`, `bitrix/`, ни gitignore секретов и `dist/`. Совпадение с тем, чего не должно быть в карте продукта, не выполняется, потому что списка исключений нет.
- `index: ready` в локальном yaml и `index: pending` в ответе MCP расходятся. Портал читает свой клон, не рабочее дерево; локальный yaml ещё и не в `git ls-files`. Пока этот файл не в клоне, который видит MCP, карта с него исключения не возьмёт.
- Отсутствие `vendor/`, `node_modules/`, `bitrix/` значит, что этих деревьев в продукте сейчас нет. Это не замена отсутствующему ignore.

### Gaps
- Содержимое уже сохранённой платной карты и локального графа неизвестно: `architecture_map` не вызывался, сборка не запускалась. Нельзя сказать, лежат ли там сегодня пути из gitignore.
- Попал ли `architecture/project.yaml` когда-либо в другой коммит под другим именем, не проверялось.

## Что блокер релиза, а что только гигиена?

### Takeaway
Блокера, который увёз бы живой секрет или приватный путь в MSI/DEB/DMG или в отслеживаемый коммит тега, на срезе 2026-11-13 нет. Оба живых секрета локальные и уже в gitignore. Остальное — дыры гигиены и охват карты, не содержимое установщика.

### Cited Findings
- Не блокер поставки. `.cursor/mcp.json`: сырой Bearer и непрозрачный id в URL; файл игнорируется, в индексе и в истории пути отсутствует. jpackage его не копирует. — [.gitignore](file:///d:/workspace/02-personal/TextTools/.gitignore); [app/build.gradle.kts](file:///d:/workspace/02-personal/TextTools/app/build.gradle.kts)
- Не блокер поставки. `config/sentry.properties`: непустой DSN; gitignore; в индекс не входит. В установщик DSN не вшивается, пример в git с пустым `dsn`. SDK Sentry как библиотека в приложение входит. — [config/sentry.properties.example](file:///d:/workspace/02-personal/TextTools/config/sentry.properties.example); [README.md](file:///d:/workspace/02-personal/TextTools/README.md)
- Не блокер. `sonar-project.properties`: gitignore, ключа токена нет, URL хоста не цитировался. В task упаковки файла нет.
- Не блокер. `.env`, `.env.example`, `.env.local`, `TextTools.ini` отсутствуют. Отслеживаемый `TextTools.ini.example` хранит пустой `api_key`.
- Не блокер. `SentrySetupTest.kt` отслеживается и попадёт в исходники тега. Строки в нём — фикстура затирания и пример DSN на `example`, не боевой ключ. В inputs MSI этот тест не указан. — [SentrySetupTest.kt](file:///d:/workspace/02-personal/TextTools/app/src/test/kotlin/dev/belyaevad/texttools/app/SentrySetupTest.kt)
- Гигиена, не содержимое артефакта. `.gitignore` не содержит `TextTools.ini` и `.env*`. Сейчас файлов нет, случайный коммит будущего ini или `.env` ничем не перекрыт. Комментарий в примере ini сам просит не коммитить `TextTools.ini`, потому что там будет API-ключ. — [TextTools.ini.example](file:///d:/workspace/02-personal/TextTools/TextTools.ini.example); [.gitignore](file:///d:/workspace/02-personal/TextTools/.gitignore)
- Гигиена сборочной копии, не список release assets. Исключения копирования в Ubuntu не включают `.cursor/` и `config/`. Грязная копия этого дерева унесёт локальные `mcp.json` и `sentry.properties` на машину сборки. Skill считает релизом только собранные msi, deb и при наличии dmg. — [SKILL.md](file:///d:/workspace/02-personal/TextTools/.cursor/skills/texttools-release/SKILL.md)
- Гигиена карты, не установщик. Нет `ignore` в `architecture/project.yaml` и нет `.codeboardingignore`. Секреты из карты этим срезом не вычищаются, потому что правила отсутствуют. На состав MSI это не влияет. — [architecture/project.yaml](file:///d:/workspace/02-personal/TextTools/architecture/project.yaml)
- Гигиена CI против текста skill. Ручной workflow заливает весь `app/build/compose/binaries/**/*`, skill публикует только готовые установщики в `dist/`. Checkout в CI идёт из git, локальные gitignore-файлы в этот artifact не входят. — [.github/workflows/ci.yml](file:///d:/workspace/02-personal/TextTools/.github/workflows/ci.yml)

### Inferences
- Единственный способ увезти найденные секреты в публикацию — обойти gitignore: принудительно добавить игнорируемый файл, скопировать грязное дерево и как-то упаковать эти пути, или выложить каталог сборочной машины целиком. Текущие task jpackage и чистый checkout этого не делают.
- Фикстура в `SentrySetupTest.kt` — шум для секрет-сканера на исходниках тега. В бинарный установщик она не входит по описанным inputs.

### Gaps
- Байты уже выложенного `TextTools-1.2.0.msi` и локального `dist/` не вскрывались. Нельзя подтвердить, что исторический артефакт собран с чистого дерева.
- Снимки экрана и `gh-pages` на видимый ключ, пароль прокси или DSN не проверялись.
- Приватность непустого `sonar.host.url` и непустого `environment` в локальном Sentry-файле: unknown.
