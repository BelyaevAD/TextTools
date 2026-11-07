package dev.belyaevad.texttools.app.ui.editor

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Button
import androidx.compose.material.Checkbox
import androidx.compose.material.OutlinedButton
import androidx.compose.material.RadioButton
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
import dev.belyaevad.texttools.domain.Action
import dev.belyaevad.texttools.domain.Factory
import dev.belyaevad.texttools.domain.KeyConflict
import dev.belyaevad.texttools.domain.OutputLanguage

/**
 * Edits one action. An empty name or prompt does not save.
 * A key owned by another row opens [keyConflictDialog], then [yieldMenuKey].
 * An [index] below zero appends the action.
 */
@Suppress("LongParameterList")
@Composable
fun actionCard(
    action: Action,
    actions: List<Action>,
    index: Int,
    languages: List<OutputLanguage>,
    models: List<String>,
    defaultModel: String,
    onSave: (List<Action>) -> Unit,
    onCancel: () -> Unit,
) {
    val langChoices = languageOptions(action, languages, tr(Ed.LANG_AUTO), tr(Ed.LANG_OFF))
    val extras = modelChoices(action, models, defaultModel)
    val keyLabels = listOf(tr(Ed.KEY_NONE)) + Factory.menuKeys.map { it.uppercase() }
    val defaultChoice =
        if (defaultModel.isBlank()) {
            tr(Ed.MODEL_DEFAULT)
        } else {
            tr(Ed.MODEL_DEFAULT) + " (" + defaultModel + ")"
        }
    var nameText by remember(action, index) { mutableStateOf(action.name) }
    var iconText by remember(action, index) { mutableStateOf(action.icon) }
    var prefixText by remember(action, index) { mutableStateOf(prefixForEdit(action.prefix)) }
    var promptText by remember(action, index) { mutableStateOf(action.task) }
    var styleOn by remember(action, index) { mutableStateOf(action.styleOn) }
    var openInWindow by remember(action, index) { mutableStateOf(action.openInWindow) }
    var langIndex by remember(action, index) { mutableStateOf(selectedLanguageIndex(action, langChoices)) }
    var modelIndex by remember(action, index) { mutableStateOf(selectedModelIndex(action, extras, defaultModel)) }
    var keyIndex by remember(action, index) { mutableStateOf(selectedKeyIndex(action)) }
    var pending by remember(action, index) { mutableStateOf<Int?>(null) }
    var notice by remember(action, index) { mutableStateOf<String?>(null) }
    val title = if (action.name.isBlank()) tr(Ed.ACT_NEW) else tr(Ed.ACT_EDIT, action.name)
    val nameEmpty = tr(Ed.NAME_EMPTY)
    val promptEmpty = tr(Ed.PROMPT_EMPTY)

    fun publish(mode: KeyConflict?) {
        val name = nameText.trim()
        val task = promptText.trim()
        if (name.isEmpty()) {
            notice = nameEmpty
            return
        }
        if (task.isEmpty()) {
            notice = promptEmpty
            return
        }
        val chosen = keyAt(keyIndex)
        val owner = occupantIndex(actions, chosen, index)
        if (owner >= 0 && mode == null) {
            pending = owner
            return
        }
        val saved =
            action.copy(
                name = name,
                icon = iconText.trim(),
                prefix = prefixFromEdit(prefixText),
                task = task,
                lang = langChoices.getOrElse(langIndex) { langChoices.first() }.id,
                styleOn = styleOn,
                openInWindow = openInWindow,
                menuKey = chosen,
                model = if (modelIndex <= 0) null else extras.getOrNull(modelIndex - 1),
            )
        val updated =
            commitAction(
                actions = actions,
                index = index,
                edited = saved,
                occupant = if (mode == null) -1 else owner,
                oldKey = normalizedKey(action.menuKey),
                mode = mode,
            )
        if (updated == null) {
            pending = null
            return
        }
        pending = null
        onSave(updated)
    }

    framedDialog(title, 740.dp, 600.dp, onCancel) {
        Column(
            Modifier.fillMaxSize().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(tr(Ed.FIELD_NAME), modifier = Modifier.width(120.dp))
                lineField(nameText, { nameText = it }, Modifier.width(240.dp))
                Text(tr(Ed.ICON), modifier = Modifier.padding(start = 12.dp))
                lineField(iconText, { iconText = it }, Modifier.padding(start = 8.dp).width(64.dp))
                Text(tr(Ed.FIELD_KEY), modifier = Modifier.padding(start = 12.dp))
                picker(keyLabels, keyIndex, { keyIndex = it }, Modifier.padding(start = 8.dp).width(96.dp))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(tr(Ed.FIELD_RESULT), modifier = Modifier.width(120.dp))
                RadioButton(selected = !openInWindow, onClick = { openInWindow = false })
                Text(tr(Ed.RESULT_REPLACE), modifier = Modifier.clickable { openInWindow = false })
                Spacer(Modifier.width(12.dp))
                RadioButton(selected = openInWindow, onClick = { openInWindow = true })
                Text(tr(Ed.RESULT_OPEN), modifier = Modifier.clickable { openInWindow = true })
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(tr(Ed.FIELD_LANG), modifier = Modifier.width(120.dp))
                picker(
                    langChoices.map { it.label },
                    langIndex,
                    { langIndex = it },
                    Modifier.width(220.dp),
                )
                Checkbox(checked = styleOn, onCheckedChange = { styleOn = it })
                Text(tr(Ed.LANG_STYLE))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(tr(Ed.FIELD_MODEL), modifier = Modifier.width(120.dp))
                picker(
                    listOf(defaultChoice) + extras,
                    modelIndex,
                    { modelIndex = it },
                    Modifier.width(280.dp),
                )
                Text(
                    if (extras.isEmpty()) tr(Ed.MODEL_HINT_EMPTY) else tr(Ed.MODEL_HINT),
                    color = Color(0xFF707070),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(start = 8.dp).weight(1f),
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(tr(Ed.FIELD_PREFIX), modifier = Modifier.width(120.dp))
                lineField(prefixText, { prefixText = it }, Modifier.weight(1f))
            }
            Text(
                tr(Ed.PREFIX_HINT),
                color = Color(0xFF707070),
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 120.dp),
            )
            Text(tr(Ed.FIELD_PROMPT))
            lineField(
                promptText,
                { promptText = it },
                Modifier.fillMaxWidth().weight(1f),
                singleLine = false,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { publish(null) }, modifier = Modifier.width(150.dp)) { Text(tr(Ed.SAVE)) }
                OutlinedButton(onClick = onCancel, modifier = Modifier.width(120.dp)) { Text(tr(Ed.CANCEL)) }
            }
        }
        val occupant = pending
        val chosen = keyAt(keyIndex)
        if (occupant != null && chosen != null) {
            keyConflictDialog(
                key = chosen,
                otherName = actions.getOrNull(occupant)?.name.orEmpty(),
                oldKey = normalizedKey(action.menuKey),
                onChoice = { choice ->
                    if (choice == KeyConflict.Cancel) pending = null else publish(choice)
                },
            )
        }
        notice?.let { text -> noticeWindow(text, tr(Ed.OK)) { notice = null } }
    }
}
