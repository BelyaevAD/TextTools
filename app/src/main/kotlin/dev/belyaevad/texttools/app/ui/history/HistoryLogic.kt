package dev.belyaevad.texttools.app.ui.history

import dev.belyaevad.texttools.data.UsageEvent
import dev.belyaevad.texttools.data.UsageRecord
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Copy, paste, and close passed through to [dev.belyaevad.texttools.app.ui.result.ResultWindow].
 * The history table calls [onCopy] with the selected result.
 */
data class ResultHooks(
    val onCopy: (String) -> Unit,
    val onPaste: (String) -> Unit,
    val onClose: () -> Unit,
)

/** One action name and how many times the log recorded it. */
internal data class ActionRun(
    val name: String,
    val count: Int,
)

private const val DASH = "—"
private const val SNIP_AT = 60
internal const val ACTION_MARK = "action: "
private const val CUSTOM_MARK = " (custom)"
internal const val INSTRUCTION_MARK = " (+instruction)"
private const val HISTORY_FILE = "TextTools_history.txt"
private const val LOG_FILE = "TextTools.log"
private const val LOG_OLD_FILE = "TextTools.log.old"

private val hourClock: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val dayClock: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM HH:mm")
private val catalogueClock: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")

/**
 * Time cell. [today] in [zone] is drawn as HH:mm. Any other day is dd.MM HH:mm.
 */
internal fun historyClock(
    atEpochMs: Long,
    zone: ZoneId = ZoneId.systemDefault(),
    today: LocalDate = LocalDate.now(zone),
): String {
    val moment = Instant.ofEpochMilli(atEpochMs).atZone(zone)
    val clock = if (moment.toLocalDate() == today) hourClock else dayClock
    return moment.format(clock)
}

/** One line, at most 60 characters, with an ellipsis when the source was longer. */
internal fun historySnip(text: String): String {
    val flat = text.replace('\r', ' ').replace('\n', ' ').trim()
    if (flat.length <= SNIP_AT) return flat
    return flat.substring(0, SNIP_AT) + "…"
}

/** Token cell. A missing usage, or a sum of zero, is a dash. Otherwise the sum, grouped. */
internal fun historyTokens(
    prompt: Int?,
    completion: Int?,
): String {
    if (prompt == null && completion == null) return DASH
    val sum = (prompt ?: 0).toLong() + (completion ?: 0).toLong()
    if (sum == 0L) return DASH
    return grouped(sum)
}

/** Cost cell. A missing or negative price is a dash. Zero is a money amount. */
internal fun historyMoney(cost: Double?): String {
    if (cost == null || !cost.isFinite() || cost < 0.0) return DASH
    return moneyText(cost)
}

/** Model cell. An empty id is a dash. */
internal fun historyModel(model: String): String = if (model.isEmpty()) DASH else model

/**
 * Counts `action: ` lines in [log]. The name ends at the first comma.
 * ` (custom)` and ` (+instruction)` are removed. ` (Shift: into window)` stays.
 * The highest count comes first. Equal counts keep the order they first appeared.
 */
internal fun actionRuns(log: String): List<ActionRun> {
    val counts = linkedMapOf<String, Int>()
    for (line in log.split('\n')) {
        val at = line.indexOf(ACTION_MARK)
        if (at < 0) continue
        val raw = line.substring(at + ACTION_MARK.length).substringBefore(',')
        val name = raw.trim().replace(CUSTOM_MARK, "").replace(INSTRUCTION_MARK, "")
        counts[name] = (counts[name] ?: 0) + 1
    }
    return counts.map { (name, count) -> ActionRun(name, count) }.sortedByDescending { it.count }
}

/** Catalogue time as dd.MM.yyyy HH:mm in [zone]. */
internal fun catalogueStamp(
    epochMs: Long,
    zone: ZoneId = ZoneId.systemDefault(),
): String = catalogueClock.withZone(zone).format(Instant.ofEpochMilli(epochMs))

/** Words the statistics document needs. [stamp] is already formatted, or empty. */
@Suppress("LongParameterList")
internal data class StatsPhrases(
    val heading: String,
    val runsTitle: String,
    val byLog: String,
    val actionColumn: String,
    val runsColumn: String,
    val usageTitle: String,
    val usageEmpty: String,
    val byModel: String,
    val modelColumn: String,
    val requestsColumn: String,
    val promptColumn: String,
    val answerColumn: String,
    val totalColumn: String,
    val costColumn: String,
    val priceUnknown: String,
    val totalLabel: String,
    val estimate: String,
    val catalogue: String,
    val stamp: String,
    val invoice: String,
    val unknownNote: String,
    val eventTitle: String,
    val failedColumn: String,
    val cancelledColumn: String,
    val inputColumn: String,
    val outputColumn: String,
    val timeColumn: String,
    val meanColumn: String,
    val maxColumn: String,
    val eventNote: String,
    val speedColumn: String,
)

/**
 * Markdown for the statistics window.
 * Action runs come from [log]. Token rows sum [usage] by model and ask [price] for the sum.
 * A null or negative price is [StatsPhrases.priceUnknown], and the total then ends with ` +`.
 */
internal fun statsDocument(
    log: String,
    usage: List<UsageRecord>,
    phrases: StatsPhrases,
    price: (model: String, promptTokens: Int, completionTokens: Int) -> Double?,
): String {
    val runs = actionRuns(log)
    val spend = modelSpend(usage, price)
    return buildString {
        append("# ")
        append(phrases.heading)
        append("\n\n## ")
        append(phrases.runsTitle)
        append("\n\n")
        append(phrases.byLog)
        append("\n\n| ")
        append(phrases.actionColumn)
        append(" | ")
        append(phrases.runsColumn)
        append(" |\n|---|---:|\n")
        for (run in runs) {
            append("| ")
            append(run.name)
            append(" | ")
            append(run.count)
            append(" |\n")
        }
        if (spend.isEmpty()) {
            append("\n## ")
            append(phrases.usageTitle)
            append("\n\n")
            append(phrases.usageEmpty)
            append("\n")
            return@buildString
        }
        appendModelTable(phrases, spend)
    }
}

/** Request statistics without localized log markers. */
internal fun usageDocument(
    events: List<UsageEvent>,
    phrases: StatsPhrases,
): String {
    val totals =
        events
            .groupBy { it.model }
            .map { (model, rows) ->
                ModelUsage(
                    model,
                    rows.size,
                    rows.count { it.outcome == "failure" },
                    rows.count { it.outcome == "cancelled" },
                    rows.sumOf { it.inputChars.toLong() + it.extraChars },
                    rows.sumOf { it.outputChars.toLong() },
                    rows.sumOf { it.durationMs },
                    rows.maxOf { it.durationMs },
                    rows.sumOf { it.promptTokens?.toLong() ?: 0L },
                    rows.sumOf { it.completionTokens?.toLong() ?: 0L },
                    tokenText(rows),
                )
            }.sortedByDescending { it.totalMs }
    return buildString {
        append("# ${phrases.eventTitle}\n\n")
        if (events.isNotEmpty()) {
            val clock = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault())
            append(clock.format(Instant.ofEpochMilli(events.minOf { it.atEpochMs })))
            append(" – ")
            append(clock.format(Instant.ofEpochMilli(events.maxOf { it.atEpochMs })))
            append("\n\n")
        }
        append("| ${phrases.modelColumn} | ${phrases.requestsColumn} | ${phrases.failedColumn} | ${phrases.cancelledColumn} | ")
        append(
            "${phrases.inputColumn} | ${phrases.outputColumn} | ${phrases.timeColumn} | ${phrases.meanColumn} | ${phrases.maxColumn} | ${phrases.totalColumn} | ${phrases.speedColumn} |\n",
        )
        append("|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|\n")
        for (row in totals) {
            append("| ${row.model} | ${row.count} | ${row.failed} | ${row.cancelled} | ")
            append("${grouped(row.inputChars)} | ${grouped(row.outputChars)} | ")
            append("${milliseconds(row.totalMs)} | ${grouped(row.totalMs / row.count)} | ${grouped(row.maxMs)} | ")
            append("${row.tokens} | ")
            append(String.format(Locale.US, "%.1f/s", row.outputChars * 1000.0 / row.totalMs.coerceAtLeast(1)))
            append(" |\n")
        }
        if (events.isNotEmpty()) {
            val totalMs = events.sumOf { it.durationMs }
            val output = events.sumOf { it.outputChars.toLong() }
            append("| **${phrases.totalLabel}** | **${events.size}** | **${events.count { it.outcome == "failure" }}** | ")
            append(
                "**${events.count { it.outcome == "cancelled" }}** | **${grouped(
                    events.sumOf { it.inputChars.toLong() + it.extraChars },
                )}** | ",
            )
            append("**${grouped(output)}** | **${milliseconds(totalMs)}** | **${grouped(totalMs / events.size)}** | ")
            append("**${grouped(events.maxOf { it.durationMs })}** | ")
            append("**${tokenText(events)}** | ")
            append("**${String.format(Locale.US, "%.1f/s", output * 1000.0 / totalMs.coerceAtLeast(1))}** |\n")
        }
        append("\n> ${phrases.eventNote}\n")
    }
}

private data class ModelUsage(
    val model: String,
    val count: Int,
    val failed: Int,
    val cancelled: Int,
    val inputChars: Long,
    val outputChars: Long,
    val totalMs: Long,
    val maxMs: Long,
    val promptTokens: Long,
    val completionTokens: Long,
    val tokens: String,
)

private fun tokenText(rows: List<UsageEvent>): String {
    if (rows.none { it.promptTokens != null || it.completionTokens != null }) return DASH
    return grouped(rows.sumOf { (it.promptTokens ?: 0).toLong() + (it.completionTokens ?: 0).toLong() })
}

private fun milliseconds(value: Long): String = String.format(Locale.US, "%.1f s", value / 1000.0)

/**
 * Deletes `TextTools_history.txt` in [directory] when that path is a file.
 * `TextTools_last.txt` stays. Stored history rows stay for the caller to replace.
 */
fun clearHistoryFile(directory: Path) {
    deleteQuiet(directory.resolve(HISTORY_FILE))
}

/**
 * Deletes `TextTools.log` and `TextTools.log.old` in [directory] when each path is a file.
 */
fun clearLogFiles(directory: Path) {
    deleteQuiet(directory.resolve(LOG_FILE))
    deleteQuiet(directory.resolve(LOG_OLD_FILE))
}

private data class ModelSpend(
    val model: String,
    val requests: Int,
    val promptTokens: Long,
    val completionTokens: Long,
    val cost: Double?,
)

private data class TokenAcc(
    val requests: Int,
    val prompt: Long,
    val completion: Long,
)

private fun modelSpend(
    usage: List<UsageRecord>,
    price: (model: String, promptTokens: Int, completionTokens: Int) -> Double?,
): List<ModelSpend> {
    val order = mutableListOf<String>()
    val totals = linkedMapOf<String, TokenAcc>()
    for (row in usage) {
        val current = totals[row.model]
        if (current == null) {
            order += row.model
            totals[row.model] = TokenAcc(1, row.promptTokens.toLong(), row.completionTokens.toLong())
        } else {
            totals[row.model] =
                TokenAcc(
                    current.requests + 1,
                    current.prompt + row.promptTokens,
                    current.completion + row.completionTokens,
                )
        }
    }
    return order
        .map { model ->
            val acc = totals.getValue(model)
            ModelSpend(model, acc.requests, acc.prompt, acc.completion, priced(model, acc, price))
        }.sortedByDescending { it.promptTokens + it.completionTokens }
}

private fun priced(
    model: String,
    acc: TokenAcc,
    price: (model: String, promptTokens: Int, completionTokens: Int) -> Double?,
): Double? {
    if (acc.prompt > Int.MAX_VALUE || acc.completion > Int.MAX_VALUE) return null
    val cost = price(model, acc.prompt.toInt(), acc.completion.toInt()) ?: return null
    if (!cost.isFinite() || cost < 0.0) return null
    return cost
}

private const val BOLD_JOIN = "** | **"

private fun StringBuilder.appendModelTable(
    phrases: StatsPhrases,
    spend: List<ModelSpend>,
) {
    append("\n## ")
    append(phrases.byModel)
    append("\n\n| ")
    append(phrases.modelColumn)
    append(" | ")
    append(phrases.requestsColumn)
    append(" | ")
    append(phrases.promptColumn)
    append(" | ")
    append(phrases.answerColumn)
    append(" | ")
    append(phrases.totalColumn)
    append(" | ")
    append(phrases.costColumn)
    append(" |\n|---|---:|---:|---:|---:|---:|\n")
    var requests = 0
    var prompt = 0L
    var completion = 0L
    var grand = 0.0
    var unknown = false
    for (row in spend) {
        requests += row.requests
        prompt += row.promptTokens
        completion += row.completionTokens
        val cost = row.cost
        if (cost == null) unknown = true else grand += cost
        append("| ")
        append(row.model)
        append(" | ")
        append(row.requests)
        append(" | ")
        append(grouped(row.promptTokens))
        append(" | ")
        append(grouped(row.completionTokens))
        append(" | ")
        append(grouped(row.promptTokens + row.completionTokens))
        append(" | ")
        append(if (cost == null) phrases.priceUnknown else moneyText(cost))
        append(" |\n")
    }
    val totalMoney = moneyText(grand) + if (unknown) " +" else ""
    append("| **")
    append(phrases.totalLabel)
    append(BOLD_JOIN)
    append(requests)
    append(BOLD_JOIN)
    append(grouped(prompt))
    append(BOLD_JOIN)
    append(grouped(completion))
    append(BOLD_JOIN)
    append(grouped(prompt + completion))
    append(BOLD_JOIN)
    append(totalMoney)
    append("** |\n\n> ")
    append(phrases.estimate)
    append(" ")
    append(phrases.catalogue)
    if (phrases.stamp.isNotEmpty()) {
        append(" ")
        append(phrases.stamp)
    }
    append(" ")
    append(phrases.invoice)
    append("\n")
    if (unknown) {
        append("\n> ")
        append(phrases.unknownNote)
        append("\n")
    }
}

private fun moneyText(value: Double): String {
    val pattern =
        when {
            value >= 1.0 -> "%.2f"
            value >= 0.01 -> "%.3f"
            else -> "%.5f"
        }
    return "$" + String.format(Locale.US, pattern, value)
}

private fun grouped(value: Long): String {
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

private fun deleteQuiet(path: Path) {
    try {
        if (Files.isRegularFile(path)) Files.delete(path)
    } catch (_: IOException) {
        return
    }
}
