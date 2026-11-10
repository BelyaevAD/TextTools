package dev.belyaevad.texttools.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.ApplicationScope
import androidx.compose.ui.window.Tray
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import dev.belyaevad.texttools.app.ui.AppIcon
import dev.belyaevad.texttools.app.ui.AppLocale
import dev.belyaevad.texttools.app.ui.AppTheme
import dev.belyaevad.texttools.app.ui.BANNER_OUTER_HEIGHT
import dev.belyaevad.texttools.app.ui.BANNER_OUTER_WIDTH
import dev.belyaevad.texttools.app.ui.MENU_OUTER_WIDTH
import dev.belyaevad.texttools.app.ui.UiText
import dev.belyaevad.texttools.app.ui.applyUiLocale
import dev.belyaevad.texttools.app.ui.bannerCorner
import dev.belyaevad.texttools.app.ui.editor.editorWindow
import dev.belyaevad.texttools.app.ui.history.ACTION_MARK
import dev.belyaevad.texttools.app.ui.history.HistoryClearedWindow
import dev.belyaevad.texttools.app.ui.history.HistoryWindow
import dev.belyaevad.texttools.app.ui.history.LastResultWindow
import dev.belyaevad.texttools.app.ui.history.LogClearedWindow
import dev.belyaevad.texttools.app.ui.history.ResultHooks
import dev.belyaevad.texttools.app.ui.history.StatsWindow
import dev.belyaevad.texttools.app.ui.history.clearHistoryFile
import dev.belyaevad.texttools.app.ui.history.clearLogFiles
import dev.belyaevad.texttools.app.ui.installApplicationIcon
import dev.belyaevad.texttools.app.ui.menu.actionMenu
import dev.belyaevad.texttools.app.ui.menuOuterHeight
import dev.belyaevad.texttools.app.ui.resolve
import dev.belyaevad.texttools.app.ui.result.RefineDelivery
import dev.belyaevad.texttools.app.ui.result.ResultWindow
import dev.belyaevad.texttools.app.ui.run.AppProfiler
import dev.belyaevad.texttools.app.ui.run.RunAction
import dev.belyaevad.texttools.app.ui.run.RunPhase
import dev.belyaevad.texttools.app.ui.run.RunStatus
import dev.belyaevad.texttools.app.ui.run.RunUsage
import dev.belyaevad.texttools.app.ui.run.bannerMoney
import dev.belyaevad.texttools.app.ui.run.groupedCount
import dev.belyaevad.texttools.app.ui.run.requestCost
import dev.belyaevad.texttools.app.ui.run.statusSeconds
import dev.belyaevad.texttools.app.ui.settings.SettingsCallbacks
import dev.belyaevad.texttools.app.ui.settings.SettingsDraft
import dev.belyaevad.texttools.app.ui.settings.SettingsProbe
import dev.belyaevad.texttools.app.ui.settings.SettingsWindow
import dev.belyaevad.texttools.app.ui.systemFormatLocale
import dev.belyaevad.texttools.app.ui.uiText
import dev.belyaevad.texttools.data.HistoryRecord
import dev.belyaevad.texttools.data.TextToolsStore
import dev.belyaevad.texttools.data.UsageEvent
import dev.belyaevad.texttools.data.UsageRecord
import dev.belyaevad.texttools.domain.Action
import dev.belyaevad.texttools.domain.AppException
import dev.belyaevad.texttools.domain.CatalogEntry
import dev.belyaevad.texttools.domain.ChatMessage
import dev.belyaevad.texttools.domain.ErrorReason
import dev.belyaevad.texttools.domain.Factory
import dev.belyaevad.texttools.domain.HISTORY_LIMIT
import dev.belyaevad.texttools.domain.QuickRefine
import dev.belyaevad.texttools.domain.RequestMetric
import dev.belyaevad.texttools.domain.RunOutcome
import dev.belyaevad.texttools.domain.Settings
import dev.belyaevad.texttools.domain.Usage
import dev.belyaevad.texttools.domain.clipWaitSeconds
import dev.belyaevad.texttools.domain.effectiveProxy
import dev.belyaevad.texttools.domain.isLoopback
import dev.belyaevad.texttools.domain.normalizeProxy
import dev.belyaevad.texttools.domain.profilingModel
import dev.belyaevad.texttools.domain.retarget
import dev.belyaevad.texttools.domain.searchModel
import dev.belyaevad.texttools.platform.ClipboardProtocol
import dev.belyaevad.texttools.platform.PasteAttempt
import dev.belyaevad.texttools.platform.win.GlobalChord
import dev.belyaevad.texttools.platform.win.InstanceAction
import dev.belyaevad.texttools.platform.win.ScreenPoint
import dev.belyaevad.texttools.platform.win.SecretSlot
import dev.belyaevad.texttools.platform.win.SourceSnapshot
import dev.belyaevad.texttools.platform.win.UserSecrets
import dev.belyaevad.texttools.platform.win.WindowBounds
import dev.belyaevad.texttools.platform.win.WindowsRunKey
import dev.belyaevad.texttools.platform.win.autostartEnabled
import dev.belyaevad.texttools.platform.win.claimFirstInstance
import dev.belyaevad.texttools.platform.win.currentInstalledExe
import dev.belyaevad.texttools.platform.win.currentWorkAreas
import dev.belyaevad.texttools.platform.win.enterSingleInstance
import dev.belyaevad.texttools.platform.win.hasUserSource
import dev.belyaevad.texttools.platform.win.installedAutostart
import dev.belyaevad.texttools.platform.win.menuPosition
import dev.belyaevad.texttools.platform.win.sourceSnapshot
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import texttools.app.generated.resources.Res
import texttools.app.generated.resources.app_copied
import texttools.app.generated.resources.app_copy_failed
import texttools.app.generated.resources.app_editor_saved
import texttools.app.generated.resources.app_folder_failed
import texttools.app.generated.resources.app_hotkey_fail
import texttools.app.generated.resources.app_need_key
import texttools.app.generated.resources.app_no_log
import texttools.app.generated.resources.app_no_selection
import texttools.app.generated.resources.app_no_source
import texttools.app.generated.resources.app_nothing_to_repeat
import texttools.app.generated.resources.app_profile_label
import texttools.app.generated.resources.app_profile_unknown
import texttools.app.generated.resources.app_request_failed
import texttools.app.generated.resources.app_request_running
import texttools.app.generated.resources.app_restart_failed
import texttools.app.generated.resources.app_run_done
import texttools.app.generated.resources.app_run_estimate
import texttools.app.generated.resources.app_run_estimate_cost
import texttools.app.generated.resources.app_run_preparing
import texttools.app.generated.resources.app_run_usage
import texttools.app.generated.resources.app_run_usage_cost
import texttools.app.generated.resources.app_save_failed
import texttools.app.generated.resources.app_seconds
import texttools.app.generated.resources.app_settings_saved
import texttools.app.generated.resources.app_tray_clear_history
import texttools.app.generated.resources.app_tray_clear_log
import texttools.app.generated.resources.app_tray_editor
import texttools.app.generated.resources.app_tray_exit
import texttools.app.generated.resources.app_tray_history
import texttools.app.generated.resources.app_tray_last
import texttools.app.generated.resources.app_tray_open_folder
import texttools.app.generated.resources.app_tray_open_log
import texttools.app.generated.resources.app_tray_restart
import texttools.app.generated.resources.app_tray_settings
import texttools.app.generated.resources.app_tray_stats
import texttools.app.generated.resources.app_tray_tip
import texttools.app.generated.resources.error_empty_response
import texttools.app.generated.resources.error_empty_url
import texttools.app.generated.resources.error_http
import texttools.app.generated.resources.error_insufficient_funds
import texttools.app.generated.resources.error_invalid_proxy
import texttools.app.generated.resources.error_invalid_url
import texttools.app.generated.resources.error_not_found
import texttools.app.generated.resources.error_provider_unavailable
import texttools.app.generated.resources.error_rate_limited
import texttools.app.generated.resources.error_secret_store
import texttools.app.generated.resources.error_unauthorized
import texttools.app.generated.resources.error_unknown
import texttools.app.generated.resources.settings_profile_added
import texttools.app.generated.resources.settings_profile_window
import java.awt.Desktop
import java.awt.MouseInfo
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.awt.event.WindowEvent
import java.awt.event.WindowFocusListener
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
import java.util.Locale
import java.util.Properties
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.system.exitProcess

private const val TOOLTIP_MILLIS = 1_600L
private const val BANNER_HOLD_MILLIS = 2_000L
private const val FOCUS_GRACE_MILLIS = 400L
private const val MENU_WIDTH = MENU_OUTER_WIDTH
private const val BANNER_WIDTH = BANNER_OUTER_WIDTH
private const val BANNER_HEIGHT = BANNER_OUTER_HEIGHT
private const val UI_FILE = "ui.properties"
private const val LAST_FILE = "TextTools_last.txt"
private const val LOG_FILE = "TextTools.log"
private const val RESTART_ENV = "TEXTTOOLS_RESTART"

private val logTime: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault())

private class SettingsSlot(
    initial: Settings,
) {
    @Volatile
    var value: Settings = initial
}

private val NO_SELECTION = uiText(Res.string.app_no_selection)
private val NO_SOURCE = uiText(Res.string.app_no_source)
private val NOTHING_TO_REPEAT = uiText(Res.string.app_nothing_to_repeat)
private val REQUEST_RUNNING = uiText(Res.string.app_request_running)
private val NEED_KEY = uiText(Res.string.app_need_key)
private val HOTKEY_FAIL = uiText(Res.string.app_hotkey_fail)
private val SAVE_FAILED = uiText(Res.string.app_save_failed)
private val REQUEST_FAILED = uiText(Res.string.app_request_failed)
private const val PROXY_UNPARSED = "invalid proxy"

private data class MenuRequest(
    val selection: String,
    val actions: List<Action>,
    val at: ScreenPoint,
    val heightPx: Int,
    val source: SourceSnapshot,
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
    installApplicationIcon()
    Locale.setDefault(Locale.Category.FORMAT, systemFormatLocale)
    ownTheInstance()
    val commands = Channel<GlobalChord>(Channel.UNLIMITED)
    val graph = AppGraph { commands.trySend(it) }
    val closed = AtomicBoolean(false)
    Runtime.getRuntime().addShutdownHook(Thread { closeGraph(graph, closed) })
    graph.watch.start()
    val hotkeysOk = graph.hotkeys.start()
    val slot = SettingsSlot(loadSettings(graph.secrets))
    applyUiLocale(slot.value.uiLang)
    val sentryFile = loadSentryFile(sentryConfigFile(Path.of(System.getProperty("user.dir")), dataDirectory()))
    applySentry(sentryFile, slot.value)
    val startupNote = startupNote(graph, slot.value, hotkeysOk)
    application {
        val telemetry =
            remember { UsageTelemetry(graph.store, { TelemetrySettings(slot.value.sentryTraces, sentryFile.dsn) }, SentryUsageMetricSink) }
        val runner =
            remember {
                RunAction(
                    complete = { model, messages -> completeCall(graph, slot.value, model, messages) },
                    clipboard = graph.clipboard,
                    store = graph.store,
                    dataDir = dataDirectory(),
                    settings = { slot.value },
                    hasKey = { hasApiKey(graph.secrets) },
                    onRequest = { event -> graph.store.appendUsageEvent(event.toStored()) },
                )
            }
        DisposableEffect(Unit) {
            onDispose {
                runner.close()
                telemetry.close()
                closeGraph(graph, closed)
            }
        }
        TextToolsApp(graph, commands, slot, runner, startupNote, sentryFile, telemetry)
    }
}

@Composable
private fun ApplicationScope.TextToolsApp(
    graph: AppGraph,
    commands: Channel<GlobalChord>,
    slot: SettingsSlot,
    runner: RunAction,
    startupNote: UiText?,
    sentryFile: SentryFile,
    telemetry: UsageTelemetry,
) {
    val uiLanguage = remember { mutableStateOf(slot.value.uiLang) }
    AppLocale(uiLanguage.value) {
        textToolsContent(graph, commands, slot, runner, startupNote, sentryFile, telemetry, uiLanguage)
    }
}

@Composable
private fun ApplicationScope.textToolsContent(
    graph: AppGraph,
    commands: Channel<GlobalChord>,
    slot: SettingsSlot,
    runner: RunAction,
    startupNote: UiText?,
    sentryFile: SentryFile,
    telemetry: UsageTelemetry,
    uiLanguage: MutableState<String>,
) {
    val language = uiLanguage.value
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
    val refineChars = remember { intArrayOf(0) }
    var bannerAt by remember { mutableStateOf(ScreenPoint(0, 0)) }
    var lastAction by remember { mutableStateOf<Action?>(null) }
    var lastExtra by remember { mutableStateOf("") }
    var runToken by remember { mutableIntStateOf(0) }
    var flashed by remember { mutableStateOf<UiText?>(startupNote) }
    var flashToken by remember { mutableIntStateOf(if (startupNote == null) 0 else 1) }
    val profileAdded = stringResource(Res.string.settings_profile_added)
    val profileWindow = stringResource(Res.string.settings_profile_window)
    val profileUnknown = stringResource(Res.string.app_profile_unknown)
    val profileLabel = stringResource(Res.string.app_profile_label)

    fun flash(message: UiText) {
        flashed = message
        flashToken++
    }

    fun publish(next: Settings) {
        slot.value = next
        settings = next
        uiLanguage.value = next.uiLang
    }

    val runs =
        ActionRun(
            graph = graph,
            runner = runner,
            scope = scope,
            slot = slot,
            profileAdded = profileAdded,
            profileWindow = profileWindow,
            profileUnknown = profileUnknown,
            profileLabel = profileLabel,
            hooks =
                ActionHooks(
                    flash = ::flash,
                    openSettings = { showSettings = true },
                    nextToken = {
                        val token = runToken + 1
                        runToken = token
                        token
                    },
                    rememberRun = { action, extra ->
                        lastAction = action
                        lastExtra = extra
                    },
                    showBanner = { visible -> showBanner = visible },
                    placeBanner = { bannerAt = it },
                    showResult = { result -> shown = result },
                    currentToken = { runToken },
                ),
        )
    val currentRuns = remember { mutableStateOf<ActionRun?>(null) }
    currentRuns.value = runs

    LaunchedEffect(flashToken) { clearFlash(flashToken) { flashed = null } }
    LaunchedEffect(telemetry) {
        while (true) {
            telemetry.flushCompleted()
            delay(TELEMETRY_HOUR_MS)
        }
    }
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            graph.catalogue.refresh(graph.store, slot.value.proxy, slot.value.proxyOn)
        }
    }
    LaunchedEffect(showBanner) {
        trackBanner(visible = { showBanner }, setBanner = { banner = it }, current = runner::banner)
    }
    LaunchedEffect(Unit) {
        handleChords(
            commands = commands,
            menuOpen = { menu != null },
            busy = { runner.busy },
            flash = ::flash,
            onMenu = {
                captureMenu(
                    graph = graph,
                    seconds = slot.value.clipWaitSeconds,
                    menuOpen = { menu != null },
                    flash = ::flash,
                ) { request ->
                    menu = request
                }
            },
            onRepeat = {
                val active = currentRuns.value
                if (active != null) {
                    captureRepeat(
                        graph = graph,
                        seconds = slot.value.clipWaitSeconds,
                        action = lastAction,
                        extra = lastExtra,
                        menuOpen = { menu != null },
                        runs = active,
                    )
                }
            },
        )
    }

    key(language) {
        val idleTip = uiText(Res.string.app_tray_tip, settings.hotkey)
        Tray(
            icon = AppIcon,
            tooltip = flashed?.let { resolve(it).replace('\n', ' ').take(240) } ?: resolve(idleTip),
            onAction = { showSettings = true },
            menu = {
                Item(stringResource(Res.string.app_tray_last)) { showLast = true }
                Item(stringResource(Res.string.app_tray_history)) { showHistory = true }
                Item(stringResource(Res.string.app_tray_stats)) { showStats = true }
                Separator()
                Item(stringResource(Res.string.app_tray_editor)) { showEditor = true }
                Item(stringResource(Res.string.app_tray_settings)) { showSettings = true }
                Separator()
                Item(stringResource(Res.string.app_tray_open_log)) { openLog()?.let { flash(it) } }
                Item(stringResource(Res.string.app_tray_open_folder)) { openDataFolder()?.let { flash(it) } }
                Separator()
                Item(stringResource(Res.string.app_tray_clear_history)) {
                    showHistory = false
                    clearHistoryFile(dataDirectory())
                    graph.store.saveHistory(emptyList())
                    showHistoryCleared = true
                }
                Item(stringResource(Res.string.app_tray_clear_log)) {
                    showStats = false
                    clearLogFiles(dataDirectory())
                    showLogCleared = true
                }
                Separator()
                Item(stringResource(Res.string.app_tray_restart)) {
                    if (relaunch()) exitApplication() else flash(uiText(Res.string.app_restart_failed))
                }
                Item(stringResource(Res.string.app_tray_exit)) { exitApplication() }
            },
        )
    }

    menu?.let { request ->
        ActionMenuWindow(
            request = request,
            onDismiss = { menu = null },
            onRun = { action, extra, shift ->
                val selection = request.selection
                menu = null
                runs.start(action, selection, extra, shift, request.at, request.source)
            },
            onEditor = {
                menu = null
                showEditor = true
            },
            language = language,
        )
    }

    val open =
        OpenWindows(
            showBanner = showBanner,
            banner = banner,
            bannerAt = bannerAt,
            shown = shown,
            showSettings = showSettings,
            showEditor = showEditor,
            showHistory = showHistory,
            showLast = showLast,
            showStats = showStats,
            showHistoryCleared = showHistoryCleared,
            showLogCleared = showLogCleared,
        )
    val windowActions =
        WindowActions(
            onSave = { draft ->
                applySavedSettings(graph, draft, sentryFile, ::publish, ::flash)
                showSettings = false
            },
            onCancelSettings = { showSettings = false },
            onCloseEditor = { showEditor = false },
            onSaved = ::flash,
            dismissShown = { shown = null },
            dismissHistory = { showHistory = false },
            dismissLast = { showLast = false },
            dismissStats = { showStats = false },
            dismissHistoryCleared = { showHistoryCleared = false },
            dismissLogCleared = { showLogCleared = false },
            copy = { text -> copyText(text, ::flash) },
            paste = { text -> pasteText(graph.clipboard, text, ::flash) },
            dismissAll = {
                showHistory = false
                showLast = false
                showStats = false
                showHistoryCleared = false
                showLogCleared = false
            },
        )
    toolWindows(
        open,
        graph,
        slot,
        settings,
        windowActions,
        refineChars,
        language,
    )
}

private data class ActionHooks(
    val flash: (UiText) -> Unit,
    val openSettings: () -> Unit,
    val nextToken: () -> Int,
    val rememberRun: (Action, String) -> Unit,
    val showBanner: (Boolean) -> Unit,
    val placeBanner: (ScreenPoint) -> Unit,
    val showResult: (ShownResult) -> Unit,
    val currentToken: () -> Int,
)

private class ActionRun(
    private val graph: AppGraph,
    private val runner: RunAction,
    private val scope: kotlinx.coroutines.CoroutineScope,
    private val slot: SettingsSlot,
    private val profileAdded: String,
    private val profileWindow: String,
    private val profileUnknown: String,
    private val profileLabel: String,
    private val hooks: ActionHooks,
) {
    fun start(
        action: Action,
        selection: String,
        instruction: String,
        shift: Boolean,
        at: ScreenPoint,
        source: SourceSnapshot,
    ) {
        if (runner.busy) {
            hooks.flash(REQUEST_RUNNING)
            return
        }
        if (!hasApiKey(graph.secrets)) {
            hooks.flash(NEED_KEY)
            hooks.openSettings()
            return
        }
        val token = hooks.nextToken()
        hooks.rememberRun(action, instruction)
        hooks.placeBanner(at)
        scope.launch {
            runBody(action, selection, instruction, shift, source.proc, source.title, source.windowClass, token)
        }
    }

    fun flash(message: UiText) = hooks.flash(message)

    private suspend fun runBody(
        action: Action,
        selection: String,
        instruction: String,
        shift: Boolean,
        proc: String,
        title: String,
        windowClass: String,
        token: Int,
    ) {
        try {
            val current = slot.value
            val description = profileDescription(title.trim().take(60), profileAdded, profileWindow)
            val model =
                searchModel(
                    profilingModel(current.profileModel, current.defaultModel),
                    current.baseUrl,
                )
            AppProfiler(
                apps = { graph.store.apps().ifEmpty { Factory.apps } },
                save = graph.store::saveApps,
                search = { procName, windowTitle ->
                    graph.search.notes(procName, windowTitle, current.proxy, current.proxyOn)
                },
                complete =
                    measureProviderCall(
                        kind = "profile",
                        inputChars = { proc.length + title.length },
                        store = graph.store,
                        complete = { _, messages -> completeCall(graph, current, model, messages) },
                    ),
                background = { block -> scope.launch(Dispatchers.IO) { block() } },
                profilingModel = model,
                note = runner::note,
                onUsage = { usage ->
                    graph.store.appendUsage(
                        UsageRecord(
                            System.currentTimeMillis(),
                            profileLabel,
                            model,
                            usage.promptTokens,
                            usage.completionTokens,
                        ),
                    )
                },
            ).consider(
                proc = proc,
                title = title,
                windowClass = windowClass,
                enabled = current.appProfiling && model.isNotBlank(),
                description = description,
                addedPrefix = profileAdded,
                unknownDescription = profileUnknown,
            )
            hooks.showBanner(true)
            withContext(Dispatchers.IO) {
                graph.catalogue.refresh(graph.store, current.proxy, current.proxyOn)
            }
            val outcome = actionOutcome(action, selection, instruction, shift, proc, title)
            val hold = publishOutcome(action, outcome)
            if (hold && token == hooks.currentToken()) delay(BANNER_HOLD_MILLIS)
        } finally {
            if (token == hooks.currentToken()) hooks.showBanner(false)
        }
    }

    private suspend fun actionOutcome(
        action: Action,
        selection: String,
        instruction: String,
        shift: Boolean,
        proc: String,
        title: String,
    ): RunOutcome? =
        try {
            withContext(Dispatchers.IO) {
                runner.run(action, selection, instruction, shift, proc, title)
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            reportFailure(error)
            null
        }

    private fun publishOutcome(
        action: Action,
        outcome: RunOutcome?,
    ): Boolean {
        when (outcome) {
            null -> hooks.flash(REQUEST_FAILED)
            is RunOutcome.Pasted -> return true
            is RunOutcome.InWindow ->
                hooks.showResult(
                    ShownResult(
                        title = action.name,
                        text = outcome.text,
                        model = outcome.model,
                        conversation = outcome.conversation,
                        chips = graph.store.quick(),
                        markdown = slot.value.markdown,
                    ),
                )
            is RunOutcome.Failed -> hooks.flash(uiText(outcome.reason.resource, outcome.httpStatus?.toString().orEmpty()))
            RunOutcome.Busy -> hooks.flash(REQUEST_RUNNING)
            RunOutcome.NeedsKey -> {
                hooks.flash(NEED_KEY)
                hooks.openSettings()
            }
        }
        return false
    }
}

private fun profileDescription(
    shortTitle: String,
    added: String,
    window: String,
): String = if (shortTitle.isEmpty()) added else "$added ${fillTemplate(window, shortTitle)}"

private suspend fun clearFlash(
    token: Int,
    clear: () -> Unit,
) {
    if (token == 0) return
    delay(TOOLTIP_MILLIS)
    clear()
}

private suspend fun trackBanner(
    visible: () -> Boolean,
    setBanner: (RunStatus?) -> Unit,
    current: () -> RunStatus?,
) {
    if (!visible()) {
        setBanner(null)
        return
    }
    while (visible()) {
        setBanner(current())
        delay(100)
    }
}

private suspend fun handleChords(
    commands: Channel<GlobalChord>,
    menuOpen: () -> Boolean,
    busy: () -> Boolean,
    flash: (UiText) -> Unit,
    onMenu: suspend () -> Unit,
    onRepeat: suspend () -> Unit,
) {
    for (chord in commands) {
        if (menuOpen()) continue
        if (busy()) {
            flash(REQUEST_RUNNING)
            continue
        }
        when (chord) {
            GlobalChord.Menu -> onMenu()
            GlobalChord.Repeat -> onRepeat()
        }
    }
}

private suspend fun takeSelection(
    graph: AppGraph,
    seconds: Double,
): Pair<String, UiText?> =
    withContext(Dispatchers.IO) {
        graph.clipboard.captureSeconds(seconds) to graph.clipboard.takeNonTextWarning()?.let { uiText(Res.string.app_no_selection) }
    }

private suspend fun captureMenu(
    graph: AppGraph,
    seconds: Double,
    menuOpen: () -> Boolean,
    flash: (UiText) -> Unit,
    open: (MenuRequest) -> Unit,
) {
    if (!hasUserSource(graph.watch)) {
        flash(NO_SOURCE)
        return
    }
    val source = sourceSnapshot(graph.watch)
    val caret = graph.watch.caretNow()
    val cursor = pointerNow()
    val (selection, warning) = takeSelection(graph, seconds)
    if (menuOpen()) return
    if (selection.isEmpty()) {
        flash(warning ?: NO_SELECTION)
        return
    }
    val actions = graph.store.actions()
    val placed = placeMenu(actions.size, caret, cursor, source.bounds)
    open(MenuRequest(selection, actions, placed.first, placed.second, source))
}

private suspend fun captureRepeat(
    graph: AppGraph,
    seconds: Double,
    action: Action?,
    extra: String,
    menuOpen: () -> Boolean,
    runs: ActionRun,
) {
    if (action == null) {
        runs.flash(NOTHING_TO_REPEAT)
        return
    }
    if (!hasUserSource(graph.watch)) {
        runs.flash(NO_SOURCE)
        return
    }
    val source = sourceSnapshot(graph.watch)
    val at = source.bounds?.anchor() ?: pointerNow()
    val (selection, warning) = takeSelection(graph, seconds)
    if (menuOpen()) return
    if (selection.isEmpty()) {
        runs.flash(warning ?: NO_SELECTION)
        return
    }
    runs.start(action, selection, extra, false, at, source)
}

private fun applySavedSettings(
    graph: AppGraph,
    draft: SettingsDraft,
    sentryFile: SentryFile,
    publish: (Settings) -> Unit,
    flash: (UiText) -> Unit,
) {
    if (saveSettings(graph, draft, sentryFile, publish)) {
        if (graph.hotkeys.swap(draft.settings.hotkey, draft.settings.hotkeyRepeat)) {
            flash(uiText(Res.string.app_settings_saved, draft.settings.hotkey))
        } else {
            flash(HOTKEY_FAIL)
        }
    } else {
        flash(SAVE_FAILED)
    }
}

private data class OpenWindows(
    val showBanner: Boolean,
    val banner: RunStatus?,
    val bannerAt: ScreenPoint,
    val shown: ShownResult?,
    val showSettings: Boolean,
    val showEditor: Boolean,
    val showHistory: Boolean,
    val showLast: Boolean,
    val showStats: Boolean,
    val showHistoryCleared: Boolean,
    val showLogCleared: Boolean,
)

private data class WindowActions(
    val onSave: (SettingsDraft) -> Unit,
    val onCancelSettings: () -> Unit,
    val onCloseEditor: () -> Unit,
    val onSaved: (UiText) -> Unit,
    val dismissShown: () -> Unit,
    val dismissHistory: () -> Unit,
    val dismissLast: () -> Unit,
    val dismissStats: () -> Unit,
    val dismissHistoryCleared: () -> Unit,
    val dismissLogCleared: () -> Unit,
    val copy: (String) -> Unit,
    val paste: (String) -> Unit,
    val dismissAll: () -> Unit,
)

@Composable
private fun toolWindows(
    open: OpenWindows,
    graph: AppGraph,
    slot: SettingsSlot,
    settings: Settings,
    actions: WindowActions,
    refineChars: IntArray,
    language: String,
) {
    if (open.showBanner) {
        key(language, open.bannerAt) {
            BusyBanner(open.banner ?: RunStatus(RunPhase.Preparing, 0), open.bannerAt)
        }
    }
    open.shown?.let { result ->
        ResultWindow(
            title = result.title,
            text = result.text,
            onCopy = actions.copy,
            onPaste = actions.paste,
            onClose = actions.dismissShown,
            model = result.model,
            conversation = result.conversation,
            chips = result.chips,
            markdown = result.markdown,
            complete =
                measureProviderCall(
                    kind = "refine",
                    inputChars = { refineChars[0] },
                    store = graph.store,
                ) { model, messages ->
                    withContext(Dispatchers.IO) { completeCall(graph, slot.value, model, messages) }
                },
            onRequest = { refineChars[0] = it },
            onDelivered = { delivery -> rememberDelivery(graph, slot.value, result, delivery) },
            language = language,
            placementId = "result",
        )
    }
    if (open.showSettings) {
        SettingsScreen(
            graph = graph,
            settings = settings,
            onSave = actions.onSave,
            onCancel = actions.onCancelSettings,
        )
    }
    if (open.showEditor) {
        EditorScreen(
            graph = graph,
            settings = settings,
            onClose = actions.onCloseEditor,
            onSaved = actions.onSaved,
            language = language,
        )
    }
    key(language) {
        localizedWindows(open, graph, settings, actions)
    }
}

@Composable
private fun localizedWindows(
    open: OpenWindows,
    graph: AppGraph,
    settings: Settings,
    actions: WindowActions,
) {
    val hooks =
        ResultHooks(
            onCopy = actions.copy,
            onPaste = actions.paste,
            onClose = actions.dismissAll,
        )
    if (open.showHistory) {
        val rows = remember { graph.store.history() }
        HistoryWindow(rows, hooks.copy(onClose = actions.dismissHistory))
    }
    if (open.showLast) {
        val text = remember { readLast() }
        LastResultWindow(text, hooks.copy(onClose = actions.dismissLast))
    }
    if (open.showStats) {
        val log = remember { readLog() }
        val usage = remember { graph.store.usage() }
        val events = remember { graph.store.usageEvents() }
        var prices by remember { mutableStateOf(graph.store.prices()) }
        LaunchedEffect(Unit) {
            withContext(Dispatchers.IO) {
                graph.catalogue.refresh(graph.store, settings.proxy, settings.proxyOn)
            }
            prices = graph.store.prices()
        }
        StatsWindow(
            log = log,
            usage = usage,
            events = events,
            catalog = catalogEntries(prices?.body),
            baseUrl = settings.baseUrl,
            catalogueAtEpochMs = prices?.fetchedAtEpochMs,
            hooks = hooks.copy(onClose = actions.dismissStats),
        )
    }
    if (open.showHistoryCleared) {
        HistoryClearedWindow(hooks.copy(onClose = actions.dismissHistoryCleared))
    }
    if (open.showLogCleared) {
        LogClearedWindow(hooks.copy(onClose = actions.dismissLogCleared))
    }
}

private fun fillTemplate(
    template: String,
    vararg values: String,
): String {
    var text = template
    values.forEachIndexed { index, value ->
        text = text.replace("%${index + 1}\$s", value)
    }
    return text
}

@Composable
private fun ActionMenuWindow(
    request: MenuRequest,
    onDismiss: () -> Unit,
    onRun: (Action, String, Boolean) -> Unit,
    onEditor: () -> Unit,
    language: String,
) {
    val instruction = remember(request) { mutableStateOf("") }
    val width = MENU_WIDTH.dp
    val height = request.heightPx.dp
    val x = request.at.x.dp
    val y = request.at.y.dp
    Window(
        onCloseRequest = onDismiss,
        title = "TextTools",
        icon = AppIcon,
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
        key(language) {
            AppTheme {
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
}

@Composable
private fun BusyBanner(
    status: RunStatus,
    anchor: ScreenPoint,
) {
    val corner = bannerCorner(anchor, currentWorkAreas())
    val width = BANNER_WIDTH.dp
    val height = BANNER_HEIGHT.dp
    val x = corner.first.dp
    val y = corner.second.dp
    Window(
        onCloseRequest = {},
        title = "TextTools",
        icon = AppIcon,
        state = rememberWindowState(width = width, height = height, position = WindowPosition(x, y)),
        undecorated = true,
        alwaysOnTop = true,
        resizable = false,
        focusable = false,
    ) {
        AppTheme {
            Surface {
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        stringResource(if (status.phase == RunPhase.Preparing) Res.string.app_run_preparing else Res.string.app_run_done),
                        fontSize = 14.sp,
                    )
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Text(
                        stringResource(Res.string.app_seconds, statusSeconds(status.elapsedMillis)),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                    )
                    status.usage?.let { usage ->
                        Text(
                            usageLine(usage),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            maxLines = 2,
                        )
                    }
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
    var catalog by remember { mutableStateOf(catalogEntries(graph.store.prices()?.body)) }
    LaunchedEffect(settings.baseUrl, settings.proxy, settings.proxyOn) {
        withContext(Dispatchers.IO) {
            graph.catalogue.refresh(graph.store, settings.proxy, settings.proxyOn)
        }
        catalog = catalogEntries(graph.store.prices()?.body)
    }
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
    onSaved: (UiText) -> Unit,
    language: String,
) {
    val actions = remember { graph.store.actions() }
    val quick = remember { graph.store.quick() }
    val apps = remember { graph.store.apps().ifEmpty { Factory.apps } }
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
                uiText(
                    Res.string.app_editor_saved,
                    nextActions.size.toString(),
                    nextQuick.size.toString(),
                    nextApps.size.toString(),
                ),
            )
            onClose()
        },
        onCancel = onClose,
        language = language,
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
): UiText? {
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

private fun closeGraph(
    graph: AppGraph,
    closed: AtomicBoolean,
) {
    if (!closed.compareAndSet(false, true)) return
    try {
        graph.close()
    } catch (error: Exception) {
        reportFailure(error)
    } finally {
        flushSentry()
    }
}

private fun RequestMetric.toStored(): UsageEvent =
    UsageEvent(atEpochMs, kind, model, outcome, inputChars, extraChars, outputChars, durationMs, promptTokens, completionTokens)

private fun measureProviderCall(
    kind: String,
    inputChars: () -> Int,
    extraChars: Int = 0,
    store: TextToolsStore,
    complete: suspend (String, List<ChatMessage>) -> Pair<String, Usage?>,
): suspend (String, List<ChatMessage>) -> Pair<String, Usage?> =
    { model, messages ->
        val started = System.nanoTime()
        val at = System.currentTimeMillis()
        var output = 0
        var usage: Usage? = null
        var outcome = "failure"
        try {
            val answer = complete(model, messages)
            output = answer.first.trim().length
            usage = answer.second
            outcome = if (output == 0) "failure" else "success"
            answer
        } catch (cancelled: CancellationException) {
            outcome = "cancelled"
            throw cancelled
        } finally {
            store.appendUsageEvent(
                UsageEvent(
                    at,
                    kind,
                    model,
                    outcome,
                    inputChars(),
                    extraChars,
                    output,
                    ((System.nanoTime() - started) / 1_000_000).coerceAtLeast(0),
                    usage?.promptTokens,
                    usage?.completionTokens,
                ),
            )
        }
    }

private suspend fun completeCall(
    graph: AppGraph,
    settings: Settings,
    model: String,
    messages: List<ChatMessage>,
): Pair<String, Usage?> {
    try {
        val key = graph.secrets.get(SecretSlot.ApiKey).orEmpty()
        return graph.llm.complete(
            baseUrl = settings.baseUrl,
            apiKey = key,
            model = model,
            messages = messages,
            proxy = settings.proxy,
            proxyOn = settings.proxyOn,
        )
    } catch (cancelled: CancellationException) {
        if (cancelled is TimeoutCancellationException) reportFailure(cancelled)
        throw cancelled
    } catch (error: Exception) {
        reportFailure(error)
        throw error
    }
}

private fun saveSettings(
    graph: AppGraph,
    draft: SettingsDraft,
    sentryFile: SentryFile,
    publish: (Settings) -> Unit,
): Boolean =
    try {
        graph.secrets.put(SecretSlot.ApiKey, draft.apiKey)
        graph.secrets.put(SecretSlot.ProxyUrl, draft.settings.proxy)
        graph.store.saveLanguages(draft.languages)
        if (draft.modelsUrl.isNotBlank()) graph.store.saveModels(draft.modelsUrl, draft.models)
        writeUiSettings(draft.settings)
        publish(draft.settings)
        applySentry(sentryFile, draft.settings)
        currentInstalledExe()?.let { WindowsRunKey().apply(draft.settings.autostart, it) }
        true
    } catch (error: Exception) {
        reportFailure(error)
        false
    }

private suspend fun rememberDelivery(
    graph: AppGraph,
    settings: Settings,
    shown: ShownResult,
    delivery: RefineDelivery,
) {
    try {
        graph.catalogue.refresh(graph.store, settings.proxy, settings.proxyOn)
        val at = System.currentTimeMillis()
        val name = if (delivery.label.isBlank()) shown.title else "${shown.title} (${delivery.label})"
        val row =
            HistoryRecord(
                atEpochMs = at,
                actionName = name,
                model = delivery.model,
                promptTokens = delivery.usage?.promptTokens,
                completionTokens = delivery.usage?.completionTokens,
                cost = requestCost(graph.store.prices()?.body, delivery.model, settings.baseUrl, delivery.usage),
                source = delivery.instruction,
                result = delivery.text,
            )
        graph.store.saveHistory((listOf(row) + graph.store.history()).take(HISTORY_LIMIT))
        delivery.usage?.let { usage ->
            graph.store.appendUsage(
                UsageRecord(at, name, delivery.model, usage.promptTokens, usage.completionTokens),
            )
        }
        writeLast(delivery.text)
        appendLog(at, "$ACTION_MARK$name, model ${delivery.model}")
        appendLog(at, "ok, response ${delivery.text.length} characters")
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        reportFailure(error)
        return
    }
}

private val ErrorReason.resource: StringResource
    get() =
        when (this) {
            ErrorReason.EMPTY_RESPONSE -> Res.string.error_empty_response
            ErrorReason.EMPTY_URL -> Res.string.error_empty_url
            ErrorReason.INVALID_URL -> Res.string.error_invalid_url
            ErrorReason.INVALID_PROXY -> Res.string.error_invalid_proxy
            ErrorReason.PROVIDER_UNAVAILABLE -> Res.string.error_provider_unavailable
            ErrorReason.UNAUTHORIZED -> Res.string.error_unauthorized
            ErrorReason.INSUFFICIENT_FUNDS -> Res.string.error_insufficient_funds
            ErrorReason.NOT_FOUND -> Res.string.error_not_found
            ErrorReason.RATE_LIMITED -> Res.string.error_rate_limited
            ErrorReason.HTTP_ERROR -> Res.string.error_http
            ErrorReason.SECRET_STORE_UNAVAILABLE -> Res.string.error_secret_store
            ErrorReason.UNKNOWN -> Res.string.error_unknown
        }

private fun copyText(
    text: String,
    flash: (UiText) -> Unit,
) {
    try {
        Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(text), null)
        flash(uiText(Res.string.app_copied))
    } catch (_: Exception) {
        flash(uiText(Res.string.app_copy_failed))
    }
}

private fun pasteText(
    clipboard: ClipboardProtocol,
    text: String,
    flash: (UiText) -> Unit,
) {
    when (clipboard.paste(text)) {
        is PasteAttempt.Sent -> Unit
        PasteAttempt.ShowInWindow -> flash(NO_SOURCE)
    }
}

private fun openLog(): UiText? {
    val log = dataDirectory().resolve(LOG_FILE)
    if (!Files.isRegularFile(log)) return uiText(Res.string.app_no_log)
    return try {
        Desktop.getDesktop().open(log.toFile())
        null
    } catch (_: IOException) {
        uiText(Res.string.app_no_log)
    }
}

private fun openDataFolder(): UiText? =
    try {
        val dir = dataDirectory()
        Files.createDirectories(dir)
        Desktop.getDesktop().open(dir.toFile())
        null
    } catch (_: IOException) {
        uiText(Res.string.app_folder_failed)
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

private data class LoadedUi(
    val settings: Settings,
    val autostartStored: Boolean?,
)

private fun loadSettings(secrets: UserSecrets): Settings {
    val loaded = readUi(secrets)
    val exe = currentInstalledExe() ?: return loaded.settings
    val matches = WindowsRunKey().read() == installedAutostart(exe).command
    val enabled = autostartEnabled(loaded.autostartStored, matches)
    val next = loaded.settings.copy(autostart = enabled)
    WindowsRunKey().apply(enabled, exe)
    if (loaded.autostartStored == null || next != loaded.settings) writeUiSettings(next)
    return next
}

private fun readUi(secrets: UserSecrets): LoadedUi {
    val factory = Factory.defaultSettings
    val proxy = secrets.get(SecretSlot.ProxyUrl).orEmpty()
    val file = dataDirectory().resolve(UI_FILE)
    if (!Files.isRegularFile(file)) {
        return LoadedUi(factory.copy(proxy = proxy, proxyOn = proxy.isNotEmpty()), null)
    }
    return try {
        val props = Properties()
        Files.newBufferedReader(file, StandardCharsets.UTF_8).use { props.load(it) }
        val clip = props.getProperty("clipWaitSeconds")?.toDoubleOrNull() ?: factory.clipWaitSeconds
        val stored = props.getProperty("autostart")
        LoadedUi(
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
                sentryErrors = boolProp(props, "sentryErrors", factory.sentryErrors),
                sentryTraces = boolProp(props, "sentryTraces", factory.sentryTraces),
                autostart = stored?.equals("true", ignoreCase = true) ?: factory.autostart,
                profileModel = textProp(props, "profileModel", factory.profileModel),
            ),
            stored?.let { it.equals("true", ignoreCase = true) },
        )
    } catch (_: IOException) {
        LoadedUi(factory.copy(proxy = proxy, proxyOn = proxy.isNotEmpty()), null)
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
    props.setProperty("sentryErrors", settings.sentryErrors.toString())
    props.setProperty("sentryTraces", settings.sentryTraces.toString())
    props.setProperty("autostart", settings.autostart.toString())
    props.setProperty("profileModel", settings.profileModel)
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

private fun placeMenu(
    actionCount: Int,
    caret: ScreenPoint?,
    cursor: ScreenPoint,
    window: WindowBounds?,
): Pair<ScreenPoint, Int> {
    val height = menuOuterHeight(actionCount)
    return menuPosition(caret, cursor, MENU_WIDTH, height, currentWorkAreas(), window) to height
}

@Composable
private fun usageLine(usage: RunUsage): String {
    val prompt = groupedCount(usage.promptTokens)
    val cost = usage.cost?.let { bannerMoney(it) }
    return if (usage.approximate) {
        val chars = groupedCount(usage.characters)
        if (cost == null) {
            stringResource(Res.string.app_run_estimate, chars, prompt)
        } else {
            stringResource(Res.string.app_run_estimate_cost, chars, prompt, cost)
        }
    } else {
        val completion = groupedCount(usage.completionTokens ?: 0)
        if (cost == null) {
            stringResource(Res.string.app_run_usage, prompt, completion)
        } else {
            stringResource(Res.string.app_run_usage_cost, prompt, completion, cost)
        }
    }
}

private fun pointerNow(): ScreenPoint {
    val pointer = MouseInfo.getPointerInfo()?.location
    return ScreenPoint(pointer?.x ?: 0, pointer?.y ?: 0)
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
    require(content.isNotEmpty()) { ErrorReason.EMPTY_RESPONSE.name }
    return content
}

private fun apiRequest(
    probe: SettingsProbe,
    path: String,
    json: String?,
): String {
    val base = probe.baseUrl.trim().trimEnd('/')
    require(base.isNotEmpty()) { AppException(ErrorReason.EMPTY_URL) }
    val uri =
        try {
            URI("$base/$path")
        } catch (_: IllegalArgumentException) {
            throw AppException(ErrorReason.INVALID_URL)
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
        require(code in 200..299) { httpError(code) }
        response.body()
    } catch (_: IOException) {
        throw AppException(ErrorReason.PROVIDER_UNAVAILABLE)
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
            error(PROXY_UNPARSED)
        }
    val uri =
        try {
            URI(value)
        } catch (_: IllegalArgumentException) {
            error(PROXY_UNPARSED)
        }
    val host = uri.host ?: error(PROXY_UNPARSED)
    val scheme = uri.scheme?.lowercase().orEmpty()
    val port =
        when {
            uri.port != -1 -> uri.port
            scheme == "http" -> 80
            scheme == "https" -> 443
            scheme.startsWith("socks") -> 1080
            else -> error(PROXY_UNPARSED)
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

private fun httpError(code: Int): AppException {
    val reason =
        when (code) {
            401 -> ErrorReason.UNAUTHORIZED
            402 -> ErrorReason.INSUFFICIENT_FUNDS
            404 -> ErrorReason.NOT_FOUND
            429 -> ErrorReason.RATE_LIMITED
            in 500..599 -> ErrorReason.PROVIDER_UNAVAILABLE
            else -> ErrorReason.HTTP_ERROR
        }
    return AppException(reason, code, "HTTP $code")
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

private fun skipJsonSpace(
    body: String,
    from: Int,
): Int {
    var index = from
    while (index < body.length && body[index].isWhitespace()) index++
    return index
}

private fun jsonUnescape(next: Char): Char =
    when (next) {
        'n' -> '\n'
        'r' -> '\r'
        't' -> '\t'
        '"' -> '"'
        '\\' -> '\\'
        else -> next
    }

private fun jsonStringFieldAt(
    body: String,
    from: Int,
): String? {
    var index = skipJsonSpace(body, from)
    if (index >= body.length || body[index] != ':') return null
    index = skipJsonSpace(body, index + 1)
    if (index >= body.length || body[index] != '"') return null
    index++
    val out = StringBuilder()
    while (index < body.length) {
        val char = body[index]
        if (char == '\\' && index + 1 < body.length) {
            out.append(jsonUnescape(body[index + 1]))
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
