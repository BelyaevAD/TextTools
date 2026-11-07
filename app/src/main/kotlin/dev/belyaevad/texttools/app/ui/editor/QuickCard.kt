package dev.belyaevad.texttools.app.ui.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Button
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.belyaevad.texttools.domain.QuickRefine

/** Edits one refinement. An empty name or instruction does not save. */
@Composable
fun quickCard(
    item: QuickRefine,
    onSave: (QuickRefine) -> Unit,
    onCancel: () -> Unit,
) {
    var label by remember(item) { mutableStateOf(item.label) }
    var instruction by remember(item) { mutableStateOf(item.instruction) }
    var notice by remember(item) { mutableStateOf<String?>(null) }
    val title = if (item.label.isBlank()) tr(Ed.QUICK_NEW) else tr(Ed.QUICK_EDIT, item.label)
    val required = tr(Ed.QUICK_REQUIRED)
    framedDialog(title, 600.dp, 440.dp, onCancel) {
        Column(
            Modifier.fillMaxSize().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(tr(Ed.FIELD_NAME), modifier = Modifier.width(120.dp))
                lineField(label, { label = it }, Modifier.width(280.dp))
            }
            Text(tr(Ed.QUICK_NAME_HINT), color = Color(0xFF707070), fontSize = 12.sp)
            Text(tr(Ed.QUICK_INSTR))
            lineField(
                instruction,
                { instruction = it },
                Modifier.fillMaxWidth().weight(1f),
                singleLine = false,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        val saved = savedQuick(label, instruction)
                        if (saved == null) {
                            notice = required
                        } else {
                            onSave(saved)
                        }
                    },
                    modifier = Modifier.width(150.dp),
                ) {
                    Text(tr(Ed.SAVE))
                }
                OutlinedButton(onClick = onCancel, modifier = Modifier.width(120.dp)) {
                    Text(tr(Ed.CANCEL))
                }
            }
        }
        notice?.let { text -> noticeWindow(text, tr(Ed.OK)) { notice = null } }
    }
}
