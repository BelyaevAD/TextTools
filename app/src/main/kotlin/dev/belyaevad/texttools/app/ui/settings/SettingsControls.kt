package dev.belyaevad.texttools.app.ui.settings

import androidx.compose.foundation.ScrollbarStyle
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.belyaevad.texttools.app.ui.AppField

@Composable
internal fun FieldLabel(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(text, modifier, fontSize = 13.sp)
}

@Composable
internal fun Hint(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(text, modifier, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
}

@Composable
internal fun LineField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    password: Boolean = false,
) {
    AppField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        password = password,
    )
}

@Composable
internal fun CheckLine(
    checked: Boolean,
    label: String,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Text(
            label,
            modifier = Modifier.weight(1f).clickable { onCheckedChange(!checked) },
            fontSize = 13.sp,
        )
    }
}

/** A track the eye can follow. The thumb length is the visible share of the list. */
internal val listScrollbar: ScrollbarStyle
    @Composable
    get() =
        ScrollbarStyle(
            minimalHeight = 28.dp,
            thickness = 12.dp,
            shape = RoundedCornerShape(6.dp),
            hoverDurationMillis = 200,
            unhoverColor = MaterialTheme.colorScheme.onSurfaceVariant,
            hoverColor = MaterialTheme.colorScheme.onSurface,
        )
