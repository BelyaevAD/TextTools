package dev.belyaevad.texttools.app.ui.history

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import texttools.app.generated.resources.Res
import texttools.app.generated.resources.hist_by_log
import texttools.app.generated.resources.hist_cleared_body
import texttools.app.generated.resources.hist_cleared_title
import texttools.app.generated.resources.hist_close
import texttools.app.generated.resources.hist_col_action
import texttools.app.generated.resources.hist_col_cost
import texttools.app.generated.resources.hist_col_model
import texttools.app.generated.resources.hist_col_result
import texttools.app.generated.resources.hist_col_source
import texttools.app.generated.resources.hist_col_time
import texttools.app.generated.resources.hist_col_tokens
import texttools.app.generated.resources.hist_copy
import texttools.app.generated.resources.hist_empty_body
import texttools.app.generated.resources.hist_empty_title
import texttools.app.generated.resources.hist_last
import texttools.app.generated.resources.hist_no_actions
import texttools.app.generated.resources.hist_no_actions_body
import texttools.app.generated.resources.hist_no_last_body
import texttools.app.generated.resources.hist_no_last_title
import texttools.app.generated.resources.hist_open_result
import texttools.app.generated.resources.hist_open_source
import texttools.app.generated.resources.hist_orig_suffix
import texttools.app.generated.resources.hist_runs
import texttools.app.generated.resources.hist_runs_n
import texttools.app.generated.resources.hist_stats
import texttools.app.generated.resources.hist_stats_heading
import texttools.app.generated.resources.hist_title
import texttools.app.generated.resources.log_cleared_body
import texttools.app.generated.resources.log_cleared_title
import texttools.app.generated.resources.log_title
import texttools.app.generated.resources.usage_by_model
import texttools.app.generated.resources.usage_col_answer
import texttools.app.generated.resources.usage_col_calls
import texttools.app.generated.resources.usage_col_cost
import texttools.app.generated.resources.usage_col_prompt
import texttools.app.generated.resources.usage_col_total
import texttools.app.generated.resources.usage_empty
import texttools.app.generated.resources.usage_no_price
import texttools.app.generated.resources.usage_note_catalog
import texttools.app.generated.resources.usage_note_diff
import texttools.app.generated.resources.usage_note_est
import texttools.app.generated.resources.usage_note_stamp
import texttools.app.generated.resources.usage_note_unknown
import texttools.app.generated.resources.usage_title
import texttools.app.generated.resources.usage_total

internal enum class Hist(
    val res: StringResource,
) {
    BY_LOG(Res.string.hist_by_log),
    CLEARED_BODY(Res.string.hist_cleared_body),
    CLEARED_TITLE(Res.string.hist_cleared_title),
    CLOSE(Res.string.hist_close),
    COL_ACTION(Res.string.hist_col_action),
    COL_COST(Res.string.hist_col_cost),
    COL_MODEL(Res.string.hist_col_model),
    COL_RESULT(Res.string.hist_col_result),
    COL_SOURCE(Res.string.hist_col_source),
    COL_TIME(Res.string.hist_col_time),
    COL_TOKENS(Res.string.hist_col_tokens),
    COPY(Res.string.hist_copy),
    EMPTY_BODY(Res.string.hist_empty_body),
    EMPTY_TITLE(Res.string.hist_empty_title),
    LAST(Res.string.hist_last),
    NO_ACTIONS(Res.string.hist_no_actions),
    NO_ACTIONS_BODY(Res.string.hist_no_actions_body),
    NO_LAST_BODY(Res.string.hist_no_last_body),
    NO_LAST_TITLE(Res.string.hist_no_last_title),
    OPEN_RESULT(Res.string.hist_open_result),
    OPEN_SOURCE(Res.string.hist_open_source),
    ORIG_SUFFIX(Res.string.hist_orig_suffix),
    RUNS(Res.string.hist_runs),
    RUNS_N(Res.string.hist_runs_n),
    STATS(Res.string.hist_stats),
    STATS_HEADING(Res.string.hist_stats_heading),
    TITLE(Res.string.hist_title),
    LOG_CLEARED_BODY(Res.string.log_cleared_body),
    LOG_CLEARED_TITLE(Res.string.log_cleared_title),
    LOG_TITLE(Res.string.log_title),
    BY_MODEL(Res.string.usage_by_model),
    COL_ANSWER(Res.string.usage_col_answer),
    COL_CALLS(Res.string.usage_col_calls),
    COL_PRICE(Res.string.usage_col_cost),
    COL_PROMPT(Res.string.usage_col_prompt),
    COL_TOTAL(Res.string.usage_col_total),
    USAGE_EMPTY(Res.string.usage_empty),
    NO_PRICE(Res.string.usage_no_price),
    NOTE_CATALOG(Res.string.usage_note_catalog),
    NOTE_DIFF(Res.string.usage_note_diff),
    NOTE_EST(Res.string.usage_note_est),
    NOTE_STAMP(Res.string.usage_note_stamp),
    NOTE_UNKNOWN(Res.string.usage_note_unknown),
    USAGE_TITLE(Res.string.usage_title),
    TOTAL(Res.string.usage_total),
}

@Composable
internal fun tr(key: Hist): String = stringResource(key.res)

@Composable
internal fun tr(
    key: Hist,
    arg: Any,
): String = stringResource(key.res, arg)
