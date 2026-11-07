package dev.belyaevad.texttools.app.ui.menu

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import texttools.app.generated.resources.Res
import texttools.app.generated.resources.menu_editor
import texttools.app.generated.resources.menu_hint
import texttools.app.generated.resources.menu_instruction
import dev.belyaevad.texttools.domain.Action
import java.util.Locale
import org.jetbrains.compose.resources.stringResource

private val ButtonWidth = 150.dp
private val ButtonHeight = 34.dp
private val EditorHeight = 24.dp
private val GridGap = 7.dp
private val HintColor = Color(0xFF707070)
private val InkColor = Color(0xFF202020)
private val LineColor = Color(0xFFB0B0B0)
private val FocusColor = Color(0xFF1A73E8)

/**
 * Three-column action grid. The label is the key, the icon, then the name.
 * Keys 1-0 and q-y run a matching action unless the instruction field is focused.
 * Escape calls [dismiss]. The gear button calls [openEditor].
 */
@Composable
fun actionMenu(
    actions: List<Action>,
    instruction: MutableState<String>,
    onRun: (action: Action, instruction: String, shift: Boolean) -> Unit,
    openEditor: () -> Unit,
    dismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var fieldFocused by remember { mutableStateOf(false) }
    var shiftHeld by remember { mutableStateOf(false) }
    val heldKeys = remember { mutableSetOf<Key>() }
    val firstButton = remember { FocusRequester() }
    LaunchedEffect(actions.isNotEmpty()) {
        if (actions.isNotEmpty()) {
            firstButton.requestFocus()
        }
    }
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .onPreviewKeyEvent { event ->
                    onMenuKey(
                        event = event,
                        held = heldKeys,
                        fieldFocused = fieldFocused,
                        actions = actions,
                        instruction = instruction.value,
                        onShift = { shiftHeld = it },
                        onRun = onRun,
                        dismiss = dismiss,
                    )
                }
                .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(GridGap),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(Res.string.menu_instruction),
                color = InkColor,
                fontSize = 14.sp,
            )
            BasicTextField(
                value = instruction.value,
                onValueChange = { instruction.value = it },
                singleLine = true,
                textStyle = TextStyle(color = InkColor, fontSize = 14.sp),
                cursorBrush = SolidColor(FocusColor),
                modifier =
                    Modifier
                        .weight(1f)
                        .height(26.dp)
                        .onFocusChanged { fieldFocused = it.isFocused }
                        .border(1.dp, if (fieldFocused) FocusColor else LineColor)
                        .background(Color.White)
                        .padding(horizontal = 6.dp),
                decorationBox = { inner ->
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) {
                        inner()
                    }
                },
            )
        }
        actions.chunked(3).forEachIndexed { rowIndex, row ->
            Row(horizontalArrangement = Arrangement.spacedBy(GridGap)) {
                row.forEachIndexed { column, action ->
                    val focus =
                        if (rowIndex == 0 && column == 0) {
                            Modifier.focusRequester(firstButton)
                        } else {
                            Modifier
                        }
                    MenuButton(
                        action = action,
                        modifier = Modifier.width(ButtonWidth).heightIn(min = ButtonHeight).then(focus),
                        onClick = { onRun(action, instruction.value.trim(), shiftHeld) },
                    )
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(Res.string.menu_hint),
                color = HintColor,
                fontSize = 12.sp,
                lineHeight = 15.sp,
                modifier = Modifier.weight(1f),
            )
            OutlinedButton(
                onClick = openEditor,
                modifier = Modifier.width(ButtonWidth).height(EditorHeight),
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                border = BorderStroke(1.dp, LineColor),
                colors =
                    ButtonDefaults.outlinedButtonColors(
                        backgroundColor = Color.White,
                        contentColor = InkColor,
                    ),
            ) {
                Text(
                    text = stringResource(Res.string.menu_editor),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun MenuButton(
    action: Action,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 1.dp),
        border = BorderStroke(1.dp, LineColor),
        colors =
            ButtonDefaults.outlinedButtonColors(
                backgroundColor = Color.White,
                contentColor = InkColor,
            ),
    ) {
        Text(
            text = action.buttonLabel(),
            fontSize = 12.sp,
            lineHeight = 14.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

private fun onMenuKey(
    event: KeyEvent,
    held: MutableSet<Key>,
    fieldFocused: Boolean,
    actions: List<Action>,
    instruction: String,
    onShift: (Boolean) -> Unit,
    onRun: (Action, String, Boolean) -> Unit,
    dismiss: () -> Unit,
): Boolean {
    if (event.type == KeyEventType.KeyUp) {
        held.remove(event.key)
        if (event.isShiftKey()) {
            onShift(false)
        }
        return false
    }
    if (event.type != KeyEventType.KeyDown) {
        return false
    }
    val firstPress = held.add(event.key)
    if (event.isShiftKey()) {
        onShift(true)
        return false
    }
    if (event.key == Key.Escape) {
        if (firstPress) {
            dismiss()
        }
        return true
    }
    val typed = shortcut(event.key) ?: return false
    if (fieldFocused) {
        return false
    }
    val action = actions.firstOrNull { it.menuKey.matchesShortcut(typed) } ?: return false
    if (!firstPress) {
        return true
    }
    onRun(action, instruction.trim(), event.isShiftPressed)
    return true
}

private fun Action.buttonLabel(): String {
    val keyText = menuKey?.trim().orEmpty().uppercase(Locale.ROOT)
    val iconText = icon.trim()
    return buildString {
        if (keyText.isNotEmpty()) {
            append(keyText)
            append(' ')
        }
        if (iconText.isNotEmpty()) {
            append(iconText)
            append("  ")
        }
        append(name)
    }.trimEnd()
}

private fun String?.matchesShortcut(shortcut: String): Boolean = this?.trim()?.equals(shortcut, ignoreCase = true) == true

private fun KeyEvent.isShiftKey(): Boolean = key == Key.ShiftLeft || key == Key.ShiftRight

private fun shortcut(key: Key): String? =
    when (key) {
        Key.One, Key.NumPad1 -> "1"
        Key.Two, Key.NumPad2 -> "2"
        Key.Three, Key.NumPad3 -> "3"
        Key.Four, Key.NumPad4 -> "4"
        Key.Five, Key.NumPad5 -> "5"
        Key.Six, Key.NumPad6 -> "6"
        Key.Seven, Key.NumPad7 -> "7"
        Key.Eight, Key.NumPad8 -> "8"
        Key.Nine, Key.NumPad9 -> "9"
        Key.Zero, Key.NumPad0 -> "0"
        Key.Q -> "q"
        Key.W -> "w"
        Key.E -> "e"
        Key.R -> "r"
        Key.T -> "t"
        Key.Y -> "y"
        else -> null
    }
