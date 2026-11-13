# Ворота качества TextTools на 2026-11-13

Аудит только читал дерево и запускал уже установленные проверки. Код продукта не менялся, пакеты не ставились, коммит не создавался. Хост: Windows. `JAVA_HOME` указывает на Temurin 21, а `java` в `PATH` — это Java 8. Карточка архитектуры на индекс не готова и ворота сама не перечисляет.

Соседние заметки этого каталога — `release_procedure.md`, `canon_cards.md` и `supply_security.md`. Этот файл их не заменяет: там процедура, карточки и состав поставки, здесь фактический прогон ворот.

## Какие команды README, навык релиза, скрипты упаковки и CI называют обязательными перед релизом?

### Takeaway

Обязательный автоматический прогон на push и pull request — это `./gradlew check dokkaGenerate --no-daemon` на Windows, macOS и Ubuntu. Навык релиза этот прогон не называет: перед публикацией он требует чистую пересборку MSI, DEB и, на Mac, DMG. Отдельного `package.json` нет.

### Cited Findings

- README в разделе Check and run называет `./gradlew check` и `./gradlew :app:run`. На Windows указано брать `gradlew.bat`. Требование к JDK — 21. — [README.md](file:///D:/workspace/02-personal/TextTools/README.md)
- README в разделе Installers называет `./gradlew :app:packageMsi`, `./gradlew :app:packageDmg` и `./gradlew :app:packageDeb`. Пакеты собираются на своей ОС и в git не хранятся. Тот же раздел говорит, что workflow check может собрать эти пакеты, если его запустить вручную, а на Mac DMG собирает `sh packaging/macos/package-dmg.sh` из корня репозитория. — [README.md](file:///D:/workspace/02-personal/TextTools/README.md)
- Навык релиза требует снести прошлый вывод и собрать пакеты заново. Windows, из корня, JDK 21 и WiX 3 в `PATH`: `./gradlew clean :app:packageMsi`. Подробный лог должен показать отдельные вызовы light по культурам и не должен содержать `LGHT0311` или `LGHT0100`. Linux: копия дерева в Ubuntu и `./gradlew clean :app:packageDeb`. macOS: `sh packaging/macos/package-dmg.sh`. Навык пишет, что в релиз кладут только что собранные msi и deb, а DMG — только если скрипт Mac его произвёл. Команд `check`, `test`, detekt, ktlint и dokka в навыке нет. — [SKILL.md](file:///D:/workspace/02-personal/TextTools/.cursor/skills/texttools-release/SKILL.md)
- Правило `.cursor/rules/texttools-release.mdc` повторяет ту же чистую пересборку: `./gradlew clean :app:packageMsi` на Windows, `./gradlew clean :app:packageDeb` из свежего дерева в Ubuntu и скрипт Mac для DMG. Публиковать только файлы этой пересборки. — [texttools-release.mdc](file:///D:/workspace/02-personal/TextTools/.cursor/rules/texttools-release.mdc)
- Workflow называется `check`. Он запускается на `push`, `pull_request` и `workflow_dispatch`. Job `test` идёт матрицей `windows-latest`, `macos-latest`, `ubuntu-latest` и выполняет `./gradlew check dokkaGenerate --no-daemon`. Job `package` стоит под `if: github.event_name == 'workflow_dispatch'` и по ОС вызывает `packageMsi`, `packageDmg` или `packageDeb`. — [ci.yml](file:///D:/workspace/02-personal/TextTools/.github/workflows/ci.yml)
- Файла `package.json` в корне нет (`Test-Path .\package.json` дал False). Упаковка — это задачи Compose Desktop и shell-скрипт, не npm-скрипты. `packageVersion` в Gradle равен `1.2.0`. — [app/build.gradle.kts](file:///D:/workspace/02-personal/TextTools/app/build.gradle.kts)
- `check` в этом репозитории тянет тесты JUnit Platform, detekt и ktlint. Корневой `build.gradle.kts` подключает detekt ко всем подпроектам и ktlint ко всем, кроме `data`. Для ktlint задана версия инструмента `1.5.0`. В каталоге плагинов: ktlint Gradle `12.1.2`, detekt `2.0.0-alpha.1`, dokka `2.2.0`. — [build.gradle.kts](file:///D:/workspace/02-personal/TextTools/build.gradle.kts), [libs.versions.toml](file:///D:/workspace/02-personal/TextTools/gradle/libs.versions.toml)
- Комментарий в корневом Gradle говорит, почему `data` без ktlint: ktlint-gradle 12.1.2 проверяет вывод SQLDelight в `data/build` даже с фильтром только по `src`. — [build.gradle.kts](file:///D:/workspace/02-personal/TextTools/build.gradle.kts)
- Скрипт DMG сам выходит с кодом 1, если `uname -s` не `Darwin`, если нет `gradlew` или `java`, либо если первая строка `java -version` не содержит `"21`. Дальше он вызывает `./gradlew :app:packageDmg --no-daemon`. Первая строка комментария скрипта: «Builds the unsigned TextTools 1.0 DMG». — [package-dmg.sh](file:///D:/workspace/02-personal/TextTools/packaging/macos/package-dmg.sh)
- Текущая запись changelog: `## v1.2 — 2026-11-12`. Для этой версии назван установщик `TextTools-1.2.0.msi`, а DEB и DMG описаны как собранные на своих системах теми же `packageDeb` и `package-dmg.sh`. — [CHANGELOG.md](file:///D:/workspace/02-personal/TextTools/CHANGELOG.md)
- `architecture_context` без id вернул `index: pending`, `tests: []`, `paths: ["README.md"]`, `must: write-cards`. Это ответ портала, не список тестов репозитория. — вызов MCP `architecture_context` 2026-11-13

### Inferences

- Перед релизом источники расходятся. CI на каждый push требует `check` и `dokkaGenerate` на трёх ОС. Навык релиза и правило cursor требуют другую тройку: чистые `packageMsi`, `packageDeb` и скрипт DMG. README называет и `check`, и `:app:run`, и три упаковки.
- Зелёный push сам по себе не есть свидетельство установщика: job `package` в CI не входит в push и pull request.
- Навык, выполненный буквально, не заставляет заново запускать тесты. Их держит CI, а не текст навыка.

### Gaps

- Не читался журнал запусков GitHub. Свежий результат удалённой матрицы на 2026-11-13 неизвестен.
- Содержимое незакоммиченных `docs/features-index.yaml` и `architecture/` не читалось. Портал по-прежнему видит индекс как pending.

## Какие из этих команд выполнены, с какими кодами выхода и строками ошибок?

### Takeaway

На этой машине дважды выполнен Windows-эквивалент команды CI `check dokkaGenerate`: оба раза код выхода 0, строк `FAILED` не было. Свежий прогон — второй, с `--rerun-tasks`: 128 тестов, 0 падений. Сборки MSI и DEB не запускались. Скрипт DMG через Git Bash завершился кодом 1 и текстом `This script runs on macOS.`

### Cited Findings

- `java` в `PATH`: `C:\Program Files (x86)\Common Files\Oracle\Java\java8path\java.exe`, версия `1.8.0_503`. `JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot\`. Процесс Gradle напечатал `openjdk version "21.0.12.1" 2026-08-18 LTS`, Temurin. В логе демона указан Gradle 8.14.3. Команда `gradle` в `PATH` отсутствует, использовался `gradlew.bat`. — прогон 2026-11-13 в `D:\workspace\02-personal\TextTools`
- `candle` и `light` есть: `C:\Users\belya\AppData\Local\WiX314\candle.exe` и `light.exe`. `csc` в `PATH` нет. Файл `%WINDIR%\Microsoft.NET\Framework64\v4.0.30319\csc.exe` существует. Строка версии WiX не снималась. — прогон 2026-11-13
- Первый запуск: `.\gradlew.bat check dokkaGenerate --offline --no-daemon --console=plain`. Код выхода 0. Последние строки: `BUILD SUCCESSFUL in 21s` и `53 actionable tasks: 5 executed, 48 up-to-date`. Заново выполнились только задачи ресурсов Compose приложения: `convertXmlValueResourcesForMain`, `prepareComposeResourcesTaskForMain`, `generateResourceAccessorsForMain`, `generateActualResourceCollectorsForMain`, `generateComposeResClass`. Задачи `:domain:test`, `:data:test`, `:platform:test` и `:app:test` были `UP-TO-DATE`. Флаги `--offline` и `--console=plain` в README и CI не записаны. `--offline` прошёл, зависимости для этого запуска не скачивались. — прогон 2026-11-13
- До второго запуска XML тестов уже были зелёными, но это прошлый вывод, не этот аудит. Время файлов: domain `2026-11-12 00:15:27`, data `2026-11-12 01:26:23`, platform `2026-11-12 01:37:50`, app `2026-11-12 01:37:52`. Сумма та же: 25 наборов, 128 тестов, failures=0, errors=0, skipped=0. — `TEST-*.xml` в `app/build/test-results`, `domain/build/test-results`, `data/build/test-results`, `platform/build/test-results`
- Второй запуск: `.\gradlew.bat check dokkaGenerate --offline --no-daemon --rerun-tasks --continue --console=plain`. Код выхода 0. `BUILD SUCCESSFUL in 1m 13s`, `53 actionable tasks: 53 executed`. Строк `FAILED` в выводе нет. В том же журнале есть строки `:check UP-TO-DATE`, а также `UP-TO-DATE` у `:domain:classes`, `:domain:testClasses`, `:data:classes`, `:data:testClasses`, `:platform:classes`, `:platform:testClasses`. Строки `:domain:test`, `:data:test`, `:platform:test` и `:app:test` без `UP-TO-DATE`. — прогон 2026-11-13
- После второго запуска те же 25 наборов: 128 тестов, failures=0, errors=0, skipped=0. Время XML: domain `2026-11-13 02:12:27`, platform `2026-11-13 02:12:34`, data `2026-11-13 02:12:36`, app `2026-11-13 02:12:58`. Это местные метки файлов, часовой пояс ОС отдельно не читался. — `TEST-*.xml` под `*/build/test-results`
- Detekt в этом прогоне: четыре отчёта `detekt.md` дают `0 number of total findings` и `## Issues (0)`, версия `2.0.0-alpha.1`. Метки генерации: data, domain и platform `2026-11-12 19:12:09 UTC`, app `2026-11-12 19:12:10 UTC`. Соответствующие `detekt.xml` — пустой корень `<checkstyle version="4.3">`. — [app detekt.md](file:///D:/workspace/02-personal/TextTools/app/build/reports/detekt/detekt.md), [domain detekt.md](file:///D:/workspace/02-personal/TextTools/domain/build/reports/detekt/detekt.md), [data detekt.md](file:///D:/workspace/02-personal/TextTools/data/build/reports/detekt/detekt.md), [platform detekt.md](file:///D:/workspace/02-personal/TextTools/platform/build/reports/detekt/detekt.md)
- Открытые текстовые отчёты ktlint пусты: `app`, `domain` и `platform`, файл `ktlintMainSourceSetCheck.txt`. Задач `data:ktlint*` в журнале Gradle не было. — каталоги `*/build/reports/ktlint`
- В журнале второго прогона выполнилась `:data:verifyMainTextToolsDatabaseMigration`. Отдельной строки ошибки для неё нет, общий код выхода 0.
- Пропущенные задачи Gradle, это не пропущенные тесты: `checkKotlinGradlePluginConfigurationErrors SKIPPED` у domain, data, platform и app; корневая `:detekt NO-SOURCE`; `:logLinkDokkaGeneratePublicationHtml SKIPPED`; `:app:runKtlintCheckOverDevSourceSet NO-SOURCE` и поэтому `:app:ktlintDevSourceSetCheck SKIPPED`. Плюс обычные `NO-SOURCE` у `compileJava` и части `processResources`.
- `sh` в `PATH` нет. `bash.exe` в WindowsApps имеет длину 0 и не запускался. Git Bash `C:\Program Files\Git\bin\bash.exe` выполнил `packaging/macos/package-dmg.sh`. Вывод: `This script runs on macOS.` Код выхода скрипта 1. До Gradle скрипт не дошёл. — [package-dmg.sh](file:///D:/workspace/02-personal/TextTools/packaging/macos/package-dmg.sh), прогон 2026-11-13
- `HEAD` равен `b28e67f9986510195549dfe544838fd0475843a6`. `git status --short` показывает изменённые отслеживаемые файлы, среди них `CHANGELOG.md`, `README.md`, `build.gradle.kts`, `app/build.gradle.kts`, исходники и тесты в `app`, `domain`, `data`, `platform`, и заметки в `docs`. Неотслеживаемые: `app/src/test/kotlin/dev/belyaevad/texttools/app/ui/run/AppProfilerTest.kt`, `platform/src/main/kotlin/dev/belyaevad/texttools/platform/win/FocusedInput.kt`, `platform/src/test/kotlin/dev/belyaevad/texttools/platform/win/FocusedInputTest.kt`, каталоги `architecture/`, `en/`, файлы `index.html`, `docs/features-index.yaml`, `docs/module-coupling.yaml`. — `git rev-parse HEAD` и `git status --short` 2026-11-13
- Не запускались: `./gradlew :app:run`; `./gradlew clean :app:packageMsi`; `./gradlew clean :app:packageDeb`; `./gradlew :app:packageDmg`; матрица CI на macOS и Ubuntu; job `package` через workflow_dispatch. Причина для MSI: задача копирует shim поверх `light.exe` в каталоге WiX и при `clean` сносит `build`, включая только что снятые отчёты тестов. Задание аудита разрешает запись только в этот файл заметок. WiX и JDK 21 при этом на машине есть, так что причина не в отсутствии инструмента. DEB требует Ubuntu. DMG как задача Gradle на этом хосте не запускалась; обёртка скрипта уже дала код 1. `:app:run` поднимает трей и не является командой с кодом успеха или падения. — [app/build.gradle.kts](file:///D:/workspace/02-personal/TextTools/app/build.gradle.kts), [SKILL.md](file:///D:/workspace/02-personal/TextTools/.cursor/skills/texttools-release/SKILL.md)

### Inferences

- Свежее свидетельство модульной проверки — второй запуск. Первый код 0 почти целиком `UP-TO-DATE` и оставил XML от 2026-11-12. Назвать первый запуск прогоном тестов 2026-11-13 было бы нечестно. Расхождение «`:check UP-TO-DATE`» и «53 executed» снимают новые метки XML и detekt, а не строка корневой задачи.
- Метки detekt `2026-11-12 19:12 UTC` и местные метки XML `2026-11-13 02:12` расходятся на 7 часов. Сессия помечена UTC+7, поэтому это один и тот же прогон, если часы ОС совпадают с этой меткой. Сам пояс ОС не проверялся.
- Зелёный `check` относится к грязному рабочему дереву, не к коммиту `b28e67f`. В 128 тестов входят и неотслеживаемые `AppProfilerTest` (1) и `FocusedInputTest` (5). Выдать этот результат за проверку чистого HEAD или за тег v1.2 было бы нечестно.
- Нечестно прикладывать к релизу MSI, DEB или DMG по итогам этого аудита: ни один установщик здесь не собран, лог WiX на `LGHT0311` и `LGHT0100` не смотрелся. Это прямо нарушает навык, если такие файлы описать как собранные сегодня.
- Нечестно называть ktlint полным: модуль `data` им не покрыт, и журнал это подтвердил отсутствием задач `data:ktlint*`.
- Нечестно называть detekt строгим набором по умолчанию. Отчёт даёт 0 замечаний при версии `2.0.0-alpha.1` и при выключенных в конфиге правилах длины строки, магических чисел, сложности, имён и слишком общего `catch`. Включённым среди прочитанных остаётся `WildcardImport`. — [detekt.yml](file:///D:/workspace/02-personal/TextTools/config/detekt/detekt.yml)
- Комментарий скрипта DMG про «TextTools 1.0» расходится с `packageVersion` `1.2.0` и с заголовком changelog `v1.2 — 2026-11-12`. Если перенести эту строку в заметки релиза, версия будет названа неверно. Сам номер пакета задаёт Gradle, не комментарий скрипта.

### Gaps

- Отдельного журнала Gradle в репозитории нет. Коды выхода записаны по выводу этой сессии.
- Не открыты все текстовые отчёты ktlint, только три `ktlintMainSourceSetCheck.txt`. Успех остальных задач ktlint виден по коду 0 сборки, не по прочитанному тексту.
- Содержимое HTML dokka не проверялось. Задача `:dokkaGenerate` в повторном прогоне выполнилась.
- Каталог `dist/` не просматривался. Неизвестно, лежит ли там уже MSI и от какого дерева.
- Строка версии WiX командой не снималась. Известен только путь `WiX314`.

## Есть ли падающие тесты, пропущенные наборы или TODO/FIXME о сломанном поведении для пользователя?

### Takeaway

Падающих тестов нет, пропущенных тестовых наборов нет, комментариев `TODO` и `FIXME` в исходниках и документах, которые искались, нет. Пропуски в журнале Gradle относятся к пустым source set и к служебным задачам Kotlin, не к выключенным тестам.

### Cited Findings

- Повторный прогон, XML от 2026-11-13 02:12 по местным часам: 25 наборов, 128 тестов, failures=0, errors=0, skipped=0. Наборы и числа: `SentrySetupTest` 4, `HistoryLogicTest` 9, `AppProfilerTest` 1, `RunActionTest` 8, `UiLocaleTest` 1, `WindowMetricsTest` 4, `UsageTelemetryTest` 5, `AppProfilingTest` 6, `BehaviorTest` 10, `AppWebSearchTest` 2, `LegacyImportTest` 6, `LlmClientTest` 2, `PriceCatalogueTest` 5, `StoreTest` 1, `ClipboardTest` 12, `AutostartTest` 3, `ElevationTest` 6, `ExeFileNameTest` 1, `FocusedInputTest` 5, `FrameSlackTest` 1, `HotkeySwapTest` 9, `InstanceHandoffTest` 2, `MenuPositionTest` 18, `SecretStoreTest` 6, `ShellClassTest` 1. — `TEST-*.xml` под `*/build/test-results`
- `ExeFileNameTest` и `FrameSlackTest` объявлены в `platform/src/test/kotlin/dev/belyaevad/texttools/platform/win/ShellClassTest.kt` (строки 19 и 31). Отдельных файлов с этими именами нет. — [ShellClassTest.kt](file:///D:/workspace/02-personal/TextTools/platform/src/test/kotlin/dev/belyaevad/texttools/platform/win/ShellClassTest.kt)
- В `*Test*.kt` нет `@Ignore`, `@Disabled`, `EnabledOnOs`, `DisabledOnOs` и проверки `os.name`. — поиск по `*Test*.kt` 2026-11-13
- Поиск `\bTODO\b` и `\bFIXME\b` по `*.kt`, `*.kts`, `*.md`, `*.yml`, `*.yaml`, `*.java`, `*.cs`, `*.ps1`, `*.wxs`, `*.wxl`, `*.sh`, `*.xml`, `*.html`, `*.properties` совпадений не дал. Строка в `LegacyImport.kt` про `\uXXXX` описывает разбор JSON-escape, это не маркер FIXME. — [LegacyImport.kt](file:///D:/workspace/02-personal/TextTools/data/src/main/kotlin/dev/belyaevad/texttools/data/LegacyImport.kt)
- Единственное совпадение слова browser в `*.md`, `*.yml`, `*.yaml`, `*.kts` — фраза ADR «Supersedes MdToHtml into the embedded browser». Это замена старого пути, не тестовый набор и не описание поломки. — [S5-result-ast.md](file:///D:/workspace/02-personal/TextTools/docs/adr/S5-result-ast.md)

### Inferences

- Сломанного пользовательского поведения, которое сами тесты или маркеры `TODO`/`FIXME` уже назвали бы, в просмотренных источниках нет. Это не значит, что поведение окна, вставки и установщика проверено: их команды в этот прогон не входили.
- `SKIPPED` в журнале Gradle нельзя переносить в отчёт как пропущенные тесты. Счётчик `skipped` в XML равен 0 по всем 25 наборам.

### Gaps

- Не искались `TODO`/`FIXME` в бинарных файлах, в `build/` и в расширениях вне списка выше. Для `.toml` поиск не делался.
- Не читались тела тестов на предмет раннего `return`, который тихо сужает проверку без JUnit-skip. Таких аннотаций пропуска нет, скрытый ранний выход не исключён.

## Какая заявленная проверка (браузер, рабочий стол, модульные тесты) не имеет свежего свидетельства в этом прогоне?

### Takeaway

Свежо подтверждены только модульные тесты, detekt, ktlint вне модуля `data`, проверка миграции SQLDelight и генерация dokka — на Windows и на грязном дереве. Запуск рабочего стола, три установщика, лог WiX и матрица CI на macOS и Ubuntu свежего свидетельства не имеют. Браузерного шлюза проект в README, навыке и CI не назначает.

### Cited Findings

- Модульная проверка на этом хосте есть: второй `gradlew.bat check dokkaGenerate` с кодом 0 и XML 128/0/0 от 2026-11-13 02:12 по местным часам. Другие ОС этой командой не покрыты. CI обещает ту же команду ещё на `macos-latest` и `ubuntu-latest`. — [ci.yml](file:///D:/workspace/02-personal/TextTools/.github/workflows/ci.yml), прогон 2026-11-13
- README ставит рядом с `check` команду `./gradlew :app:run`. Она не запускалась. Навык релиза её не требует. — [README.md](file:///D:/workspace/02-personal/TextTools/README.md)
- Навык требует чистый `packageMsi` и отсутствие `LGHT0311` и `LGHT0100` в подробном логе light. Этого лога в прогоне нет, потому что `packageMsi` не запускался. — [SKILL.md](file:///D:/workspace/02-personal/TextTools/.cursor/skills/texttools-release/SKILL.md)
- `packageDeb` не запускался. Хост Windows, команда в навыке предназначена для Ubuntu. — [SKILL.md](file:///D:/workspace/02-personal/TextTools/.cursor/skills/texttools-release/SKILL.md)
- Скрипт DMG на этом хосте выполнен и отвергнут кодом 1. Задача `:app:packageDmg` не стартовала. Свидетельства собранного DMG нет. — [package-dmg.sh](file:///D:/workspace/02-personal/TextTools/packaging/macos/package-dmg.sh)
- Навык требует три снимка текущего приложения в `docs/assets/screenshots/en/`. Файлы снимков в этом аудите не открывались и с интерфейсом не сверялись. — [SKILL.md](file:///D:/workspace/02-personal/TextTools/.cursor/skills/texttools-release/SKILL.md)
- README описывает захват и вставку в чужие окна как поведение Windows, а сборки Linux и macOS — как открытие трея. Автотеста этого поведения в названных командах нет. Документы модулей называют отдельные тестовые классы (`BehaviorTest`, `SentrySetupTest`, `WindowMetricsTest`, `AppProfilingTest`, `AutostartTest`, `PriceCatalogueTest`); эти классы входят в прошедшие 128 тестов. Это не проверка живого окна. — [README.md](file:///D:/workspace/02-personal/TextTools/README.md)
- Поиск `browser`, `Playwright`, `e2e` и `manual` по `*.md`, `*.yml`, `*.yaml`, `*.kts` дал одно совпадение, ADR про отказ от встроенного браузера. Команды браузерной проверки README, навык и CI не содержат. — [S5-result-ast.md](file:///D:/workspace/02-personal/TextTools/docs/adr/S5-result-ast.md)

### Inferences

- Честный статус на 2026-11-13: модульные ворота Windows для текущего грязного дерева зелёные. Ворота установщика и ручного запуска приложения не выполнены, поэтому релиз по навыку из этого прогона не следует.
- Утверждение «CI собрал установщики» было бы ложным даже при зелёном push: упаковка в workflow только у `workflow_dispatch`. Удалённый статус этого запуска неизвестен.
- Утверждение «приложение проверено в браузере» проект этими источниками не делает, и свежего браузерного прогона нет, потому что нечего было запускать.
- Утверждение «проверено на трёх ОС» из одного Windows-прогона не следует. Скрипт DMG это явно отказал.

### Gaps

- Неизвестен последний результат GitHub Actions для job `test` на macOS и Ubuntu и для ручного job `package`.
- Неизвестно, совпадает ли какой-либо уже лежащий MSI с этим деревом: `dist/` не читался, `packageMsi` не запускался.
- Не проверялось, что скриншоты README соответствуют текущему окну. Навык такой проверки картинками требует, команды для неё нет.
- `:app:run` не запускался намеренно. Поведение трея, вставки и горячих клавиш в живой сессии этим аудитом не наблюдалось.
