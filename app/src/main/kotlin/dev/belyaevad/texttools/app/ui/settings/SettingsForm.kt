package dev.belyaevad.texttools.app.ui.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.belyaevad.texttools.domain.KnownModel
import dev.belyaevad.texttools.domain.OutputLanguage
import dev.belyaevad.texttools.domain.Settings
import dev.belyaevad.texttools.domain.clipWaitSeconds
import dev.belyaevad.texttools.domain.mergeModels
import dev.belyaevad.texttools.domain.modelIsUseless
import dev.belyaevad.texttools.domain.normalizeProxy
import dev.belyaevad.texttools.domain.replaceModels

internal sealed interface ProxyRead {
    data class Ok(
        val proxy: String,
        val enabled: Boolean,
    ) : ProxyRead

    data class Bad(
        val detail: String,
    ) : ProxyRead
}

internal sealed interface LoadOutcome {
    data class Replaced(
        val count: Int,
        val previousUrl: String,
    ) : LoadOutcome

    data class Merged(
        val providerCount: Int,
        val added: Int,
        val removed: Int,
    ) : LoadOutcome
}

internal enum class DeleteLanguage {
    NoSelection,
    Last,
    Removed,
}

internal sealed interface SaveResult {
    data class Ready(
        val draft: SettingsDraft,
    ) : SaveResult

    data class Blocked(
        val notice: SettingsNotice,
    ) : SaveResult
}

/**
 * Editable copy of the settings passed into [SettingsWindow].
 * Ticking a model or a language replaces that row in place and does not sort the list.
 * [modelsUrl] is the address the current rows belong to.
 */
class SettingsForm(
    settings: Settings,
    initialKey: String,
    initialModels: List<KnownModel>,
    initialLanguages: List<OutputLanguage>,
) {
    var baseUrl by mutableStateOf(settings.baseUrl.trim())
    var apiKey by mutableStateOf(initialKey)
    var proxy by mutableStateOf(settings.proxy)
    var proxyOn by mutableStateOf(proxyStartsOn(settings))
    var defaultModel by mutableStateOf(settings.defaultModel.trim())
    var models by mutableStateOf(initialModels.toList())
    var modelsUrl by mutableStateOf(settings.baseUrl.trim())
    var selectedModel by mutableStateOf<Int?>(null)
    var languages by mutableStateOf(initialLanguages.toList())
    var selectedLanguage by mutableStateOf<Int?>(null)
    var hotkey by mutableStateOf(settings.hotkey)
    var hotkeyRepeat by mutableStateOf(settings.hotkeyRepeat)
    var markdown by mutableStateOf(settings.markdown)
    var uiLang by mutableStateOf(settings.uiLang.ifBlank { "en" })
    var profiling by mutableStateOf(settings.appProfiling)
    var profileModel by mutableStateOf(settings.profileModel.trim())
    var autostart by mutableStateOf(settings.autostart)
    var sentryErrors by mutableStateOf(settings.sentryErrors)
    var sentryTraces by mutableStateOf(settings.sentryTraces)
    var clipWait by mutableStateOf(formatClip(settings.clipWaitSeconds))
    var busy by mutableStateOf(false)

    /** A cleared checkbox keeps the text and does not have to be a valid proxy. */
    internal fun readProxy(): ProxyRead {
        if (!proxyOn) return ProxyRead.Ok(proxy.trim(), enabled = false)
        return try {
            ProxyRead.Ok(normalizeProxy(proxy), enabled = true)
        } catch (error: IllegalArgumentException) {
            ProxyRead.Bad(error.message.orEmpty())
        }
    }

    fun probe(
        proxyValue: String,
        enabled: Boolean,
    ): SettingsProbe =
        SettingsProbe(
            baseUrl = baseUrl.trim(),
            apiKey = apiKey.trim(),
            model = defaultModel.trim(),
            proxy = proxyValue,
            proxyOn = enabled,
        )

    fun setModelSelected(
        index: Int,
        selected: Boolean,
    ) {
        val row = models.getOrNull(index) ?: return
        if (row.selected == selected) return
        models = models.mapIndexed { i, item -> if (i == index) item.copy(selected = selected) else item }
    }

    /** Points the default field at this row. The list order stays as it is. */
    fun makeDefault(index: Int): String? {
        val row = models.getOrNull(index) ?: return null
        selectedModel = index
        defaultModel = row.id
        return row.id
    }

    /**
     * A different address replaces the rows and clears ticks.
     * The same address merges, and a ticked model stays even when the provider dropped it.
     */
    internal fun applyLoaded(ids: List<String>): LoadOutcome {
        val url = baseUrl.trim()
        val replace = modelsUrl.isNotEmpty() && modelsUrl != url
        if (replace) {
            val previous = modelsUrl
            val next = replaceModels(ids)
            models = next
            modelsUrl = url
            selectedModel = null
            return LoadOutcome.Replaced(next.size, previous)
        }
        val before = models.map { it.id }.toSet()
        val next = mergeModels(models, ids)
        val after = next.map { it.id }.toSet()
        val fresh = ids.filterNot(::modelIsUseless).distinct()
        models = next
        modelsUrl = url
        selectedModel = null
        return LoadOutcome.Merged(
            providerCount = fresh.size,
            added = fresh.count { it !in before },
            removed = before.count { it !in after },
        )
    }

    /** Ticks recommended ids in place and appends any the list did not have. */
    internal fun applyRecommended(picks: List<RecommendedModel>): List<String> {
        var next = models
        val missing =
            buildList {
                for (pick in picks) {
                    val (updated, added) = withRecommendation(next, pick)
                    next = updated
                    if (added != null) add(added)
                }
            }
        models = next
        return missing
    }

    private fun withRecommendation(
        current: List<KnownModel>,
        pick: RecommendedModel,
    ): Pair<List<KnownModel>, String?> {
        if (pick.id.isEmpty()) return current to null
        val index = current.indexOfFirst { it.id == pick.id }
        if (index < 0) return (current + KnownModel(pick.id, selected = true)) to pick.id
        if (current[index].selected) return current to null
        val ticked = current.mapIndexed { i, row -> if (i == index) row.copy(selected = true) else row }
        return ticked to null
    }

    /** Ticks retargeted ids in place and appends any the list did not have. */
    fun tickModels(ids: List<String>) {
        var next = models
        for (raw in ids) {
            val id = raw.trim()
            if (id.isEmpty()) continue
            val index = next.indexOfFirst { it.id == id }
            if (index >= 0) {
                if (!next[index].selected) {
                    next = next.mapIndexed { i, row -> if (i == index) row.copy(selected = true) else row }
                }
            } else {
                next = next + KnownModel(id, selected = true)
            }
        }
        models = next
    }

    fun setLanguageEnabled(
        index: Int,
        enabled: Boolean,
    ) {
        val row = languages.getOrNull(index) ?: return
        if (row.enabled == enabled) return
        languages = languages.mapIndexed { i, item -> if (i == index) item.copy(enabled = enabled) else item }
    }

    fun addLanguage(
        code: String,
        name: String,
        prompt: String,
    ): Boolean {
        val trimmed = code.trim()
        if (trimmed.isEmpty()) return false
        if (languages.any { it.code.equals(trimmed, ignoreCase = true) }) return false
        val label = name.trim().ifEmpty { trimmed }
        val promptName = prompt.trim().ifEmpty { label }
        languages = languages + OutputLanguage(trimmed, label, promptName, enabled = true)
        return true
    }

    internal fun deleteLanguage(): DeleteLanguage {
        val index = selectedLanguage ?: return DeleteLanguage.NoSelection
        if (index !in languages.indices) return DeleteLanguage.NoSelection
        if (languages.size <= 1) return DeleteLanguage.Last
        languages = languages.filterIndexed { i, _ -> i != index }
        selectedLanguage = null
        return DeleteLanguage.Removed
    }

    internal fun trySave(): SaveResult {
        val menu = hotkey.trim()
        val repeat = hotkeyRepeat.trim()
        if (menu.isEmpty()) return SaveResult.Blocked(SettingsNotice.HotkeyEmpty)
        if (repeat.isNotEmpty() && menu.equals(repeat, ignoreCase = true)) {
            return SaveResult.Blocked(SettingsNotice.HotkeySame)
        }
        if (!ahkHotkey(menu)) return SaveResult.Blocked(SettingsNotice.HotkeyBad(menu))
        if (repeat.isNotEmpty() && !ahkHotkey(repeat)) return SaveResult.Blocked(SettingsNotice.HotkeyBad(repeat))
        if (defaultModel.trim().isEmpty()) return SaveResult.Blocked(SettingsNotice.ModelEmpty)
        val clip = parseClip(clipWait) ?: return SaveResult.Blocked(SettingsNotice.ClipBad)
        val accepted =
            when (val read = readProxy()) {
                is ProxyRead.Bad -> {
                    return SaveResult.Blocked(SettingsNotice.ProxyBad(redactSecret(read.detail, apiKey)))
                }
                is ProxyRead.Ok -> read
            }
        return SaveResult.Ready(
            SettingsDraft(
                settings =
                    Settings(
                        baseUrl = baseUrl.trim(),
                        defaultModel = defaultModel.trim(),
                        proxy = accepted.proxy,
                        proxyOn = accepted.enabled,
                        hotkey = menu,
                        hotkeyRepeat = repeat,
                        markdown = markdown,
                        uiLang = uiLang.trim().ifEmpty { "en" },
                        appProfiling = profiling,
                        clipWaitSeconds = clipWaitSeconds(clip),
                        sentryErrors = sentryErrors,
                        sentryTraces = sentryTraces,
                        autostart = autostart,
                        profileModel = profileModel.trim(),
                    ),
                apiKey = apiKey.trim(),
                models = models.toList(),
                modelsUrl = modelsUrl,
                languages = languages.toList(),
            ),
        )
    }
}
