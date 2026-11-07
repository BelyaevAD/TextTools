package dev.belyaevad.texttools.app.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.rememberWindowState
import dev.belyaevad.texttools.domain.Action
import dev.belyaevad.texttools.domain.AppProfile
import dev.belyaevad.texttools.domain.Factory
import dev.belyaevad.texttools.domain.OutputLanguage
import dev.belyaevad.texttools.domain.QuickRefine

/**
 * Action editor with menu buttons, quick refinements and application profiles.
 * The lists are copied on entry. [onSave] receives those copies, including an empty quick list.
 * [onCancel] and Escape discard them. Reset restores the factory copies and still waits for Save.
 * Profile order is kept as given. This window does not open the database.
 */
@Suppress("LongParameterList")
@Composable
fun editorWindow(
    actions: List<Action>,
    quick: List<QuickRefine>,
    apps: List<AppProfile>,
    languages: List<OutputLanguage>,
    models: List<String>,
    defaultModel: String,
    onSave: (List<Action>, List<QuickRefine>, List<AppProfile>) -> Unit,
    onCancel: () -> Unit,
) {
    var draftActions by remember { mutableStateOf(actions.toList()) }
    var draftQuick by remember { mutableStateOf(quick.toList()) }
    var draftApps by remember { mutableStateOf(apps.toList()) }
    var tab by remember { mutableStateOf(0) }
    var actionIndex by remember { mutableStateOf(-1) }
    var quickIndex by remember { mutableStateOf(-1) }
    var appIndex by remember { mutableStateOf(-1) }
    var card by remember { mutableStateOf<OpenCard?>(null) }
    var deleteActionAt by remember { mutableStateOf<Int?>(null) }
    var deleteAppAt by remember { mutableStateOf<Int?>(null) }
    var confirmReset by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf<String?>(null) }
    val copySuffix = tr(Ed.COPY_SUFFIX)
    val lastActionText = tr(Ed.LAST_ACTION)
    val dialogOpen = deleteActionAt != null || deleteAppAt != null || confirmReset || notice != null
    val blocked = card != null || dialogOpen

    Window(
        onCloseRequest = onCancel,
        title = tr(Ed.WINDOW_TITLE),
        state = rememberWindowState(width = 860.dp, height = 620.dp),
        onPreviewKeyEvent = { event ->
            if (!blocked && event.type == KeyEventType.KeyDown && event.key == Key.Escape) {
                onCancel()
                true
            } else {
                false
            }
        },
    ) {
        MaterialTheme {
            Surface(Modifier.fillMaxSize()) {
                Column(Modifier.fillMaxSize().padding(10.dp)) {
                    editorTabs(
                        selected = tab,
                        labels = listOf(tr(Ed.TAB_ACTIONS), tr(Ed.TAB_QUICK), tr(Ed.TAB_APPS)),
                        onSelect = { tab = it },
                    )
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(top = 6.dp)
                            .border(1.dp, Color(0xFFB0B0B0))
                            .padding(8.dp),
                    ) {
                        when (tab) {
                            0 ->
                                actionsPage(
                                    rows = draftActions,
                                    languages = languages,
                                    selected = actionIndex,
                                    onSelect = { actionIndex = it },
                                    onOpen = { row ->
                                        actionIndex = row
                                        card = OpenCard.ActionRow(row, draftActions[row])
                                    },
                                    onAdd = {
                                        card =
                                            OpenCard.ActionRow(
                                                -1,
                                                Action(
                                                    factoryId = null,
                                                    name = "",
                                                    icon = "",
                                                    prefix = "",
                                                    task = "",
                                                    lang = "auto",
                                                    styleOn = true,
                                                    openInWindow = false,
                                                    menuKey = freeMenuKey(draftActions),
                                                    model = null,
                                                    role = null,
                                                ),
                                            )
                                    },
                                    onDuplicate = {
                                        val next = duplicated(draftActions, actionIndex, copySuffix) ?: return@actionsPage
                                        draftActions = next
                                        actionIndex += 1
                                    },
                                    onDelete = {
                                        if (!canDeleteAction(draftActions.size)) {
                                            notice = lastActionText
                                        } else if (actionIndex in draftActions.indices) {
                                            deleteActionAt = actionIndex
                                        }
                                    },
                                    onUp = {
                                        val moved = moveItem(draftActions, actionIndex, -1) ?: return@actionsPage
                                        draftActions = moved.first
                                        actionIndex = moved.second
                                    },
                                    onDown = {
                                        val moved = moveItem(draftActions, actionIndex, 1) ?: return@actionsPage
                                        draftActions = moved.first
                                        actionIndex = moved.second
                                    },
                                )
                            1 ->
                                quickPage(
                                    rows = draftQuick,
                                    selected = quickIndex,
                                    onSelect = { quickIndex = it },
                                    onOpen = { row ->
                                        quickIndex = row
                                        card = OpenCard.QuickRow(row, draftQuick[row])
                                    },
                                    onAdd = { card = OpenCard.QuickRow(-1, QuickRefine("", "")) },
                                    onDelete = {
                                        if (quickIndex !in draftQuick.indices) return@quickPage
                                        val index = quickIndex
                                        val next = draftQuick.toMutableList()
                                        next.removeAt(index)
                                        draftQuick = next
                                        quickIndex = index.coerceAtMost(next.lastIndex)
                                    },
                                    onUp = {
                                        val moved = moveItem(draftQuick, quickIndex, -1) ?: return@quickPage
                                        draftQuick = moved.first
                                        quickIndex = moved.second
                                    },
                                    onDown = {
                                        val moved = moveItem(draftQuick, quickIndex, 1) ?: return@quickPage
                                        draftQuick = moved.first
                                        quickIndex = moved.second
                                    },
                                )
                            else ->
                                appsPage(
                                    rows = draftApps,
                                    selected = appIndex,
                                    onSelect = { appIndex = it },
                                    onOpen = { row ->
                                        appIndex = row
                                        card = OpenCard.AppRow(row, draftApps[row])
                                    },
                                    onAdd = { card = OpenCard.AppRow(-1, AppProfile("", "", "", "", "")) },
                                    onDelete = {
                                        if (appIndex in draftApps.indices) deleteAppAt = appIndex
                                    },
                                )
                        }
                    }
                    Text(
                        tr(Ed.HINT, Factory.menuKeys.size),
                        color = Color(0xFF707070),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                    Row(
                        Modifier.padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Button(
                            onClick = { onSave(draftActions.toList(), draftQuick.toList(), draftApps.toList()) },
                            modifier = Modifier.width(150.dp),
                        ) {
                            Text(tr(Ed.SAVE))
                        }
                        OutlinedButton(onClick = onCancel, modifier = Modifier.width(120.dp)) {
                            Text(tr(Ed.CANCEL))
                        }
                        OutlinedButton(
                            onClick = { confirmReset = true },
                            modifier = Modifier.width(200.dp),
                        ) {
                            Text(tr(Ed.RESET))
                        }
                    }
                }
            }
        }
        when (val open = card) {
            is OpenCard.ActionRow ->
                actionCard(
                    action = open.action,
                    actions = draftActions,
                    index = open.index,
                    languages = languages,
                    models = models,
                    defaultModel = defaultModel,
                    onSave = { updated ->
                        draftActions = updated
                        if (open.index < 0) actionIndex = updated.lastIndex
                        card = null
                    },
                    onCancel = { card = null },
                )
            is OpenCard.QuickRow ->
                quickCard(
                    item = open.item,
                    onSave = { item ->
                        val next = draftQuick.toMutableList()
                        if (open.index in next.indices) next[open.index] = item else next.add(item)
                        draftQuick = next
                        quickIndex = if (open.index in next.indices) open.index else next.lastIndex
                        card = null
                    },
                    onCancel = { card = null },
                )
            is OpenCard.AppRow ->
                appCard(
                    profile = open.profile,
                    onSave = { profile ->
                        val next = draftApps.toMutableList()
                        if (open.index in next.indices) next[open.index] = profile else next.add(profile)
                        draftApps = next
                        appIndex = if (open.index in next.indices) open.index else next.lastIndex
                        card = null
                    },
                    onCancel = { card = null },
                )
            null -> Unit
        }
        deleteActionAt?.let { index ->
            confirmWindow(
                text = tr(Ed.DEL_ACTION, draftActions.getOrNull(index)?.name.orEmpty()),
                yes = tr(Ed.YES),
                no = tr(Ed.NO),
                onYes = {
                    deleteActionAt = null
                    val next = draftActions.toMutableList()
                    if (index in next.indices) next.removeAt(index)
                    draftActions = next
                    actionIndex = index.coerceAtMost(next.lastIndex)
                },
                onNo = { deleteActionAt = null },
            )
        }
        deleteAppAt?.let { index ->
            confirmWindow(
                text = tr(Ed.DEL_APP, draftApps.getOrNull(index)?.name.orEmpty()),
                yes = tr(Ed.YES),
                no = tr(Ed.NO),
                onYes = {
                    deleteAppAt = null
                    val next = draftApps.toMutableList()
                    if (index in next.indices) next.removeAt(index)
                    draftApps = next
                    appIndex = index.coerceAtMost(next.lastIndex)
                },
                onNo = { deleteAppAt = null },
            )
        }
        if (confirmReset) {
            confirmWindow(
                text = tr(Ed.RESET_ASK),
                yes = tr(Ed.YES),
                no = tr(Ed.NO),
                onYes = {
                    confirmReset = false
                    draftActions = Factory.actions.toList()
                    draftQuick = Factory.quick.toList()
                    draftApps = Factory.apps.toList()
                    actionIndex = if (draftActions.isEmpty()) -1 else 0
                    quickIndex = if (draftQuick.isEmpty()) -1 else 0
                    appIndex = if (draftApps.isEmpty()) -1 else 0
                },
                onNo = { confirmReset = false },
            )
        }
        notice?.let { text -> noticeWindow(text, tr(Ed.OK)) { notice = null } }
    }
}

private sealed interface OpenCard {
    data class ActionRow(
        val index: Int,
        val action: Action,
    ) : OpenCard

    data class QuickRow(
        val index: Int,
        val item: QuickRefine,
    ) : OpenCard

    data class AppRow(
        val index: Int,
        val profile: AppProfile,
    ) : OpenCard
}

@Composable
private fun editorTabs(
    selected: Int,
    labels: List<String>,
    onSelect: (Int) -> Unit,
) {
    Row {
        labels.forEachIndexed { index, label ->
            val active = index == selected
            Box(
                modifier =
                    Modifier
                        .background(if (active) Color.White else Color(0xFFE6E6E6))
                        .border(1.dp, Color(0xFFB0B0B0))
                        .clickable { onSelect(index) }
                        .padding(horizontal = 14.dp, vertical = 6.dp),
            ) {
                Text(label)
            }
        }
    }
}

@Composable
private fun wideButton(
    label: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Button(onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
        Text(label)
    }
}

@Composable
private fun orderButtons(
    label: String,
    upEnabled: Boolean,
    downEnabled: Boolean,
    onUp: () -> Unit,
    onDown: () -> Unit,
) {
    Text(label, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Button(onClick = onUp, enabled = upEnabled, modifier = Modifier.weight(1f)) { Text(tr(Ed.UP)) }
        Button(onClick = onDown, enabled = downEnabled, modifier = Modifier.weight(1f)) { Text(tr(Ed.DOWN)) }
    }
}

@Suppress("LongParameterList")
@Composable
private fun actionsPage(
    rows: List<Action>,
    languages: List<OutputLanguage>,
    selected: Int,
    onSelect: (Int) -> Unit,
    onOpen: (Int) -> Unit,
    onAdd: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    onUp: () -> Unit,
    onDown: () -> Unit,
) {
    val autoShort = tr(Ed.LANG_AUTO_SHORT)
    val inWindow = tr(Ed.RESULT_WINDOW)
    val replace = tr(Ed.RESULT_REPLACE)
    val defaultModel = tr(Ed.MODEL_DEFAULT)
    val chosen = selected in rows.indices
    Row(Modifier.fillMaxSize()) {
        editorTable(
            headers =
                listOf(
                    tr(Ed.COL_KEY),
                    tr(Ed.COL_NAME),
                    tr(Ed.COL_RESULT),
                    tr(Ed.COL_LANG),
                    tr(Ed.COL_MODEL),
                ),
            weights = listOf(0.7f, 1.8f, 1.5f, 1.1f, 1.3f),
            count = rows.size,
            selected = selected,
            onSelect = onSelect,
            onOpen = onOpen,
            modifier = Modifier.weight(1f).fillMaxSize(),
            text = { row, column ->
                val action = rows[row]
                when (column) {
                    0 -> keyListLabel(action.menuKey)
                    1 -> actionListName(action)
                    2 -> if (action.openInWindow) inWindow else replace
                    3 -> languageListLabel(action.lang, languages, autoShort)
                    else -> modelListLabel(action.model, defaultModel)
                }
            },
        )
        Column(
            Modifier.width(168.dp).padding(start = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            wideButton(tr(Ed.ADD), onClick = onAdd)
            wideButton(tr(Ed.EDIT), enabled = chosen, onClick = { if (chosen) onOpen(selected) })
            wideButton(tr(Ed.DUP), enabled = chosen, onClick = onDuplicate)
            wideButton(tr(Ed.DELETE), enabled = chosen, onClick = onDelete)
            orderButtons(
                tr(Ed.GRID_ORDER),
                upEnabled = selected > 0,
                downEnabled = chosen && selected < rows.lastIndex,
                onUp = onUp,
                onDown = onDown,
            )
        }
    }
}

@Suppress("LongParameterList")
@Composable
private fun quickPage(
    rows: List<QuickRefine>,
    selected: Int,
    onSelect: (Int) -> Unit,
    onOpen: (Int) -> Unit,
    onAdd: () -> Unit,
    onDelete: () -> Unit,
    onUp: () -> Unit,
    onDown: () -> Unit,
) {
    val chosen = selected in rows.indices
    Row(Modifier.fillMaxSize()) {
        editorTable(
            headers = listOf(tr(Ed.COL_NAME), tr(Ed.COL_INSTR)),
            weights = listOf(1.2f, 3f),
            count = rows.size,
            selected = selected,
            onSelect = onSelect,
            onOpen = onOpen,
            modifier = Modifier.weight(1f).fillMaxSize(),
            text = { row, column -> if (column == 0) rows[row].label else rows[row].instruction },
        )
        Column(
            Modifier.width(168.dp).padding(start = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            wideButton(tr(Ed.ADD), onClick = onAdd)
            wideButton(tr(Ed.EDIT), enabled = chosen, onClick = { if (chosen) onOpen(selected) })
            wideButton(tr(Ed.DELETE), enabled = chosen, onClick = onDelete)
            orderButtons(
                tr(Ed.ROW_ORDER),
                upEnabled = selected > 0,
                downEnabled = chosen && selected < rows.lastIndex,
                onUp = onUp,
                onDown = onDown,
            )
        }
    }
}

@Suppress("LongParameterList")
@Composable
private fun appsPage(
    rows: List<AppProfile>,
    selected: Int,
    onSelect: (Int) -> Unit,
    onOpen: (Int) -> Unit,
    onAdd: () -> Unit,
    onDelete: () -> Unit,
) {
    val emptyHint = tr(Ed.NO_HINT)
    val chosen = selected in rows.indices
    Row(Modifier.fillMaxSize()) {
        editorTable(
            headers = listOf(tr(Ed.COL_PROC), tr(Ed.COL_TITLE), tr(Ed.COL_NAME), tr(Ed.COL_HINT)),
            weights = listOf(1.2f, 1.3f, 1.3f, 1.6f),
            count = rows.size,
            selected = selected,
            onSelect = onSelect,
            onOpen = onOpen,
            modifier = Modifier.weight(1f).fillMaxSize(),
            text = { row, column ->
                val app = rows[row]
                when (column) {
                    0 -> app.proc
                    1 -> titleCell(app.title)
                    2 -> app.name
                    else -> hintCell(app.hint, emptyHint)
                }
            },
        )
        Column(
            Modifier.width(168.dp).padding(start = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            wideButton(tr(Ed.ADD), onClick = onAdd)
            wideButton(tr(Ed.EDIT), enabled = chosen, onClick = { if (chosen) onOpen(selected) })
            wideButton(tr(Ed.DELETE), enabled = chosen, onClick = onDelete)
            Text(tr(Ed.APP_ORDER), fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
            Text(tr(Ed.APP_ORDER_HINT), color = Color(0xFF707070), fontSize = 12.sp)
        }
    }
}
