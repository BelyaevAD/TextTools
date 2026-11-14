package dev.belyaevad.texttools.app.ui.editor

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.rememberDialogState
import dev.belyaevad.texttools.app.ui.AppIcon
import dev.belyaevad.texttools.app.ui.AppPrimaryButton
import dev.belyaevad.texttools.app.ui.AppSecondaryButton
import dev.belyaevad.texttools.app.ui.AppSelect
import dev.belyaevad.texttools.app.ui.AppTheme
import dev.belyaevad.texttools.app.ui.fitVisibleFrame
import dev.belyaevad.texttools.app.ui.keepWindowFrame
import dev.belyaevad.texttools.app.ui.rememberPlacement

@Composable
internal fun framedDialog(
    title: String,
    width: Dp,
    height: Dp,
    onClose: () -> Unit,
    resizable: Boolean = true,
    placementId: String,
    content: @Composable () -> Unit,
) {
    val place = rememberPlacement(placementId, width, height, atLeastDesign = true)
    val state = rememberDialogState(position = place.position, width = place.width, height = place.height)
    DialogWindow(
        onCloseRequest = onClose,
        title = title,
        icon = AppIcon,
        state = state,
        resizable = resizable,
        onPreviewKeyEvent = { event ->
            if (event.type == KeyEventType.KeyDown && event.key == Key.Escape) {
                onClose()
                true
            } else {
                false
            }
        },
    ) {
        if (!place.restored) fitVisibleFrame(state.size) { state.size = it }
        keepWindowFrame(placementId)
        AppTheme {
            Surface(Modifier.fillMaxSize()) {
                content()
            }
        }
    }
}

@Composable
private fun materialAlertDialog(
    onDismiss: () -> Unit,
    text: @Composable () -> Unit,
    confirmButton: @Composable () -> Unit,
    dismissButton: (@Composable () -> Unit)? = null,
) {
    DialogWindow(
        onCloseRequest = onDismiss,
        title = APP_TITLE,
        icon = AppIcon,
        undecorated = true,
        transparent = true,
        resizable = false,
        onPreviewKeyEvent = { event ->
            if (event.type == KeyEventType.KeyDown && event.key == Key.Escape) {
                onDismiss()
                true
            } else {
                false
            }
        },
    ) {
        AppTheme {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text(APP_TITLE) },
                text = text,
                confirmButton = confirmButton,
                dismissButton = dismissButton,
            )
        }
    }
}

@Composable
internal fun noticeWindow(
    text: String,
    ok: String,
    onClose: () -> Unit,
) {
    materialAlertDialog(
        onDismiss = onClose,
        text = { Text(text) },
        confirmButton = {
            AppPrimaryButton(ok, onClose, Modifier.width(100.dp))
        },
    )
}

@Composable
internal fun confirmWindow(
    text: String,
    yes: String,
    no: String,
    onYes: () -> Unit,
    onNo: () -> Unit,
) {
    materialAlertDialog(
        onDismiss = onNo,
        text = { Text(text) },
        confirmButton = {
            AppPrimaryButton(yes, onYes, Modifier.width(100.dp))
        },
        dismissButton = {
            AppSecondaryButton(no, onNo, Modifier.width(100.dp))
        },
    )
}

@Composable
internal fun picker(
    options: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    AppSelect(
        options = options,
        selected = selected,
        onSelect = onSelect,
        modifier = modifier,
        editable = false,
    )
}

@Composable
internal fun editorTable(
    headers: List<String>,
    weights: List<Float>,
    count: Int,
    selected: Int,
    onSelect: (Int) -> Unit,
    onOpen: (Int) -> Unit,
    modifier: Modifier = Modifier,
    text: (Int, Int) -> String,
) {
    val state = rememberLazyListState()
    Column(
        modifier
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant)
            .background(MaterialTheme.colorScheme.surface),
    ) {
        Row(Modifier.background(MaterialTheme.colorScheme.surfaceVariant).padding(end = 12.dp)) {
            headers.forEachIndexed { index, title ->
                Text(
                    title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 13.sp,
                    modifier = Modifier.weight(weights[index]).padding(horizontal = 6.dp, vertical = 4.dp),
                )
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outlineVariant))
        Box(Modifier.weight(1f).fillMaxWidth()) {
            LazyColumn(state = state, modifier = Modifier.fillMaxSize().padding(end = 12.dp)) {
                items(count) { row ->
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .background(
                                    if (row == selected) {
                                        MaterialTheme.colorScheme.secondaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.surface
                                    },
                                ).combinedClickable(
                                    role = Role.Button,
                                    onClick = { onSelect(row) },
                                    onDoubleClick = { onOpen(row) },
                                ).semantics { this.selected = row == selected },
                    ) {
                        headers.indices.forEach { column ->
                            Text(
                                text(row, column),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontSize = 13.sp,
                                modifier =
                                    Modifier
                                        .weight(weights[column])
                                        .padding(horizontal = 6.dp, vertical = 4.dp),
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
