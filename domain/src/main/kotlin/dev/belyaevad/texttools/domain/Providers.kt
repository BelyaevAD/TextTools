package dev.belyaevad.texttools.domain

/** Effective proxy. A missing checkbox means the URL is in force. A cleared checkbox means direct. */
fun effectiveProxy(
    proxy: String,
    proxyOn: Boolean?,
): String = if (proxyOn == false) "" else proxy.trim()

/** Accepts the six curl schemes. A bare host:port becomes http. */
fun normalizeProxy(raw: String): String {
    var value = raw.trim()
    if (value.startsWith("\"") && value.endsWith("\"") && value.length >= 2) {
        value = value.substring(1, value.length - 1)
    }
    if (value.isEmpty()) {
        return ""
    }
    require(!(value.contains('"') || value.contains('\n') || value.contains('\r'))) {
        "invalid proxy: quotes or line breaks"
    }
    val known = listOf("http://", "https://", "socks4://", "socks4a://", "socks5://", "socks5h://")
    val low = value.lowercase()
    if (known.any { low.startsWith(it) }) {
        return value
    }
    require(!value.contains("://")) {
        "invalid proxy scheme: use http://, https://, socks4://, socks4a://, socks5:// or socks5h://"
    }
    return "http://$value"
}

/** Loopback targets skip the proxy, including IPv6 localhost. Only the exact host counts; an unparsable URL is not loopback. */
fun isLoopback(url: String): Boolean {
    val host =
        try {
            java.net.URI(url.trim()).host
        } catch (_: java.net.URISyntaxException) {
            null
        }
    return host?.lowercase() in LOOPBACK_HOSTS
}

private val LOOPBACK_HOSTS = setOf("localhost", "127.0.0.1", "::1", "[::1]")

/** Clipboard wait clamped to the AHK range of 0.2–5 seconds. */
fun clipWaitSeconds(value: Double): Double = value.coerceIn(0.2, 5.0)

private val uselessNeedles =
    listOf(
        "embed",
        "whisper",
        "tts",
        "dall-e",
        "moderation",
        "audio",
        "realtime",
        "transcribe",
        "image",
        "sora",
        "babbage",
        "davinci",
        "codex",
        "search-preview",
        "computer-use",
        "guard",
    )

/** True when the id is not a chat model. The needle list matches ModelIsUseless. */
fun modelIsUseless(id: String): Boolean = uselessNeedles.any { id.lowercase().contains(it) }

/** A new address replaces the list and clears ticks. */
fun replaceModels(ids: List<String>): List<KnownModel> =
    ids
        .filterNot(::modelIsUseless)
        .distinct()
        .sorted()
        .map { KnownModel(it, selected = false) }

/** The same address merges. A ticked model stays even if the provider dropped it. */
fun mergeModels(
    current: List<KnownModel>,
    fetched: List<String>,
): List<KnownModel> {
    val selected = current.filter { it.selected }.map { it.id }.toSet()
    val fresh = fetched.filterNot(::modelIsUseless).toSet()
    return (selected + fresh).distinct().sorted().map { KnownModel(it, selected = it in selected) }
}

/** Eight dropdown addresses. ProviderOf may recognise more hosts than this list. */
fun providerUrls(): List<Pair<String, String>> =
    listOf(
        "https://api.openai.com/v1" to "openai",
        "https://openrouter.ai/api/v1" to "openrouter",
        "https://api.anthropic.com/v1" to "anthropic",
        "https://generativelanguage.googleapis.com/v1beta/openai" to "google",
        "https://api.deepseek.com/v1" to "deepseek",
        "https://api.mistral.ai/v1" to "mistral",
        "https://api.x.ai/v1" to "xai",
        "http://localhost:11434/v1" to "ollama",
    )

/** Host substring detector. Do not narrow it to the eight dropdown URLs. */
fun providerOf(baseUrl: String): String {
    val url = baseUrl.lowercase()
    return when {
        "openrouter.ai" in url -> "openrouter"
        "api.openai.com" in url -> "openai"
        "anthropic.com" in url -> "anthropic"
        "generativelanguage" in url || "googleapis.com" in url -> "google"
        "deepseek.com" in url -> "deepseek"
        "mistral.ai" in url -> "mistral"
        "api.x.ai" in url -> "xai"
        "localhost" in url || "127.0.0.1" in url || ":11434" in url -> "ollama"
        else -> ""
    }
}

private val recommendations: Map<String, List<Pair<String, String>>> =
    mapOf(
        "openai" to listOf("mech" to "gpt-4.1-nano", "main" to "gpt-4.1-mini", "deep" to "gpt-4.1", "alt" to "gpt-5.6-luna"),
        "openrouter" to
            listOf(
                "mech" to "qwen/qwen3.7-flash",
                "main" to "openai/gpt-5.6-luna",
                "deep" to "anthropic/claude-sonnet-5",
                "alt" to "deepseek/deepseek-v4-pro",
            ),
        "anthropic" to
            listOf("mech" to "claude-haiku-4-5", "main" to "claude-sonnet-5", "deep" to "claude-opus-5", "alt" to "claude-sonnet-4-6"),
        "google" to
            listOf(
                "mech" to "gemini-3.5-flash-lite",
                "main" to "gemini-3.5-flash",
                "deep" to "gemini-3.6-flash",
                "alt" to "gemini-3.1-flash-lite",
            ),
        "deepseek" to listOf("main" to "deepseek-chat", "deep" to "deepseek-reasoner"),
        "mistral" to
            listOf(
                "mech" to "mistral-small-latest",
                "main" to "mistral-medium-latest",
                "deep" to "mistral-large-latest",
                "alt" to "ministral-8b-latest",
            ),
        "xai" to listOf("main" to "grok-4.3", "deep" to "grok-4.5", "alt" to "grok-4.20"),
        "ollama" to listOf("mech" to "qwen3:1.7b", "main" to "qwen3:4b", "deep" to "gemma3:4b", "alt" to "phi4-mini"),
    )

/** Role slug for a provider. An empty role stays empty. A missing role falls back to main. */
fun roleModel(
    role: String,
    baseUrl: String,
): String {
    if (role.isEmpty()) return ""
    val rows = recommendations[providerOf(baseUrl)].orEmpty()
    return rows.firstOrNull { it.first == role }?.second
        ?: rows.firstOrNull { it.first == "main" }?.second.orEmpty()
}

/** Retargets factory ids from the seed roles. A copy with a new id is left alone. */
fun retarget(
    actions: List<Action>,
    baseUrl: String,
): List<Action> {
    val seed = Factory.actions.associateBy { it.factoryId }
    return actions.map { action ->
        val source = action.factoryId?.let { seed[it] } ?: return@map action
        val model = roleModel(source.role.orEmpty(), baseUrl)
        action.copy(model = model.ifEmpty { null })
    }
}

/** A copy of a factory action must not keep the original id. */
fun duplicateFactory(action: Action): Action = action.copy(factoryId = null)

/** Title-specific profiles match before a generic process row. */
fun matchApp(
    apps: List<AppProfile>,
    proc: String,
    title: String,
): AppProfile? {
    if (proc.isEmpty()) return null
    val same = apps.filter { it.proc.equals(proc, ignoreCase = true) }
    return same.firstOrNull { it.title.isNotEmpty() && title.contains(it.title, ignoreCase = true) }
        ?: same.firstOrNull { it.title.isEmpty() }
}

/**
 * Rough token count used before the provider answers.
 * ASCII-heavy text is about 4 characters per token. Other text is about 2.
 * A sample of the first 4000 characters sets the mix. Matches the old script's EstTokens.
 */
fun estimateTokens(text: String): Int {
    val count = text.length
    if (count == 0) return 0
    val sample = if (count > 4000) text.substring(0, 4000) else text
    val ascii = sample.count { it.code < 128 }
    val share = ascii.toDouble() / sample.length
    return kotlin.math.round((count * share / 4.0) + (count * (1.0 - share) / 2.0)).toInt()
}

/** Cost estimate. Null when the catalogue has no price, never a fake zero. */
fun costOf(
    entry: CatalogEntry?,
    promptTokens: Int,
    completionTokens: Int,
): Double? {
    val prompt = entry?.promptPerToken ?: return null
    val completion = entry.completionPerToken ?: return null
    return promptTokens * prompt + completionTokens * completion
}
