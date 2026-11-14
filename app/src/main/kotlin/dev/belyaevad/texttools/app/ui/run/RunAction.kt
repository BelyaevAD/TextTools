package dev.belyaevad.texttools.app.ui.run

import dev.belyaevad.texttools.app.ui.history.ACTION_MARK
import dev.belyaevad.texttools.app.ui.history.INSTRUCTION_MARK
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
import dev.belyaevad.texttools.domain.RequestMetric
import dev.belyaevad.texttools.domain.RunOutcome
import dev.belyaevad.texttools.domain.Settings
import dev.belyaevad.texttools.domain.Usage
import dev.belyaevad.texttools.domain.buildInstruction
import dev.belyaevad.texttools.domain.buildUserContent
import dev.belyaevad.texttools.domain.costOf
import dev.belyaevad.texttools.domain.estimateTokens
import dev.belyaevad.texttools.domain.matchApp
import dev.belyaevad.texttools.platform.ClipboardProtocol
import dev.belyaevad.texttools.platform.PasteAttempt
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
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

internal const val LAST_FILE: String = "TextTools_last.txt"
internal const val LOG_FILE: String = "TextTools.log"

internal val logTime: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault())

/** Text UW can paint. Seconds use a point and one decimal. */
enum class RunPhase { Preparing, Done }

/**
 * Token line for the progress window.
 * [approximate] is the estimate sent with the request. A finished call carries the provider counts.
 */
data class RunUsage(
    val characters: Int,
    val promptTokens: Int,
    val completionTokens: Int?,
    val cost: Double?,
    val approximate: Boolean,
)

/** A language-neutral status; Compose resolves its text. */
data class RunStatus(
    val phase: RunPhase,
    val elapsedMillis: Long,
    val usage: RunUsage? = null,
    val source: String? = null,
)

/** Elapsed time for the banner. A negative count is zero. */
fun statusSeconds(elapsedMillis: Long): String {
    val seconds = elapsedMillis.coerceAtLeast(0).toDouble() / 1000.0
    return String.format(Locale.US, "%.1f", seconds)
}

/** Thousands grouped with a space, as the old script's NumFmt. */
fun groupedCount(value: Int): String {
    val digits = value.coerceAtLeast(0).toString()
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

/** Dollar text for the progress window. A missing price is not passed here. */
fun bannerMoney(value: Double): String {
    val pattern =
        when {
            value >= 1.0 -> "%.2f"
            value >= 0.01 -> "%.3f"
            else -> "%.5f"
        }
    return "$" + String.format(Locale.US, pattern, value)
}

/**
 * Clock seam. [onStatus] fires when the banner text changes.
 */
class RunHooks(
    val clock: () -> Long = { System.currentTimeMillis() },
    val onStatus: (RunStatus?) -> Unit = { _ -> },
)

/**
 * Runs one action through [ActionRunner].
 * Busy ends when paste returns. The pasted text stays on the clipboard.
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
    private val monotonic: () -> Long = { System.nanoTime() },
    private val onRequest: (RequestMetric) -> Unit = {},
) {
    private val gate = AtomicBoolean(false)

    @Volatile
    private var phase: Phase? = null

    @Volatile
    private var measuredInput: Int = 0

    @Volatile
    private var measuredExtra: Int = 0

    @Volatile
    private var reported: Usage? = null

    @Volatile
    private var pricedModel: String = ""

    private val runner = ActionRunner(measureCall(complete), ::deliver)

    /** True from the start of [run] until paste has returned and the result is stored. */
    val busy: Boolean
        get() = gate.get()

    /**
     * Preparing text with a live clock, or done text with the clock stopped.
     * Null before a call and after a failure.
     */
    fun banner(nowMillis: Long = now()): RunStatus? {
        val current = phase ?: return null
        return RunStatus(
            current.phase,
            current.frozenMillis ?: (nowMillis - current.startedAt).coerceAtLeast(0),
            current.usage,
            current.source,
        )
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
            measuredInput = selection.length
            measuredExtra = extra.length
            reported = null
            val profile = matchApp(profiles(), proc, title)
            val hint = profile?.hint.orEmpty()
            val source = profile?.takeIf { it.hint.isNotEmpty() }?.name?.takeIf { it.isNotEmpty() }
            val system =
                buildInstruction(action, store.languages().ifEmpty { Factory.languages }, hint)
            val user = buildUserContent(action, selection, extra)
            val characters = system.length + user.length
            val estimated = estimateTokens(system + "\n" + user)
            val modelName = action.model ?: current.defaultModel
            pricedModel = modelName
            showPreparing(
                now(),
                RunUsage(
                    characters = characters,
                    promptTokens = estimated,
                    completionTokens = null,
                    cost = costOf(priceEntry(store.prices()?.body, modelName, current.baseUrl), estimated, 0),
                    approximate = true,
                ),
                source,
            )
            val outcome =
                runner.run(
                    action = action,
                    selection = selection,
                    customText = extra,
                    settings = current,
                    languages = store.languages().ifEmpty { Factory.languages },
                    sourceHint = hint,
                    forceWindow = shift,
                )

            fun remember(
                text: String,
                model: String,
                usage: Usage?,
            ) {
                val at = now()
                val row =
                    HistoryRecord(
                        atEpochMs = at,
                        actionName = action.name,
                        model = model,
                        promptTokens = usage?.promptTokens,
                        completionTokens = usage?.completionTokens,
                        cost = requestCost(store.prices()?.body, model, current.baseUrl, usage),
                        source = selection,
                        result = text,
                    )
                store.saveHistory((listOf(row) + store.history()).take(HISTORY_LIMIT))
                writeLast(text)
                val note =
                    buildString {
                        append(ACTION_MARK)
                        append(action.name)
                        if (extra.isNotEmpty()) append(INSTRUCTION_MARK)
                        if (shift) append(" (Shift: into window)")
                        append(", selected ")
                        append(selection.length)
                        append(" characters, model ")
                        append(model)
                    }
                appendLog(at, note)
                appendLog(at, "ok, response ${text.length} characters")
                if (usage != null) {
                    val spent =
                        "spend: prompt ${usage.promptTokens}, completion ${usage.completionTokens} tokens, model $model"
                    appendLog(at, spent)
                    store.appendUsage(
                        UsageRecord(at, action.name, model, usage.promptTokens, usage.completionTokens),
                    )
                }
                appendLog(at, "action finished")
            }

            when (outcome) {
                is RunOutcome.Pasted -> remember(outcome.text, outcome.model, outcome.usage)
                is RunOutcome.InWindow -> remember(outcome.text, outcome.model, outcome.usage)
                is RunOutcome.Failed -> {
                    appendLog(now(), "failed: ${outcome.message}")
                    hide()
                }
                RunOutcome.Busy, RunOutcome.NeedsKey -> hide()
            }
            return outcome
        } catch (error: Exception) {
            hide()
            if (error is CancellationException) appendLog(now(), "cancelled")
            throw error
        } finally {
            gate.set(false)
        }
    }

    /** Measures only the provider call, not clipboard delivery. */
    private fun measureCall(
        call: suspend (String, List<ChatMessage>) -> Pair<String, Usage?>,
    ): suspend (String, List<ChatMessage>) -> Pair<String, Usage?> =
        { model, messages ->
            pricedModel = model
            val started = monotonic()
            val at = now()
            var output = 0
            var usage: Usage? = null
            var outcome = "failure"
            try {
                val answer = call(model, messages)
                output = answer.first.trim().length
                usage = answer.second
                reported = answer.second
                outcome = if (output == 0) "failure" else "success"
                answer
            } catch (cancelled: CancellationException) {
                outcome = "cancelled"
                throw cancelled
            } finally {
                onRequest(
                    RequestMetric(
                        atEpochMs = at,
                        kind = "action",
                        model = model,
                        outcome = outcome,
                        inputChars = measuredInput,
                        extraChars = measuredExtra,
                        outputChars = output,
                        durationMs = ((monotonic() - started) / 1_000_000).coerceAtLeast(0),
                        promptTokens = usage?.promptTokens,
                        completionTokens = usage?.completionTokens,
                    ),
                )
            }
        }

    /** One line in the run log. Profiling uses this for the lookup. */
    fun note(line: String) {
        appendLog(now(), line)
    }

    private suspend fun deliver(
        text: String,
        delivery: Delivery,
    ): Boolean {
        // An answer that arrived after cancel is dropped; a paste already past this line finishes.
        currentCoroutineContext().ensureActive()
        showDone(now())
        if (delivery == Delivery.Window) return true
        return when (clipboard.paste(text, captured = true)) {
            is PasteAttempt.Sent -> true
            PasteAttempt.ShowInWindow -> false
        }
    }

    private fun profiles(): List<AppProfile> {
        val stored = store.apps()
        if (stored.isEmpty()) return Factory.apps
        return stored
    }

    private fun showPreparing(
        at: Long,
        usage: RunUsage,
        source: String?,
    ) {
        phase = Phase(RunPhase.Preparing, at, null, usage, source)
        hooks.onStatus(banner(at))
    }

    private fun showDone(at: Long) {
        val started = phase?.startedAt ?: at
        val previous = phase?.usage
        val actual = reported
        val line =
            if (actual == null) {
                previous
            } else {
                val modelName = pricedModel
                RunUsage(
                    characters = previous?.characters ?: 0,
                    promptTokens = actual.promptTokens,
                    completionTokens = actual.completionTokens,
                    cost = requestCost(store.prices()?.body, modelName, settings().baseUrl, actual),
                    approximate = false,
                )
            }
        phase = Phase(RunPhase.Done, started, (at - started).coerceAtLeast(0), line, phase?.source)
        hooks.onStatus(banner(at))
    }

    private fun hide() {
        phase = null
        hooks.onStatus(null)
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
    val phase: RunPhase,
    val startedAt: Long,
    val frozenMillis: Long?,
    val usage: RunUsage?,
    val source: String?,
)

/** Cost of one call. Null when [usage] or the catalogue price is missing. */
internal fun requestCost(
    body: String?,
    model: String,
    baseUrl: String,
    usage: Usage?,
): Double? {
    val tokens = usage ?: return null
    return costOf(priceEntry(body, model, baseUrl), tokens.promptTokens, tokens.completionTokens)
}

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

internal fun numberAfter(
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
