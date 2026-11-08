package dev.belyaevad.texttools.app.ui.settings

import dev.belyaevad.texttools.domain.CatalogEntry
import dev.belyaevad.texttools.domain.ErrorReason
import dev.belyaevad.texttools.domain.KnownModel
import dev.belyaevad.texttools.domain.OutputLanguage
import dev.belyaevad.texttools.domain.Settings
import dev.belyaevad.texttools.domain.providerOf
import dev.belyaevad.texttools.domain.roleModel
import java.util.Locale

/** One interface language in the dropdown. [name] is the language's own name. */
data class UiLanguageChoice(
    val code: String,
    val name: String,
)

/** English and Russian packs shipped with the app. */
fun bundledUiLanguages(): List<UiLanguageChoice> =
    listOf(
        UiLanguageChoice("en", "English"),
        UiLanguageChoice("ru", "Русский"),
    )

/**
 * Values sent to the provider. [toString] omits the key and the proxy, both of which can be secrets.
 * The key is a request header only.
 */
data class SettingsProbe(
    val baseUrl: String,
    val apiKey: String,
    val model: String,
    val proxy: String,
    val proxyOn: Boolean,
) {
    override fun toString(): String = "SettingsProbe(baseUrl=$baseUrl, model=$model, proxyOn=$proxyOn, apiKeySet=${apiKey.isNotEmpty()})"
}

/**
 * Accepted settings. [models] belong to [modelsUrl], which stays on the previous address until a load
 * from the new one replaces them. The key is not part of [settings].
 */
data class SettingsDraft(
    val settings: Settings,
    val apiKey: String,
    val models: List<KnownModel>,
    val modelsUrl: String,
    val languages: List<OutputLanguage>,
) {
    override fun toString(): String =
        "SettingsDraft(baseUrl=${settings.baseUrl}, model=${settings.defaultModel}, modelsUrl=$modelsUrl, " +
            "models=${models.size}, languages=${languages.size}, apiKeySet=${apiKey.isNotEmpty()})"
}

/**
 * Work the window cannot do by itself.
 * [onLoadModels] returns provider model ids. [onTestApi] returns the reply or throws.
 * [onRetarget] returns model ids that factory actions now use. [onSave] is called only after validation.
 * The window does not close itself: the caller hides it from [onSave] or [onCancel].
 */
data class SettingsCallbacks(
    val onLoadModels: suspend (SettingsProbe) -> List<String>,
    val onTestApi: suspend (SettingsProbe) -> String,
    val onRetarget: suspend (String) -> List<String>,
    val onSave: (SettingsDraft) -> Unit,
    val onCancel: () -> Unit,
    val catalog: List<CatalogEntry> = emptyList(),
    val uiLanguages: List<UiLanguageChoice> = bundledUiLanguages(),
)

/** One recommended model. [role] is mech, main, deep, or alt. */
internal data class RecommendedModel(
    val id: String,
    val role: String,
)

/**
 * Roles that exist for each provider. Asking [roleModel] for a missing role would fall back to main,
 * so this map lists only the roles stored for that provider.
 */
private val recommendationRoles: Map<String, List<String>> =
    mapOf(
        "openai" to listOf("mech", "main", "deep", "alt"),
        "openrouter" to listOf("mech", "main", "deep", "alt"),
        "anthropic" to listOf("mech", "main", "deep", "alt"),
        "google" to listOf("mech", "main", "deep", "alt"),
        "deepseek" to listOf("main", "deep"),
        "mistral" to listOf("mech", "main", "deep", "alt"),
        "xai" to listOf("main", "deep", "alt"),
        "ollama" to listOf("mech", "main", "deep", "alt"),
    )

internal fun recommendedModels(baseUrl: String): List<RecommendedModel> {
    val url = baseUrl.trim()
    val roles = recommendationRoles[providerOf(url)] ?: return emptyList()
    return roles.mapNotNull { role ->
        val id = roleModel(role, url)
        if (id.isEmpty()) null else RecommendedModel(id, role)
    }
}

internal fun redactSecret(
    text: String,
    secret: String,
): String {
    val hidden = secret.trim()
    if (hidden.length < 4) return text
    return text.replace(hidden, "••••")
}

/** Replaces `%1$s` style tokens without parsing percent signs inside [parts]. */
internal fun fill(
    pattern: String,
    vararg parts: String,
): String {
    var result = pattern
    parts.forEachIndexed { index, part ->
        result = result.replace("%${index + 1}\$s", part)
    }
    return result
}

internal fun proxyStartsOn(settings: Settings): Boolean =
    when (settings.proxyOn) {
        true -> true
        false -> false
        null -> settings.proxy.isNotBlank()
    }

internal fun formatClip(seconds: Double): String {
    val text = seconds.toString()
    return if (text.endsWith(".0")) text.dropLast(2) else text
}

internal fun parseClip(raw: String): Double? {
    val text = raw.trim().replace(',', '.')
    if (text.isEmpty()) return 0.8
    return text.toDoubleOrNull()
}

/** Same chords the Windows hotkey parser accepts, such as `^!Space`. */
internal fun ahkHotkey(spec: String): Boolean {
    if (spec.isEmpty()) return false
    var index = 0
    while (index < spec.length && spec[index] in "^!+#") index++
    val key = spec.substring(index)
    if (key.isEmpty()) return false
    when (key.lowercase()) {
        "space", "enter", "tab", "esc", "escape" -> return true
    }
    if (key.length == 1) {
        val character = key[0]
        return character in 'a'..'z' || character in 'A'..'Z' || character in '0'..'9'
    }
    if (key.length !in 2..3 || (key[0] != 'f' && key[0] != 'F')) return false
    val number = key.drop(1).toIntOrNull() ?: return false
    return number in 1..12
}

internal fun uiChoices(
    current: String,
    bundled: List<UiLanguageChoice>,
): List<UiLanguageChoice> {
    val base = bundled.ifEmpty { bundledUiLanguages() }
    if (current.isBlank() || base.any { it.code.equals(current, ignoreCase = true) }) return base
    return listOf(UiLanguageChoice(current, current)) + base
}

internal const val MISSING = "—"

internal fun priceSlug(
    modelId: String,
    baseUrl: String,
): String {
    if ('/' in modelId) return modelId
    val url = baseUrl.lowercase()
    return when {
        "openai.com" in url -> "openai/$modelId"
        "anthropic.com" in url -> "anthropic/$modelId"
        else -> modelId
    }
}

internal fun catalogRow(
    catalog: List<CatalogEntry>,
    modelId: String,
    baseUrl: String,
): CatalogEntry? {
    val slug = priceSlug(modelId, baseUrl)
    return catalog.firstOrNull { it.id.equals(slug, ignoreCase = true) }
        ?: catalog.firstOrNull { it.id.equals(modelId, ignoreCase = true) }
}

internal fun contextText(entry: CatalogEntry?): String {
    val length = entry?.contextLength ?: return MISSING
    if (length <= 0) return MISSING
    return groupDigits(length)
}

internal fun priceText(perToken: Double?): String {
    if (perToken == null) return MISSING
    return moneyPerMillion(perToken * 1_000_000.0)
}

internal fun moneyPerMillion(value: Double): String {
    if (value < 0.0 || value.isNaN()) return MISSING
    val pattern =
        when {
            value >= 1.0 -> "%.2f"
            value >= 0.01 -> "%.3f"
            else -> "%.5f"
        }
    return "$" + String.format(Locale.US, pattern, value)
}

internal fun groupDigits(value: Int): String {
    val digits = value.toString()
    val out = StringBuilder()
    var count = 0
    for (index in digits.indices.reversed()) {
        if (count == 3) {
            out.insert(0, ' ')
            count = 0
        }
        out.insert(0, digits[index])
        count++
    }
    return out.toString()
}

internal sealed interface SettingsNotice {
    data object HotkeyEmpty : SettingsNotice

    data object HotkeySame : SettingsNotice

    data class HotkeyBad(
        val value: String,
    ) : SettingsNotice

    data object ModelEmpty : SettingsNotice

    data object ClipBad : SettingsNotice

    data class ProxyBad(
        val detail: String,
    ) : SettingsNotice

    data object LangLast : SettingsNotice

    data object LangDuplicate : SettingsNotice

    data class RecommendUnknown(
        val url: String,
    ) : SettingsNotice

    data class RecommendDone(
        val providerKey: String,
        val picks: List<RecommendedModel>,
        val missing: List<String>,
    ) : SettingsNotice

    data class LoadReplaced(
        val count: Int,
        val previousUrl: String,
    ) : SettingsNotice

    data class LoadMerged(
        val providerCount: Int,
        val added: Int,
        val removed: Int,
    ) : SettingsNotice

    data class LoadFailed(
        val detail: String,
        val reason: ErrorReason = ErrorReason.UNKNOWN,
        val httpStatus: Int? = null,
    ) : SettingsNotice

    data class TestOk(
        val reply: String,
    ) : SettingsNotice

    data class TestFailed(
        val detail: String,
        val reason: ErrorReason = ErrorReason.UNKNOWN,
        val httpStatus: Int? = null,
    ) : SettingsNotice

    data object RetargetDone : SettingsNotice

    data class RetargetFailed(
        val detail: String,
        val reason: ErrorReason = ErrorReason.UNKNOWN,
        val httpStatus: Int? = null,
    ) : SettingsNotice
}
