# Готовность TextTools к релизу на 2026-11-13

Аудит только читал дерево и команды git. Релиз не выполнялся: коммита, тега, push, подъёма версии и правок продуктовых файлов не было. Единственная запись этого хода — этот файл.

`architecture_context` (namespace `project-0-TextTools-Architecture`, без id, 2026-11-13) вернул `index: pending`, `paths: ["README.md"]`, `must: write-cards`. Карточки в этом ходе не писались: задание запрещает править продуктовые файлы. Сборку графа и карты аудит не запускал.

Дата среза: 2026-11-13. Команды git ниже выполнены в `D:\workspace\02-personal\TextTools` в этот день, только на чтение.

## Что скилл требует по шагам, включая версию, changelog, git и ворота проверки

### Takeaway

Процедура релиза записана в `.cursor/skills/texttools-release/SKILL.md` и короче повторена в `.cursor/rules/texttools-release.mdc`. Скилл не содержит отдельного шага «поднять номер в Gradle». Он требует чистую пересборку пакетов, заметки и снимки текущего приложения, коммит changelog и README, аннотированный тег `vX.Y` на этом коммите и push. Ворота проверки внутри скилла — verbose-лог WiX light: вызовы по культурам и отсутствие `LGHT0311` и `LGHT0100`. Команды `./gradlew check` в скилле нет.

### Cited Findings

- Имя скилла: `texttools-release`. Назначение: публикация версии, changelog, снимки README, обновление GitHub Pages. — [SKILL.md](file:///D:/workspace/02-personal/TextTools/.cursor/skills/texttools-release/SKILL.md)
- В каталоге скилла лежит только этот файл. — [листинг `.cursor/skills/texttools-release`](file:///D:/workspace/02-personal/TextTools/.cursor/skills/texttools-release/SKILL.md)
- Шаг «Rebuild first». Вывод сборки удаляется, и каждый пакет собирается заново, даже если `dist/` уже не пуст. Пакет, собранный со старого дерева, не прикладывается. — [SKILL.md](file:///D:/workspace/02-personal/TextTools/.cursor/skills/texttools-release/SKILL.md)
- Windows, из корня репозитория, при JDK 21 и WiX 3 в `PATH`: `./gradlew clean :app:packageMsi`. `prepareLightShim` компилирует `packaging/windows/LightShim.cs` и ставит этот `light.exe` первым на путь jpackage. Один вызов WiX light со всеми культурами неприемлем (`LGHT0100`); shim собирает один MSI на культуру и вкладывает остальные как language transforms, база `en-us`. Verbose-лог должен показать эти покультурные вызовы light и не должен сообщать `LGHT0311` или `LGHT0100`. — [SKILL.md](file:///D:/workspace/02-personal/TextTools/.cursor/skills/texttools-release/SKILL.md)
- Linux: дерево копируется в Ubuntu без `build`, `.gradle`, `dist`, `tmp`, `.git`, затем `./gradlew clean :app:packageDeb`. — [SKILL.md](file:///D:/workspace/02-personal/TextTools/.cursor/skills/texttools-release/SKILL.md)
- macOS: `sh packaging/macos/package-dmg.sh` на Mac. Если скрипт не запускался, заметки так и говорят и не ссылаются на DMG. — [SKILL.md](file:///D:/workspace/02-personal/TextTools/.cursor/skills/texttools-release/SKILL.md)
- Новые msi и deb копируются в `dist/`. Только они, и DMG лишь когда скрипт Mac его произвёл, являются ассетами релиза. — [SKILL.md](file:///D:/workspace/02-personal/TextTools/.cursor/skills/texttools-release/SKILL.md)
- Шаг «Notes and pictures». `CHANGELOG.md` на `main` совпадает с `archive/ahk`: заголовок `## vX.Y — YYYY-MM-DD`, затем что изменилось для пользователя. История AutoHotkey остаётся на `archive/ahk`. — [SKILL.md](file:///D:/workspace/02-personal/TextTools/.cursor/skills/texttools-release/SKILL.md)
- Снимки README — три кадра текущего приложения (menu, settings, result) в `docs/assets/screenshots/en/`. Снимки AutoHotkey с `gh-pages` не используются. Кадр с ключом, паролем прокси или DSN не публикуется. — [SKILL.md](file:///D:/workspace/02-personal/TextTools/.cursor/skills/texttools-release/SKILL.md)
- Текст релиза ссылается только на только что собранные ассеты. — [SKILL.md](file:///D:/workspace/02-personal/TextTools/.cursor/skills/texttools-release/SKILL.md)
- Шаг «Git». Changelog и README коммитятся до перемещения тега. Команда: `git tag -f -a vX.Y` на этом коммите, затем push `main` и тега. Теги `v0.1.0`–`v0.3.1` остаются. Force-push `main` запрещён. — [SKILL.md](file:///D:/workspace/02-personal/TextTools/.cursor/skills/texttools-release/SKILL.md)
- Сайт — отдельный коммит на `gh-pages`: `index.html` и `en/` меняются вместе (title, description, download links, screenshots). — [SKILL.md](file:///D:/workspace/02-personal/TextTools/.cursor/skills/texttools-release/SKILL.md)
- `.cursor/` остаётся в ignore, кроме этого скилла и `.cursor/rules/texttools-release.mdc`. — [SKILL.md](file:///D:/workspace/02-personal/TextTools/.cursor/skills/texttools-release/SKILL.md)
- Шаг «Installer languages», постоянное условие упаковки. `packaging/windows/main.wxs` использует `!(loc.ProductLanguage)`, `!(loc.AutostartDlgBody)` и `!(loc.AutostartCheck)`. Каждый `l10n-*.wxl` задаёт эти три строки. Для en, de, ja и zh скилл пишет, что jpackage уже передаёт stock-файлы, поэтому эти четыре файла добавляют только эти три строки. `ru-ru` (LCID 1049, code page 1251) повторяет английские stock-строки, потому что культура линкуется отдельно. Непереведённые русские фразы остаются по-английски. Имена файлов — `l10n-*.wxl`, не `MsiInstallerStrings_*.wxl`. — [SKILL.md](file:///D:/workspace/02-personal/TextTools/.cursor/skills/texttools-release/SKILL.md)
- Правило `.cursor/rules/texttools-release.mdc` повторяет чистую пересборку, формат changelog, запрет чужих ассетов и секретов, отдельные коммиты `main` и `gh-pages` и запрет переписывать `main`. Про DMG правило говорит: если образ здесь не собирался, в заметках сказано, что DMG собирается на Mac. Кодов `LGHT0311` и `LGHT0100` и команды `git tag -f` в правиле нет. Правило добавляет, что задача MSI копирует `main.wxs` и `l10n-*.wxl` в каталог ресурсов jpackage. — [texttools-release.mdc](file:///D:/workspace/02-personal/TextTools/.cursor/rules/texttools-release.mdc)
- README в разделе «Check and run» пишет `./gradlew check` и для Windows просит `gradlew.bat` вместо `./gradlew`. Это не текст скилла. — [README.md](file:///D:/workspace/02-personal/TextTools/README.md)
- Workflow `.github/workflows/ci.yml` называется `check`. Job `test` на push и pull request запускает `./gradlew check dokkaGenerate` на Windows, macOS и Ubuntu с Java 21. Job `package` запускается только по `workflow_dispatch` и вызывает `packageMsi`, `packageDmg` или `packageDeb` без `clean`. macOS-задача CI — `packageDmg`, не `packaging/macos/package-dmg.sh`. — [ci.yml](file:///D:/workspace/02-personal/TextTools/.github/workflows/ci.yml)
- Номер пакета приложения в дереве — `packageVersion` и `description` внутри `compose.desktop.application.nativeDistributions` в `app/build.gradle.kts`. Скилл этот файл не называет. — [app/build.gradle.kts](file:///D:/workspace/02-personal/TextTools/app/build.gradle.kts)
- Строка `version.set("1.5.0")` в корневом `build.gradle.kts` стоит в блоке ktlint, не в версии приложения. — [build.gradle.kts](file:///D:/workspace/02-personal/TextTools/build.gradle.kts)

### Inferences

- Ворота скилла для Windows-установщика — текст verbose-лога light, а не зелёный `./gradlew check` и не job `package` в CI. CI собирает пакеты без `clean` и на Mac вызывает другую задачу, чем скилл.
- Номер версии скилл задаёт как `vX.Y` в теге и в заголовке changelog. Согласование с `packageVersion` в Gradle — свойство дерева, без которого имя MSI (`TextTools-1.1.0.msi` при зафиксированном `1.1.0`) разойдётся с текстом changelog.
- Обновление `gh-pages` в скилле стоит после фразы про тег и push. К состоянию «готово к тегу» этот коммит сайта не приписан.

### Gaps

- Скилл не называет команду, которая прикрепляет файлы из `dist/` к GitHub Release. Как ассет покидает `dist/`, из скилла неизвестно.
- Скилл не говорит, что дата в заголовке changelog обязана совпасть с календарным днём тега.
- Четырёхстрочный незакоммиченный diff корневого `build.gradle.kts` в этом аудите не прочитан. Меняет ли он что-то кроме уже видимого блока ktlint, неизвестно.
- `./gradlew -version` не запускался, поэтому какой JDK реально берёт wrapper, этим ходом не подтверждён. Известны только `JAVA_HOME` и порядок `java` в `PATH` (см. следующий раздел).

## Где лежат версия, changelog и заметки релиза, какие значения сейчас, какие теги и какие незакоммиченные изменения попали бы в релиз

### Takeaway

Зафиксированный релиз — тег `v1.1` на `HEAD` `b28e67f` (2026-11-11), `packageVersion` `1.1.0`. В рабочем дереве уже написаны версия 1.2, заголовок `## v1.2 — 2026-11-12` и `packageVersion` `1.2.0`, но это не закоммичено и в тег `v1.1` не входит. Тега `v1.2` нет. В `dist/` нет `TextTools-1.2.0.msi`. Сайт `gh-pages` всё ещё указывает на v1.1.

### Cited Findings

- `HEAD` — ветка `main`, коммит `b28e67f9986510195549dfe544838fd0475843a6`, сообщение «Place the menu and the progress window on the input window, and show token cost while a request runs.», дата 2026-11-11. На этом коммите одновременно `HEAD`, `tag: v1.1`, `origin/main`, `origin/HEAD`. — команда `git log -15 --date=short --pretty=format:"%h %ad %d %s"` и `git rev-parse HEAD`, [репозиторий](file:///D:/workspace/02-personal/TextTools)
- `git describe --tags --always` напечатал `v1.1`. `git rev-list --left-right --count origin/main...HEAD` напечатал `0	0`: локальный `main` и `origin/main` совпадают. Индекс пуст: `git diff --cached --stat` пустой. — те же команды, [репозиторий](file:///D:/workspace/02-personal/TextTools)
- В `HEAD` README говорит «version 1.1». В рабочем README та же строка говорит «version 1.2». Весь diff README — эта одна замена. — `git show HEAD:README.md`, `git diff -- README.md`, [README.md](file:///D:/workspace/02-personal/TextTools/README.md)
- В `HEAD` changelog начинается с `## v1.1 — 2026-11-11` и называет загрузку `TextTools-1.1.0.msi`. В рабочем файле сверху добавлен блок `## v1.2 — 2026-11-12` (10 строк diff). Дальше идут только `## v1.1 — 2026-11-11` и `## v1.0 — 2026-11-09`. Заголовков `v0.*` в рабочем `CHANGELOG.md` нет. — `git show HEAD:CHANGELOG.md`, `git diff --stat`, [CHANGELOG.md](file:///D:/workspace/02-personal/TextTools/CHANGELOG.md)
- Текст блока v1.2 в рабочем дереве: меню у поля ввода или у курсора; вставка результата в поле, если поле принимает текст, иначе окно результата у курсора; окно прогресса называет приложение, если у приложения есть prompt note; вызов модели ждёт ответ до 180 секунд; сохраняются зарегистрированные горячие клавиши, а при отказе Windows для клавиши меню используются Ctrl+Alt+Space и Ctrl+Shift+Space. Следующая фраза: загрузки этой версии — установщик `TextTools-1.2.0.msi`; пакет Debian и образ macOS собираются на своих системах командами `./gradlew :app:packageDeb` и `sh packaging/macos/package-dmg.sh`. Ссылки на файл `.deb` или `.dmg` в этом абзаце нет. — [CHANGELOG.md](file:///D:/workspace/02-personal/TextTools/CHANGELOG.md)
- В `HEAD` `packageVersion = "1.1.0"` и `description = "TextTools 1.1"`. В рабочем файле `packageVersion = "1.2.0"` и `description = "TextTools 1.2"`. Остальной diff `app/build.gradle.kts` — переносы строк в уже существующей задаче jpackage, фильтр копирования по-прежнему берёт `main.wxs` и файлы `.wxl`. — `git diff -- app/build.gradle.kts`, [app/build.gradle.kts](file:///D:/workspace/02-personal/TextTools/app/build.gradle.kts)
- Образец заголовка на `archive/ahk`: `## v0.3.1 — 2026-10-24`, затем абзац и маркированный список. — `git show archive/ahk:CHANGELOG.md`, ветка `archive/ahk`
- Локальные теги: `v0.1.0`, `v0.2.0`, `v0.2.1`, `v0.2.2`, `v0.2.3`, `v0.3.0`, `v0.3.1`, `v1.0`, `v1.1`. Тега `v1.2` в `git tag -l` нет. `v1.1` — аннотированный тег от 2026-11-11 на коммит `b28e67f`. `v0.3.1` датирован 2026-10-24. — `git tag -l`, `git for-each-ref refs/tags`
- `git branch -a --contains v0.3.1` перечисляет `archive/ahk`, `main`, `rewrite` и соответствующие remote-ветки. `git branch -a --contains v1.1` перечисляет только `main` и `origin/main`. — эти две команды
- Ветки: локальные `archive/ahk`, `gh-pages`, `main` (текущая), `rewrite`; remote `origin/archive/ahk`, `origin/gh-pages`, `origin/main`. — `git branch -a`
- Вершина `gh-pages` и `origin/gh-pages`: `d7c4892`, 2026-11-11, «Point the site at the v1.1 Windows installer.» — `git log -5 origin/gh-pages`
- Неотслеживаемые `index.html` и `en/index.html` побайтно совпадают с блобами `gh-pages`: hash `index.html` `31e9ddb3bf40aeca084d66fb23c35640cf20b1a2`, hash `en/index.html` `2bffd24cda741a7b0d8656b667617a2d920757f7`, оба равны `git rev-parse gh-pages:<path>`. В обоих файлах `softwareVersion` `1.1.0`, ссылки на `releases/tag/v1.1` и `releases/download/v1.1/TextTools-1.1.0.msi`. Текст говорит, что снимки сняты с установленной версии 1.0 и что пакет Debian и образ Mac для этого тега не пересобирались. — `git hash-object`, `git rev-parse`, [index.html](file:///D:/workspace/02-personal/TextTools/index.html), [en/index.html](file:///D:/workspace/02-personal/TextTools/en/index.html)
- `git status --porcelain` : 35 изменённых отслеживаемых файлов и неотслеживаемые пути. `git diff --stat`: 35 files changed, 851 insertions(+), 326 deletions(-). Изменённые пути: `CHANGELOG.md`, `README.md`, `app/build.gradle.kts`, `app/src/main/composeResources/values/app.xml`, `app/src/main/composeResources/values-ru/app.xml`, `app/src/main/kotlin/dev/belyaevad/texttools/app/AppGraph.kt`, `Main.kt`, `UsageTelemetry.kt`, `ui/AppIcon.kt`, `ui/WindowPlacement.kt`, `ui/editor/EditorLogic.kt`, `ui/history/HistoryLogic.kt`, `ui/result/ResultWindow.kt`, `ui/run/AppProfiler.kt`, `ui/run/RunAction.kt`, `ui/settings/SettingsForm.kt`, `ui/settings/SettingsWindow.kt`, тесты `UsageTelemetryTest.kt` и `RunActionTest.kt`, корневой `build.gradle.kts`, `data/.../LegacyImport.kt`, `docs/dev/profiling.md`, `docs/dev/sentry.md`, `docs/dev/windows.md`, `docs/modules/app.md`, `docs/modules/data.md`, `docs/modules/platform.md`, `domain/.../Markdown.kt`, `domain/.../Prompt.kt`, `platform/.../Clipboard.kt`, `platform/.../win/WinSession.kt`, тесты `ClipboardTest.kt`, `ElevationTest.kt`, `HotkeySwapTest.kt`, `MenuPositionTest.kt`. — `git status --porcelain`, `git diff --stat`
- Неотслеживаемые пути на момент статуса: `app/src/test/kotlin/dev/belyaevad/texttools/app/ui/run/AppProfilerTest.kt`, `architecture/project.yaml`, `docs/features-index.yaml`, `docs/module-coupling.yaml`, `en/index.html`, `index.html`, `platform/src/main/kotlin/dev/belyaevad/texttools/platform/win/FocusedInput.kt`, `platform/src/test/kotlin/dev/belyaevad/texttools/platform/win/FocusedInputTest.kt`. — `git status --porcelain=v1 --untracked-files=all`
- `FocusedInput.kt` объявляет `internal data class FocusedField` и `internal fun readFocusedField`. Поиск `FocusedField` и `readFocusedField` по `*.kt` находит только этот файл. — [FocusedInput.kt](file:///D:/workspace/02-personal/TextTools/platform/src/main/kotlin/dev/belyaevad/texttools/platform/win/FocusedInput.kt)
- В рабочем `AppGraph.kt` есть `CHAT_READ_TIMEOUT` на 180 секунд. Файл входит в список изменённых. — [AppGraph.kt](file:///D:/workspace/02-personal/TextTools/app/src/main/kotlin/dev/belyaevad/texttools/app/AppGraph.kt)
- Каталог `dist/` в gitignore. Состав на диске 2026-11-13: `dist/RELEASE-v1.0.md` (837 байт, 2026-11-09 01:43), `dist/TextTools-1.0-macos-build.zip` (1083 байта, 2026-11-09 01:21), `dist/TextTools-1.1.0.msi` (87 506 944 байта, 2026-11-11 00:44), `dist/texttools_1.0.0_amd64.deb` (81 119 880 байт, 2026-11-10 04:27). Файла `TextTools-1.2.0.msi` и файла `.dmg` в листинге нет. Файла `TextTools-1.0.0.msi`, который назван в `RELEASE-v1.0.md`, в этом листинге тоже нет. — [.gitignore](file:///D:/workspace/02-personal/TextTools/.gitignore), `Get-ChildItem dist`, [dist/RELEASE-v1.0.md](file:///D:/workspace/02-personal/TextTools/dist/RELEASE-v1.0.md)
- `RELEASE-v1.0.md` сам называет себя локальной заметкой «not uploaded» и пишет, что ссылки заработают после прикрепления файлов к GitHub release тега v1.0 и что «Nothing has been pushed». Это текст файла от 2026-11-09, не проверка текущего GitHub. — [dist/RELEASE-v1.0.md](file:///D:/workspace/02-personal/TextTools/dist/RELEASE-v1.0.md)
- Снимки, на которые ссылается README: `docs/assets/screenshots/en/menu.png`, `tray-menu.png`, `settings-api.png`, `settings-languages.png`, `settings-interface.png`, `result.png`. В `git status` их нет, содержимое совпадает с `HEAD`. Последний коммит, менявший этот каталог: `591d50f`, 2026-11-10, «Show the installed 1.0 windows and correct the description.» Время модификации файлов в каталоге: 2026-11-11 00:48:56. Рядом лежит `_capture-report.txt` с теми же часами, но он перечисляет другие имена (`01-action-menu.png`, `02-settings-api.png`, `03-settings-ui.png`, `04-actions-editor.png`, `05-prompt-editor.png`, `06-result-window.png`, `07-key-conflict.png`), которых в каталоге нет. В `docs/assets/screenshots/ru/` из листинга виден только ещё один `_capture-report.txt`, без png. — [README.md](file:///D:/workspace/02-personal/TextTools/README.md), `git log -3 -- docs/assets/screenshots/en`, `Get-ChildItem`, [en/_capture-report.txt](file:///D:/workspace/02-personal/TextTools/docs/assets/screenshots/en/_capture-report.txt)
- Просмотр `settings-api.png`: поле API key пустое, флажок Proxy снят, поле прокси пустое, на этом кадре нет DSN. Просмотр `settings-interface.png`: переключатели отчётов об ошибках выключены, значения DSN на кадре нет; в нижней строке кадра виден пример `key: ollama` у `http://localhost:11434/v1`. Остальные четыре png не открывались. — [settings-api.png](file:///D:/workspace/02-personal/TextTools/docs/assets/screenshots/en/settings-api.png), [settings-interface.png](file:///D:/workspace/02-personal/TextTools/docs/assets/screenshots/en/settings-interface.png)
- `JAVA_HOME` = `C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot\`. Первый `java.exe` в `PATH` — `C:\Program Files (x86)\Common Files\Oracle\Java\java8path\java.exe`, и `java -version` печатает `1.8.0_503`. Дальше в `PATH` есть `jdk-21.0.12.101-hotspot\bin\java.exe` и `jdk-21.0.12.8-hotspot\bin\java.exe`. `light.exe` и `candle.exe` находятся как `C:\Users\belya\AppData\Local\WiX314\light.exe` и `candle.exe`. — `java -version`, `where.exe java`, `Get-Command light.exe, candle.exe`
- Поиск `LGHT0311`, `LGHT0100` и `per-culture` по `*.txt` в `app/build` совпадений не дал. В `app/build/compose/tmp/packageMsi.args.txt` есть файл от 2026-11-11 00:42, рядом с временем `TextTools-1.1.0.msi`. — поиск по `app/build`, листинг `dist` и `app/build`
- `.gitignore` игнорирует `.cursor/*`, затем снова показывает `.cursor/skills/**` целиком и из правил только `.cursor/rules/texttools-release.mdc`. `git ls-files .cursor` печатает ровно два пути: `.cursor/rules/texttools-release.mdc` и `.cursor/skills/texttools-release/SKILL.md`. `dist/` и `config/sentry.properties` в ignore. Строки `research_notes` в `.gitignore` нет. — [.gitignore](file:///D:/workspace/02-personal/TextTools/.gitignore), `git ls-files .cursor`
- Установщик, не входящий в грязный diff. `main.wxs` содержит `Language="!(loc.ProductLanguage)"`, `!(loc.AutostartDlgBody)`, `!(loc.AutostartCheck)`. Файлы: `l10n-en.wxl` (`en-us`, 1033), `l10n-de.wxl` (`de-de`, 1031), `l10n-ja.wxl` (`ja-jp`, 1041), `l10n-zh.wxl` (`zh-cn`, 2052), `l10n-ru.wxl` (`ru-ru`, code page 1251, `ProductLanguage` 1049). В каждом из пяти есть `ProductLanguage`, `AutostartDlgBody`, `AutostartCheck`, а также `DesktopShortcutCheck`, `StartMenuShortcutCheck`, `LaunchAfterCheck`. В `l10n-ru.wxl` после русских строк идут английские stock-строки (`DowngradeErrorMessage`, `MainFeatureTitle` и соседние). — [main.wxs](file:///D:/workspace/02-personal/TextTools/packaging/windows/main.wxs), [l10n-ru.wxl](file:///D:/workspace/02-personal/TextTools/packaging/windows/l10n-ru.wxl), [l10n-en.wxl](file:///D:/workspace/02-personal/TextTools/packaging/windows/l10n-en.wxl) и соседние `l10n-*.wxl`

### Inferences

- Незакоммиченные правки в тег `v1.1` не входят: тег стоит на `b28e67f`, а diff не в индексе. Они попадут в будущий релиз только если их закоммитить до тега. Коммит всего грязного дерева утащил бы и продуктовый diff, и черновик v1.2, и `packageVersion` 1.2.0.
- `index.html` и `en/index.html` — копия уже опубликованного сайта v1.1, лежащая в рабочем дереве `main` без учёта git. В коммит `main` скилл их не кладёт. Сами по себе они не являются заметками релиза 1.2.
- `FocusedInput.kt` по поиску имён сейчас ни один другой `*.kt` не вызывает. Сам по себе он не объясняет пункты changelog v1.2. Файл `AppGraph.kt` содержит таймаут 180 секунд, и этот один пункт changelog в рабочем дереве подтверждён. Остальные пункты по символам не разбирались.
- Имя `TextTools-1.1.0.msi` совпадает с зафиксированным `packageVersion` `1.1.0`. Имени `TextTools-1.2.0.msi`, которое уже написано в черновике changelog, на диске нет, значит эта фраза описывает файл, которого в `dist/` нет.
- Этот файл заметок после записи будет неотслеживаемым: `research_notes` не в `.gitignore`. Скилл релиза его не называет.

### Gaps

- Живёт ли URL `https://github.com/BelyaevAD/TextTools/releases/download/v1.1/TextTools-1.1.0.msi` и прикреплён ли MSI к релизу GitHub, этот ход не проверял. Сеть к GitHub не запрашивалась.
- Содержимое `dist/TextTools-1.0-macos-build.zip` (1083 байта) не распаковывалось. Это не DMG по размеру и расширению, но список файлов внутри неизвестен.
- Совпадает ли набор английских строк в `l10n-ru.wxl` с полным stock-файлом jpackage, неизвестно: сравнивался только сам файл.
- Покрывает ли грязный diff каждый пункт списка v1.2, неизвестно, кроме таймаута 180 секунд в `AppGraph.kt`.
- Четыре png (`menu.png`, `tray-menu.png`, `settings-languages.png`, `result.png`) на ключ, пароль прокси и DSN не открывались.

## Какие пункты чеклиста сегодня проходят, какие нет и какие по дереву не судить

### Takeaway

К тегу дерево не готово. Формат changelog, сохранность старых тегов, ignore для `.cursor` и три обязательных ключа локализации установщика на месте. Пересборка 1.2, лог light, свежие снимки, коммит и совпадение текста загрузок с файлами в `dist/` не выполнены. Проверку `./gradlew check`, сборки Linux и macOS и полноту пунктов changelog по коду по дереву не судить.

### Cited Findings

Проходят сегодня:

- Заголовки changelog имеют вид `## vX.Y — YYYY-MM-DD` и на черновике v1.2, и на зафиксированных v1.1 и v1.0, и на образце `archive/ahk` `## v0.3.1 — 2026-10-24`. — [CHANGELOG.md](file:///D:/workspace/02-personal/TextTools/CHANGELOG.md), `git show archive/ahk:CHANGELOG.md`
- Разделов AutoHotkey в `CHANGELOG.md` на `main` нет: в файле только v1.2, v1.1 и v1.0. — [CHANGELOG.md](file:///D:/workspace/02-personal/TextTools/CHANGELOG.md)
- Теги `v0.1.0`, `v0.2.0`, `v0.2.1`, `v0.2.2`, `v0.2.3`, `v0.3.0`, `v0.3.1` существуют. Тег `v1.1` не сдвинут с `b28e67f`. Force-push в этом срезе не выполнялся: `main` совпадает с `origin/main`. — `git tag -l`, `git rev-list --left-right --count origin/main...HEAD`
- В git учитываются только скилл релиза и `.cursor/rules/texttools-release.mdc`. — `git ls-files .cursor`, [.gitignore](file:///D:/workspace/02-personal/TextTools/.gitignore)
- `main.wxs` использует три `!(loc.*)` из скилла. Пять культур `en-us`, `de-de`, `ja-jp`, `zh-cn`, `ru-ru` лежат в файлах `l10n-*.wxl`. У `ru-ru` LCID 1049 и code page 1251, английские stock-строки в файле есть. Эти файлы не входят в грязный diff. — [packaging/windows](file:///D:/workspace/02-personal/TextTools/packaging/windows/main.wxs), `git status`
- Абзац v1.2 не ссылается на файл DMG. — [CHANGELOG.md](file:///D:/workspace/02-personal/TextTools/CHANGELOG.md)
- На двух открытых снимках настроек нет заполненного ключа API, пароля прокси и значения DSN. — [settings-api.png](file:///D:/workspace/02-personal/TextTools/docs/assets/screenshots/en/settings-api.png), [settings-interface.png](file:///D:/workspace/02-personal/TextTools/docs/assets/screenshots/en/settings-interface.png)
- `dist/` в gitignore, поэтому лежащие там установщики сами в git-коммит не войдут. — [.gitignore](file:///D:/workspace/02-personal/TextTools/.gitignore)
- В рабочем дереве README «version 1.2», заголовок changelog `v1.2` и `packageVersion` `1.2.0` называют одну и ту же линейку 1.2. На `HEAD` все три места всё ещё говорят 1.1 / 1.1.0. — [README.md](file:///D:/workspace/02-personal/TextTools/README.md), [CHANGELOG.md](file:///D:/workspace/02-personal/TextTools/CHANGELOG.md), [app/build.gradle.kts](file:///D:/workspace/02-personal/TextTools/app/build.gradle.kts)
- WiX 3: `light.exe` и `candle.exe` разрешаются из каталога `WiX314`. JDK 21 установлен, `JAVA_HOME` указывает на `jdk-21.0.12.101-hotspot`, и этот `java.exe` есть в `PATH`, но не первым. — `Get-Command`, `where.exe java`

Не проходят сегодня:

- Состояние «готово к тегу» не достигнуто. Индекс пуст, `HEAD` — это уже помеченный `v1.1`. Черновик changelog и README в коммите тега не лежат. — `git diff --cached --stat`, `git describe`
- Чистой пересборки пакета 1.2 нет. В `dist/` лежит `TextTools-1.1.0.msi` от 2026-11-11 00:44, а changelog v1.2 уже называет `TextTools-1.2.0.msi`. — листинг `dist`, [CHANGELOG.md](file:///D:/workspace/02-personal/TextTools/CHANGELOG.md)
- Verbose-лог с покультурными вызовами light и без `LGHT0311` / `LGHT0100` для текущего дерева не найден. — поиск по `app/build`
- Debian-пакет в `dist/` — `texttools_1.0.0_amd64.deb` от 2026-11-10, не результат `clean :app:packageDeb` с дерева 1.2. DMG в `dist/` нет. — листинг `dist`
- Снимки README не являются кадрами текущего рабочего дерева. Их последний коммит — `591d50f` («installed 1.0 windows»), diff README файлы картинок не меняет. Скилл просит три кадра (menu, settings, result). README ссылается на шесть png, включая `tray-menu.png` и три отдельных кадра настроек. Сайт v1.1 прямо пишет, что снимки сняты с установленной 1.0. — `git log`, `git diff -- README.md`, [en/index.html](file:///D:/workspace/02-personal/TextTools/en/index.html)
- Фраза скилла «these four files add only the three strings» не совпадает с файлами: `l10n-en.wxl`, `l10n-de.wxl`, `l10n-ja.wxl` и `l10n-zh.wxl` содержат шесть `String Id`, не три. — [l10n-en.wxl](file:///D:/workspace/02-personal/TextTools/packaging/windows/l10n-en.wxl) и соседние файлы, [SKILL.md](file:///D:/workspace/02-personal/TextTools/.cursor/skills/texttools-release/SKILL.md)
- Сайт не обновлён под 1.2. `gh-pages` на `d7c4892` говорит о v1.1. Неотслеживаемые копии в корне `main` с этим коммитом совпадают и тоже говорят о v1.1. — `git log origin/gh-pages`, `git hash-object`
- Первый `java` в `PATH` — Java 8, тогда как скилл пишет «JDK 21 and WiX 3 on PATH». JDK 21 при этом является `JAVA_HOME` и стоит в `PATH` ниже. — `java -version`, `where.exe java`
- Исключение в `.gitignore` открывает весь `.cursor/skills/**`, а скилл говорит об одном скилле. Сейчас в индексе git всё равно только `texttools-release`, других скиллов в `git ls-files .cursor` нет. — [.gitignore](file:///D:/workspace/02-personal/TextTools/.gitignore), `git ls-files .cursor`

По дереву не судить:

- Зелёный ли `./gradlew check` на грязном дереве. Команда в этом ходе не запускалась. Отчёты ktlint в `app/build/reports/ktlint` датированы 2026-11-12 01:35–01:36 и не являются прогоном check после всех нынешних правок. — листинг `app/build`, отсутствие запуска gradle в этом аудите
- Прошёл ли лог light у уже лежащего `TextTools-1.1.0.msi`. Текста `LGHT0311` / `LGHT0100` в сохранённых `*.txt` нет, самого verbose-лога тоже не найдено. — поиск по `app/build`
- Выполнены ли пункты списка v1.2 в коде, кроме наличия таймаута 180 секунд в рабочем `AppGraph.kt`.
- Есть ли ключ, пароль прокси или DSN на четырёх неоткрытых png.
- Собираются ли сейчас Debian и DMG: для них скилл требует другую ОС, этих машин в срезе нет.
- Нужно ли заменить дату `2026-11-12` на `2026-11-13`. Скилл этого правила не задаёт. Черновик датирован предыдущим календарным днём относительно даты аудита.
- Полон ли повтор английских stock-строк в `l10n-ru.wxl` относительно шаблона jpackage.
- Что меняют 4 строки diff корневого `build.gradle.kts`.
- Доступны ли файлы релиза v1.1 на GitHub. Ссылки в `gh-pages` на них есть, HTTP-проверка не делалась.

### Inferences

- Согласованные номера 1.2 в рабочем дереве — черновик, а не выпущенная версия. Пока они не в коммите, а в `dist/` нет MSI с именем из changelog, пункт «текст релиза ссылается только на только что собранные ассеты» не выполнен.
- Шесть снимков вместо трёх — расхождение с буквой скилла даже после обновления кадров. Сейчас важнее другое расхождение: кадры относятся к коммиту про окна 1.0, а рабочее приложение после `591d50f` менялось и в `v1.1`, и в незакоммиченном diff.
- Строка `key: ollama` на снимке интерфейса — подпись локального примера на кадре, не заполненное поле ключа API. Два просмотренных кадра запрет скилла на ключ пользователя, пароль прокси и DSN не нарушают. Это не заключение про все шесть файлов.

### Gaps

- Нельзя по одному `dist/TextTools-1.1.0.msi` решить, был ли его лог light когда-то чистым: лог не сохранён в дереве.
- Нельзя по дереву Windows решить, что Ubuntu-копия и Mac-скрипт для 1.2 уже где-то выполнены. Локальный `dist/` таких свежих файлов не содержит, и это отрицательный факт только про этот каталог.

## Какая минимальная оставшаяся последовательность доводит дерево до «готово к тегу»

### Takeaway

«Готово к тегу» сейчас ложно. Оставшаяся последовательность, которая этим аудитом не выполнялась: собрать MSI на этом JDK 21 с WiX 3 и проверить лог light, собрать deb на Ubuntu, на Mac либо собрать DMG, либо оставить в заметках явное «скрипт не запускался» без ссылки на DMG, обновить три снимка текущего приложения, переписать строку загрузок так, чтобы она называла только что собранный MSI, и закоммитить changelog, README и тот код, без которого пакет останется 1.1.0. Только такой коммит скилл помечает тегом. Тег, push и коммит `gh-pages` в эту готовность не входят и здесь запрещены.

### Cited Findings

Факты, из которых складывается последовательность. Ни один шаг ниже не выполнялся.

- Повторять подъём версии не из чего: рабочее дерево уже содержит README «version 1.2», заголовок `## v1.2 — 2026-11-12` и `packageVersion = "1.2.0"`. На `HEAD` всё ещё `1.1` / `1.1.0`. Отдельной команды bump в скилле нет. — [SKILL.md](file:///D:/workspace/02-personal/TextTools/.cursor/skills/texttools-release/SKILL.md), `git diff` README и `app/build.gradle.kts`
- Windows-сборка, которую скилл ставит первой: из корня `./gradlew clean :app:packageMsi` при JDK 21 и WiX 3. `JAVA_HOME` уже указывает на JDK 21.0.12.101, `light.exe` WiX 3.14 уже в `PATH`. README для Windows пишет вызывать `gradlew.bat`. Лог должен показать light по культурам и не содержать `LGHT0311` и `LGHT0100`. Новый msi копируется в `dist/`. Сейчас там `TextTools-1.1.0.msi`, не `TextTools-1.2.0.msi`. — [SKILL.md](file:///D:/workspace/02-personal/TextTools/.cursor/skills/texttools-release/SKILL.md), [README.md](file:///D:/workspace/02-personal/TextTools/README.md), листинг `dist`
- Debian, которой на этом дереве нет: копия в Ubuntu без `build`, `.gradle`, `dist`, `tmp`, `.git`, затем `./gradlew clean :app:packageDeb`, deb в `dist/`. Сейчас в `dist/` лежит `texttools_1.0.0_amd64.deb` от 2026-11-10. — [SKILL.md](file:///D:/workspace/02-personal/TextTools/.cursor/skills/texttools-release/SKILL.md), листинг `dist`
- macOS: либо `sh packaging/macos/package-dmg.sh` на Mac, либо заметки говорят, что скрипт не запускался, и не ссылаются на DMG. Черновик v1.2 ссылки на DMG не содержит и пишет, что образ собирается на своей системе этой командой. Отдельной фразы «в этом срезе скрипт не запускался» в абзаце нет. DMG в `dist/` нет. — [SKILL.md](file:///D:/workspace/02-personal/TextTools/.cursor/skills/texttools-release/SKILL.md), [texttools-release.mdc](file:///D:/workspace/02-personal/TextTools/.cursor/rules/texttools-release.mdc), [CHANGELOG.md](file:///D:/workspace/02-personal/TextTools/CHANGELOG.md)
- Снимки: три кадра текущего приложения, menu, settings и result, в `docs/assets/screenshots/en/`, без ключа, пароля прокси и DSN, без кадров AutoHotkey с `gh-pages`. Текущие шесть png последний раз менялись коммитом `591d50f` про окна 1.0. Пока они остаются такими, пункт скилла не выполнен. — [SKILL.md](file:///D:/workspace/02-personal/TextTools/.cursor/skills/texttools-release/SKILL.md), `git log -- docs/assets/screenshots/en`
- Строка «Downloads for this version are the Windows installer `TextTools-1.2.0.msi`» станет истинной только после того, как этот файл появится из новой сборки. Пока его в `dist/` нет, текст называет несобранный ассет. — [CHANGELOG.md](file:///D:/workspace/02-personal/TextTools/CHANGELOG.md), листинг `dist`
- Коммит, который скилл тегирует, содержит changelog и README. Если в тот же коммит не войдёт `app/build.gradle.kts` с `packageVersion` `1.2.0`, тег останется на исходниках, которые пакуются как `1.1.0`. Продуктовый diff (35 файлов, +851/−326) тоже ещё не в коммите; скилл поимённо называет только changelog и README. Индекс пуст, так что такого коммита нет. — [SKILL.md](file:///D:/workspace/02-personal/TextTools/.cursor/skills/texttools-release/SKILL.md), `git diff --stat`, `git show HEAD:app/build.gradle.kts`
- `index.html` и `en/index.html` в коммит `main` не входят: скилл держит сайт отдельным коммитом на `gh-pages`, а эти два файла побайтно равны уже опубликованному v1.1. Коммит сайта в тексте скилла стоит после тега и push. — [SKILL.md](file:///D:/workspace/02-personal/TextTools/.cursor/skills/texttools-release/SKILL.md), `git hash-object`
- Скилл не называет `architecture/project.yaml`, `docs/features-index.yaml`, `docs/module-coupling.yaml`, `FocusedInput.kt`, `FocusedInputTest.kt`, `AppProfilerTest.kt` и этот файл заметок. `FocusedField` и `readFocusedField` другими `*.kt` не вызываются. В коммит «готово к тегу» по тексту скилла они не требуются. — `git status --porcelain=v1 --untracked-files=all`, поиск по `*.kt`
- Следующая команда скилла после такого коммита — `git tag -f -a vX.Y` на нём. Имени `v1.2` среди тегов нет. `v1.1` уже указывает на `b28e67f`. Push `main` и тега идут в скилле после тега. — [SKILL.md](file:///D:/workspace/02-personal/TextTools/.cursor/skills/texttools-release/SKILL.md), `git tag -l`

Что делать нельзя. Это ограничения скилла и этого задания, не выполненные действия.

- Это задание запрещает коммит, тег, push, подъём версии и правку продуктовых файлов. Аудит их не делал.
- Скилл запрещает force-push `main`. — [SKILL.md](file:///D:/workspace/02-personal/TextTools/.cursor/skills/texttools-release/SKILL.md)
- Скилл требует, чтобы теги `v0.1.0`–`v0.3.1` остались. Команда `git tag -f` на одном из этих имён или на уже опубликованном `v1.1` сдвинула бы существующий тег. Для нового среза такого тега `v1.2` ещё нет. — [SKILL.md](file:///D:/workspace/02-personal/TextTools/.cursor/skills/texttools-release/SKILL.md), `git tag -l`
- Скилл запрещает прикладывать пакет, собранный со старого дерева. Лежащие в `dist/` MSI 1.1.0 и deb 1.0.0 этому условию для среза 1.2 не удовлетворяют. — [SKILL.md](file:///D:/workspace/02-personal/TextTools/.cursor/skills/texttools-release/SKILL.md), листинг `dist`
- Скилл запрещает ссылку на DMG, если скрипт Mac его не произвёл, и запрещает кадр с ключом, паролем прокси или DSN и кадры AutoHotkey с `gh-pages`. — [SKILL.md](file:///D:/workspace/02-personal/TextTools/.cursor/skills/texttools-release/SKILL.md)
- Сайтовые `index.html` и `en/` скилл не коммитит в `main`. — [SKILL.md](file:///D:/workspace/02-personal/TextTools/.cursor/skills/texttools-release/SKILL.md)

### Inferences

- Минимальный коммит, после которого тег по скиллу вообще имеет смысл, шире двух файлов, названных в разделе Git. Без `packageVersion` `1.2.0` имя MSI останется `1.1.0` и разойдётся с уже написанной строкой загрузок. Без продуктового diff коммит опишет поведение, которого в родительском `b28e67f` ещё нет. Скилл это следствие сам не формулирует.
- `gh-pages` можно не трогать до тега: в скилле этот коммит позже. Готовность к тегу его не включает. После тега сайт всё ещё нужно менять отдельно, и нынешние копии `index.html` и `en/index.html` для 1.2 не годятся, потому что они равны v1.1.
- Пока нет нового MSI, чистого лога light и снимков текущего приложения, даже идеальный коммит changelog не делает дерево готовым к тегу. Скилл ставит пересборку раньше git.

### Gaps

- Неизвестно, должен ли день в заголовке смениться с 2026-11-12 на день фактического тега. Скилл молчит.
- Неизвестно, какая команда публикует `dist/*.msi` и `dist/*.deb` на GitHub Release. Без неё тег и push по скиллу ещё не равны появлению файлов по ссылкам, которые уже использует сайт v1.1.
- Неизвестно, захочет ли следующий срез включить в коммит неотслеживаемые тесты и `FocusedInput.kt`. Для компиляции по поиску имён они не требуются. Содержательно ли `FocusedInput.kt` нужен пунктам v1.2, этот аудит не устанавливал.
