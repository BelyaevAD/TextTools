package dev.belyaevad.texttools.app.ui.editor

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.belyaevad.texttools.app.ui.AppField
import dev.belyaevad.texttools.app.ui.AppPrimaryButton
import dev.belyaevad.texttools.app.ui.AppSecondaryButton
import dev.belyaevad.texttools.app.ui.QuickCardHeight
import dev.belyaevad.texttools.app.ui.QuickCardWidth
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
    framedDialog(title, QuickCardWidth, QuickCardHeight, onCancel, placementId = "quick-card") {
        Column(
            Modifier.fillMaxSize().padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(tr(Ed.FIELD_NAME), modifier = Modifier.width(110.dp))
                AppField(label, { label = it }, Modifier.width(250.dp))
            }
            Text(tr(Ed.QUICK_NAME_HINT), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            Text(tr(Ed.QUICK_INSTR))
            AppField(
                instruction,
                { instruction = it },
                Modifier.fillMaxWidth().height(136.dp),
                singleLine = false,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AppPrimaryButton(
                    text = tr(Ed.SAVE),
                    onClick = {
                        val saved = savedQuick(label, instruction)
                        if (saved == null) {
                            notice = required
                        } else {
                            onSave(saved)
                        }
                    },
                    modifier = Modifier.width(150.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                )
                AppSecondaryButton(
                    text = tr(Ed.CANCEL),
                    onClick = onCancel,
                    modifier = Modifier.width(120.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                )
            }
        }
        notice?.let { text -> noticeWindow(text, tr(Ed.OK)) { notice = null } }
    }
}
