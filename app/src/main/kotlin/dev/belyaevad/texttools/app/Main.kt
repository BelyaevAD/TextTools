package dev.belyaevad.texttools.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.ApplicationScope
import androidx.compose.ui.window.Tray
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import dev.belyaevad.texttools.app.ui.editor.editorWindow
import dev.belyaevad.texttools.app.ui.history.HistoryClearedWindow
import dev.belyaevad.texttools.app.ui.history.HistoryWindow
import dev.belyaevad.texttools.app.ui.history.LastResultWindow
import dev.belyaevad.texttools.app.ui.history.LogClearedWindow
import dev.belyaevad.texttools.app.ui.history.ResultHooks
import dev.belyaevad.texttools.app.ui.history.StatsWindow
import dev.belyaevad.texttools.app.ui.history.clearHistoryFile
import dev.belyaevad.texttools.app.ui.history.clearLogFiles
import dev.belyaevad.texttools.app.ui.menu.actionMenu
import dev.belyaevad.texttools.app.ui.result.RefineDelivery
import dev.belyaevad.texttools.app.ui.result.ResultWindow
import dev.belyaevad.texttools.app.ui.run.RunAction
import dev.belyaevad.texttools.app.ui.run.RunStatus
import dev.belyaevad.texttools.app.ui.run.STATUS_PREPARING
import dev.belyaevad.texttools.app.ui.run.statusSeconds
import dev.belyaevad.texttools.app.ui.settings.SettingsCallbacks
import dev.belyaevad.texttools.app.ui.settings.SettingsDraft
import dev.belyaevad.texttools.app.ui.settings.SettingsProbe
import dev.belyaevad.texttools.app.ui.settings.SettingsWindow
import dev.belyaevad.texttools.data.HistoryRecord
import dev.belyaevad.texttools.data.UsageRecord
import dev.belyaevad.texttools.domain.Action
import dev.belyaevad.texttools.domain.CatalogEntry
import dev.belyaevad.texttools.domain.ChatMessage
import dev.belyaevad.texttools.domain.Factory
import dev.belyaevad.texttools.domain.HISTORY_LIMIT
import dev.belyaevad.texttools.domain.QuickRefine
import dev.belyaevad.texttools.domain.RunOutcome
import dev.belyaevad.texttools.domain.Settings
import dev.belyaevad.texttools.domain.Usage
import dev.belyaevad.texttools.domain.clipWaitSeconds
import dev.belyaevad.texttools.domain.effectiveProxy
import dev.belyaevad.texttools.domain.isLoopback
import dev.belyaevad.texttools.domain.normalizeProxy
import dev.belyaevad.texttools.domain.retarget
import dev.belyaevad.texttools.platform.ClipboardProtocol
import dev.belyaevad.texttools.platform.PasteAttempt
import dev.belyaevad.texttools.platform.win.ForegroundWatch
import dev.belyaevad.texttools.platform.win.GlobalChord
import dev.belyaevad.texttools.platform.win.InstanceAction
import dev.belyaevad.texttools.platform.win.ScreenPoint
import dev.belyaevad.texttools.platform.win.SecretSlot
import dev.belyaevad.texttools.platform.win.UserSecrets
import dev.belyaevad.texttools.platform.win.WorkArea
import dev.belyaevad.texttools.platform.win.claimFirstInstance
import dev.belyaevad.texttools.platform.win.enterSingleInstance
import dev.belyaevad.texttools.platform.win.menuPosition
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.awt.Desktop
import java.awt.Font
import java.awt.GraphicsEnvironment
import java.awt.MouseInfo
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.awt.event.WindowEvent
import java.awt.event.WindowFocusListener
import java.awt.image.BufferedImage
import java.io.IOException
import java.net.Authenticator
import java.net.InetSocketAddress
import java.net.PasswordAuthentication
import java.net.Proxy
import java.net.ProxySelector
import java.net.SocketAddress
import java.net.URI
import java.net.URLDecoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Properties
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.system.exitProcess
import java.awt.Color as AwtColor

private const val TOOLTIP_MILLIS = 1_600L
private const val BANNER_HOLD_MILLIS = 2_000L
private const val FOCUS_GRACE_MILLIS = 400L
private const val MENU_WIDTH = 500
private const val BANNER_WIDTH = 340
private const val BANNER_HEIGHT = 110
private const val UI_FILE = "ui.properties"
private const val LAST_FILE = "TextTools_last.txt"
private const val LOG_FILE = "TextTools.log"
private const val RESTART_ENV = "TEXTTOOLS_RESTART"

private const val NO_SELECTION = "Нет выделенного текста"
private const val NO_SOURCE = "Не нашлось окна с текстом"
private const val NOTHING_TO_REPEAT = "Повторять нечего - действий еще не было"
private const val REQUEST_RUNNING = "Запрос уже выполняется"
private const val COPIED = "Скопировано"
private const val NEED_KEY = "Укажи API key в настройках (иконка в трее)"
private const val HOTKEY_FAIL = "Хоткеи не встали. Их уже держит другой TextTools или AutoHotkey."
private const val NO_LOG = "Лога ещё нет"
private const val SAVE_FAILED = "Настройки не записались"
private const val REQUEST_FAILED = "Запрос не выполнен"
private const val COPY_FAILED = "Не удалось скопировать"
private const val FOLDER_FAILED = "Папку не удалось открыть"
private const val RESTART_FAILED = "Не удалось перезапустить"

private val logTime: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault())

private class SettingsSlot(
    initial: Settings,
) {
    @Volatile
    var value: Settings = initial
}

private data class MenuRequest(
    val selection: String,
    val actions: List<Action>,
    val at: ScreenPoint,
    val heightPx: Int,
)

private data class ShownResult(
    val title: String,
    val text: String,
    val model: String,
    val conversation: List<ChatMessage>,
    val chips: List<QuickRefine>,
    val markdown: Boolean,
)

/** Tray entry. Hotkeys are started here; [AppGraph] construction does not start them. */
fun main() {
    ownTheInstance()
    val commands = Channel<GlobalChord>(Channel.UNLIMITED)
    val graph = AppGraph { commands.trySend(it) }
    val closed = AtomicBoolean(false)
    Runtime.getRuntime().addShutdownHook(Thread { closeGraph(graph, closed) })
    graph.watch.start()
    val hotkeysOk = graph.hotkeys.start()
    val slot = SettingsSlot(loadSettings(graph.secrets))
    val startupNote = startupNote(graph, slot.value, hotkeysOk)
    application {
        val runner =
            remember {
                RunAction(
                    complete = { model, messages -> completeCall(graph, slot.value, model, messages) },
                    clipboard = graph.clipboard,
                    store = graph.store,
                    dataDir = dataDirectory(),
                    settings = { slot.value },
                    hasKey = { hasApiKey(graph.secrets) },
                )
            }
        DisposableEffect(Unit) {
            onDispose {
                runner.close()
                closeGraph(graph, closed)
            }
        }
        TextToolsApp(graph, commands, slot, runner, startupNote)
    }
}

@Composable
private fun ApplicationScope.TextToolsApp(
    graph: AppGraph,
    commands: Channel<GlobalChord>,
    slot: SettingsSlot,
    runner: RunAction,
    startupNote: String?,
) {
    val scope = rememberCoroutineScope()
    var settings by remember { mutableStateOf(slot.value) }
    var menu by remember { mutableStateOf<MenuRequest?>(null) }
    var showSettings by remember { mutableStateOf(false) }
    var showEditor by remember { mutableStateOf(false) }
    var showHistory by remember { mutableStateOf(false) }
    var showLast by remember { mutableStateOf(false) }
    var showStats by remember { mutableStateOf(false) }
    var showHistoryCleared by remember { mutableStateOf(false) }
    var showLogCleared by remember { mutableStateOf(false) }
    var shown by remember { mutableStateOf<ShownResult?>(null) }
    var showBanner by remember { mutableStateOf(false) }
    var banner by remember { mutableStateOf<RunStatus?>(null) }
    var lastAction by remember { mutableStateOf<Action?>(null) }
    var lastExtra by remember { mutableStateOf("") }
    var runToken by remember { mutableIntStateOf(0) }
    var flashed by remember { mutableStateOf(startupNote) }
    var flashToken by remember { mutableIntStateOf(if (startupNote == null) 0 else 1) }

    fun flash(text: String) {
        flashed = text.replace('\n', ' ').take(240)
        flashToken++
    }

    fun publish(next: Settings) {
        slot.value = next
        settings = next
    }

    fun launchAction(
        action: Action,
        selection: String,
        instruction: String,
        shift: Boolean,
    ) {
        if (runner.busy) {
            flash(REQUEST_RUNNING)
            return
        }
        if (!hasApiKey(graph.secrets)) {
            flash(NEED_KEY)
            return
        }
        val token = runToken + 1
        runToken = token
        lastAction = action
        lastExtra = instruction
        showBanner = true
        scope.launch {
            var hold = false
            try {
                val outcome =
                    try {
                        withContext(Dispatchers.IO) {
                            runner.run(action, selection, instruction, shift, "", "")
                        }
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        null
                    }
                when (outcome) {
                    null -> flash(REQUEST_FAILED)
                    is RunOutcome.Pasted -> hold = true
                    is RunOutcome.InWindow ->
                        shown =
                            ShownResult(
                                title = action.name,
                                text = outcome.text,
                                model = outcome.model,
                                conversation = outcome.conversation,
                                chips = graph.store.quick(),
                                markdown = slot.value.markdown,
                            )
                    is RunOutcome.Failed -> flash(outcome.message)
                    RunOutcome.Busy -> flash(REQUEST_RUNNING)
                    RunOutcome.NeedsKey -> flash(NEED_KEY)
                }
                if (hold && token == runToken) delay(BANNER_HOLD_MILLIS)
            } finally {
                if (token == runToken) showBanner = false
            }
        }
    }

    LaunchedEffect(flashToken) {
        if (flashToken == 0) return@LaunchedEffect
        delay(TOOLTIP_MILLIS)
        flashed = null
    }
    LaunchedEffect(showBanner) {
        if (!showBanner) {
            banner = null
            return@LaunchedEffect
        }
        while (showBanner) {
            banner = runner.banner()
            delay(100)
        }
    }
    LaunchedEffect(Unit) {
        suspend fun captured(): Pair<String, String?> =
            withContext(Dispatchers.IO) {
                val text = graph.clipboard.captureSeconds(slot.value.clipWaitSeconds)
                text to graph.clipboard.takeNonTextWarning()
            }

        for (chord in commands) {
            if (menu != null) continue
            if (runner.busy) {
                flash(REQUEST_RUNNING)
                continue
            }
            when (chord) {
                GlobalChord.Menu -> {
                    if (!hasSource(graph.watch)) {
                        flash(NO_SOURCE)
                        continue
                    }
                    val (selection, warning) = captured()
                    if (menu != null) continue
                    if (selection.isEmpty()) {
                        flash(warning ?: NO_SELECTION)
                        continue
                    }
                    val actions = graph.store.actions()
                    val placed = placeMenu(actions.size)
                    menu = MenuRequest(selection, actions, placed.first, placed.second)
                }
                GlobalChord.Repeat -> {
                    val action = lastAction
                    if (action == null) {
                        flash(NOTHING_TO_REPEAT)
                        continue
                    }
                    if (!hasSource(graph.watch)) {
                        flash(NO_SOURCE)
                        continue
                    }
                    val (selection, warning) = captured()
                    if (menu != null) continue
                    if (selection.isEmpty()) {
                        flash(warning ?: NO_SELECTION)
                        continue
                    }
                    launchAction(action, selection, lastExtra, false)
                }
            }
        }
    }

    val idleTip = "TextTools - обработка текста через LLM (${settings.hotkey})"
    Tray(
        icon = trayIcon(),
        tooltip = flashed ?: idleTip,
        onAction = { showSettings = true },
        menu = {
            Item("История…") { showHistory = true }
            Item("Показать последний результат") { showLast = true }
            Item("Статистика использования") { showStats = true }
            Separator()
            Item("Редактор действий…") { showEditor = true }
            Item("Настройки") { showSettings = true }
            Separator()
            Item("Очистить историю…") {
                showHistory = false
                clearHistoryFile(dataDirectory())
                graph.store.saveHistory(emptyList())
                showHistoryCleared = true
            }
            Item("Очистить лог…") {
                showStats = false
                clearLogFiles(dataDirectory())
                showLogCleared = true
            }
            Separator()
            Item("Открыть лог") { openLog()?.let { flash(it) } }
            Item("Открыть папку скрипта") { openDataFolder()?.let { flash(it) } }
            Item("Перезапустить") {
                if (relaunch()) exitApplication() else flash(RESTART_FAILED)
            }
            Item("Выход") { exitApplication() }
        },
    )

    menu?.let { request ->
        ActionMenuWindow(
            request = request,
            onDismiss = { menu = null },
            onRun = { action, extra, shift ->
                val selection = request.selection
                menu = null
                launchAction(action, selection, extra, shift)
            },
            onEditor = {
                menu = null
                showEditor = true
            },
        )
    }

    if (showBanner) {
        BusyBanner(banner ?: RunStatus(STATUS_PREPARING, statusSeconds(0)))
    }

    shown?.let { result ->
        ResultWindow(
            title = result.title,
            text = result.text,
            onCopy = { text -> copyText(text) { flash(it) } },
            onPaste = { text -> pasteText(graph.clipboard, text) { flash(it) } },
            onClose = { shown = null },
            model = result.model,
            conversation = result.conversation,
            chips = result.chips,
            markdown = result.markdown,
            complete = { model, messages ->
                withContext(Dispatchers.IO) { completeCall(graph, slot.value, model, messages) }
            },
            onDelivered = { delivery -> rememberDelivery(graph, result, delivery) },
        )
    }

    if (showSettings) {
        SettingsScreen(
            graph = graph,
            settings = settings,
            onSave = { draft ->
                if (saveSettings(graph, draft) { publish(it) }) {
                    if (graph.hotkeys.swap(draft.settings.hotkey, draft.settings.hotkeyRepeat)) {
                        flash("Сохранено. Хоткей меню: ${draft.settings.hotkey}")
                    } else {
                        flash(HOTKEY_FAIL)
                    }
                } else {
                    flash(SAVE_FAILED)
                }
                showSettings = false
            },
            onCancel = { showSettings = false },
        )
    }

    if (showEditor) {
        EditorScreen(
            graph = graph,
            settings = settings,
            onClose = { showEditor = false },
            onSaved = { flash(it) },
        )
    }

    val hooks =
        ResultHooks(
            onCopy = { text -> copyText(text) { flash(it) } },
            onPaste = { text -> pasteText(graph.clipboard, text) { flash(it) } },
            onClose = {
                showHistory = false
                showLast = false
                showStats = false
                showHistoryCleared = false
                showLogCleared = false
            },
        )
    if (showHistory) {
        val rows = remember { graph.store.history() }
        HistoryWindow(rows, hooks.copy(onClose = { showHistory = false }))
    }
    if (showLast) {
        val text = remember { readLast() }
        LastResultWindow(text, hooks.copy(onClose = { showLast = false }))
    }
    if (showStats) {
        val log = remember { readLog() }
        val usage = remember { graph.store.usage() }
        val prices = remember { graph.store.prices() }
        StatsWindow(
            log = log,
            usage = usage,
            catalog = catalogEntries(prices?.body),
            baseUrl = settings.baseUrl,
            catalogueAtEpochMs = prices?.fetchedAtEpochMs,
            hooks = hooks.copy(onClose = { showStats = false }),
        )
    }
    if (showHistoryCleared) {
        HistoryClearedWindow(hooks.copy(onClose = { showHistoryCleared = false }))
    }
    if (showLogCleared) {
        LogClearedWindow(hooks.copy(onClose = { showLogCleared = false }))
    }
}

@Composable
private fun ActionMenuWindow(
    request: MenuRequest,
    onDismiss: () -> Unit,
    onRun: (Action, String, Boolean) -> Unit,
    onEditor: () -> Unit,
) {
    val instruction = remember(request) { mutableStateOf("") }
    val density = LocalDensity.current
    val width = with(density) { MENU_WIDTH.toDp() }
    val height = with(density) { request.heightPx.toDp() }
    val x = with(density) { request.at.x.toDp() }
    val y = with(density) { request.at.y.toDp() }
    Window(
        onCloseRequest = onDismiss,
        title = "TextTools",
        state = rememberWindowState(width = width, height = height, position = WindowPosition(x, y)),
        undecorated = true,
        alwaysOnTop = true,
        resizable = false,
        onPreviewKeyEvent = { event ->
            if (event.type == KeyEventType.KeyDown && event.key == Key.Enter && instruction.value.isNotBlank()) {
                onRun(Factory.custom, instruction.value.trim(), event.isShiftPressed)
                true
            } else {
                false
            }
        },
    ) {
        val host = window
        val openedAt = remember { System.currentTimeMillis() }
        DisposableEffect(host) {
            val listener =
                object : WindowFocusListener {
                    override fun windowGainedFocus(event: WindowEvent) = Unit

                    override fun windowLostFocus(event: WindowEvent) {
                        if (System.currentTimeMillis() - openedAt > FOCUS_GRACE_MILLIS) onDismiss()
                    }
                }
            host.addWindowFocusListener(listener)
            onDispose { host.removeWindowFocusListener(listener) }
        }
        MaterialTheme {
            Surface {
                actionMenu(
                    actions = request.actions,
                    instruction = instruction,
                    onRun = onRun,
                    openEditor = onEditor,
                    dismiss = onDismiss,
                )
            }
        }
    }
}

@Composable
private fun BusyBanner(status: RunStatus) {
    val density = LocalDensity.current
    val corner = bannerTopLeft()
    val width = with(density) { BANNER_WIDTH.toDp() }
    val height = with(density) { BANNER_HEIGHT.toDp() }
    val x = with(density) { corner.first.toDp() }
    val y = with(density) { corner.second.toDp() }
    Window(
        onCloseRequest = {},
        title = "TextTools",
        state = rememberWindowState(width = width, height = height, position = WindowPosition(x, y)),
        undecorated = true,
        alwaysOnTop = true,
        resizable = false,
        focusable = false,
    ) {
        MaterialTheme {
            Surface {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(status.text, fontSize = 14.sp)
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(12.dp))
                    Text(status.seconds, color = Color(0xFF707070), fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun SettingsScreen(
    graph: AppGraph,
    settings: Settings,
    onSave: (SettingsDraft) -> Unit,
    onCancel: () -> Unit,
) {
    val apiKey = remember(settings) { graph.secrets.get(SecretSlot.ApiKey).orEmpty() }
    val models = remember(settings.baseUrl) { graph.store.models(settings.baseUrl) }
    val languages = remember { graph.store.languages().ifEmpty { Factory.languages } }
    val catalog = remember { catalogEntries(graph.store.prices()?.body) }
    SettingsWindow(
        settings = settings,
        apiKey = apiKey,
        models = models,
        languages = languages,
        callbacks =
            SettingsCallbacks(
                onLoadModels = { probe -> withContext(Dispatchers.IO) { loadModelIds(probe) } },
                onTestApi = { probe -> withContext(Dispatchers.IO) { testApi(probe) } },
                onRetarget = { url ->
                    val next = retarget(graph.store.actions(), url)
                    graph.store.saveActions(next)
                    next.mapNotNull { it.model }.filter { it.isNotBlank() }.distinct()
                },
                onSave = onSave,
                onCancel = onCancel,
                catalog = catalog,
            ),
    )
}

@Composable
private fun EditorScreen(
    graph: AppGraph,
    settings: Settings,
    onClose: () -> Unit,
    onSaved: (String) -> Unit,
) {
    val actions = remember { graph.store.actions() }
    val quick = remember { graph.store.quick() }
    val apps = remember { graph.store.apps() }
    val languages = remember { graph.store.languages().ifEmpty { Factory.languages } }
    val models = remember(settings.baseUrl) { graph.store.models(settings.baseUrl).map { it.id } }
    editorWindow(
        actions = actions,
        quick = quick,
        apps = apps,
        languages = languages,
        models = models,
        defaultModel = settings.defaultModel,
        onSave = { nextActions, nextQuick, nextApps ->
            graph.store.saveActions(nextActions)
            graph.store.saveQuick(nextQuick)
            graph.store.saveApps(nextApps)
            onSaved(
                "Сохранено: действий ${nextActions.size}, уточнений ${nextQuick.size}, приложений ${nextApps.size}",
            )
            onClose()
        },
        onCancel = onClose,
    )
}

private fun ownTheInstance() {
    if (System.getenv(RESTART_ENV) == "1") {
        if (!waitToOwnInstance()) exitProcess(0)
    } else {
        enterSingleInstance { exitProcess(0) }
    }
}

private fun waitToOwnInstance(): Boolean {
    val deadline = System.currentTimeMillis() + 15_000
    while (true) {
        if (claimFirstInstance() == InstanceAction.Continue) return true
        if (System.currentTimeMillis() >= deadline) return false
        Thread.sleep(200)
    }
}

private fun startupNote(
    graph: AppGraph,
    settings: Settings,
    hotkeysOk: Boolean,
): String? {
    val factory = Factory.defaultSettings
    val custom = settings.hotkey != factory.hotkey || settings.hotkeyRepeat != factory.hotkeyRepeat
    val applied = if (hotkeysOk && custom) graph.hotkeys.swap(settings.hotkey, settings.hotkeyRepeat) else hotkeysOk
    return when {
        !hotkeysOk || (custom && !applied) -> HOTKEY_FAIL
        !hasApiKey(graph.secrets) -> NEED_KEY
        else -> null
    }
}

private fun hasApiKey(secrets: UserSecrets): Boolean = !secrets.get(SecretSlot.ApiKey).isNullOrBlank()

private fun hasSource(watch: ForegroundWatch): Boolean =
    try {
        watch.javaClass.getMethod("sourceNow").invoke(watch) != null
    } catch (_: Exception) {
        false
    }

private fun closeGraph(
    graph: AppGraph,
    closed: AtomicBoolean,
) {
    if (!closed.compareAndSet(false, true)) return
    try {
        graph.close()
    } catch (_: Exception) {
        return
    }
}

private suspend fun completeCall(
    graph: AppGraph,
    settings: Settings,
    model: String,
    messages: List<ChatMessage>,
): Pair<String, Usage?> {
    val key = graph.secrets.get(SecretSlot.ApiKey).orEmpty()
    return graph.llm.complete(
        baseUrl = settings.baseUrl,
        apiKey = key,
        model = model,
        messages = messages,
        proxy = settings.proxy,
        proxyOn = settings.proxyOn,
    )
}

private fun saveSettings(
    graph: AppGraph,
    draft: SettingsDraft,
    publish: (Settings) -> Unit,
): Boolean =
    try {
        graph.secrets.put(SecretSlot.ApiKey, draft.apiKey)
        graph.secrets.put(SecretSlot.ProxyUrl, draft.settings.proxy)
        graph.store.saveLanguages(draft.languages)
        if (draft.modelsUrl.isNotBlank()) graph.store.saveModels(draft.modelsUrl, draft.models)
        writeUiSettings(draft.settings)
        publish(draft.settings)
        true
    } catch (_: Exception) {
        false
    }

private fun rememberDelivery(
    graph: AppGraph,
    shown: ShownResult,
    delivery: RefineDelivery,
) {
    try {
        val at = System.currentTimeMillis()
        val name = if (delivery.label.isBlank()) shown.title else "${shown.title} (${delivery.label})"
        val row =
            HistoryRecord(
                atEpochMs = at,
                actionName = name,
                model = delivery.model,
                promptTokens = delivery.usage?.promptTokens,
                completionTokens = delivery.usage?.completionTokens,
                cost = null,
                source = delivery.instruction,
                result = delivery.text,
            )
        graph.store.saveHistory((listOf(row) + graph.store.history()).take(HISTORY_LIMIT))
        delivery.usage?.let { usage ->
            graph.store.saveUsage(
                graph.store.usage() + UsageRecord(at, name, delivery.model, usage.promptTokens, usage.completionTokens),
            )
        }
        writeLast(delivery.text)
        appendLog(at, "действие: $name, модель ${delivery.model}")
        appendLog(at, "ok, ответ ${delivery.text.length} символов")
    } catch (_: Exception) {
        return
    }
}

private fun copyText(
    text: String,
    flash: (String) -> Unit,
) {
    try {
        Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(text), null)
        flash(COPIED)
    } catch (_: Exception) {
        flash(COPY_FAILED)
    }
}

private fun pasteText(
    clipboard: ClipboardProtocol,
    text: String,
    flash: (String) -> Unit,
) {
    when (clipboard.paste(text)) {
        is PasteAttempt.Sent -> Unit
        PasteAttempt.ShowInWindow -> flash(NO_SOURCE)
    }
}

private fun openLog(): String? {
    val log = dataDirectory().resolve(LOG_FILE)
    if (!Files.isRegularFile(log)) return NO_LOG
    return try {
        Desktop.getDesktop().open(log.toFile())
        null
    } catch (_: IOException) {
        NO_LOG
    }
}

private fun openDataFolder(): String? =
    try {
        val dir = dataDirectory()
        Files.createDirectories(dir)
        Desktop.getDesktop().open(dir.toFile())
        null
    } catch (_: IOException) {
        FOLDER_FAILED
    }

private fun relaunch(): Boolean =
    try {
        val info = ProcessHandle.current().info()
        val command = info.command().orElse("")
        val args = info.arguments().orElse(emptyArray())
        val builder =
            if (command.isNotEmpty() && args.isNotEmpty()) {
                ProcessBuilder(listOf(command) + args.toList())
            } else {
                val javaBin = Path.of(System.getProperty("java.home"), "bin", "java.exe").toString()
                val classpath = System.getProperty("java.class.path")
                ProcessBuilder(javaBin, "-cp", classpath, "dev.belyaevad.texttools.app.MainKt")
            }
        builder.directory(Path.of(System.getProperty("user.dir")).toFile())
        builder.environment()[RESTART_ENV] = "1"
        builder.start()
        true
    } catch (_: IOException) {
        false
    }

private fun dataDirectory(): Path {
    val local = System.getenv("LOCALAPPDATA")
    if (local.isNullOrBlank()) return Path.of(System.getProperty("user.home"), "AppData", "Local", "TextTools")
    return Path.of(local, "TextTools")
}

private fun loadSettings(secrets: UserSecrets): Settings {
    val factory = Factory.defaultSettings
    val proxy = secrets.get(SecretSlot.ProxyUrl).orEmpty()
    val file = dataDirectory().resolve(UI_FILE)
    if (!Files.isRegularFile(file)) return factory.copy(proxy = proxy, proxyOn = proxy.isNotEmpty())
    return try {
        val props = Properties()
        Files.newBufferedReader(file, StandardCharsets.UTF_8).use { props.load(it) }
        val clip = props.getProperty("clipWaitSeconds")?.toDoubleOrNull() ?: factory.clipWaitSeconds
        factory.copy(
            baseUrl = textProp(props, "baseUrl", factory.baseUrl).ifBlank { factory.baseUrl },
            defaultModel = textProp(props, "defaultModel", factory.defaultModel).ifBlank { factory.defaultModel },
            proxy = proxy,
            proxyOn = proxyFlag(props, proxy.isNotEmpty()),
            hotkey = textProp(props, "hotkey", factory.hotkey).ifBlank { factory.hotkey },
            hotkeyRepeat = textProp(props, "hotkeyRepeat", factory.hotkeyRepeat),
            markdown = boolProp(props, "markdown", factory.markdown),
            uiLang = textProp(props, "uiLang", factory.uiLang).ifBlank { factory.uiLang },
            appProfiling = boolProp(props, "appProfiling", factory.appProfiling),
            clipWaitSeconds = clipWaitSeconds(clip),
        )
    } catch (_: IOException) {
        factory.copy(proxy = proxy, proxyOn = proxy.isNotEmpty())
    }
}

private fun writeUiSettings(settings: Settings) {
    val props = Properties()
    props.setProperty("baseUrl", settings.baseUrl)
    props.setProperty("defaultModel", settings.defaultModel)
    props.setProperty("proxyOn", settings.proxyOn?.toString().orEmpty())
    props.setProperty("hotkey", settings.hotkey)
    props.setProperty("hotkeyRepeat", settings.hotkeyRepeat)
    props.setProperty("markdown", settings.markdown.toString())
    props.setProperty("uiLang", settings.uiLang)
    props.setProperty("appProfiling", settings.appProfiling.toString())
    props.setProperty("clipWaitSeconds", settings.clipWaitSeconds.toString())
    val dir = dataDirectory()
    Files.createDirectories(dir)
    Files.newBufferedWriter(dir.resolve(UI_FILE), StandardCharsets.UTF_8).use { props.store(it, null) }
}

private fun textProp(
    props: Properties,
    key: String,
    fallback: String,
): String = if (props.containsKey(key)) props.getProperty(key).orEmpty().trim() else fallback

private fun boolProp(
    props: Properties,
    key: String,
    fallback: Boolean,
): Boolean = props.getProperty(key)?.toBooleanStrictOrNull() ?: fallback

private fun proxyFlag(
    props: Properties,
    fallback: Boolean,
): Boolean? {
    if (!props.containsKey("proxyOn")) return fallback
    val raw = props.getProperty("proxyOn").orEmpty()
    if (raw.isEmpty()) return null
    return raw.toBooleanStrictOrNull() ?: fallback
}

private fun readLast(): String? {
    val file = dataDirectory().resolve(LAST_FILE)
    if (!Files.isRegularFile(file)) return null
    return try {
        Files.readString(file)
    } catch (_: IOException) {
        null
    }
}

private fun readLog(): String {
    val file = dataDirectory().resolve(LOG_FILE)
    if (!Files.isRegularFile(file)) return ""
    return try {
        Files.readString(file)
    } catch (_: IOException) {
        ""
    }
}

private fun writeLast(text: String) {
    val dir = dataDirectory()
    Files.createDirectories(dir)
    Files.writeString(dir.resolve(LAST_FILE), text, StandardCharsets.UTF_8)
}

private fun appendLog(
    at: Long,
    line: String,
) {
    val dir = dataDirectory()
    Files.createDirectories(dir)
    val stamp = logTime.format(Instant.ofEpochMilli(at))
    Files.writeString(
        dir.resolve(LOG_FILE),
        "$stamp  $line\n",
        StandardCharsets.UTF_8,
        StandardOpenOption.CREATE,
        StandardOpenOption.APPEND,
    )
}

private fun placeMenu(actionCount: Int): Pair<ScreenPoint, Int> {
    val height = menuHeightPx(actionCount)
    val pointer = MouseInfo.getPointerInfo()?.location
    val cursor = ScreenPoint(pointer?.x ?: 0, pointer?.y ?: 0)
    return menuPosition(null, cursor, MENU_WIDTH, height, workAreas()) to height
}

private fun menuHeightPx(actionCount: Int): Int {
    val rows = (actionCount + 2) / 3
    return (160 + rows * 48).coerceIn(240, 760)
}

private fun bannerTopLeft(): Pair<Int, Int> {
    val area = workAreas().firstOrNull() ?: WorkArea(0, 0, 1280, 720)
    val x = (area.right - BANNER_WIDTH - 12).coerceAtLeast(area.left)
    val y = (area.bottom - BANNER_HEIGHT - 12).coerceAtLeast(area.top)
    return x to y
}

private fun workAreas(): List<WorkArea> {
    val toolkit = Toolkit.getDefaultToolkit()
    return GraphicsEnvironment.getLocalGraphicsEnvironment().screenDevices.map { device ->
        val bounds = device.defaultConfiguration.bounds
        val insets = toolkit.getScreenInsets(device.defaultConfiguration)
        WorkArea(
            left = bounds.x + insets.left,
            top = bounds.y + insets.top,
            right = bounds.x + bounds.width - insets.right,
            bottom = bounds.y + bounds.height - insets.bottom,
        )
    }
}

private fun loadModelIds(probe: SettingsProbe): List<String> = modelIds(apiRequest(probe, "models", null))

private fun testApi(probe: SettingsProbe): String {
    val body =
        buildString {
            append("{\"model\":")
            append(jsonQuote(probe.model))
            append(",\"stream\":false,\"messages\":[{\"role\":\"user\",\"content\":\"Reply with ok\"}]}")
        }
    val reply = apiRequest(probe, "chat/completions", body)
    val messageAt = reply.indexOf("\"message\"")
    val slice = if (messageAt < 0) reply else reply.substring(messageAt)
    val content = jsonStringField(slice, "content")?.trim().orEmpty()
    if (content.isEmpty()) error("Модель вернула пустой ответ. Выделенный текст не изменён.")
    return content
}

private fun apiRequest(
    probe: SettingsProbe,
    path: String,
    json: String?,
): String {
    val base = probe.baseUrl.trim().trimEnd('/')
    if (base.isEmpty()) error("Адрес API пуст")
    val uri =
        try {
            URI("$base/$path")
        } catch (_: IllegalArgumentException) {
            error("Адрес API не разобран")
        }
    val previous = Authenticator.getDefault()
    var client: HttpClient? = null
    return try {
        installProxyAuth(probe)
        client = openClient(probe)
        val builder =
            HttpRequest
                .newBuilder(uri)
                .timeout(Duration.ofSeconds(60))
                .header("Authorization", "Bearer ${probe.apiKey}")
        val request =
            if (json == null) {
                builder.GET().build()
            } else {
                builder
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                    .build()
            }
        val response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
        val code = response.statusCode()
        if (code !in 200..299) error(httpStatus(code))
        response.body()
    } catch (_: IOException) {
        error("Провайдер недоступен")
    } finally {
        Authenticator.setDefault(previous)
        client?.close()
    }
}

private fun openClient(probe: SettingsProbe): HttpClient {
    val builder = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20))
    val proxy = proxyFor(probe) ?: return builder.build()
    builder.proxy(FixedProxy(proxy))
    return builder.build()
}

private fun proxyFor(probe: SettingsProbe): Proxy? {
    val raw = effectiveProxy(probe.proxy, probe.proxyOn)
    if (raw.isEmpty() || isLoopback(probe.baseUrl)) return null
    val value =
        try {
            normalizeProxy(raw)
        } catch (_: IllegalArgumentException) {
            error("Прокси не разобран")
        }
    val uri =
        try {
            URI(value)
        } catch (_: IllegalArgumentException) {
            error("Прокси не разобран")
        }
    val host = uri.host ?: error("Прокси не разобран")
    val scheme = uri.scheme?.lowercase().orEmpty()
    val port =
        when {
            uri.port != -1 -> uri.port
            scheme == "http" -> 80
            scheme == "https" -> 443
            scheme.startsWith("socks") -> 1080
            else -> error("Прокси не разобран")
        }
    val type = if (scheme.startsWith("socks")) Proxy.Type.SOCKS else Proxy.Type.HTTP
    return Proxy(type, InetSocketAddress(host, port))
}

private fun installProxyAuth(probe: SettingsProbe) {
    val raw = effectiveProxy(probe.proxy, probe.proxyOn)
    if (raw.isEmpty()) return
    val value =
        try {
            normalizeProxy(raw)
        } catch (_: IllegalArgumentException) {
            return
        }
    val info =
        try {
            URI(value).rawUserInfo
        } catch (_: IllegalArgumentException) {
            return
        }
    if (info.isNullOrEmpty()) return
    val colon = info.indexOf(':')
    val user = if (colon < 0) info else info.substring(0, colon)
    val password = if (colon < 0) "" else info.substring(colon + 1)
    val decoded =
        try {
            URLDecoder.decode(user, StandardCharsets.UTF_8) to URLDecoder.decode(password, StandardCharsets.UTF_8)
        } catch (_: IllegalArgumentException) {
            return
        }
    Authenticator.setDefault(
        object : Authenticator() {
            override fun getPasswordAuthentication(): PasswordAuthentication? {
                if (requestorType != RequestorType.PROXY) return null
                return PasswordAuthentication(decoded.first, decoded.second.toCharArray())
            }
        },
    )
}

private class FixedProxy(
    private val proxy: Proxy,
) : ProxySelector() {
    override fun select(uri: URI?): List<Proxy> = listOf(proxy)

    override fun connectFailed(
        uri: URI?,
        address: SocketAddress?,
        error: IOException?,
    ) = Unit
}

private fun httpStatus(code: Int): String =
    when (code) {
        401 -> "Ключ не принят (401)."
        402 -> "Не хватает средств (402)."
        404 -> "Адрес или модель не найдены (404)."
        429 -> "Слишком много запросов (429)."
        in 500..599 -> "Провайдер недоступен ($code)."
        else -> "HTTP $code"
    }

private fun modelIds(body: String): List<String> {
    val ids = mutableListOf<String>()
    var from = 0
    while (from < body.length) {
        val at = body.indexOf("\"id\"", from)
        if (at < 0) break
        val value = jsonStringFieldAt(body, at + 4)
        if (!value.isNullOrEmpty()) ids += value
        from = at + 4
    }
    return ids.distinct()
}

private fun catalogEntries(body: String?): List<CatalogEntry> {
    if (body.isNullOrBlank()) return emptyList()
    val marks = mutableListOf<Pair<String, Int>>()
    var from = 0
    while (from < body.length) {
        val at = body.indexOf("\"id\"", from)
        if (at < 0) break
        val id = jsonStringFieldAt(body, at + 4)
        if (!id.isNullOrEmpty()) marks += id to at
        from = at + 4
    }
    return marks.mapIndexed { index, (id, at) ->
        val end = marks.getOrNull(index + 1)?.second ?: body.length
        val slice = body.substring(at, end)
        CatalogEntry(
            id = id,
            promptPerToken = numberIn(slice, "prompt"),
            completionPerToken = numberIn(slice, "completion"),
            contextLength = numberIn(slice, "context_length")?.toInt(),
        )
    }
}

private fun jsonStringField(
    body: String,
    field: String,
): String? {
    val at = body.indexOf("\"$field\"")
    if (at < 0) return null
    return jsonStringFieldAt(body, at + field.length + 2)
}

private fun jsonStringFieldAt(
    body: String,
    from: Int,
): String? {
    var index = from
    while (index < body.length && body[index].isWhitespace()) index++
    if (index >= body.length || body[index] != ':') return null
    index++
    while (index < body.length && body[index].isWhitespace()) index++
    if (index >= body.length || body[index] != '"') return null
    index++
    val out = StringBuilder()
    while (index < body.length) {
        val char = body[index]
        if (char == '\\' && index + 1 < body.length) {
            out.append(
                when (val next = body[index + 1]) {
                    'n' -> '\n'
                    'r' -> '\r'
                    't' -> '\t'
                    '"' -> '"'
                    '\\' -> '\\'
                    else -> next
                },
            )
            index += 2
            continue
        }
        if (char == '"') return out.toString()
        out.append(char)
        index++
    }
    return null
}

private fun jsonQuote(text: String): String =
    buildString {
        append('"')
        text.forEach { char ->
            when (char) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                else -> append(char)
            }
        }
        append('"')
    }

private fun numberIn(
    slice: String,
    key: String,
): Double? {
    val keyAt = slice.indexOf("\"$key\"")
    if (keyAt < 0) return null
    var index = keyAt + key.length + 2
    while (index < slice.length && (slice[index].isWhitespace() || slice[index] == ':')) index++
    if (index >= slice.length || slice.startsWith("null", index)) return null
    if (slice[index] == '"') {
        val end = slice.indexOf('"', index + 1)
        if (end < 0) return null
        return slice.substring(index + 1, end).toDoubleOrNull()
    }
    val end =
        slice.indexOfAny(charArrayOf(',', '}', ' ', '\n', '\r', '\t'), index).let { found ->
            if (found < 0) slice.length else found
        }
    return slice.substring(index, end).toDoubleOrNull()
}

private fun trayIcon(): BitmapPainter {
    val image = BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB)
    val graphics = image.createGraphics()
    graphics.color = AwtColor(108, 77, 255)
    graphics.fillRoundRect(2, 2, 28, 28, 8, 8)
    graphics.color = AwtColor.WHITE
    graphics.font = Font("Segoe UI", Font.BOLD, 18)
    graphics.drawString("T", 8, 23)
    graphics.dispose()
    return BitmapPainter(image.toComposeImageBitmap())
}
