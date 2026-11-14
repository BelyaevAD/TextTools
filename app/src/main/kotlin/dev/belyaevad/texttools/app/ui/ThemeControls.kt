package dev.belyaevad.texttools.app.ui

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.OutputTransformation
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableLongState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.awt.Window

private val FieldHeight = 34.dp
private val FieldHorizontalPadding = 12.dp
private val FieldVerticalPadding = 8.dp

/** Compact Material 3 outline. Its decorator does not impose the 280×56 dp default. */
@Composable
fun AppField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    password: Boolean = false,
    singleLine: Boolean = true,
) {
    AppField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        password = password,
        singleLine = singleLine,
        readOnly = false,
        trailingIcon = null,
    )
}

@Composable
private fun AppField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    password: Boolean,
    singleLine: Boolean,
    readOnly: Boolean,
    trailingIcon: (@Composable () -> Unit)?,
) {
    val state = rememberTextFieldState(value)
    val currentValue by rememberUpdatedState(value)
    LaunchedEffect(state) {
        snapshotFlow { state.text.toString() }.collect { changed ->
            if (changed != currentValue) onValueChange(changed)
        }
    }
    LaunchedEffect(state, value) {
        if (state.text.toString() != value) state.setTextAndPlaceCursorAtEnd(value)
    }
    val interactionSource = remember { MutableInteractionSource() }
    val lineLimits = if (singleLine) TextFieldLineLimits.SingleLine else TextFieldLineLimits.MultiLine()
    val outputTransformation =
        if (password) {
            OutputTransformation { replace(0, length, "•".repeat(length)) }
        } else {
            null
        }
    BasicTextField(
        state = state,
        modifier = if (singleLine) modifier.height(FieldHeight) else modifier,
        enabled = enabled,
        readOnly = readOnly,
        textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp),
        lineLimits = lineLimits,
        interactionSource = interactionSource,
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        outputTransformation = outputTransformation,
        decorator =
            OutlinedTextFieldDefaults.decorator(
                state = state,
                enabled = enabled,
                lineLimits = lineLimits,
                outputTransformation = outputTransformation,
                interactionSource = interactionSource,
                trailingIcon = trailingIcon,
                contentPadding =
                    OutlinedTextFieldDefaults.contentPaddingWithoutLabel(
                        start = FieldHorizontalPadding,
                        top = if (singleLine) 0.dp else FieldVerticalPadding,
                        end = FieldHorizontalPadding,
                        bottom = if (singleLine) 0.dp else FieldVerticalPadding,
                    ),
            ),
    )
}

/** Full-width Material 3 tabs with the standard primary indicator. */
@Composable
fun AppTabs(
    labels: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (labels.isEmpty()) return
    val current = selected.coerceIn(labels.indices)
    PrimaryTabRow(
        selectedTabIndex = current,
        modifier = modifier.fillMaxWidth(),
        indicator = {
            TabRowDefaults.PrimaryIndicator(
                modifier = Modifier.tabIndicatorOffset(current, matchContentSize = false),
            )
        },
    ) {
        labels.forEachIndexed { index, label ->
            Tab(
                selected = current == index,
                onClick = { onSelect(index) },
                text = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            )
        }
    }
}

/** Material 3 select. Editable mode also reports manual text changes. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSelect(
    options: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    editable: Boolean = false,
    onValueChange: ((String) -> Unit)? = null,
) {
    val expandedState = remember { mutableStateOf(false) }
    var expanded by expandedState
    val openedAt = remember { mutableLongStateOf(0L) }
    var editedValue by remember { mutableStateOf("") }
    val optionsState = rememberUpdatedState(options)
    val shownValue = if (editable) editedValue else options.getOrNull(selected).orEmpty()
    val canOpen = options.isNotEmpty()
    LaunchedEffect(options, selected, editable) {
        if (editable) editedValue = options.getOrNull(selected).orEmpty()
        if (options.isEmpty()) expanded = false
    }
    KeepMenuAboveOwner(expanded && canOpen)
    Box(modifier) {
        AppField(
            value = shownValue,
            onValueChange = { changed ->
                if (editable) {
                    editedValue = changed
                    onValueChange?.invoke(changed)
                }
            },
            modifier =
                Modifier.fillMaxWidth().onPreviewKeyEvent { event ->
                    val open = canOpen && event.type == KeyEventType.KeyDown && event.key == Key.DirectionDown
                    if (open) {
                        openedAt.longValue = System.nanoTime()
                        expanded = true
                    }
                    open
                },
            enabled = editable || canOpen,
            password = false,
            singleLine = true,
            readOnly = !editable,
            trailingIcon = {
                val iconModifier =
                    if (editable) {
                        Modifier.selectGesture(canOpen, expandedState, openedAt, optionsState)
                    } else {
                        Modifier
                    }
                Box(iconModifier) {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded && canOpen)
                }
            },
        )
        if (!editable) {
            Box(Modifier.matchParentSize().selectGesture(canOpen, expandedState, openedAt, optionsState))
        }
        DropdownMenu(
            expanded = expanded && canOpen,
            onDismissRequest = {
                if (System.nanoTime() - openedAt.longValue < MENU_DISMISS_GRACE_NANOS) return@DropdownMenu
                expanded = false
            },
        ) {
            options.forEachIndexed { index, option ->
                DropdownMenuItem(
                    text = { Text(option, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    onClick = {
                        editedValue = option
                        onSelect(index)
                        onValueChange?.takeIf { editable }?.invoke(option)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                )
            }
        }
    }
}

/**
 * Compose 1.12 draws [DropdownMenu] in its own window. That window is not always-on-top,
 * so a menu opened from Settings stays behind the settings window until it is raised.
 */
@Composable
private fun KeepMenuAboveOwner(active: Boolean) {
    LaunchedEffect(active) {
        if (!active) return@LaunchedEffect
        repeat(POPUP_RAISE_FRAMES) {
            withFrameNanos { }
            raisePopupsAboveAlwaysOnTopOwners()
        }
    }
}

private fun raisePopupsAboveAlwaysOnTopOwners() {
    Window.getWindows().forEach { popup ->
        val owner = popup.owner ?: return@forEach
        if (popup.type != Window.Type.POPUP || !popup.isVisible) return@forEach
        if (!owner.isAlwaysOnTop || popup.isAlwaysOnTop) return@forEach
        popup.isAlwaysOnTop = true
    }
}

private fun Modifier.selectGesture(
    enabled: Boolean,
    expanded: MutableState<Boolean>,
    openedAt: MutableLongState,
    options: State<List<String>>,
): Modifier =
    if (!enabled) {
        this
    } else {
        pointerInput(enabled) {
            detectTapGestures(
                onPress = {
                    val wasOpen = expanded.value
                    if (tryAwaitRelease()) {
                        if (wasOpen) {
                            expanded.value = false
                        } else if (options.value.isNotEmpty()) {
                            openedAt.longValue = System.nanoTime()
                            expanded.value = true
                        }
                    }
                },
            )
        }
    }

private const val MENU_DISMISS_GRACE_NANOS = 200_000_000L
private const val POPUP_RAISE_FRAMES = 4

@Composable
fun AppPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier,
    enabled: Boolean = true,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    fontSize: TextUnit = TextUnit.Unspecified,
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = ButtonDefaults.squareShape,
        contentPadding = contentPadding,
    ) {
        ButtonText(text, fontSize)
    }
}

@Composable
fun AppSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier,
    enabled: Boolean = true,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    fontSize: TextUnit = TextUnit.Unspecified,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = ButtonDefaults.squareShape,
        contentPadding = contentPadding,
    ) {
        ButtonText(text, fontSize)
    }
}

@Composable
fun AppDangerButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier,
    enabled: Boolean = true,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    fontSize: TextUnit = TextUnit.Unspecified,
) {
    TextButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = ButtonDefaults.squareShape,
        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
        contentPadding = contentPadding,
    ) {
        ButtonText(text, fontSize)
    }
}

@Composable
private fun ButtonText(
    text: String,
    fontSize: TextUnit,
) {
    Text(text, fontSize = fontSize, maxLines = 2, overflow = TextOverflow.Ellipsis)
}
