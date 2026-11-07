package dev.belyaevad.texttools.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.Checkbox
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
    Text(text, modifier, color = Color(0xFF707070), fontSize = 12.sp)
}

@Composable
internal fun LineField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    password: Boolean = false,
) {
    val textColor = if (enabled) MaterialTheme.colors.onSurface else Color(0xFF808080)
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier =
            modifier
                .height(28.dp)
                .border(1.dp, if (enabled) Color(0xFF808080) else Color(0xFFC8C8C8))
                .background(if (enabled) Color.White else Color(0xFFF2F2F2))
                .padding(horizontal = 6.dp),
        enabled = enabled,
        singleLine = true,
        textStyle = TextStyle(color = textColor, fontSize = 13.sp),
        cursorBrush = SolidColor(MaterialTheme.colors.primary),
        visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None,
        decorationBox = { inner ->
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) { inner() }
        },
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
