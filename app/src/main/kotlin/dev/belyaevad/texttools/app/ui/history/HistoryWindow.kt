package dev.belyaevad.texttools.app.ui.history

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.rememberWindowState
import dev.belyaevad.texttools.app.ui.AppIcon
import dev.belyaevad.texttools.app.ui.AppSecondaryButton
import dev.belyaevad.texttools.app.ui.AppTheme
import dev.belyaevad.texttools.app.ui.HistoryHeight
import dev.belyaevad.texttools.app.ui.HistoryWidth
import dev.belyaevad.texttools.app.ui.fitVisibleFrame
import dev.belyaevad.texttools.app.ui.keepWindowFrame
import dev.belyaevad.texttools.app.ui.rememberPlacement
import dev.belyaevad.texttools.app.ui.result.ResultWindow
import dev.belyaevad.texttools.data.HistoryRecord
import dev.belyaevad.texttools.domain.HISTORY_LIMIT

private val Ink = Color(0xFF1B1B1B)
private val Frame = Color(0xFFB4B4B4)
private val OpenWidth = 170.dp
private val CopyWidth = 180.dp
private val CloseWidth = 100.dp

private data class OpenedText(
    val title: String,
    val text: String,
)

private data class HistoryColumn(
    val title: String,
    val weight: Float,
    val value: (HistoryRecord) -> String,
)

/**
 * History table. Shows [rows] from the start, at most 20, in that order.
 * An empty list opens [ResultWindow] with the empty-history notice: four buttons, and no quick row.
 * The table columns are time, action, model, tokens, cost, source, and result.
 * Today's time has no date. The first row starts selected. A double click opens the result.
 * Open result and open original use [ResultWindow]. Copy passes the selected result to [hooks].
 * This window does not open the database.
 */
@Composable
fun HistoryWindow(
    rows: List<HistoryRecord>,
    hooks: ResultHooks,
) {
    val shown = rows.take(HISTORY_LIMIT)
    if (shown.isEmpty()) {
        noticeResult(Hist.TITLE, Hist.EMPTY_TITLE, Hist.EMPTY_BODY, hooks)
        return
    }
    HistoryTableWindow(shown, hooks)
}

@Composable
private fun HistoryTableWindow(
    rows: List<HistoryRecord>,
    hooks: ResultHooks,
) {
    var selected by remember { mutableIntStateOf(0) }
    var opened by remember { mutableStateOf<OpenedText?>(null) }
    val index = selected.coerceIn(0, rows.lastIndex)
    val columns = historyColumns()
    val suffix = tr(Hist.ORIG_SUFFIX)
    val title = tr(Hist.TITLE)
    val place = rememberPlacement("history", HistoryWidth, HistoryHeight)
    val windowState = rememberWindowState(width = place.width, height = place.height, position = place.position)

    Window(
        onCloseRequest = hooks.onClose,
        title = "TextTools - $title",
        icon = AppIcon,
        state = windowState,
        resizable = true,
        alwaysOnTop = true,
        onPreviewKeyEvent = { event ->
            if (event.type == KeyEventType.KeyDown && event.key == Key.Escape) {
                hooks.onClose()
                true
            } else {
                false
            }
        },
    ) {
        if (!place.restored) fitVisibleFrame(windowState.size) { windowState.size = it }
        keepWindowFrame("history")
        AppTheme {
            Surface(modifier = Modifier.fillMaxSize()) {
                Column(Modifier.fillMaxSize().padding(10.dp)) {
                    HistoryTable(
                        rows = rows,
                        columns = columns,
                        selected = index,
                        onSelect = { selected = it },
                        onOpen = {
                            selected = it
                            opened = OpenedText(rows[it].actionName, rows[it].result)
                        },
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                    )
                    Row(
                        Modifier.padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        HistoryButton(tr(Hist.OPEN_RESULT), Modifier.width(OpenWidth)) {
                            opened = OpenedText(rows[index].actionName, rows[index].result)
                        }
                        HistoryButton(tr(Hist.OPEN_SOURCE), Modifier.width(OpenWidth)) {
                            opened = OpenedText("${rows[index].actionName} - $suffix", rows[index].source)
                        }
                        HistoryButton(tr(Hist.COPY), Modifier.width(CopyWidth)) {
                            hooks.onCopy(rows[index].result)
                        }
                        HistoryButton(tr(Hist.CLOSE), Modifier.width(CloseWidth), hooks.onClose)
                    }
                }
            }
        }
    }

    opened?.let { item ->
        ResultWindow(
            title = item.title,
            text = item.text,
            onCopy = hooks.onCopy,
            onPaste = hooks.onPaste,
            onClose = { opened = null },
            placementId = "history-text",
        )
    }
}

@Composable
private fun historyColumns(): List<HistoryColumn> =
    listOf(
        HistoryColumn(tr(Hist.COL_TIME), 55f) { historyClock(it.atEpochMs) },
        HistoryColumn(tr(Hist.COL_ACTION), 150f) { it.actionName },
        HistoryColumn(tr(Hist.COL_MODEL), 125f) { historyModel(it.model) },
        HistoryColumn(tr(Hist.COL_TOKENS), 70f) { historyTokens(it.promptTokens, it.completionTokens) },
        HistoryColumn(tr(Hist.COL_COST), 80f) { historyMoney(it.cost) },
        HistoryColumn(tr(Hist.COL_SOURCE), 235f) { historySnip(it.source) },
        HistoryColumn(tr(Hist.COL_RESULT), 235f) { historySnip(it.result) },
    )

@Composable
private fun HistoryTable(
    rows: List<HistoryRecord>,
    columns: List<HistoryColumn>,
    selected: Int,
    onSelect: (Int) -> Unit,
    onOpen: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state = rememberLazyListState()
    Column(modifier.border(1.dp, Frame).background(Color.White)) {
        Row(
            Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant).padding(end = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            columns.forEach { column ->
                Text(
                    column.title,
                    color = Ink,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(column.weight).padding(horizontal = 6.dp, vertical = 4.dp),
                )
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(Frame))
        Box(Modifier.weight(1f).fillMaxWidth()) {
            LazyColumn(state = state, modifier = Modifier.fillMaxSize().padding(end = 12.dp)) {
                items(rows.size) { row ->
                    val marked = row == selected
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .background(if (marked) MaterialTheme.colorScheme.secondaryContainer else Color.White)
                            .combinedClickable(
                                role = Role.Button,
                                onClick = { onSelect(row) },
                                onDoubleClick = { onOpen(row) },
                            ).semantics { this.selected = marked },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val color = if (marked) MaterialTheme.colorScheme.onSecondaryContainer else Ink
                        columns.forEach { column ->
                            Text(
                                column.value(rows[row]),
                                color = color,
                                fontSize = 13.sp,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(column.weight).padding(horizontal = 6.dp, vertical = 3.dp),
                            )
                        }
                    }
                }
            }
            VerticalScrollbar(
                adapter = rememberScrollbarAdapter(state),
                modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
            )
        }
    }
}

@Composable
private fun HistoryButton(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    AppSecondaryButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        fontSize = 13.sp,
    )
}
