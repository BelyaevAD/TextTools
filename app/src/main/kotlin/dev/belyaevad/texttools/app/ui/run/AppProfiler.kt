package dev.belyaevad.texttools.app.ui.run

import dev.belyaevad.texttools.domain.AppProfile
import dev.belyaevad.texttools.domain.ChatMessage
import dev.belyaevad.texttools.domain.Usage
import dev.belyaevad.texttools.domain.applyAppDescription
import dev.belyaevad.texttools.domain.parseAppDescription
import dev.belyaevad.texttools.domain.profileMessages
import dev.belyaevad.texttools.domain.withNeutralApp
import kotlinx.coroutines.CancellationException

/**
 * Adds an unknown application at once and, when profiling is on, looks it up in the background.
 * The action is not held for the reply. A neutral profile is not asked about again.
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
    /** Remembers [proc] when it is new and starts the lookup without waiting for it. */
    fun consider(
        proc: String,
        title: String,
        enabled: Boolean,
        description: String,
    ) {
        val current = apps()
        val stored = withNeutralApp(current, proc, title, description)
        if (stored === current) return
        save(stored)
        note("new application: ${proc.trim()} - profile stays neutral")
        if (!enabled) return
        note("requesting description: ${proc.trim()}")
        background {
            fill(proc, title)
        }
    }

    private suspend fun fill(
        proc: String,
        title: String,
    ) {
        val notes =
            try {
                search(proc, title)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                ""
            }
        val text =
            try {
                val (body, usage) = complete(profilingModel, profileMessages(proc, title, notes))
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
}
