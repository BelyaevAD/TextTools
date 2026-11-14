package dev.belyaevad.texttools.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.rememberWindowState
import dev.belyaevad.texttools.app.APP_VERSION
import dev.belyaevad.texttools.app.reportFailure
import dev.belyaevad.texttools.app.scrubSentryText
import dev.belyaevad.texttools.app.ui.AppField
import dev.belyaevad.texttools.app.ui.AppIcon
import dev.belyaevad.texttools.app.ui.AppPrimaryButton
import dev.belyaevad.texttools.app.ui.AppSecondaryButton
import dev.belyaevad.texttools.app.ui.AppTabs
import dev.belyaevad.texttools.app.ui.AppTheme
import dev.belyaevad.texttools.app.ui.SettingsHeight
import dev.belyaevad.texttools.app.ui.SettingsWidth
import dev.belyaevad.texttools.app.ui.fitVisibleFrame
import dev.belyaevad.texttools.app.ui.keepWindowFrame
import dev.belyaevad.texttools.app.ui.rememberPlacement
import dev.belyaevad.texttools.app.ui.resolveError
import dev.belyaevad.texttools.domain.ErrorReason
import dev.belyaevad.texttools.domain.KnownModel
import dev.belyaevad.texttools.domain.OutputLanguage
import dev.belyaevad.texttools.domain.Settings
import dev.belyaevad.texttools.domain.errorHttpStatus
import dev.belyaevad.texttools.domain.errorReason
import dev.belyaevad.texttools.domain.providerOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import texttools.app.generated.resources.Res
import texttools.app.generated.resources.settings_busy
import texttools.app.generated.resources.settings_cancel
import texttools.app.generated.resources.settings_clip_bad
import texttools.app.generated.resources.settings_default_set
import texttools.app.generated.resources.settings_hotkey_bad
import texttools.app.generated.resources.settings_hotkey_empty
import texttools.app.generated.resources.settings_hotkey_same
import texttools.app.generated.resources.settings_lang_code
import texttools.app.generated.resources.settings_lang_dup
import texttools.app.generated.resources.settings_lang_label
import texttools.app.generated.resources.settings_lang_last
import texttools.app.generated.resources.settings_lang_new
import texttools.app.generated.resources.settings_lang_prompt
import texttools.app.generated.resources.settings_loading
import texttools.app.generated.resources.settings_model_empty
import texttools.app.generated.resources.settings_models_check
import texttools.app.generated.resources.settings_models_failed
import texttools.app.generated.resources.settings_models_loaded
import texttools.app.generated.resources.settings_models_switched
import texttools.app.generated.resources.settings_models_switched_hint
import texttools.app.generated.resources.settings_ok
import texttools.app.generated.resources.settings_pick_row
import texttools.app.generated.resources.settings_provider_ollama
import texttools.app.generated.resources.settings_proxy_bad
import texttools.app.generated.resources.settings_rec_done
import texttools.app.generated.resources.settings_rec_missing
import texttools.app.generated.resources.settings_rec_unknown
import texttools.app.generated.resources.settings_retarget_done
import texttools.app.generated.resources.settings_role_alt
import texttools.app.generated.resources.settings_role_deep
import texttools.app.generated.resources.settings_role_main
import texttools.app.generated.resources.settings_role_mech
import texttools.app.generated.resources.settings_save
import texttools.app.generated.resources.settings_tab_api
import texttools.app.generated.resources.settings_tab_langs
import texttools.app.generated.resources.settings_tab_ui
import texttools.app.generated.resources.settings_test_fail
import texttools.app.generated.resources.settings_test_ok
import texttools.app.generated.resources.settings_testing
import texttools.app.generated.resources.settings_title

/**
 * One settings window: API, output languages, and interface, with Save and Cancel under the tabs.
 *
 * The star marks the default model. A double click sets that default. A tick does not reorder rows.
 * Loading the same base URL merges the model list and keeps ticks. Loading another URL replaces the list
 * and clears ticks. A proxy the normalizer rejects is shown on the API tab and the window stays open.
 * The last language cannot be deleted, and a language code cannot be repeated.
 * An empty menu hotkey, two identical hotkeys, or an empty default model do not save.
 * Hotkeys stay in AutoHotkey text, for example `^!Space`.
 *
 * The window does not open the database. [models] are the rows for [settings.baseUrl].
 */
@Composable
fun SettingsWindow(
    settings: Settings,
    apiKey: String,
    models: List<KnownModel>,
    languages: List<OutputLanguage>,
    callbacks: SettingsCallbacks,
) {
    val form = remember { SettingsForm(settings, apiKey, models, languages, callbacks.storedModels) }
    val scope = rememberCoroutineScope()
    val accepting = remember { mutableStateOf(true) }
    val tab = remember { mutableIntStateOf(0) }
    val apiStatus = remember { mutableStateOf<Status?>(null) }
    val dialog = remember { mutableStateOf<SettingsNotice?>(null) }
    val ask = remember { mutableStateOf<LangDraft?>(null) }
    val copy =
        SettingsCopy(
            proxyBad = stringResource(Res.string.settings_proxy_bad),
            pickRow = stringResource(Res.string.settings_pick_row),
            defaultSet = stringResource(Res.string.settings_default_set),
            busyText = stringResource(Res.string.settings_busy),
            loadingText = stringResource(Res.string.settings_loading),
            testingText = stringResource(Res.string.settings_testing),
        )
    val ops =
        SettingsOps(
            form = form,
            scope = scope,
            callbacks = callbacks,
            bindings = SettingsBindings(accepting, tab, apiStatus, dialog, ask),
            copy = copy,
        )
    val labels =
        listOf(
            stringResource(Res.string.settings_tab_api),
            stringResource(Res.string.settings_tab_langs),
            stringResource(Res.string.settings_tab_ui),
        )
    val place = rememberPlacement("settings", SettingsWidth, SettingsHeight)
    val windowState = rememberWindowState(width = place.width, height = place.height, position = place.position)
    val actions = ops.actions()

    Window(
        onCloseRequest = { ops.dismiss() },
        title = "TextTools - ${stringResource(Res.string.settings_title)}",
        icon = AppIcon,
        state = windowState,
        resizable = true,
        alwaysOnTop = true,
        onPreviewKeyEvent = ops::onKey,
    ) {
        if (!place.restored) fitVisibleFrame(windowState.size) { windowState.size = it }
        keepWindowFrame("settings")
        settingsContent(ops, actions, labels)
    }
}

@Composable
private fun settingsContent(
    ops: SettingsOps,
    actions: ApiActions,
    labels: List<String>,
) {
    AppTheme {
        Surface(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize().padding(10.dp)) {
                AppTabs(selected = ops.bindings.tab.intValue, labels = labels, onSelect = { ops.bindings.tab.intValue = it })
                Box(Modifier.weight(1f).padding(top = 8.dp).fillMaxWidth()) {
                    settingsPage(ops, actions)
                }
                Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppPrimaryButton(
                        text = stringResource(Res.string.settings_save),
                        onClick = { ops.save() },
                        modifier = Modifier.width(160.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    )
                    AppSecondaryButton(
                        text = stringResource(Res.string.settings_cancel),
                        onClick = { ops.dismiss() },
                        modifier = Modifier.width(120.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        "TextTools $APP_VERSION",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        modifier = Modifier.align(Alignment.CenterVertically),
                    )
                }
            }
        }
        settingsDialogs(ops)
    }
}

@Composable
private fun settingsPage(
    ops: SettingsOps,
    actions: ApiActions,
) {
    val status = ops.bindings.apiStatus.value
    when (ops.bindings.tab.intValue) {
        0 ->
            SettingsApiTab(
                form = ops.form,
                catalog = ops.callbacks.catalog,
                status = status?.text,
                statusIsError = status?.error == true,
                actions = actions,
            )
        1 ->
            SettingsLanguagesTab(
                form = ops.form,
                onAdd = { ops.bindings.ask.value = LangDraft(step = 0, code = "", name = "", value = "") },
                onDelete = { ops.deleteLanguage() },
            )
        else ->
            SettingsInterfaceTab(
                form = ops.form,
                uiLanguages = ops.callbacks.uiLanguages,
            )
    }
}

@Composable
private fun settingsDialogs(ops: SettingsOps) {
    val current = ops.bindings.dialog.value
    if (current != null) {
        AlertDialog(
            onDismissRequest = { ops.bindings.dialog.value = null },
            title = { Text("TextTools") },
            text = {
                Column(Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState())) {
                    Text(noticeText(current))
                }
            },
            confirmButton = {
                Button(onClick = { ops.bindings.dialog.value = null }) { Text(stringResource(Res.string.settings_ok)) }
            },
        )
    }
    val question = ops.bindings.ask.value
    if (question != null) {
        LanguageQuestion(
            draft = question,
            onValue = { value ->
                ops.bindings.ask.value =
                    ops.bindings.ask.value
                        ?.copy(value = value)
            },
            onDismiss = { ops.bindings.ask.value = null },
            onConfirm = { ops.confirmCurrent() },
        )
    }
}

private data class SettingsBindings(
    val accepting: MutableState<Boolean>,
    val tab: MutableIntState,
    val apiStatus: MutableState<Status?>,
    val dialog: MutableState<SettingsNotice?>,
    val ask: MutableState<LangDraft?>,
)

private data class SettingsCopy(
    val proxyBad: String,
    val pickRow: String,
    val defaultSet: String,
    val busyText: String,
    val loadingText: String,
    val testingText: String,
)

private class SettingsOps(
    val form: SettingsForm,
    private val scope: CoroutineScope,
    val callbacks: SettingsCallbacks,
    val bindings: SettingsBindings,
    private val copy: SettingsCopy,
) {
    fun hidden(
        text: String?,
        sentKey: String,
    ): String = scrubSentryText(redactSecret(redactSecret(text.orEmpty(), sentKey), form.apiKey))

    private fun hidden(
        error: Throwable,
        sentKey: String,
    ): String = hidden(error.message, sentKey)

    fun showProxy(detail: String) {
        bindings.tab.intValue = 0
        val text = if (detail.isBlank()) copy.proxyBad else copy.proxyBad + "\n" + detail
        bindings.apiStatus.value = Status(text, error = true)
    }

    fun route(notice: SettingsNotice) {
        when (notice) {
            is SettingsNotice.ProxyBad -> showProxy(notice.detail)
            SettingsNotice.HotkeyEmpty,
            SettingsNotice.HotkeySame,
            is SettingsNotice.HotkeyBad,
            SettingsNotice.ClipBad,
            -> {
                bindings.tab.intValue = 2
                bindings.dialog.value = notice
            }
            SettingsNotice.ModelEmpty -> {
                bindings.tab.intValue = 0
                bindings.dialog.value = notice
            }
            else -> bindings.dialog.value = notice
        }
    }

    fun save() {
        if (!bindings.accepting.value) return
        when (val result = form.trySave()) {
            is SaveResult.Ready -> {
                bindings.accepting.value = false
                if (!callbacks.onSave(result.draft)) bindings.accepting.value = true
            }
            is SaveResult.Blocked -> route(result.notice)
        }
    }

    fun dismiss() {
        if (!bindings.accepting.value) return
        bindings.accepting.value = false
        callbacks.onCancel()
    }

    fun idle(): Boolean {
        if (!form.busy) return true
        bindings.apiStatus.value = Status(copy.busyText, error = false)
        return false
    }

    fun loadModels() {
        if (!bindings.accepting.value) return
        when (val read = form.readProxy()) {
            is ProxyRead.Bad -> showProxy(redactSecret(read.detail, form.apiKey))
            is ProxyRead.Ok -> {
                if (!idle()) return
                val probe = form.probe(read.proxy, read.enabled)
                form.busy = true
                bindings.apiStatus.value = Status(copy.loadingText, error = false)
                scope.launch { finishLoad(probe) }
            }
        }
    }

    private suspend fun finishLoad(probe: SettingsProbe) {
        try {
            val ids = callbacks.onLoadModels(probe)
            if (!bindings.accepting.value) return
            val outcome = form.applyLoaded(ids)
            bindings.apiStatus.value = null
            bindings.dialog.value = loadedNotice(outcome)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            reportFailure(error)
            if (!bindings.accepting.value) return
            bindings.apiStatus.value = null
            bindings.dialog.value = SettingsNotice.LoadFailed(hidden(error, probe.apiKey), error.errorReason(), error.errorHttpStatus())
        } finally {
            form.busy = false
        }
    }

    private fun loadedNotice(outcome: LoadOutcome): SettingsNotice =
        when (outcome) {
            is LoadOutcome.Replaced -> SettingsNotice.LoadReplaced(outcome.count, outcome.previousUrl)
            is LoadOutcome.Merged ->
                SettingsNotice.LoadMerged(outcome.providerCount, outcome.added, outcome.removed)
        }

    fun testApi() {
        if (!bindings.accepting.value) return
        when (val read = form.readProxy()) {
            is ProxyRead.Bad -> showProxy(redactSecret(read.detail, form.apiKey))
            is ProxyRead.Ok -> {
                if (!idle()) return
                val probe = form.probe(read.proxy, read.enabled)
                form.busy = true
                bindings.apiStatus.value = Status(copy.testingText, error = false)
                scope.launch { finishTest(probe) }
            }
        }
    }

    private suspend fun finishTest(probe: SettingsProbe) {
        try {
            val reply = callbacks.onTestApi(probe)
            if (!bindings.accepting.value) return
            bindings.apiStatus.value = null
            bindings.dialog.value = SettingsNotice.TestOk(hidden(reply, probe.apiKey))
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            reportFailure(error)
            if (!bindings.accepting.value) return
            bindings.apiStatus.value = null
            bindings.dialog.value = SettingsNotice.TestFailed(hidden(error, probe.apiKey), error.errorReason(), error.errorHttpStatus())
        } finally {
            form.busy = false
        }
    }

    fun recommend() {
        if (!bindings.accepting.value || !idle()) return
        val url = form.baseUrl.trim()
        val picks = recommendedModels(url)
        if (picks.isEmpty()) {
            bindings.dialog.value = SettingsNotice.RecommendUnknown(url)
            return
        }
        val missing = form.applyRecommended(picks)
        bindings.dialog.value = SettingsNotice.RecommendDone(providerOf(url), picks, missing)
    }

    fun retarget() {
        if (!bindings.accepting.value || !idle()) return
        val url = form.baseUrl.trim()
        form.busy = true
        scope.launch { finishRetarget(url) }
    }

    private suspend fun finishRetarget(url: String) {
        try {
            val ids = callbacks.onRetarget(url)
            if (!bindings.accepting.value) return
            form.retargetedUrl = url
            form.tickModels(ids)
            bindings.dialog.value = SettingsNotice.RetargetDone
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            reportFailure(error)
            if (!bindings.accepting.value) return
            bindings.dialog.value = SettingsNotice.RetargetFailed(hidden(error, form.apiKey), error.errorReason(), error.errorHttpStatus())
        } finally {
            form.busy = false
        }
    }

    fun confirmAsk(draft: LangDraft) {
        when (draft.step) {
            0 -> confirmCode(draft)
            1 -> {
                val name = draft.value.trim().ifEmpty { draft.code }
                bindings.ask.value = LangDraft(step = 2, code = draft.code, name = name, value = name)
            }
            else -> confirmPrompt(draft)
        }
    }

    fun confirmCurrent() {
        val question = bindings.ask.value ?: return
        confirmAsk(question.copy(value = bindings.ask.value?.value ?: question.value))
    }

    private fun confirmCode(draft: LangDraft) {
        val code = draft.value.trim()
        if (code.isEmpty()) {
            bindings.ask.value = null
            return
        }
        if (form.languages.any { it.code.equals(code, ignoreCase = true) }) {
            bindings.ask.value = null
            bindings.dialog.value = SettingsNotice.LangDuplicate
            return
        }
        bindings.ask.value = LangDraft(step = 1, code = code, name = "", value = code)
    }

    private fun confirmPrompt(draft: LangDraft) {
        val prompt = draft.value.trim().ifEmpty { draft.name }
        val added = form.addLanguage(draft.code, draft.name, prompt)
        bindings.ask.value = null
        if (!added) bindings.dialog.value = SettingsNotice.LangDuplicate
    }

    fun deleteLanguage() {
        when (form.deleteLanguage()) {
            DeleteLanguage.Last -> bindings.dialog.value = SettingsNotice.LangLast
            DeleteLanguage.NoSelection, DeleteLanguage.Removed -> Unit
        }
    }

    fun makeDefault(index: Int?) {
        if (index == null) {
            bindings.apiStatus.value = Status(copy.pickRow, error = true)
            return
        }
        val id = form.makeDefault(index)
        bindings.apiStatus.value =
            if (id == null) {
                Status(copy.pickRow, error = true)
            } else {
                Status(fill(copy.defaultSet, id), error = false)
            }
    }

    fun dismissError() {
        if (bindings.apiStatus.value?.error == true) bindings.apiStatus.value = null
    }

    fun actions(): ApiActions =
        ApiActions(
            onLoad = ::loadModels,
            onRecommend = ::recommend,
            onTest = ::testApi,
            onRetarget = ::retarget,
            onMakeDefault = ::makeDefault,
            onDismissError = ::dismissError,
        )

    fun onKey(event: KeyEvent): Boolean {
        if (event.type == KeyEventType.KeyDown && event.key == Key.Escape) {
            when {
                bindings.ask.value != null -> bindings.ask.value = null
                bindings.dialog.value != null -> bindings.dialog.value = null
                else -> dismiss()
            }
            return true
        }
        if (event.type == KeyEventType.KeyUp && event.key == Key.Enter) {
            val question = bindings.ask.value
            when {
                question != null -> confirmAsk(question.copy(value = bindings.ask.value?.value ?: question.value))
                bindings.dialog.value != null -> bindings.dialog.value = null
                else -> return false
            }
            return true
        }
        return false
    }
}

@Composable
private fun noticeText(notice: SettingsNotice): String =
    when (notice) {
        SettingsNotice.HotkeyEmpty -> stringResource(Res.string.settings_hotkey_empty)
        SettingsNotice.HotkeySame -> stringResource(Res.string.settings_hotkey_same)
        is SettingsNotice.HotkeyBad -> stringResource(Res.string.settings_hotkey_bad) + "\n" + notice.value
        SettingsNotice.ModelEmpty -> stringResource(Res.string.settings_model_empty)
        SettingsNotice.ClipBad -> stringResource(Res.string.settings_clip_bad)
        is SettingsNotice.ProxyBad ->
            if (notice.detail.isBlank()) {
                stringResource(Res.string.settings_proxy_bad)
            } else {
                stringResource(Res.string.settings_proxy_bad) + "\n" + notice.detail
            }
        SettingsNotice.LangLast -> stringResource(Res.string.settings_lang_last)
        SettingsNotice.LangDuplicate -> stringResource(Res.string.settings_lang_dup)
        is SettingsNotice.RecommendUnknown -> stringResource(Res.string.settings_rec_unknown) + "\n\n" + notice.url
        is SettingsNotice.RecommendDone -> recommendText(notice)
        is SettingsNotice.LoadReplaced ->
            stringResource(Res.string.settings_models_switched, notice.count.toString(), notice.previousUrl) +
                "\n\n" +
                stringResource(Res.string.settings_models_switched_hint)
        is SettingsNotice.LoadMerged ->
            stringResource(
                Res.string.settings_models_loaded,
                notice.providerCount.toString(),
                notice.added.toString(),
                notice.removed.toString(),
            ) +
                "\n\n" +
                stringResource(Res.string.settings_models_check)
        is SettingsNotice.LoadFailed ->
            stringResource(Res.string.settings_models_failed) + "\n\n" +
                noticeText(notice.reason, notice.httpStatus, notice.detail)
        is SettingsNotice.TestOk -> stringResource(Res.string.settings_test_ok) + "\n\n" + notice.reply
        is SettingsNotice.TestFailed -> failedNotice(notice.reason, notice.httpStatus, notice.detail)
        SettingsNotice.RetargetDone -> stringResource(Res.string.settings_retarget_done)
        is SettingsNotice.RetargetFailed -> failedNotice(notice.reason, notice.httpStatus, notice.detail)
    }

@Composable
private fun failedNotice(
    reason: ErrorReason,
    status: Int?,
    detail: String,
): String = stringResource(Res.string.settings_test_fail) + "\n\n" + noticeText(reason, status, detail)

@Composable
private fun noticeText(
    reason: ErrorReason,
    status: Int?,
    detail: String,
): String = if (reason == ErrorReason.UNKNOWN) detail else resolveError(reason, status)

@Composable
private fun recommendText(notice: SettingsNotice.RecommendDone): String {
    val done =
        stringResource(
            Res.string.settings_rec_done,
            providerTitle(notice.providerKey),
            notice.picks.size.toString(),
        )
    val missingHeader = stringResource(Res.string.settings_rec_missing)
    val roles =
        mapOf(
            "mech" to stringResource(Res.string.settings_role_mech),
            "main" to stringResource(Res.string.settings_role_main),
            "deep" to stringResource(Res.string.settings_role_deep),
            "alt" to stringResource(Res.string.settings_role_alt),
        )
    return buildString {
        append(done)
        notice.picks.forEach { pick ->
            append("\n  • ")
            append(pick.id)
            append(" — ")
            append(roles[pick.role] ?: pick.role)
        }
        if (notice.missing.isNotEmpty()) {
            append("\n\n")
            append(missingHeader)
            notice.missing.forEach { id ->
                append("\n  • ")
                append(id)
            }
        }
    }
}

@Composable
private fun providerTitle(key: String): String =
    when (key) {
        "openai" -> "OpenAI"
        "openrouter" -> "OpenRouter"
        "anthropic" -> "Anthropic"
        "google" -> "Google Gemini"
        "deepseek" -> "DeepSeek"
        "mistral" -> "Mistral"
        "xai" -> "xAI"
        "ollama" -> stringResource(Res.string.settings_provider_ollama)
        else -> key
    }

@Composable
private fun LanguageQuestion(
    draft: LangDraft,
    onValue: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val question =
        when (draft.step) {
            0 -> stringResource(Res.string.settings_lang_code)
            1 -> stringResource(Res.string.settings_lang_label)
            else -> stringResource(Res.string.settings_lang_prompt)
        }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.settings_lang_new)) },
        text = {
            Column {
                Text(question)
                AppField(
                    value = draft.value,
                    onValueChange = onValue,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) { Text(stringResource(Res.string.settings_ok)) }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text(stringResource(Res.string.settings_cancel)) }
        },
    )
}

private data class Status(
    val text: String,
    val error: Boolean,
)

private data class LangDraft(
    val step: Int,
    val code: String,
    val name: String,
    val value: String,
)
