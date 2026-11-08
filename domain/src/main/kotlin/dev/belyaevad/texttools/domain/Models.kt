package dev.belyaevad.texttools.domain

/** One menu action. A null model, key or language means "use the default". */
data class Action(
    val factoryId: String?,
    val name: String,
    val icon: String,
    val prefix: String,
    val task: String,
    val lang: String,
    val styleOn: Boolean,
    val openInWindow: Boolean,
    val menuKey: String?,
    val model: String?,
    val role: String?,
)

/** A chip under the result window. */
data class QuickRefine(
    val label: String,
    val instruction: String,
)

/** A source-application profile. An empty title matches any window of that process. */
data class AppProfile(
    val proc: String,
    val title: String,
    val name: String,
    val description: String,
    val hint: String,
)

/** A language the action card may offer. */
data class OutputLanguage(
    val code: String,
    val name: String,
    val prompt: String,
    val enabled: Boolean,
)

/** Non-secret settings. The API key and proxy URL live in the OS secret store. */
data class Settings(
    val baseUrl: String,
    val defaultModel: String,
    val proxy: String,
    val proxyOn: Boolean?,
    val hotkey: String,
    val hotkeyRepeat: String,
    val markdown: Boolean,
    val uiLang: String,
    val appProfiling: Boolean,
    val clipWaitSeconds: Double,
    val sentryErrors: Boolean = false,
    val sentryTraces: Boolean = false,
    val autostart: Boolean = true,
    val profileModel: String = "",
)

/** One chat turn sent to the model. */
data class ChatMessage(
    val role: String,
    val content: String,
)

/** Token counts from the provider. Money is never part of this value. */
data class Usage(
    val promptTokens: Int,
    val completionTokens: Int,
)

data class RequestMetric(
    val atEpochMs: Long,
    val kind: String,
    val model: String,
    val outcome: String,
    val inputChars: Int,
    val extraChars: Int,
    val outputChars: Int,
    val durationMs: Long,
    val promptTokens: Int?,
    val completionTokens: Int?,
)

/** Outcome of one action. An empty model text is a failure, not a result. */
sealed class RunOutcome {
    /** The result was sent to the source window. */
    data class Pasted(
        val text: String,
        val model: String,
        val usage: Usage?,
    ) : RunOutcome()

    /** The result is shown in the result window. */
    data class InWindow(
        val text: String,
        val model: String,
        val usage: Usage?,
        val conversation: List<ChatMessage>,
    ) : RunOutcome()

    /** The call failed and the selection was left untouched. */
    data class Failed(
        val reason: ErrorReason,
        val detail: String,
        val httpStatus: Int? = null,
    ) : RunOutcome() {
        constructor(message: String) : this(ErrorReason.UNKNOWN, message)

        val message: String get() = detail
    }

    /** Another call is still inside paste. */
    data object Busy : RunOutcome()

    /** The secret store has no API key. */
    data object NeedsKey : RunOutcome()
}

/** A model id the user can tick. Selected models survive a reload of the same address. */
data class KnownModel(
    val id: String,
    val selected: Boolean,
)

/** One row of the OpenRouter price catalogue. Missing numbers stay null. */
data class CatalogEntry(
    val id: String,
    val promptPerToken: Double?,
    val completionPerToken: Double?,
    val contextLength: Int?,
)

/** A stored history row. Text is already trimmed. */
data class HistoryEntry(
    val atEpochMs: Long,
    val actionName: String,
    val model: String,
    val promptTokens: Int?,
    val completionTokens: Int?,
    val source: String,
    val result: String,
)
