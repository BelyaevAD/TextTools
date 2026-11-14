package dev.belyaevad.texttools.app

import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.ApplicationScope
import androidx.compose.ui.window.Notification
import androidx.compose.ui.window.Tray
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberTrayState
import androidx.compose.ui.window.rememberWindowState
import dev.belyaevad.texttools.app.ui.AppIcon
import dev.belyaevad.texttools.app.ui.AppLocale
import dev.belyaevad.texttools.app.ui.AppTheme
import dev.belyaevad.texttools.app.ui.BANNER_OUTER_HEIGHT
import dev.belyaevad.texttools.app.ui.BANNER_OUTER_WIDTH
import dev.belyaevad.texttools.app.ui.MENU_OUTER_WIDTH
import dev.belyaevad.texttools.app.ui.ResultWidth
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
import dev.belyaevad.texttools.app.ui.resource
import dev.belyaevad.texttools.app.ui.result.RefineDelivery
import dev.belyaevad.texttools.app.ui.result.ResultWindow
import dev.belyaevad.texttools.app.ui.resultOuterHeight
import dev.belyaevad.texttools.app.ui.run.AppProfiler
import dev.belyaevad.texttools.app.ui.run.LAST_FILE
import dev.belyaevad.texttools.app.ui.run.LOG_FILE
import dev.belyaevad.texttools.app.ui.run.RunAction
import dev.belyaevad.texttools.app.ui.run.RunPhase
import dev.belyaevad.texttools.app.ui.run.RunStatus
import dev.belyaevad.texttools.app.ui.run.RunUsage
import dev.belyaevad.texttools.app.ui.run.bannerMoney
import dev.belyaevad.texttools.app.ui.run.groupedCount
import dev.belyaevad.texttools.app.ui.run.logTime
import dev.belyaevad.texttools.app.ui.run.numberAfter
import dev.belyaevad.texttools.app.ui.run.requestCost
import dev.belyaevad.texttools.app.ui.run.statusSeconds
import dev.belyaevad.texttools.app.ui.settings.SettingsCallbacks
import dev.belyaevad.texttools.app.ui.settings.SettingsDraft
import dev.belyaevad.texttools.app.ui.settings.SettingsProbe
import dev.belyaevad.texttools.app.ui.settings.SettingsWindow
import dev.belyaevad.texttools.app.ui.settings.fill
import dev.belyaevad.texttools.app.ui.systemFormatLocale
import dev.belyaevad.texttools.app.ui.uiText
import dev.belyaevad.texttools.data.HistoryRecord
import dev.belyaevad.texttools.data.LegacyImport
import dev.belyaevad.texttools.data.LlmClient
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
import dev.belyaevad.texttools.domain.profilingModel
import dev.belyaevad.texttools.domain.retarget
import dev.belyaevad.texttools.domain.searchModel
import dev.belyaevad.texttools.platform.ClipboardProtocol
import dev.belyaevad.texttools.platform.PasteAttempt
import dev.belyaevad.texttools.platform.win.GlobalChord
import dev.belyaevad.texttools.platform.win.ScreenPoint
import dev.belyaevad.texttools.platform.win.SecretSlot
import dev.belyaevad.texttools.platform.win.SecretStoreException
import dev.belyaevad.texttools.platform.win.SourceSnapshot
import dev.belyaevad.texttools.platform.win.UserSecrets
import dev.belyaevad.texttools.platform.win.WindowBounds
import dev.belyaevad.texttools.platform.win.WindowsRunKey
import dev.belyaevad.texttools.platform.win.autostartEnabled
import dev.belyaevad.texttools.platform.win.claimFirstInstance
import dev.belyaevad.texttools.platform.win.currentInstalledExe
import dev.belyaevad.texttools.platform.win.currentWorkAreas
import dev.belyaevad.texttools.platform.win.hasUserSource
import dev.belyaevad.texttools.platform.win.installedAutostart
import dev.belyaevad.texttools.platform.win.popupBeside
import dev.belyaevad.texttools.platform.win.selectionAnchor
import dev.belyaevad.texttools.platform.win.sourceSnapshot
import dev.belyaevad.texttools.platform.win.userDataRoot
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.yield
import org.jetbrains.compose.resources.stringResource
import texttools.app.generated.resources.Res
import texttools.app.generated.resources.app_clipboard_busy
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
import texttools.app.generated.resources.app_run_cancel_hint
import texttools.app.generated.resources.app_run_done
import texttools.app.generated.resources.app_run_done_for
import texttools.app.generated.resources.app_run_estimate
import texttools.app.generated.resources.app_run_estimate_cost
import texttools.app.generated.resources.app_run_preparing
import texttools.app.generated.resources.app_run_preparing_for
import texttools.app.generated.resources.app_run_usage
import texttools.app.generated.resources.app_run_usage_cost
import texttools.app.generated.resources.app_save_failed
import texttools.app.generated.resources.app_seconds
import texttools.app.generated.resources.app_secret_unreadable
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
import texttools.app.generated.resources.settings_profile_added
import texttools.app.generated.resources.settings_profile_window
import java.awt.Desktop
import java.awt.MouseInfo
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.awt.event.WindowEvent
import java.awt.event.WindowFocusListener
import java.io.IOException
import java.io.UncheckedIOException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.time.Instant
import java.util.Locale
import java.util.Properties
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.system.exitProcess

private const val TOOLTIP_MILLIS = 1_600L
private const val BANNER_HOLD_MILLIS = 2_000L
private const val FOCUS_GRACE_MILLIS = 400L
private const val MENU_WIDTH = MENU_OUTER_WIDTH
private const val BANNER_WIDTH = BANNER_OUTER_WIDTH
private const val BANNER_HEIGHT = BANNER_OUTER_HEIGHT
private const val UI_FILE = "ui.properties"
private const val RESTART_ENV = "TEXTTOOLS_RESTART"

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
private val SECRET_UNREADABLE = uiText(Res.string.app_secret_unreadable)

/** Routine successes stay in the tray tooltip; everything else also becomes a Windows notification. */
private val TOOLTIP_ONLY = setOf(Res.string.app_settings_saved, Res.string.app_copied, Res.string.app_editor_saved)
private val SAVE_FAILED = uiText(Res.string.app_save_failed)
private val REQUEST_FAILED = uiText(Res.string.app_request_failed)

// Settings probes gave up after 60 s before they moved to the chat clients, whose reads wait 180 s.
private const val PROBE_TIMEOUT_MILLIS = 60_000L

private data class MenuRequest(
    val selection: String,
    val actions: List<Action>,
    val at: ScreenPoint,
    val heightPx: Int,
    val source: SourceSnapshot,
    val anchor: WindowBounds,
    val cursor: ScreenPoint,
)

// Not a data class: two runs with the same answer are still two results, so the window state resets.
private class ShownResult(
    val title: String,
    val text: String,
    val model: String,
    val conversation: List<ChatMessage>,
    val chips: List<QuickRefine>,
    val markdown: Boolean,
    val at: ScreenPoint? = null,
)

/** Tray entry. Hotkeys are started here; [AppGraph] construction does not start them. */
fun main() {
    installApplicationIcon()
    Locale.setDefault(Locale.Category.FORMAT, systemFormatLocale)
    ownTheInstance()
    try {
        prepareUserData()
    } catch (error: IOException) {
        logLine("data copy failed: ${error.javaClass.simpleName}")
    } catch (error: UncheckedIOException) {
        logLine("data copy failed: ${error.javaClass.simpleName}")
    }
    val commands = Channel<GlobalChord>(Channel.UNLIMITED)
    val graph = AppGraph { commands.trySend(it) }
    val closed = AtomicBoolean(false)
    Runtime.getRuntime().addShutdownHook(Thread { closeGraph(graph, closed) })
    graph.watch.start()
    importScriptSettings(graph)
    val slot = SettingsSlot(loadSettings(graph.secrets))
    val hotkeysOk = graph.hotkeys.start(slot.value.hotkey, slot.value.hotkeyRepeat)
    applyUiLocale(slot.value.uiLang)
    val sentryFile = loadSentryFile(sentryConfigFile(Path.of(System.getProperty("user.dir")), dataDirectory()))
    applySentry(sentryFile, slot.value)
    val startupNote = startupNote(graph, hotkeysOk)
    noteStartup(slot.value.hotkey, slot.value.hotkeyRepeat, hotkeysOk, !hotkeysOk)
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
                    onRequest = { event -> graph.store.appendUsageEvent(event.toStored()) },
                )
            }
        DisposableEffect(Unit) {
            onDispose {
                closeGraph(graph, closed)
            }
        }
        TextToolsApp(graph, commands, slot, runner, startupNote, sentryFile)
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
) {
    val uiLanguage = remember { mutableStateOf(slot.value.uiLang) }
    AppLocale(uiLanguage.value) {
        textToolsContent(graph, commands, slot, runner, startupNote, sentryFile, uiLanguage)
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
    val trayState = rememberTrayState()
    val flashedText = flashed?.let { resolve(it) }
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

    // ActionRun is rebuilt on every recomposition, so the running job lives outside it.
    val runJob = remember { AtomicReference<Job?>() }
    val runs =
        ActionRun(
            job = runJob,
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
                val secretNote = secretNote(graph.secrets)
                if (secretNote != null || !hasApiKey(graph.secrets)) {
                    secretNote?.let(::flash)
                    showSettings = true
                } else {
                    captureMenu(
                        graph = graph,
                        seconds = slot.value.clipWaitSeconds,
                        menuOpen = { menu != null },
                        flash = ::flash,
                    ) { request ->
                        menu = request
                    }
                }
            },
            onRepeat = {
                val secretNote = secretNote(graph.secrets)
                if (secretNote != null || !hasApiKey(graph.secrets)) {
                    secretNote?.let(::flash)
                    showSettings = true
                } else {
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
                }
            },
        )
    }

    key(language) {
        val idleTip = uiText(Res.string.app_tray_tip, settings.hotkey)
        Tray(
            icon = AppIcon,
            state = trayState,
            tooltip = flashedText?.replace('\n', ' ')?.take(240) ?: resolve(idleTip),
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
    val notice = flashedText?.takeUnless { flashed?.resource in TOOLTIP_ONLY }
    LaunchedEffect(flashToken) {
        // TrayState drops a notification nobody is collecting; the Tray above launches its collector in this frame.
        yield()
        notice?.let { trayState.sendNotification(Notification("TextTools", it)) }
        clearFlash(flashToken) { flashed = null }
    }

    menu?.let { request ->
        ActionMenuWindow(
            request = request,
            onDismiss = { menu = null },
            onRun = { action, extra, shift ->
                val selection = request.selection
                menu = null
                runs.start(action, selection, extra, shift, RunTarget(request.at, request.source, request.anchor, request.cursor))
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
                val saved = applySavedSettings(graph, draft, sentryFile, ::publish, ::flash)
                if (saved) showSettings = false
                saved
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
            paste = { text -> scope.launch { pasteText(graph.clipboard, text, ::flash) } },
            cancelRun = runs::cancel,
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

private data class RunTarget(
    val at: ScreenPoint,
    val source: SourceSnapshot,
    val anchor: WindowBounds,
    val cursor: ScreenPoint,
)

private class ActionRun(
    private val job: AtomicReference<Job?>,
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
        target: RunTarget,
    ) {
        if (runner.busy) {
            hooks.flash(REQUEST_RUNNING)
            return
        }
        val secretNote = secretNote(graph.secrets)
        if (secretNote != null || !hasApiKey(graph.secrets)) {
            hooks.flash(secretNote ?: NEED_KEY)
            hooks.openSettings()
            return
        }
        val token = hooks.nextToken()
        hooks.rememberRun(action, instruction)
        hooks.placeBanner(target.at)
        job.set(
            scope.launch {
                runBody(action, selection, instruction, shift, target, token)
            },
        )
    }

    /** Stops the running request. The busy gate and the banner clear in their finally blocks. */
    fun cancel() {
        job.get()?.cancel()
    }

    fun flash(message: UiText) = hooks.flash(message)

    private suspend fun runBody(
        action: Action,
        selection: String,
        instruction: String,
        shift: Boolean,
        target: RunTarget,
        token: Int,
    ) {
        val proc = target.source.proc
        val title = target.source.title
        val windowClass = target.source.windowClass
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
                        complete = { name, messages -> completeCall(graph, current, name, messages) },
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
            scope.launch(Dispatchers.IO) {
                graph.catalogue.refresh(graph.store, current.proxy, current.proxyOn)
            }
            val outcome = actionOutcome(action, selection, instruction, shift, proc, title)
            val hold = publishOutcome(action, outcome, target.anchor, target.cursor)
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
        anchor: WindowBounds,
        cursor: ScreenPoint,
    ): Boolean {
        when (outcome) {
            null -> hooks.flash(REQUEST_FAILED)
            is RunOutcome.Pasted -> return true
            is RunOutcome.InWindow -> {
                val chips = graph.store.quick()
                val height =
                    resultOuterHeight(outcome.conversation.isNotEmpty(), chips.isNotEmpty()).value.toInt()
                hooks.showResult(
                    ShownResult(
                        title = action.name,
                        text = outcome.text,
                        model = outcome.model,
                        conversation = outcome.conversation,
                        chips = chips,
                        markdown = slot.value.markdown,
                        at = popupBeside(anchor, ResultWidth.value.toInt(), height, currentWorkAreas(), cursor),
                    ),
                )
            }
            is RunOutcome.Failed -> hooks.flash(uiText(outcome.reason.resource, outcome.httpStatus?.toString().orEmpty()))
            RunOutcome.Busy -> hooks.flash(REQUEST_RUNNING)
            RunOutcome.NeedsKey -> {
                hooks.flash(secretNote(graph.secrets) ?: NEED_KEY)
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
): String = if (shortTitle.isEmpty()) added else "$added ${fill(window, shortTitle)}"

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
        val text = graph.clipboard.captureSeconds(seconds)
        val busy = graph.clipboard.takeBusyNotice()
        val nonText = graph.clipboard.takeNonTextWarning()
        text to
            when {
                busy -> uiText(Res.string.app_clipboard_busy)
                nonText != null -> uiText(Res.string.app_no_selection)
                else -> null
            }
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
    val anchor = selectionAnchor(graph.watch, caret, cursor, source.bounds)
    val (selection, warning) = takeSelection(graph, seconds)
    if (menuOpen()) return
    if (selection.isEmpty()) {
        flash(warning ?: NO_SELECTION)
        return
    }
    val actions = graph.store.actions()
    val placed = placeMenu(actions.size, anchor, cursor)
    open(MenuRequest(selection, actions, placed.first, placed.second, source, anchor, cursor))
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
    val caret = graph.watch.caretNow()
    val cursor = pointerNow()
    val anchor = selectionAnchor(graph.watch, caret, cursor, source.bounds)
    val at = popupBeside(anchor, BANNER_OUTER_WIDTH, BANNER_OUTER_HEIGHT, currentWorkAreas(), cursor)
    val (selection, warning) = takeSelection(graph, seconds)
    if (menuOpen()) return
    if (selection.isEmpty()) {
        runs.flash(warning ?: NO_SELECTION)
        return
    }
    runs.start(action, selection, extra, false, RunTarget(at, source, anchor, cursor))
}

private fun applySavedSettings(
    graph: AppGraph,
    draft: SettingsDraft,
    sentryFile: SentryFile,
    publish: (Settings) -> Unit,
    flash: (UiText) -> Unit,
): Boolean {
    val saved = saveSettings(graph, draft, sentryFile, publish)
    val hotkeysOk = saved && graph.hotkeys.swap(draft.settings.hotkey, draft.settings.hotkeyRepeat)
    val done = if (hotkeysOk) uiText(Res.string.app_settings_saved, draft.settings.hotkey) else HOTKEY_FAIL
    // A save that leaves a secret unreadable does not report plain success.
    flash(if (saved) secretNote(graph.secrets) ?: done else SAVE_FAILED)
    return saved
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
    val onSave: (SettingsDraft) -> Boolean,
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
    val cancelRun: () -> Unit,
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
            BusyBanner(open.banner ?: RunStatus(RunPhase.Preparing, 0), open.bannerAt, actions.cancelRun)
        }
    }
    open.shown?.let { result ->
        key(result) {
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
                anchor = result.at,
            )
        }
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
            catalog = remember(prices) { catalogEntries(prices?.body) },
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
    onCancel: () -> Unit,
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
            Surface(Modifier.clickable(enabled = status.phase == RunPhase.Preparing, onClick = onCancel)) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    val program = status.source
                    val heading =
                        if (program.isNullOrEmpty()) {
                            stringResource(
                                if (status.phase == RunPhase.Preparing) {
                                    Res.string.app_run_preparing
                                } else {
                                    Res.string.app_run_done
                                },
                            )
                        } else {
                            stringResource(
                                if (status.phase == RunPhase.Preparing) {
                                    Res.string.app_run_preparing_for
                                } else {
                                    Res.string.app_run_done_for
                                },
                                program,
                            )
                        }
                    Text(heading, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    val seconds = stringResource(Res.string.app_seconds, statusSeconds(status.elapsedMillis))
                    Text(
                        if (status.phase == RunPhase.Preparing) "$seconds · ${stringResource(Res.string.app_run_cancel_hint)}" else seconds,
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
    onSave: (SettingsDraft) -> Boolean,
    onCancel: () -> Unit,
) {
    val apiKey = remember(settings) { secretOrNull(graph.secrets, SecretSlot.ApiKey).orEmpty() }
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
                onLoadModels = { probe -> probeApi(graph, probe) { it.modelIds(probe.baseUrl, probe.apiKey, probe.proxy, probe.proxyOn) } },
                onTestApi = { probe ->
                    probeApi(graph, probe) {
                        val ask = listOf(ChatMessage("user", "Reply with ok"))
                        it.complete(probe.baseUrl, probe.apiKey, probe.model, ask, probe.proxy, probe.proxyOn).first
                    }
                },
                onRetarget = { url -> retarget(graph.store.actions(), url).mapNotNull { it.model }.filter { it.isNotBlank() }.distinct() },
                onSave = onSave,
                onCancel = onCancel,
                catalog = catalog,
                storedModels = graph.store::models,
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
    // The action's own model and the default are added back by modelChoices. No ticks at all shows every model.
    val models =
        remember(settings.baseUrl) {
            graph.store
                .models(settings.baseUrl)
                .let { all -> all.filter { it.selected }.ifEmpty { all } }
                .map { it.id }
        }
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
        if (!claimFirstInstance()) exitProcess(0)
    }
}

private fun waitToOwnInstance(): Boolean {
    val deadline = System.currentTimeMillis() + 15_000
    while (true) {
        if (claimFirstInstance()) return true
        if (System.currentTimeMillis() >= deadline) return false
        Thread.sleep(200)
    }
}

private fun noteStartup(
    hotkey: String,
    repeat: String,
    registered: Boolean,
    failed: Boolean,
) {
    val line =
        if (failed) {
            "hotkey refused: $hotkey / $repeat (factory registered=$registered)"
        } else {
            "hotkey active: $hotkey / $repeat"
        }
    logLine(line)
}

private fun logLine(line: String) {
    try {
        val dir = dataDirectory()
        Files.createDirectories(dir)
        Files.writeString(
            dir.resolve(LOG_FILE),
            "${logTime.format(Instant.now())}  $line\n",
            StandardCharsets.UTF_8,
            StandardOpenOption.CREATE,
            StandardOpenOption.APPEND,
        )
    } catch (_: IOException) {
        // A failed log write must not stop the hotkey.
    }
}

private fun startupNote(
    graph: AppGraph,
    hotkeysOk: Boolean,
): UiText? =
    secretNote(graph.secrets) ?: when {
        !hotkeysOk -> HOTKEY_FAIL
        !hasApiKey(graph.secrets) -> NEED_KEY
        else -> null
    }

/** The reason to show when a saved key or proxy exists but cannot be decrypted here. */
private fun secretNote(secrets: UserSecrets): UiText? =
    SECRET_UNREADABLE.takeIf { secretUnreadable(secrets, SecretSlot.ApiKey) || secretUnreadable(secrets, SecretSlot.ProxyUrl) }

private fun hasApiKey(secrets: UserSecrets): Boolean = !secretOrNull(secrets, SecretSlot.ApiKey).isNullOrBlank()

/** An unreadable secret (a blob from another account) reads as absent here; startup tells the user why. */
private fun secretOrNull(
    secrets: UserSecrets,
    slot: SecretSlot,
): String? =
    try {
        secrets.get(slot)
    } catch (_: SecretStoreException) {
        null
    }

private fun secretUnreadable(
    secrets: UserSecrets,
    slot: SecretSlot,
): Boolean =
    try {
        secrets.get(slot)
        false
    } catch (_: SecretStoreException) {
        true
    }

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
                    0,
                    output,
                    ((System.nanoTime() - started) / 1_000_000).coerceAtLeast(0),
                    usage?.promptTokens,
                    usage?.completionTokens,
                ),
            )
        }
    }

private fun readApiKey(secrets: UserSecrets): String =
    try {
        secrets.get(SecretSlot.ApiKey).orEmpty()
    } catch (e: SecretStoreException) {
        throw AppException(ErrorReason.SECRET_STORE_UNAVAILABLE, cause = e)
    }

private suspend fun completeCall(
    graph: AppGraph,
    settings: Settings,
    model: String,
    messages: List<ChatMessage>,
): Pair<String, Usage?> {
    try {
        val key = readApiKey(graph.secrets)
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
        putUnlessUnreadable(graph.secrets, SecretSlot.ApiKey, draft.apiKey)
        // Saving with the proxy switched off is the way to drop a proxy file that cannot be read.
        putUnlessUnreadable(graph.secrets, SecretSlot.ProxyUrl, draft.settings.proxy, cleared = draft.settings.proxyOn == false)
        graph.store.saveLanguages(draft.languages)
        if (draft.modelsUrl.isNotBlank()) graph.store.saveModels(draft.modelsUrl, draft.models)
        // The table is read now, so edits saved in the action editor meanwhile are kept.
        draft.retargetUrl?.let { graph.store.saveActions(retarget(graph.store.actions(), it)) }
        writeUiSettings(draft.settings, persistAutostart = true)
        publish(draft.settings)
        applySentry(sentryFile, draft.settings)
        currentInstalledExe()?.let { WindowsRunKey().apply(draft.settings.autostart, it) }
        true
    } catch (error: Exception) {
        reportFailure(error)
        false
    }

private fun putUnlessUnreadable(
    secrets: UserSecrets,
    slot: SecretSlot,
    value: String,
    cleared: Boolean = false,
) {
    if (secretWritable(value, secretUnreadable(secrets, slot), cleared)) secrets.put(slot, value)
}

private suspend fun rememberDelivery(
    graph: AppGraph,
    settings: Settings,
    shown: ShownResult,
    delivery: RefineDelivery,
) {
    try {
        withContext(Dispatchers.IO) {
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
        }
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        reportFailure(error)
        return
    }
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

private suspend fun pasteText(
    clipboard: ClipboardProtocol,
    text: String,
    flash: (UiText) -> Unit,
) {
    // Activation polling and the settle pauses stay off the UI thread; flash runs back on the caller's thread.
    when (withContext(Dispatchers.IO) { clipboard.paste(text) }) {
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
        val builder =
            currentInstalledExe()?.let { ProcessBuilder(it.toString()) } ?: run {
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

private fun dataDirectory(): Path = userDataRoot()

private fun prepareUserData() {
    val dest = userDataRoot()
    val local = System.getenv("LOCALAPPDATA") ?: return
    val source = Path.of(local, "TextTools")
    if (source == dest) return
    Files.createDirectories(dest)
    for (name in listOf(
        "texttools.db",
        "texttools.db-wal",
        "texttools.db-shm",
        "ui.properties",
        "TextTools_last.txt",
        "sentry.properties",
    )) {
        copyIfMissing(source.resolve(name), dest.resolve(name))
    }
    moveNewerWindowPositions(source.resolve("windows.properties"), dest.resolve("windows.properties"))
    val secrets = source.resolve("secrets")
    if (!Files.isDirectory(secrets)) return
    val secretDest = dest.resolve("secrets")
    Files.createDirectories(secretDest)
    Files.list(secrets).use { stream ->
        stream
            .filter { file -> Files.isRegularFile(file) && file.fileName.toString().endsWith(".dpapi") }
            .forEach { file ->
                copyIfMissing(file, secretDest.resolve(file.fileName))
            }
    }
}

private fun copyIfMissing(
    from: Path,
    to: Path,
) {
    if (!Files.isRegularFile(from) || Files.exists(to)) return
    Files.copy(from, to)
}

/** Older builds wrote this file into the install folder. Keep the newer copy, then remove the install-folder one. */
private fun moveNewerWindowPositions(
    from: Path,
    to: Path,
) {
    if (!Files.isRegularFile(from) || from == to) return
    val sourceTime = Files.getLastModifiedTime(from)
    val destTime = if (Files.isRegularFile(to)) Files.getLastModifiedTime(to) else null
    if (destTime == null || sourceTime > destTime) {
        Files.copy(from, to, StandardCopyOption.REPLACE_EXISTING)
    }
    Files.deleteIfExists(from)
}

private fun importScriptSettings(graph: AppGraph) {
    val marker = dataDirectory().resolve("legacy-imported")
    if (Files.exists(marker)) return
    if (secretUnreadable(graph.secrets, SecretSlot.ApiKey)) return
    if (hasApiKey(graph.secrets)) {
        Files.write(marker, byteArrayOf())
        return
    }
    val local = System.getenv("LOCALAPPDATA") ?: return
    val root = Path.of(local, "Scripts", "TextTools").toFile()
    if (!root.resolve("TextTools.ini").isFile) return
    try {
        val result = LegacyImport(graph.store).importFolder(root, dataDirectory().toFile())
        if (result.apiKey.isNotBlank()) graph.secrets.put(SecretSlot.ApiKey, result.apiKey)
        if (result.proxyUrl.isNotBlank()) graph.secrets.put(SecretSlot.ProxyUrl, result.proxyUrl)
        val factory = Factory.defaultSettings
        writeUiSettings(
            factory.copy(
                baseUrl = result.baseUrl.ifBlank { factory.baseUrl },
                defaultModel = result.model.ifBlank { factory.defaultModel },
                proxy = result.proxyUrl,
                proxyOn = result.proxyOn,
                hotkey = result.hotkey.ifBlank { factory.hotkey },
                hotkeyRepeat = result.hotkeyRepeat.ifBlank { factory.hotkeyRepeat },
                markdown = result.markdown,
                uiLang = result.uiLang.ifBlank { factory.uiLang },
                appProfiling = result.profiling,
                clipWaitSeconds = result.clipWaitSeconds,
            ),
            persistAutostart = false,
        )
        Files.write(marker, byteArrayOf())
    } catch (_: Exception) {
        // The next start tries again. A failed import must not stop the tray.
    }
}

private data class LoadedUi(
    val settings: Settings,
    val autostartStored: Boolean?,
    val rewrite: Boolean,
)

private fun loadSettings(secrets: UserSecrets): Settings {
    val loaded = readUi(secrets)
    val exe = currentInstalledExe()
    val next =
        if (exe == null) {
            loaded.settings
        } else {
            val matches = WindowsRunKey().read() == installedAutostart(exe).command
            val enabled = autostartEnabled(loaded.autostartStored, matches)
            WindowsRunKey().apply(enabled, exe)
            loaded.settings.copy(autostart = enabled)
        }
    if (loaded.rewrite || next != loaded.settings) {
        writeUiSettings(next, persistAutostart = loaded.autostartStored != null || exe != null)
    }
    return next
}

private fun readUi(secrets: UserSecrets): LoadedUi {
    val factory = Factory.defaultSettings
    val proxy = secretOrNull(secrets, SecretSlot.ProxyUrl).orEmpty()
    val proxyUnreadable = secretUnreadable(secrets, SecretSlot.ProxyUrl)
    val file = dataDirectory().resolve(UI_FILE)
    val props = Properties()
    val missing = !Files.isRegularFile(file)
    var failed = false
    if (!missing) {
        try {
            Files.newBufferedReader(file, StandardCharsets.UTF_8).use { props.load(it) }
        } catch (_: IOException) {
            failed = true
        }
    }
    val stored = if (props.containsKey("autostart")) props.getProperty("autostart") else null
    return LoadedUi(
        settingsFromProperties(props, proxy, factory, proxyUnreadable),
        stored?.equals("true", ignoreCase = true),
        rewrite = !failed && (missing || uiSettingsStale(props)),
    )
}

private fun writeUiSettings(
    settings: Settings,
    persistAutostart: Boolean,
) {
    val previous = Properties()
    val file = dataDirectory().resolve(UI_FILE)
    if (Files.isRegularFile(file)) {
        try {
            Files.newBufferedReader(file, StandardCharsets.UTF_8).use { previous.load(it) }
        } catch (_: IOException) {
            return
        }
    }
    val props = currentUiProperties(previous, settings, persistAutostart)
    val dir = dataDirectory()
    Files.createDirectories(dir)
    writeUiFile(dir.resolve(UI_FILE), props)
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
    anchor: WindowBounds,
    cursor: ScreenPoint,
): Pair<ScreenPoint, Int> {
    val height = menuOuterHeight(actionCount)
    return popupBeside(anchor, MENU_WIDTH, height, currentWorkAreas(), cursor) to height
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

/** "Load models" and "Test API": the chat clients and route rules, with the unsaved key, address and proxy. */
private suspend fun <T : Any> probeApi(
    graph: AppGraph,
    probe: SettingsProbe,
    block: suspend (LlmClient) -> T,
): T =
    withContext(Dispatchers.IO) {
        withTimeoutOrNull(PROBE_TIMEOUT_MILLIS) { graph.probe(probe.proxy, block) }
            ?: throw AppException(ErrorReason.PROVIDER_UNAVAILABLE)
    }

private fun catalogEntries(body: String?): List<CatalogEntry> {
    if (body.isNullOrBlank()) return emptyList()
    var from = 0
    val marks =
        buildList {
            while (from < body.length) {
                val at = body.indexOf("\"id\"", from)
                if (at < 0) break
                val id = jsonStringFieldAt(body, at + 4)
                if (!id.isNullOrEmpty()) add(id to at)
                from = at + 4
            }
        }
    return marks.mapIndexed { index, (id, at) ->
        val end = marks.getOrNull(index + 1)?.second ?: body.length
        val slice = body.substring(at, end)
        CatalogEntry(
            id = id,
            promptPerToken = numberAfter(slice, "prompt"),
            completionPerToken = numberAfter(slice, "completion"),
            contextLength = numberAfter(slice, "context_length")?.toInt(),
        )
    }
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
