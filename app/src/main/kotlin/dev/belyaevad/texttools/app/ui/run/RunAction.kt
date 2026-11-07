package dev.belyaevad.texttools.app.ui.run

import dev.belyaevad.texttools.data.HistoryRecord
import dev.belyaevad.texttools.data.TextToolsStore
import dev.belyaevad.texttools.data.UsageRecord
import dev.belyaevad.texttools.domain.Action
import dev.belyaevad.texttools.domain.ActionRunner
import dev.belyaevad.texttools.domain.AppProfile
import dev.belyaevad.texttools.domain.CatalogEntry
import dev.belyaevad.texttools.domain.ChatMessage
import dev.belyaevad.texttools.domain.Delivery
import dev.belyaevad.texttools.domain.Factory
import dev.belyaevad.texttools.domain.HISTORY_LIMIT
import dev.belyaevad.texttools.domain.RunOutcome
import dev.belyaevad.texttools.domain.Settings
import dev.belyaevad.texttools.domain.Usage
import dev.belyaevad.texttools.domain.costOf
import dev.belyaevad.texttools.domain.matchApp
import dev.belyaevad.texttools.platform.ClipboardProtocol
import dev.belyaevad.texttools.platform.PasteAttempt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

/** Banner line while the model call is in flight. UW draws it. */
const val STATUS_PREPARING: String = "готовлю запрос"

/** Banner line after the model has answered. The seconds stay at this moment. */
const val STATUS_DONE: String = "готово"

/**
 * Delay before [ClipboardProtocol.restoreAfterPaste].
 * The busy flag is already clear. AHK uses the same 3 seconds.
 */
const val RESTORE_AFTER_PASTE_MILLIS: Long = 3_000L

private const val LAST_FILE: String = "TextTools_last.txt"
private const val LOG_FILE: String = "TextTools.log"

private val logTime: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault())

/** Text UW can paint. Seconds use a point and one decimal. */
data class RunStatus(
    val text: String,
    val seconds: String,
)

/** Elapsed time for the banner. A negative count is zero. */
fun statusSeconds(elapsedMillis: Long): String {
    val seconds = elapsedMillis.coerceAtLeast(0).toDouble() / 1000.0
    return String.format(Locale.US, "%.1f \u0441", seconds)
}

/**
 * Clock and timer seams. A null [schedule] waits on this process.
 * [pause] is the delay inside that wait. [onStatus] fires when the banner text changes.
 */
class RunHooks(
    val clock: () -> Long = { System.currentTimeMillis() },
    val schedule: ((suspend () -> Unit) -> Unit)? = null,
    val pause: suspend (Long) -> Unit = { delay(it) },
    val onStatus: (RunStatus?) -> Unit = { _ -> },
)

/**
 * Runs one action through [ActionRunner].
 * Busy ends when paste returns. Restoring the previous clipboard is a later timer,
 * and only while the clipboard still equals the pasted text.
 * Empty model text and errors are not pasted and are not written to history.
 * This type does not read the API key. [hasKey] false returns [RunOutcome.NeedsKey]
 * and does not call the model.
 */
class RunAction(
    complete: suspend (model: String, messages: List<ChatMessage>) -> Pair<String, Usage?>,
    private val clipboard: ClipboardProtocol,
    private val store: TextToolsStore,
    private val dataDir: Path,
    private val settings: () -> Settings,
    private val hasKey: () -> Boolean,
    private val hooks: RunHooks = RunHooks(),
) {
    private val jobs = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var restoreJob: Job? = null
    private val enqueue: (suspend () -> Unit) -> Unit =
        hooks.schedule ?: { block ->
            restoreJob?.cancel()
            restoreJob = jobs.launch { block() }
        }
    private val gate = AtomicBoolean(false)

    @Volatile
    private var phase: Phase? = null

    private val runner = ActionRunner(complete, ::deliver)

    /** True from the start of [run] until paste has returned and the result is stored. */
    val busy: Boolean
        get() = gate.get()

    /**
     * Preparing text with a live clock, or done text with the clock stopped.
     * Null before a call and after a failure.
     */
    fun banner(nowMillis: Long = now()): RunStatus? {
        val current = phase ?: return null
        val seconds = current.frozenSeconds ?: statusSeconds(nowMillis - current.startedAt)
        return RunStatus(current.text, seconds)
    }

    /**
     * Runs [action]. [shift] or [Action.openInWindow] shows a window.
     * A paste that does not land returns [RunOutcome.InWindow] as well.
     * A second call while [busy] returns [RunOutcome.Busy].
     * [instruction] is the extra field. Blank adds nothing.
     * [proc] and [title] select the source hint through [matchApp].
     */
    suspend fun run(
        action: Action,
        selection: String,
        instruction: String,
        shift: Boolean,
        proc: String,
        title: String,
    ): RunOutcome {
        if (!gate.compareAndSet(false, true)) return RunOutcome.Busy
        try {
            if (!hasKey()) return RunOutcome.NeedsKey
            val current = settings()
            val extra = instruction.trim()
            showPreparing(now())
            val outcome =
                runner.run(
                    action = action,
                    selection = selection,
                    customText = extra,
                    settings = current,
                    languages = store.languages().ifEmpty { Factory.languages },
                    sourceHint = matchApp(profiles(), proc, title)?.hint.orEmpty(),
                    hasKey = true,
                    forceWindow = shift,
                )

            fun remember(
                text: String,
                model: String,
                usage: Usage?,
            ) {
                val at = now()
                val priced = priceEntry(store.prices()?.body, model, current.baseUrl)
                val row =
                    HistoryRecord(
                        atEpochMs = at,
                        actionName = action.name,
                        model = model,
                        promptTokens = usage?.promptTokens,
                        completionTokens = usage?.completionTokens,
                        cost = usage?.let { costOf(priced, it.promptTokens, it.completionTokens) },
                        source = selection,
                        result = text,
                    )
                store.saveHistory((listOf(row) + store.history()).take(HISTORY_LIMIT))
                writeLast(text)
                val note =
                    buildString {
                        append("действие: ")
                        append(action.name)
                        if (extra.isNotEmpty()) append(" (+инструкция)")
                        if (shift) append(" (Shift: в окно)")
                        append(", выделено ")
                        append(selection.length)
                        append(" символов, модель ")
                        append(model)
                    }
                appendLog(at, note)
                appendLog(at, "ok, ответ ${text.length} символов")
                if (usage != null) {
                    val spent = "расход: промпт ${usage.promptTokens}, ответ ${usage.completionTokens} токенов, модель $model"
                    appendLog(at, spent)
                    store.saveUsage(
                        store.usage() + UsageRecord(at, action.name, model, usage.promptTokens, usage.completionTokens),
                    )
                }
                appendLog(at, "действие завершено")
            }

            when (outcome) {
                is RunOutcome.Pasted -> remember(outcome.text, outcome.model, outcome.usage)
                is RunOutcome.InWindow -> remember(outcome.text, outcome.model, outcome.usage)
                is RunOutcome.Failed -> {
                    appendLog(now(), "сбой: ${outcome.message}")
                    hide()
                }
                RunOutcome.Busy, RunOutcome.NeedsKey -> hide()
            }
            return outcome
        } catch (error: Exception) {
            hide()
            throw error
        } finally {
            gate.set(false)
        }
    }

    /**
     * Enter in the instruction field.
     * Blank text returns null and does not call the model.
     * Any other text runs [Factory.custom]. [shift] still asks for a window.
     */
    suspend fun enter(
        instruction: String,
        selection: String,
        shift: Boolean,
        proc: String,
        title: String,
    ): RunOutcome? {
        if (instruction.trim().isEmpty()) return null
        return run(Factory.custom, selection, instruction, shift, proc, title)
    }

    /** Drops a pending clipboard restore. */
    fun close() {
        restoreJob?.cancel()
        jobs.cancel()
    }

    private suspend fun deliver(
        text: String,
        delivery: Delivery,
    ): Boolean {
        showDone(now())
        if (delivery == Delivery.Window) return true
        cancelRestore()
        return when (val attempt = clipboard.paste(text)) {
            is PasteAttempt.Sent -> {
                val saved = attempt.saved
                val pasted = attempt.pasted
                enqueue {
                    hooks.pause(RESTORE_AFTER_PASTE_MILLIS)
                    clipboard.restoreAfterPaste(saved, pasted)
                }
                true
            }
            PasteAttempt.ShowInWindow -> false
        }
    }

    private fun profiles(): List<AppProfile> {
        val stored = store.apps()
        if (stored.isEmpty()) return Factory.apps
        return stored
    }

    private fun showPreparing(at: Long) {
        phase = Phase(STATUS_PREPARING, at, null)
        hooks.onStatus(banner(at))
    }

    private fun showDone(at: Long) {
        val started = phase?.startedAt ?: at
        phase = Phase(STATUS_DONE, started, statusSeconds(at - started))
        hooks.onStatus(banner(at))
    }

    private fun hide() {
        phase = null
        hooks.onStatus(null)
    }

    private fun cancelRestore() {
        restoreJob?.cancel()
        restoreJob = null
    }

    private fun now(): Long = hooks.clock()

    private fun writeLast(text: String) {
        try {
            Files.createDirectories(dataDir)
            Files.writeString(dataDir.resolve(LAST_FILE), text, StandardCharsets.UTF_8)
        } catch (_: IOException) {
            return
        }
    }

    private fun appendLog(
        at: Long,
        line: String,
    ) {
        try {
            Files.createDirectories(dataDir)
            val stamp = logTime.format(Instant.ofEpochMilli(at))
            Files.writeString(
                dataDir.resolve(LOG_FILE),
                "$stamp  $line\n",
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND,
            )
        } catch (_: IOException) {
            return
        }
    }
}

private class Phase(
    val text: String,
    val startedAt: Long,
    val frozenSeconds: String?,
)

private fun priceEntry(
    body: String?,
    model: String,
    baseUrl: String,
): CatalogEntry? {
    if (body.isNullOrEmpty() || model.contains('"') || model.contains('\\')) return null
    val slug = priceSlug(model, baseUrl)
    val quoted = "\"$slug\""
    var from = 0
    while (from < body.length) {
        val at = body.indexOf(quoted, from)
        if (at < 0) return null
        if (isIdKey(body, at)) return pricesAfter(body, slug, at)
        from = at + quoted.length
    }
    return null
}

private fun pricesAfter(
    body: String,
    slug: String,
    at: Int,
): CatalogEntry? {
    val next = body.indexOf("\"id\"", at + 4).let { if (it < 0) body.length else it }
    val slice = body.substring(at, next)
    val pricingAt = slice.indexOf("\"pricing\"")
    if (pricingAt < 0) return null
    val endBrace = slice.indexOf('}', pricingAt)
    val pricing = if (endBrace < 0) slice.substring(pricingAt) else slice.substring(pricingAt, endBrace)
    val prompt = numberAfter(pricing, "prompt") ?: return null
    val completion = numberAfter(pricing, "completion") ?: return null
    return CatalogEntry(slug, prompt, completion, null)
}

private fun isIdKey(
    body: String,
    valueStart: Int,
): Boolean {
    var index = valueStart - 1
    while (index >= 0 && body[index].isWhitespace()) index--
    if (index < 0 || body[index] != ':') return false
    index--
    while (index >= 0 && body[index].isWhitespace()) index--
    if (index < 3) return false
    return body.regionMatches(index - 3, "\"id\"", 0, 4)
}

private fun numberAfter(
    slice: String,
    key: String,
): Double? {
    val keyAt = slice.indexOf("\"$key\"")
    if (keyAt < 0) return null
    var index = keyAt + key.length + 2
    while (index < slice.length && (slice[index].isWhitespace() || slice[index] == ':')) index++
    if (index >= slice.length || slice.startsWith("null", index)) return null
    if (slice[index] == '"') {
        val end = slice.indexOf('"', index + 1)
        if (end < 0) return null
        return slice.substring(index + 1, end).toDoubleOrNull()
    }
    val end =
        slice.indexOfAny(charArrayOf(',', '}', ' ', '\n', '\r', '\t'), index).let { found ->
            if (found < 0) slice.length else found
        }
    return slice.substring(index, end).toDoubleOrNull()
}

private fun priceSlug(
    model: String,
    baseUrl: String,
): String {
    if ('/' in model) return model
    val url = baseUrl.lowercase()
    return when {
        "openai.com" in url -> "openai/$model"
        "anthropic.com" in url -> "anthropic/$model"
        else -> model
    }
}
