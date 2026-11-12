package dev.belyaevad.texttools.app.ui.run

import dev.belyaevad.texttools.domain.AppProfile
import dev.belyaevad.texttools.domain.ChatMessage
import dev.belyaevad.texttools.domain.Usage
import dev.belyaevad.texttools.domain.applyAppDescription
import dev.belyaevad.texttools.domain.parseAppDescription
import dev.belyaevad.texttools.domain.profileMessages
import dev.belyaevad.texttools.domain.profilePending
import dev.belyaevad.texttools.domain.settleUnidentified
import dev.belyaevad.texttools.domain.withNeutralApp
import kotlinx.coroutines.CancellationException
import java.util.concurrent.ConcurrentHashMap

/**
 * Adds an unknown application at once and, when profiling is on, looks it up in the background.
 * The action is not held for the reply. A failed lookup can be asked again. UNKNOWN is not.
 */
class AppProfiler(
    private val apps: () -> List<AppProfile>,
    private val save: (List<AppProfile>) -> Unit,
    private val search: suspend (String, String) -> String,
    private val complete: suspend (String, List<ChatMessage>) -> Pair<String, Usage?>,
    private val background: (suspend () -> Unit) -> Unit,
    private val profilingModel: String,
    private val note: (String) -> Unit = {},
    private val onUsage: (Usage) -> Unit = {},
) {
    /**
     * One lookup per executable for the whole process.
     * [consider] builds a new profiler on every action, so an instance field would not stop a second action from asking again while the first lookup is still running.
     */
    private companion object {
        val asking = ConcurrentHashMap.newKeySet<String>()
    }

    /**
     * Remembers [proc] when it is new and, when profiling is on, looks it up without waiting.
     * An automatic row whose description still starts with the added prefix is asked again
     * after a failed request. A finished UNKNOWN answer is not asked again.
     */
    fun consider(
        proc: String,
        title: String,
        windowClass: String,
        enabled: Boolean,
        description: String,
        addedPrefix: String,
        unknownDescription: String,
    ) {
        val current = apps()
        val stored = withNeutralApp(current, proc, title, description)
        if (stored !== current) {
            save(stored)
            note("new application: ${proc.trim()} - profile stays neutral")
        }
        if (!enabled) return
        val latest = if (stored !== current) stored else current
        if (!profilePending(latest, proc, title, addedPrefix)) return
        val key = proc.trim().lowercase()
        if (!asking.add(key)) return
        note("requesting description: ${proc.trim()}")
        background {
            try {
                fill(proc, title, windowClass, unknownDescription)
            } finally {
                asking.remove(key)
            }
        }
    }

    private suspend fun fill(
        proc: String,
        title: String,
        windowClass: String,
        unknownDescription: String,
    ) {
        val notes =
            try {
                search(proc, title)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                ""
            }
        val messages = profileMessages(proc, title, notes, windowClass)
        val text =
            try {
                val (body, usage) = ask(profilingModel, messages)
                if (usage != null) onUsage(usage)
                body
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                note("description for ${proc.trim()} was not requested (${error.message})")
                return
            }
        val parsed = parseAppDescription(text)
        if (parsed == null) {
            val current = apps()
            val settled = settleUnidentified(current, proc, unknownDescription)
            if (settled !== current) save(settled)
            note("the model does not know ${proc.trim()} - profile stays neutral")
            return
        }
        val latest = apps()
        val updated = applyAppDescription(latest, proc, parsed)
        if (updated === latest) {
            note("description for ${proc.trim()}: profile is no longer neutral")
            return
        }
        save(updated)
        val suffix = if (parsed.hint.isEmpty()) " (no prompt note)" else ""
        note("description received: ${proc.trim()}$suffix")
    }

    /** OpenRouter `:online` can reject a model. The web notes are already in the prompt, so the plain id is enough. */
    private suspend fun ask(
        model: String,
        messages: List<ChatMessage>,
    ): Pair<String, Usage?> =
        try {
            complete(model, messages)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            val plain = model.removeSuffix(":online")
            if (plain == model) throw error
            note("web model $model was refused, asking $plain")
            complete(plain, messages)
        }
}
