package dev.belyaevad.texttools.app.ui.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.belyaevad.texttools.app.ui.AppPrimaryButton
import dev.belyaevad.texttools.app.ui.AppSecondaryButton
import dev.belyaevad.texttools.app.ui.KeyConflictHeight
import dev.belyaevad.texttools.app.ui.KeyConflictWidth
import dev.belyaevad.texttools.domain.KeyConflict

/**
 * Asks how to take a menu key.
 * [onChoice] receives [KeyConflict.Cancel], [KeyConflict.Swap] or [KeyConflict.Take].
 * Swap is disabled when [oldKey] is blank.
 */
@Composable
fun keyConflictDialog(
    key: String,
    otherName: String,
    oldKey: String?,
    onChoice: (KeyConflict) -> Unit,
) {
    val shown = key.trim().uppercase()
    val previous = oldKey?.trim()?.takeIf { it.isNotEmpty() }
    framedDialog(
        APP_TITLE,
        KeyConflictWidth,
        KeyConflictHeight,
        { onChoice(KeyConflict.Cancel) },
        resizable = false,
        placementId = "key-conflict",
    ) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Text("\u26A0", fontSize = 28.sp, modifier = Modifier.padding(end = 12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(tr(Ed.KEY_TAKEN, shown, otherName))
                    Text(tr(Ed.KEY_TAKEN_BODY, shown))
                    if (previous != null) {
                        Text(
                            tr(Ed.KEY_TAKEN_SWAP, otherName, previous.uppercase()),
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                    Text(
                        tr(Ed.KEY_TAKEN_TAKE, otherName),
                        modifier = Modifier.padding(top = if (previous == null) 8.dp else 0.dp),
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AppSecondaryButton(
                    text = tr(Ed.CANCEL),
                    onClick = { onChoice(KeyConflict.Cancel) },
                    modifier = Modifier.width(100.dp),
                )
                AppSecondaryButton(
                    text = tr(Ed.KEY_SWAP),
                    onClick = { onChoice(KeyConflict.Swap) },
                    modifier = Modifier.width(110.dp),
                    enabled = previous != null,
                )
                AppPrimaryButton(
                    text = tr(Ed.KEY_TAKE),
                    onClick = { onChoice(KeyConflict.Take) },
                    modifier = Modifier.width(110.dp),
                )
            }
        }
    }
}
