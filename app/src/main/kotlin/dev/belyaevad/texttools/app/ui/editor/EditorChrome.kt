package dev.belyaevad.texttools.app.ui.editor

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.rememberDialogState

@Composable
internal fun framedDialog(
    title: String,
    width: Dp,
    height: Dp,
    onClose: () -> Unit,
    resizable: Boolean = true,
    content: @Composable () -> Unit,
) {
    DialogWindow(
        onCloseRequest = onClose,
        title = title,
        state = rememberDialogState(size = DpSize(width, height)),
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
        MaterialTheme {
            Surface(Modifier.fillMaxSize()) {
                content()
            }
        }
    }
}

@Composable
internal fun noticeWindow(
    text: String,
    ok: String,
    onClose: () -> Unit,
) {
    framedDialog(APP_TITLE, 440.dp, 180.dp, onClose, resizable = false) {
        Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(text, modifier = Modifier.weight(1f))
            Button(onClick = onClose, modifier = Modifier.width(100.dp)) { Text(ok) }
        }
    }
}

@Composable
internal fun confirmWindow(
    text: String,
    yes: String,
    no: String,
    onYes: () -> Unit,
    onNo: () -> Unit,
) {
    framedDialog(APP_TITLE, 480.dp, 230.dp, onNo, resizable = false) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Text(text, modifier = Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onYes, modifier = Modifier.width(100.dp)) { Text(yes) }
                OutlinedButton(onClick = onNo, modifier = Modifier.width(100.dp)) { Text(no) }
            }
        }
    }
}

@Composable
internal fun lineField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
) {
    val sized = if (singleLine) modifier.heightIn(min = 22.dp) else modifier
    val decoration =
        Modifier
            .background(Color.White)
            .border(1.dp, Color(0xFF8A8A8A))
            .padding(horizontal = 6.dp, vertical = 4.dp)
    val scrolling =
        if (singleLine) {
            decoration
        } else {
            decoration.verticalScroll(rememberScrollState())
        }
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = singleLine,
        textStyle = TextStyle(fontSize = 14.sp, color = MaterialTheme.colors.onSurface),
        cursorBrush = SolidColor(MaterialTheme.colors.primary),
        modifier = sized.then(scrolling),
    )
}

@Composable
internal fun picker(
    options: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        OutlinedButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth()) {
            Text(
                options.getOrElse(selected) { "" },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEachIndexed { index, option ->
                DropdownMenuItem(
                    onClick = {
                        onSelect(index)
                        open = false
                    },
                ) {
                    Text(option, maxLines = 1)
                }
            }
        }
    }
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
    Column(modifier.border(1.dp, Color(0xFFB0B0B0)).background(Color.White)) {
        Row(Modifier.background(Color(0xFFF3F3F3)).padding(end = 12.dp)) {
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
        Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFD0D0D0)))
        Box(Modifier.weight(1f).fillMaxWidth()) {
            LazyColumn(state = state, modifier = Modifier.fillMaxSize().padding(end = 12.dp)) {
                items(count) { row ->
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .background(if (row == selected) Color(0xFFCCE8FF) else Color.White)
                                .pointerInput(row) {
                                    detectTapGestures(
                                        onTap = { onSelect(row) },
                                        onDoubleTap = { onOpen(row) },
                                    )
                                },
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
