package dev.belyaevad.texttools.domain

/** Where a finished result should land. */
enum class Delivery { Paste, Window }

/**
 * Runs one action against a caller-supplied model.
 * The busy flag stays set until this function returns, which is after paste is attempted.
 */
class ActionRunner(
    private val model: suspend (model: String, messages: List<ChatMessage>) -> Pair<String, Usage?>,
    private val deliver: suspend (text: String, delivery: Delivery) -> Boolean,
) {
    /** True while a call has not yet returned from paste. */
    var busy: Boolean = false
        private set

    /** Executes the action. An empty model text does not paste and does not become history. */
    suspend fun run(
        action: Action,
        selection: String,
        customText: String,
        settings: Settings,
        languages: List<OutputLanguage>,
        sourceHint: String,
        hasKey: Boolean,
        forceWindow: Boolean,
    ): RunOutcome {
        if (busy) return RunOutcome.Busy
        if (!hasKey) return RunOutcome.NeedsKey
        busy = true
        try {
            val chosen = action.model ?: settings.defaultModel
            val system = buildInstruction(action, languages, sourceHint)
            val user = buildUserContent(action, selection, customText)
            val (raw, usage) =
                try {
                    model(chosen, listOf(ChatMessage("system", system), ChatMessage("user", user)))
                } catch (error: Exception) {
                    return RunOutcome.Failed(error.errorReason(), error.message ?: "request failed", error.errorHttpStatus())
                }
            val text = raw.trim()
            if (text.isEmpty()) {
                return RunOutcome.Failed(ErrorReason.EMPTY_RESPONSE, "empty response")
            }
            val delivery = if (action.openInWindow || forceWindow) Delivery.Window else Delivery.Paste
            val pasted = deliver(text, delivery)
            val conversation =
                listOf(
                    ChatMessage("system", system),
                    ChatMessage("user", user),
                    ChatMessage("assistant", text),
                )
            return if (delivery == Delivery.Window || !pasted) {
                RunOutcome.InWindow(text, chosen, usage, conversation)
            } else {
                RunOutcome.Pasted(text, chosen, usage)
            }
        } finally {
            busy = false
        }
    }
}
