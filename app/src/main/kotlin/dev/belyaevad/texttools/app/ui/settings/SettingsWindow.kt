package dev.belyaevad.texttools.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.LocalMinimumInteractiveComponentEnforcement
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Surface
import androidx.compose.material.Tab
import androidx.compose.material.TabRow
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.rememberWindowState
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
import dev.belyaevad.texttools.domain.KnownModel
import dev.belyaevad.texttools.domain.OutputLanguage
import dev.belyaevad.texttools.domain.Settings
import dev.belyaevad.texttools.domain.providerOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

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
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun SettingsWindow(
    settings: Settings,
    apiKey: String,
    models: List<KnownModel>,
    languages: List<OutputLanguage>,
    callbacks: SettingsCallbacks,
) {
    val form = remember { SettingsForm(settings, apiKey, models, languages) }
    val scope = rememberCoroutineScope()
    var accepting by remember { mutableStateOf(true) }
    var tab by remember { mutableIntStateOf(0) }
    var apiStatus by remember { mutableStateOf<Status?>(null) }
    var dialog by remember { mutableStateOf<SettingsNotice?>(null) }
    var ask by remember { mutableStateOf<LangDraft?>(null) }
    val proxyBad = stringResource(Res.string.settings_proxy_bad)
    val pickRow = stringResource(Res.string.settings_pick_row)
    val defaultSet = stringResource(Res.string.settings_default_set)
    val busyText = stringResource(Res.string.settings_busy)
    val loadingText = stringResource(Res.string.settings_loading)
    val testingText = stringResource(Res.string.settings_testing)
    val windowState = rememberWindowState(width = 680.dp, height = 720.dp)

    fun hidden(
        text: String?,
        sentKey: String,
    ): String = redactSecret(redactSecret(text.orEmpty(), sentKey), form.apiKey)

    fun showProxy(detail: String) {
        tab = 0
        apiStatus = Status(if (detail.isBlank()) proxyBad else proxyBad + "\n" + detail, error = true)
    }

    fun route(notice: SettingsNotice) {
        when (notice) {
            is SettingsNotice.ProxyBad -> showProxy(notice.detail)
            SettingsNotice.HotkeyEmpty,
            SettingsNotice.HotkeySame,
            is SettingsNotice.HotkeyBad,
            SettingsNotice.ClipBad,
            -> {
                tab = 2
                dialog = notice
            }
            SettingsNotice.ModelEmpty -> {
                tab = 0
                dialog = notice
            }
            else -> dialog = notice
        }
    }

    fun save() {
        if (!accepting) return
        when (val result = form.trySave()) {
            is SaveResult.Ready -> {
                accepting = false
                callbacks.onSave(result.draft)
            }
            is SaveResult.Blocked -> route(result.notice)
        }
    }

    fun dismiss() {
        if (!accepting) return
        accepting = false
        callbacks.onCancel()
    }

    fun idle(): Boolean {
        if (!form.busy) return true
        apiStatus = Status(busyText, error = false)
        return false
    }

    fun loadModels() {
        if (!accepting) return
        when (val read = form.readProxy()) {
            is ProxyRead.Bad -> showProxy(redactSecret(read.detail, form.apiKey))
            is ProxyRead.Ok -> {
                if (!idle()) return
                val probe = form.probe(read.proxy, read.enabled)
                form.busy = true
                apiStatus = Status(loadingText, error = false)
                scope.launch {
                    try {
                        val ids = callbacks.onLoadModels(probe)
                        if (!accepting) return@launch
                        val outcome = form.applyLoaded(ids)
                        apiStatus = null
                        dialog =
                            when (outcome) {
                                is LoadOutcome.Replaced ->
                                    SettingsNotice.LoadReplaced(outcome.count, outcome.previousUrl)
                                is LoadOutcome.Merged ->
                                    SettingsNotice.LoadMerged(
                                        outcome.providerCount,
                                        outcome.added,
                                        outcome.removed,
                                    )
                            }
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (error: Exception) {
                        if (!accepting) return@launch
                        apiStatus = null
                        dialog = SettingsNotice.LoadFailed(hidden(error.message, probe.apiKey))
                    } finally {
                        form.busy = false
                    }
                }
            }
        }
    }

    fun testApi() {
        if (!accepting) return
        when (val read = form.readProxy()) {
            is ProxyRead.Bad -> showProxy(redactSecret(read.detail, form.apiKey))
            is ProxyRead.Ok -> {
                if (!idle()) return
                val probe = form.probe(read.proxy, read.enabled)
                form.busy = true
                apiStatus = Status(testingText, error = false)
                scope.launch {
                    try {
                        val reply = callbacks.onTestApi(probe)
                        if (!accepting) return@launch
                        apiStatus = null
                        dialog = SettingsNotice.TestOk(hidden(reply, probe.apiKey))
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (error: Exception) {
                        if (!accepting) return@launch
                        apiStatus = null
                        dialog = SettingsNotice.TestFailed(hidden(error.message, probe.apiKey))
                    } finally {
                        form.busy = false
                    }
                }
            }
        }
    }

    fun recommend() {
        if (!accepting || !idle()) return
        val url = form.baseUrl.trim()
        val picks = recommendedModels(url)
        if (picks.isEmpty()) {
            dialog = SettingsNotice.RecommendUnknown(url)
            return
        }
        val missing = form.applyRecommended(picks)
        dialog = SettingsNotice.RecommendDone(providerOf(url), picks, missing)
    }

    fun retarget() {
        if (!accepting || !idle()) return
        val url = form.baseUrl.trim()
        form.busy = true
        scope.launch {
            try {
                val ids = callbacks.onRetarget(url)
                if (!accepting) return@launch
                form.tickModels(ids)
                dialog = SettingsNotice.RetargetDone
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                if (!accepting) return@launch
                dialog = SettingsNotice.RetargetFailed(hidden(error.message, form.apiKey))
            } finally {
                form.busy = false
            }
        }
    }

    fun confirmAsk(draft: LangDraft) {
        when (draft.step) {
            0 -> {
                val code = draft.value.trim()
                if (code.isEmpty()) {
                    ask = null
                    return
                }
                if (form.languages.any { it.code.equals(code, ignoreCase = true) }) {
                    ask = null
                    dialog = SettingsNotice.LangDuplicate
                    return
                }
                ask = LangDraft(step = 1, code = code, name = "", value = code)
            }
            1 -> {
                val name = draft.value.trim().ifEmpty { draft.code }
                ask = LangDraft(step = 2, code = draft.code, name = name, value = name)
            }
            else -> {
                val prompt = draft.value.trim().ifEmpty { draft.name }
                val added = form.addLanguage(draft.code, draft.name, prompt)
                ask = null
                if (!added) dialog = SettingsNotice.LangDuplicate
            }
        }
    }

    val actions =
        ApiActions(
            onLoad = { loadModels() },
            onRecommend = { recommend() },
            onTest = { testApi() },
            onRetarget = { retarget() },
            onMakeDefault = { index ->
                if (index == null) {
                    apiStatus = Status(pickRow, error = true)
                } else {
                    val id = form.makeDefault(index)
                    apiStatus =
                        if (id == null) {
                            Status(pickRow, error = true)
                        } else {
                            Status(fill(defaultSet, id), error = false)
                        }
                }
            },
            onDismissError = { if (apiStatus?.error == true) apiStatus = null },
        )

    Window(
        onCloseRequest = { dismiss() },
        title = "TextTools - ${stringResource(Res.string.settings_title)}",
        state = windowState,
        resizable = true,
        alwaysOnTop = true,
        onPreviewKeyEvent = { event ->
            if (event.type == KeyEventType.KeyDown && event.key == Key.Escape) {
                when {
                    ask != null -> ask = null
                    dialog != null -> dialog = null
                    else -> dismiss()
                }
                true
            } else if (event.type == KeyEventType.KeyUp && event.key == Key.Enter) {
                val question = ask
                when {
                    question != null -> confirmAsk(question)
                    dialog != null -> dialog = null
                    else -> save()
                }
                true
            } else {
                false
            }
        },
    ) {
        MaterialTheme {
            CompositionLocalProvider(LocalMinimumInteractiveComponentEnforcement provides false) {
                Surface(Modifier.fillMaxSize()) {
                    Column(Modifier.fillMaxSize().padding(10.dp)) {
                        TabRow(selectedTabIndex = tab) {
                            Tab(
                                selected = tab == 0,
                                onClick = { tab = 0 },
                                text = { Text(stringResource(Res.string.settings_tab_api)) },
                            )
                            Tab(
                                selected = tab == 1,
                                onClick = { tab = 1 },
                                text = { Text(stringResource(Res.string.settings_tab_langs)) },
                            )
                            Tab(
                                selected = tab == 2,
                                onClick = { tab = 2 },
                                text = { Text(stringResource(Res.string.settings_tab_ui)) },
                            )
                        }
                        Box(Modifier.weight(1f).padding(top = 8.dp)) {
                            when (tab) {
                                0 ->
                                    SettingsApiTab(
                                        form = form,
                                        catalog = callbacks.catalog,
                                        status = apiStatus?.text,
                                        statusIsError = apiStatus?.error == true,
                                        actions = actions,
                                    )
                                1 ->
                                    SettingsLanguagesTab(
                                        form = form,
                                        status = null,
                                        onAdd = { ask = LangDraft(step = 0, code = "", name = "", value = "") },
                                        onDelete = {
                                            when (form.deleteLanguage()) {
                                                DeleteLanguage.Last -> dialog = SettingsNotice.LangLast
                                                DeleteLanguage.NoSelection, DeleteLanguage.Removed -> Unit
                                            }
                                        },
                                    )
                                else ->
                                    SettingsInterfaceTab(
                                        form = form,
                                        uiLanguages = callbacks.uiLanguages,
                                        status = null,
                                    )
                            }
                        }
                        Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { save() },
                                modifier = Modifier.width(160.dp).height(32.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                            ) {
                                Text(stringResource(Res.string.settings_save))
                            }
                            OutlinedButton(
                                onClick = { dismiss() },
                                modifier = Modifier.width(120.dp).height(32.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                            ) {
                                Text(stringResource(Res.string.settings_cancel))
                            }
                        }
                    }
                }
                val current = dialog
                if (current != null) {
                    AlertDialog(
                        onDismissRequest = { dialog = null },
                        title = { Text("TextTools") },
                        text = {
                            Column(Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState())) {
                                Text(noticeText(current))
                            }
                        },
                        confirmButton = {
                            Button(onClick = { dialog = null }) { Text(stringResource(Res.string.settings_ok)) }
                        },
                    )
                }
                val question = ask
                if (question != null) {
                    LanguageQuestion(
                        draft = question,
                        onValue = { value -> ask = ask?.copy(value = value) },
                        onDismiss = { ask = null },
                        onConfirm = { confirmAsk(question.copy(value = ask?.value ?: question.value)) },
                    )
                }
            }
        }
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
        is SettingsNotice.LoadFailed -> stringResource(Res.string.settings_models_failed) + "\n\n" + notice.detail
        is SettingsNotice.TestOk -> stringResource(Res.string.settings_test_ok) + "\n\n" + notice.reply
        is SettingsNotice.TestFailed -> stringResource(Res.string.settings_test_fail) + "\n\n" + notice.detail
        SettingsNotice.RetargetDone -> stringResource(Res.string.settings_retarget_done)
        is SettingsNotice.RetargetFailed -> stringResource(Res.string.settings_test_fail) + "\n\n" + notice.detail
    }

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
                LineField(
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
