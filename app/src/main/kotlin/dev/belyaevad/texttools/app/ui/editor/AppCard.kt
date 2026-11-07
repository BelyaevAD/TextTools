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
import dev.belyaevad.texttools.domain.AppProfile

/** Edits one application profile. An empty process does not save. A blank name is stored as the process. */
@Composable
fun appCard(
    profile: AppProfile,
    onSave: (AppProfile) -> Unit,
    onCancel: () -> Unit,
) {
    var proc by remember(profile) { mutableStateOf(profile.proc) }
    var titleText by remember(profile) { mutableStateOf(profile.title) }
    var name by remember(profile) { mutableStateOf(profile.name) }
    var description by remember(profile) { mutableStateOf(profile.description) }
    var hint by remember(profile) { mutableStateOf(profile.hint) }
    var notice by remember(profile) { mutableStateOf<String?>(null) }
    val procRequired = tr(Ed.APP_PROC_REQ)
    val heading =
        if (profile.proc.isBlank()) {
            tr(Ed.APP_NEW)
        } else {
            tr(Ed.APP_EDIT, profile.name.ifBlank { profile.proc })
        }
    framedDialog(heading, 780.dp, 520.dp, onCancel) {
        Column(
            Modifier.fillMaxSize().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(tr(Ed.APP_PROC), modifier = Modifier.width(150.dp))
                lineField(proc, { proc = it }, Modifier.width(200.dp))
                Text(tr(Ed.APP_TITLE), modifier = Modifier.padding(start = 16.dp).width(160.dp))
                lineField(titleText, { titleText = it }, Modifier.weight(1f))
            }
            Text(
                tr(Ed.APP_TITLE_HINT),
                color = Color(0xFF707070),
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 150.dp),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(tr(Ed.FIELD_NAME), modifier = Modifier.width(150.dp))
                lineField(name, { name = it }, Modifier.weight(1f))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(tr(Ed.APP_DESC), modifier = Modifier.width(150.dp))
                lineField(description, { description = it }, Modifier.weight(1f))
            }
            Text(tr(Ed.APP_HINT))
            lineField(hint, { hint = it }, Modifier.fillMaxWidth().weight(1f), singleLine = false)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        val saved = savedApp(proc, titleText, name, description, hint)
                        if (saved == null) {
                            notice = procRequired
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
