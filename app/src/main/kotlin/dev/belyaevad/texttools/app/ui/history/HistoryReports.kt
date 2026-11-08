package dev.belyaevad.texttools.app.ui.history

import androidx.compose.runtime.Composable
import dev.belyaevad.texttools.app.ui.result.ResultWindow
import dev.belyaevad.texttools.app.ui.settings.catalogRow
import dev.belyaevad.texttools.data.UsageEvent
import dev.belyaevad.texttools.data.UsageRecord
import dev.belyaevad.texttools.domain.CatalogEntry
import dev.belyaevad.texttools.domain.costOf

/**
 * Last model answer. A null [text] means `TextTools_last.txt` is missing, and [ResultWindow]
 * shows that notice with four buttons and no quick row.
 * This window does not open the database.
 */
@Composable
fun LastResultWindow(
    text: String?,
    hooks: ResultHooks,
) {
    if (text == null) {
        noticeResult(Hist.LAST, Hist.NO_LAST_TITLE, Hist.NO_LAST_BODY, hooks)
        return
    }
    ResultWindow(
        title = tr(Hist.LAST),
        text = text,
        onCopy = hooks.onCopy,
        onPaste = hooks.onPaste,
        onClose = hooks.onClose,
        placementId = "last",
    )
}

/**
 * Usage statistics. Action runs are counted from [log]. Token rows are [usage], priced from [catalog]
 * for [baseUrl]. An unknown price uses the words for "price unknown", and the total keeps a plus.
 * No runs and no usage rows open the empty notice in [ResultWindow], with four buttons and no quick row.
 * [catalogueAtEpochMs] is the catalogue load time. Null omits that sentence.
 * This window does not open the database.
 */
@Composable
fun StatsWindow(
    log: String,
    usage: List<UsageRecord>,
    events: List<UsageEvent>,
    catalog: List<CatalogEntry>,
    baseUrl: String,
    catalogueAtEpochMs: Long?,
    hooks: ResultHooks,
) {
    if (actionRuns(log).isEmpty() && usage.isEmpty() && events.isEmpty()) {
        noticeResult(Hist.STATS, Hist.NO_ACTIONS, Hist.NO_ACTIONS_BODY, hooks)
        return
    }
    val stamp =
        if (catalogueAtEpochMs == null) {
            ""
        } else {
            tr(Hist.NOTE_STAMP, catalogueStamp(catalogueAtEpochMs))
        }
    ResultWindow(
        title = tr(Hist.STATS),
        text =
            buildString {
                append(usageDocument(events, reportPhrases(stamp)))
                append("\n\n")
                append(
                    statsDocument(log, usage, reportPhrases(stamp)) { model, prompt, completion ->
                        costOf(catalogRow(catalog, model, baseUrl), prompt, completion)
                    },
                )
            },
        onCopy = hooks.onCopy,
        onPaste = hooks.onPaste,
        onClose = hooks.onClose,
        placementId = "stats",
    )
}

/**
 * Notice after history was cleared, drawn by [ResultWindow].
 * Call [clearHistoryFile] before showing this. That deletes `TextTools_history.txt`.
 * `TextTools_last.txt` stays. The caller replaces the stored rows.
 */
@Composable
fun HistoryClearedWindow(hooks: ResultHooks) {
    noticeResult(Hist.TITLE, Hist.CLEARED_TITLE, Hist.CLEARED_BODY, hooks)
}

/**
 * Notice after the log was cleared, drawn by [ResultWindow].
 * Call [clearLogFiles] before showing this. That deletes `TextTools.log` and `TextTools.log.old`.
 */
@Composable
fun LogClearedWindow(hooks: ResultHooks) {
    noticeResult(Hist.LOG_TITLE, Hist.LOG_CLEARED_TITLE, Hist.LOG_CLEARED_BODY, hooks)
}

@Composable
internal fun noticeResult(
    title: Hist,
    heading: Hist,
    body: Hist,
    hooks: ResultHooks,
) {
    ResultWindow(
        title = tr(title),
        text = "# ${tr(heading)}\n\n${tr(body)}",
        onCopy = hooks.onCopy,
        onPaste = hooks.onPaste,
        onClose = hooks.onClose,
        placementId = "notice",
    )
}

@Composable
private fun reportPhrases(stamp: String): StatsPhrases =
    StatsPhrases(
        heading = tr(Hist.STATS_HEADING),
        runsTitle = tr(Hist.RUNS),
        byLog = tr(Hist.BY_LOG),
        actionColumn = tr(Hist.COL_ACTION),
        runsColumn = tr(Hist.RUNS_N),
        usageTitle = tr(Hist.USAGE_TITLE),
        usageEmpty = tr(Hist.USAGE_EMPTY),
        byModel = tr(Hist.BY_MODEL),
        modelColumn = tr(Hist.COL_MODEL),
        requestsColumn = tr(Hist.COL_CALLS),
        promptColumn = tr(Hist.COL_PROMPT),
        answerColumn = tr(Hist.COL_ANSWER),
        totalColumn = tr(Hist.COL_TOTAL),
        costColumn = tr(Hist.COL_PRICE),
        priceUnknown = tr(Hist.NO_PRICE),
        totalLabel = tr(Hist.TOTAL),
        estimate = tr(Hist.NOTE_EST),
        catalogue = tr(Hist.NOTE_CATALOG),
        stamp = stamp,
        invoice = tr(Hist.NOTE_DIFF),
        unknownNote = tr(Hist.NOTE_UNKNOWN),
        eventTitle = tr(Hist.EVENT_TITLE),
        failedColumn = tr(Hist.COL_FAILED),
        cancelledColumn = tr(Hist.COL_CANCELLED),
        inputColumn = tr(Hist.COL_INPUT),
        outputColumn = tr(Hist.COL_OUTPUT),
        timeColumn = tr(Hist.COL_TIME_TOTAL),
        meanColumn = tr(Hist.COL_MEAN),
        maxColumn = tr(Hist.COL_MAX),
        eventNote = tr(Hist.EVENT_NOTE),
        speedColumn = tr(Hist.COL_SPEED),
    )
