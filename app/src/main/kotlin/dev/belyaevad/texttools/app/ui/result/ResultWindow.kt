package dev.belyaevad.texttools.app.ui.result

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.LocalMinimumInteractiveComponentEnforcement
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.rememberWindowState
import texttools.app.generated.resources.Res
import texttools.app.generated.resources.result_close
import texttools.app.generated.resources.result_copy
import texttools.app.generated.resources.result_markup
import texttools.app.generated.resources.result_paste
import texttools.app.generated.resources.result_quick
import texttools.app.generated.resources.result_refine
import texttools.app.generated.resources.result_send
import texttools.app.generated.resources.result_source
import dev.belyaevad.texttools.domain.ChatMessage
import dev.belyaevad.texttools.domain.MdBlock
import dev.belyaevad.texttools.domain.QuickRefine
import dev.belyaevad.texttools.domain.Usage
import dev.belyaevad.texttools.domain.parseMarkdown
import dev.belyaevad.texttools.domain.trimConversation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

private val Ink = Color(0xFF1B1B1B)
private val Chrome = Color(0xFFF0F0F0)
private val Frame = Color(0xFFB4B4B4)
private val Line = Color(0xFFB5B5B5)
private val HeaderBg = Color(0xFFEFEFEF)
private val CodeBg = Color(0xFFF2F2F2)
private val SendBorder = Color(0xFF1A73E8)
private val CopyWidth = 130.dp
private val PasteWidth = 168.dp
private val SourceWidth = 130.dp
private val CloseWidth = 108.dp
private val SendWidth = 130.dp

/**
 * A finished refine. [instruction] is the user turn that was sent.
 * [label] is the chip name, or empty when the text field was used.
 * [text] is the trimmed model answer. The caller writes history from this value.
 */
data class RefineDelivery(
    val model: String,
    val instruction: String,
    val label: String,
    val text: String,
    val usage: Usage?,
)

/**
 * Result window. Draws [parseMarkdown] blocks: heading, list, table, paragraph, and fenced code.
 * Inline markers are painted here. Model text is not turned into HTML.
 *
 * The quick row appears only when [conversation] is present and [chips] is not empty.
 * The refine row appears only when [conversation] is present.
 * A blank refine string does not call [complete].
 * [complete] receives [model] and [trimConversation] of the stored dialogue plus the new user turn.
 * The stored dialogue itself keeps every turn; only the request is trimmed.
 *
 * [markdown] false opens on the raw text, so the toggle reads Markup. Otherwise it reads Source.
 * Copy reports the current source through [onCopy].
 * Paste calls [onClose], then [onPaste] with that source.
 * Escape and Close call [onClose].
 *
 * The model call keeps running after this window leaves the composition.
 * [onDelivered] still receives a non-empty answer. This function does not write history.
 */
@OptIn(ExperimentalMaterialApi::class)
@Suppress("LongParameterList")
@Composable
fun ResultWindow(
    title: String,
    text: String,
    onCopy: (String) -> Unit,
    onPaste: (String) -> Unit,
    onClose: () -> Unit,
    model: String = "",
    conversation: List<ChatMessage>? = null,
    chips: List<QuickRefine> = emptyList(),
    markdown: Boolean = true,
    complete: suspend (model: String, messages: List<ChatMessage>) -> Pair<String, Usage?> = { _, _ -> Pair("", null) },
    onDelivered: (RefineDelivery) -> Unit = {},
) {
    val live = remember { ResultLive(text, conversation, raw = !markdown) }
    // Left running on close so a refine in flight can still call onDelivered.
    val jobs = remember { CoroutineScope(SupervisorJob() + Dispatchers.Main) }
    DisposableEffect(Unit) {
        onDispose { live.alive = false }
    }
    val sourceLabel = stringResource(Res.string.result_source)
    val markupLabel = stringResource(Res.string.result_markup)

    Window(
        onCloseRequest = onClose,
        title = "TextTools - $title",
        state = rememberWindowState(width = 820.dp, height = 640.dp),
        resizable = true,
        alwaysOnTop = true,
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
            CompositionLocalProvider(LocalMinimumInteractiveComponentEnforcement provides false) {
                Surface(modifier = Modifier.fillMaxSize(), color = Chrome) {
                    Column(Modifier.fillMaxSize().padding(10.dp)) {
                        DocumentPane(live)
                        if (live.conversation != null && chips.isNotEmpty()) {
                            QuickRow(
                                chips = chips,
                                enabled = !live.busy,
                                onChip = { chip ->
                                    launchRefine(
                                        jobs,
                                        live,
                                        model,
                                        chip.instruction,
                                        chip.label,
                                        complete,
                                        onDelivered,
                                    )
                                },
                            )
                        }
                        if (live.conversation != null) {
                            RefineRow(
                                draft = live.draft,
                                busy = live.busy,
                                onDraft = { live.draft = it },
                                onSend = {
                                    launchRefine(jobs, live, model, live.draft, "", complete, onDelivered)
                                },
                            )
                        }
                        ActionRow(
                            toggle = if (live.raw) markupLabel else sourceLabel,
                            onCopy = { onCopy(live.text) },
                            onPaste = {
                                val current = live.text
                                onClose()
                                onPaste(current)
                            },
                            onToggle = { live.raw = !live.raw },
                            onClose = onClose,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Sends [question] on [model]. An empty question does nothing.
 * [messages] passed to [complete] are the trimmed dialogue, including the new user turn.
 */
@Suppress("LongParameterList", "SwallowedException")
private fun launchRefine(
    jobs: CoroutineScope,
    live: ResultLive,
    model: String,
    question: String,
    label: String,
    complete: suspend (model: String, messages: List<ChatMessage>) -> Pair<String, Usage?>,
    onDelivered: (RefineDelivery) -> Unit,
) {
    val instruction = question.trim()
    if (instruction.isEmpty()) return
    val previous = live.conversation ?: return
    if (live.busy) return
    val messages = trimConversation(previous + ChatMessage(role = "user", content = instruction))
    live.busy = true
    jobs.launch {
        val reply =
            try {
                complete(model, messages)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                null
            } finally {
                if (live.alive) live.busy = false
            }
        if (reply == null) return@launch
        val answer = reply.first.trim()
        if (answer.isEmpty()) return@launch
        onDelivered(
            RefineDelivery(
                model = model,
                instruction = instruction,
                label = label,
                text = answer,
                usage = reply.second,
            ),
        )
        if (!live.alive) return@launch
        live.text = answer
        live.conversation =
            previous +
                ChatMessage(role = "user", content = instruction) +
                ChatMessage(role = "assistant", content = answer)
        live.draft = ""
    }
}

@Composable
private fun ColumnScope.DocumentPane(live: ResultLive) {
    val scroll = rememberScrollState()
    Box(
        Modifier
            .weight(1f)
            .fillMaxWidth()
            .border(1.dp, Frame)
            .background(Color.White),
    ) {
        Box(Modifier.fillMaxSize().verticalScroll(scroll)) {
            SelectionContainer {
                Column(Modifier.fillMaxWidth().padding(12.dp)) {
                    if (live.raw) {
                        Text(live.text, color = Ink, fontSize = 14.sp)
                    } else {
                        parseMarkdown(live.text).forEach { block -> MarkdownBlock(block) }
                    }
                }
            }
        }
        VerticalScrollbar(
            adapter = rememberScrollbarAdapter(scroll),
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
        )
    }
}

@Composable
private fun MarkdownBlock(block: MdBlock) {
    when (block) {
        is MdBlock.Heading ->
            Text(
                inlineText(block.text),
                color = Ink,
                fontSize = headingSize(block.level),
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 2.dp),
            )
        is MdBlock.Paragraph ->
            Text(
                inlineText(block.text),
                color = Ink,
                fontSize = 14.sp,
                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
            )
        is MdBlock.ListBlock -> MarkdownList(block)
        is MdBlock.Table -> MarkdownTable(block)
        is MdBlock.Code ->
            Text(
                block.text,
                color = Ink,
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .background(CodeBg)
                        .padding(8.dp),
            )
    }
}

private fun headingSize(level: Int) =
    when (level) {
        1 -> 20.sp
        2 -> 18.sp
        3 -> 16.sp
        else -> 14.sp
    }

@Composable
private fun MarkdownList(block: MdBlock.ListBlock) {
    Column(Modifier.fillMaxWidth().padding(start = 8.dp, top = 2.dp, bottom = 2.dp)) {
        block.items.forEachIndexed { index, item ->
            Row(Modifier.fillMaxWidth().padding(vertical = 1.dp)) {
                Text(
                    if (block.ordered) "${index + 1}." else "•",
                    color = Ink,
                    fontSize = 14.sp,
                    modifier = Modifier.width(24.dp),
                )
                Text(inlineText(item), color = Ink, fontSize = 14.sp, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun MarkdownTable(block: MdBlock.Table) {
    val columns = maxOf(block.header.size, block.rows.maxOfOrNull { it.size } ?: 0)
    if (columns == 0) return
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        TableCells(block.header, columns, header = true)
        block.rows.forEach { row -> TableCells(row, columns, header = false) }
    }
}

@Composable
private fun TableCells(
    cells: List<String>,
    columns: Int,
    header: Boolean,
) {
    Row(Modifier.fillMaxWidth().background(if (header) HeaderBg else Color.White)) {
        for (index in 0 until columns) {
            Text(
                inlineText(cells.getOrElse(index) { "" }),
                color = Ink,
                fontSize = 14.sp,
                fontWeight = if (header) FontWeight.Bold else FontWeight.Normal,
                modifier =
                    Modifier
                        .weight(1f)
                        .border(1.dp, Line)
                        .padding(horizontal = 9.dp, vertical = 4.dp),
            )
        }
    }
}

@Composable
private fun QuickRow(
    chips: List<QuickRefine>,
    enabled: Boolean,
    onChip: (QuickRefine) -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            stringResource(Res.string.result_quick),
            color = Ink,
            fontSize = 13.sp,
            modifier = Modifier.padding(end = 8.dp),
        )
        chips.forEach { chip ->
            ResultButton(
                text = chip.label,
                onClick = { onChip(chip) },
                enabled = enabled,
                modifier = Modifier.padding(end = 6.dp),
            )
        }
    }
}

@Composable
private fun RefineRow(
    draft: String,
    busy: Boolean,
    onDraft: (String) -> Unit,
    onSend: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(top = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            stringResource(Res.string.result_refine),
            color = Ink,
            fontSize = 14.sp,
            modifier = Modifier.padding(end = 8.dp),
        )
        BasicTextField(
            value = draft,
            onValueChange = onDraft,
            singleLine = true,
            textStyle = TextStyle(color = Ink, fontSize = 14.sp),
            cursorBrush = SolidColor(SendBorder),
            modifier =
                Modifier
                    .weight(1f)
                    .height(28.dp)
                    .onPreviewKeyEvent { event ->
                        if (event.type == KeyEventType.KeyDown && event.key == Key.Enter) {
                            onSend()
                            true
                        } else {
                            false
                        }
                    }
                    .border(1.dp, Frame)
                    .background(Color.White)
                    .padding(horizontal = 6.dp),
            decorationBox = { inner ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) { inner() }
            },
        )
        ResultButton(
            text = stringResource(Res.string.result_send),
            onClick = onSend,
            enabled = !busy,
            borderColor = SendBorder,
            modifier = Modifier.padding(start = 8.dp).width(SendWidth),
        )
    }
}

@Composable
private fun ActionRow(
    toggle: String,
    onCopy: () -> Unit,
    onPaste: () -> Unit,
    onToggle: () -> Unit,
    onClose: () -> Unit,
) {
    Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ResultButton(stringResource(Res.string.result_copy), onCopy, Modifier.width(CopyWidth))
        ResultButton(stringResource(Res.string.result_paste), onPaste, Modifier.width(PasteWidth))
        ResultButton(toggle, onToggle, Modifier.width(SourceWidth))
        ResultButton(stringResource(Res.string.result_close), onClose, Modifier.width(CloseWidth))
    }
}

@Composable
private fun ResultButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    borderColor: Color = Frame,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(28.dp),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
        border = BorderStroke(1.dp, if (enabled) borderColor else Color(0xFFD0D0D0)),
        colors =
            ButtonDefaults.outlinedButtonColors(
                backgroundColor = Color.White,
                contentColor = Ink,
                disabledContentColor = Color(0xFF808080),
            ),
    ) {
        Text(text, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

private fun inlineText(source: String): AnnotatedString {
    val out = AnnotatedString.Builder()
    out.appendInline(source)
    return out.toAnnotatedString()
}

private fun AnnotatedString.Builder.appendInline(source: String) {
    var index = 0
    while (index < source.length) {
        val mark = markAt(source, index)
        if (mark == null) {
            append(source[index])
            index += 1
            continue
        }
        when (mark.kind) {
            InlineKind.Code ->
                withStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = CodeBg)) {
                    append(mark.body)
                }
            InlineKind.Bold -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { appendInline(mark.body) }
            InlineKind.Italic -> withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { appendInline(mark.body) }
            InlineKind.Link -> appendInline(mark.body)
        }
        index = mark.next
    }
}

private fun markAt(
    source: String,
    index: Int,
): InlineMark? {
    val code = delimited(source, index, "`", "`")
    if (code != null) return InlineMark(InlineKind.Code, code.first, code.second)
    val bold = delimited(source, index, "**", "**") ?: delimited(source, index, "__", "__")
    if (bold != null) return InlineMark(InlineKind.Bold, bold.first, bold.second)
    val link = linkAt(source, index)
    if (link != null) return link
    if (source.startsWith("*", index)) {
        val italic = delimited(source, index, "*", "*")
        if (italic != null && !source.startsWith("*", italic.second)) {
            return InlineMark(InlineKind.Italic, italic.first, italic.second)
        }
    }
    return null
}

private fun delimited(
    source: String,
    index: Int,
    open: String,
    close: String,
): Pair<String, Int>? {
    if (!source.startsWith(open, index)) return null
    val start = index + open.length
    val end = source.indexOf(close, start)
    if (end <= start) return null
    return source.substring(start, end) to (end + close.length)
}

private fun linkAt(
    source: String,
    index: Int,
): InlineMark? {
    if (!source.startsWith("[", index)) return null
    val labelEnd = source.indexOf("](", index + 1)
    if (labelEnd <= index + 1) return null
    val urlEnd = source.indexOf(')', labelEnd + 2)
    if (urlEnd <= labelEnd + 2) return null
    return InlineMark(InlineKind.Link, source.substring(index + 1, labelEnd), urlEnd + 1)
}

private class ResultLive(
    text: String,
    conversation: List<ChatMessage>?,
    raw: Boolean,
) {
    var text by mutableStateOf(text)
    var conversation by mutableStateOf(conversation)
    var raw by mutableStateOf(raw)
    var draft by mutableStateOf("")
    var busy by mutableStateOf(false)

    @Volatile
    var alive: Boolean = true
}

private class InlineMark(
    val kind: InlineKind,
    val body: String,
    val next: Int,
)

private enum class InlineKind {
    Code,
    Bold,
    Italic,
    Link,
}
