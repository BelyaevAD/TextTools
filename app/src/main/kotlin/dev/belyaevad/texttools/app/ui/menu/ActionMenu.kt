package dev.belyaevad.texttools.app.ui.menu

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.belyaevad.texttools.app.ui.AppField
import dev.belyaevad.texttools.app.ui.AppSecondaryButton
import dev.belyaevad.texttools.domain.Action
import org.jetbrains.compose.resources.stringResource
import texttools.app.generated.resources.Res
import texttools.app.generated.resources.menu_editor
import texttools.app.generated.resources.menu_hint
import texttools.app.generated.resources.menu_instruction
import java.util.Locale

private val ButtonWidth = 150.dp
private val ButtonHeight = 34.dp
private val EditorHeight = 24.dp
private val GridGap = 7.dp

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
                .fillMaxSize()
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant)
                .onPreviewKeyEvent { event ->
                    onMenuKey(
                        event = event,
                        held = heldKeys,
                        fieldFocused = fieldFocused,
                        actions = actions,
                        instruction = instruction.value,
                        keys =
                            MenuKeyActions(
                                onShift = { shiftHeld = it },
                                onRun = onRun,
                                dismiss = dismiss,
                            ),
                    )
                }.padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(GridGap),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(Res.string.menu_instruction),
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp,
            )
            AppField(
                value = instruction.value,
                onValueChange = { instruction.value = it },
                singleLine = true,
                modifier = Modifier.weight(1f).onFocusChanged { fieldFocused = it.isFocused },
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
                        modifier = Modifier.width(ButtonWidth).height(ButtonHeight).then(focus),
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
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                lineHeight = 15.sp,
                modifier = Modifier.weight(1f),
            )
            AppSecondaryButton(
                text = stringResource(Res.string.menu_editor),
                onClick = openEditor,
                modifier = Modifier.width(ButtonWidth).height(EditorHeight),
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                fontSize = 12.sp,
            )
        }
    }
}

@Composable
private fun MenuButton(
    action: Action,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    AppSecondaryButton(
        text = action.buttonLabel(),
        onClick = onClick,
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 1.dp),
        fontSize = 12.sp,
    )
}

private data class MenuKeyActions(
    val onShift: (Boolean) -> Unit,
    val onRun: (Action, String, Boolean) -> Unit,
    val dismiss: () -> Unit,
)

private fun onMenuKey(
    event: KeyEvent,
    held: MutableSet<Key>,
    fieldFocused: Boolean,
    actions: List<Action>,
    instruction: String,
    keys: MenuKeyActions,
): Boolean {
    if (event.type == KeyEventType.KeyUp) {
        held.remove(event.key)
        if (event.isShiftKey()) {
            keys.onShift(false)
        }
        return false
    }
    if (event.type != KeyEventType.KeyDown) {
        return false
    }
    val firstPress = held.add(event.key)
    if (event.isShiftKey()) {
        keys.onShift(true)
        return false
    }
    if (event.key == Key.Escape) {
        if (firstPress) {
            keys.dismiss()
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
    keys.onRun(action, instruction.trim(), event.isShiftPressed)
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
