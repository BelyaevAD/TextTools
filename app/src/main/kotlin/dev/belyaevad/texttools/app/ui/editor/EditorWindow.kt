package dev.belyaevad.texttools.app.ui.editor

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.rememberWindowState
import dev.belyaevad.texttools.app.ui.AppDangerButton
import dev.belyaevad.texttools.app.ui.AppIcon
import dev.belyaevad.texttools.app.ui.AppPrimaryButton
import dev.belyaevad.texttools.app.ui.AppSecondaryButton
import dev.belyaevad.texttools.app.ui.AppTabs
import dev.belyaevad.texttools.app.ui.AppTheme
import dev.belyaevad.texttools.app.ui.EditorHeight
import dev.belyaevad.texttools.app.ui.EditorWidth
import dev.belyaevad.texttools.app.ui.fitVisibleFrame
import dev.belyaevad.texttools.app.ui.keepWindowFrame
import dev.belyaevad.texttools.app.ui.rememberPlacement
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
    language: String,
) {
    val lists =
        EditorLists(
            actions = remember { mutableStateOf(actions.toList()) },
            quick = remember { mutableStateOf(quick.toList()) },
            apps = remember { mutableStateOf(apps.toList()) },
            actionIndex = remember { mutableStateOf(-1) },
            quickIndex = remember { mutableStateOf(-1) },
            appIndex = remember { mutableStateOf(-1) },
        )
    val dialogs =
        EditorDialogs(
            card = remember { mutableStateOf<OpenCard?>(null) },
            deleteActionAt = remember { mutableStateOf<Int?>(null) },
            deleteAppAt = remember { mutableStateOf<Int?>(null) },
            confirmReset = remember { mutableStateOf(false) },
            notice = remember { mutableStateOf<String?>(null) },
        )
    val tab = remember { mutableStateOf(0) }
    val copySuffix = tr(Ed.COPY_SUFFIX)
    val lastActionText = tr(Ed.LAST_ACTION)
    val place = rememberPlacement("editor", EditorWidth, EditorHeight)
    val windowState = rememberWindowState(width = place.width, height = place.height, position = place.position)

    Window(
        onCloseRequest = onCancel,
        title = tr(Ed.WINDOW_TITLE),
        icon = AppIcon,
        state = windowState,
        onPreviewKeyEvent = { event ->
            if (!editorBlocked(dialogs) && event.type == KeyEventType.KeyDown && event.key == Key.Escape) {
                onCancel()
                true
            } else {
                false
            }
        },
    ) {
        if (!place.restored) fitVisibleFrame(windowState.size) { windowState.size = it }
        keepWindowFrame("editor")
        key(language) {
            AppTheme {
                Surface(Modifier.fillMaxSize()) {
                    Column(Modifier.fillMaxSize().padding(10.dp)) {
                        editorTabs(
                            selected = tab.value,
                            labels = listOf(tr(Ed.TAB_ACTIONS), tr(Ed.TAB_QUICK), tr(Ed.TAB_APPS)),
                            onSelect = { tab.value = it },
                        )
                        Box(
                            Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(top = 6.dp)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                .padding(8.dp),
                        ) {
                            editorPage(lists, dialogs, tab.value, languages, copySuffix, lastActionText)
                        }
                        Text(
                            if (tab.value == 1) tr(Ed.HINT_QUICK) else tr(Ed.HINT, Factory.menuKeys.size),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                        Row(
                            Modifier.padding(top = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            AppPrimaryButton(
                                text = tr(Ed.SAVE),
                                onClick = {
                                    onSave(lists.actions.value.toList(), lists.quick.value.toList(), lists.apps.value.toList())
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
                            AppSecondaryButton(
                                text = tr(Ed.RESET),
                                onClick = { dialogs.confirmReset.value = true },
                                modifier = Modifier.width(200.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                            )
                        }
                    }
                }
            }
            editorOverlays(lists, dialogs, languages, models, defaultModel)
        }
    }
}

private data class EditorLists(
    val actions: MutableState<List<Action>>,
    val quick: MutableState<List<QuickRefine>>,
    val apps: MutableState<List<AppProfile>>,
    val actionIndex: MutableState<Int>,
    val quickIndex: MutableState<Int>,
    val appIndex: MutableState<Int>,
) {
    fun duplicate(copySuffix: String) {
        val next = duplicated(actions.value, actionIndex.value, copySuffix) ?: return
        actions.value = next
        actionIndex.value += 1
    }

    fun moveAction(delta: Int) {
        val moved = moveItem(actions.value, actionIndex.value, delta) ?: return
        actions.value = moved.first
        actionIndex.value = moved.second
    }

    fun moveQuick(delta: Int) {
        val moved = moveItem(quick.value, quickIndex.value, delta) ?: return
        quick.value = moved.first
        quickIndex.value = moved.second
    }

    fun requestDeleteAction(
        dialogs: EditorDialogs,
        lastActionText: String,
    ) {
        if (!canDeleteAction(actions.value.size)) {
            dialogs.notice.value = lastActionText
        } else if (actionIndex.value in actions.value.indices) {
            dialogs.deleteActionAt.value = actionIndex.value
        }
    }

    fun deleteQuick() {
        if (quickIndex.value !in quick.value.indices) return
        val index = quickIndex.value
        val next = quick.value.toMutableList()
        next.removeAt(index)
        quick.value = next
        quickIndex.value = index.coerceAtMost(next.lastIndex)
    }

    fun requestDeleteApp(dialogs: EditorDialogs) {
        if (appIndex.value in apps.value.indices) dialogs.deleteAppAt.value = appIndex.value
    }

    fun saveAction(
        open: OpenCard.ActionRow,
        updated: List<Action>,
    ) {
        actions.value = updated
        if (open.index < 0) actionIndex.value = updated.lastIndex
    }

    fun saveQuick(
        open: OpenCard.QuickRow,
        item: QuickRefine,
    ) {
        val next = quick.value.toMutableList()
        if (open.index in next.indices) next[open.index] = item else next.add(item)
        quick.value = next
        quickIndex.value = if (open.index in next.indices) open.index else next.lastIndex
    }

    fun saveApp(
        open: OpenCard.AppRow,
        profile: AppProfile,
    ) {
        val next = apps.value.toMutableList()
        if (open.index in next.indices) next[open.index] = profile else next.add(profile)
        apps.value = next
        appIndex.value = if (open.index in next.indices) open.index else next.lastIndex
    }

    fun removeAction(index: Int) {
        val next = actions.value.toMutableList()
        if (index in next.indices) next.removeAt(index)
        actions.value = next
        actionIndex.value = index.coerceAtMost(next.lastIndex)
    }

    fun removeApp(index: Int) {
        val next = apps.value.toMutableList()
        if (index in next.indices) next.removeAt(index)
        apps.value = next
        appIndex.value = index.coerceAtMost(next.lastIndex)
    }

    fun resetToFactory() {
        actions.value = Factory.actions.toList()
        quick.value = Factory.quick.toList()
        apps.value = Factory.apps.toList()
        actionIndex.value = if (actions.value.isEmpty()) -1 else 0
        quickIndex.value = if (quick.value.isEmpty()) -1 else 0
        appIndex.value = if (apps.value.isEmpty()) -1 else 0
    }
}

private data class EditorDialogs(
    val card: MutableState<OpenCard?>,
    val deleteActionAt: MutableState<Int?>,
    val deleteAppAt: MutableState<Int?>,
    val confirmReset: MutableState<Boolean>,
    val notice: MutableState<String?>,
)

private fun editorBlocked(dialogs: EditorDialogs): Boolean =
    dialogs.card.value != null ||
        dialogs.deleteActionAt.value != null ||
        dialogs.deleteAppAt.value != null ||
        dialogs.confirmReset.value ||
        dialogs.notice.value != null

@Composable
private fun editorPage(
    lists: EditorLists,
    dialogs: EditorDialogs,
    tab: Int,
    languages: List<OutputLanguage>,
    copySuffix: String,
    lastActionText: String,
) {
    when (tab) {
        0 ->
            actionsPage(
                rows = lists.actions.value,
                languages = languages,
                selected = lists.actionIndex.value,
                onSelect = { lists.actionIndex.value = it },
                onOpen = { row ->
                    lists.actionIndex.value = row
                    dialogs.card.value = OpenCard.ActionRow(row, lists.actions.value[row])
                },
                onAdd = {
                    dialogs.card.value =
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
                                menuKey = freeMenuKey(lists.actions.value),
                                model = null,
                                role = null,
                            ),
                        )
                },
                onDuplicate = { lists.duplicate(copySuffix) },
                onDelete = { lists.requestDeleteAction(dialogs, lastActionText) },
                onUp = { lists.moveAction(-1) },
                onDown = { lists.moveAction(1) },
            )
        1 ->
            quickPage(
                rows = lists.quick.value,
                selected = lists.quickIndex.value,
                onSelect = { lists.quickIndex.value = it },
                onOpen = { row ->
                    lists.quickIndex.value = row
                    dialogs.card.value = OpenCard.QuickRow(row, lists.quick.value[row])
                },
                onAdd = { dialogs.card.value = OpenCard.QuickRow(-1, QuickRefine("", "")) },
                onDelete = { lists.deleteQuick() },
                onUp = { lists.moveQuick(-1) },
                onDown = { lists.moveQuick(1) },
            )
        else ->
            appsPage(
                rows = lists.apps.value,
                selected = lists.appIndex.value,
                onSelect = { lists.appIndex.value = it },
                onOpen = { row ->
                    lists.appIndex.value = row
                    dialogs.card.value = OpenCard.AppRow(row, lists.apps.value[row])
                },
                onAdd = { dialogs.card.value = OpenCard.AppRow(-1, AppProfile("", "", "", "", "")) },
                onDelete = { lists.requestDeleteApp(dialogs) },
            )
    }
}

@Composable
private fun editorOverlays(
    lists: EditorLists,
    dialogs: EditorDialogs,
    languages: List<OutputLanguage>,
    models: List<String>,
    defaultModel: String,
) {
    when (val open = dialogs.card.value) {
        is OpenCard.ActionRow ->
            actionCard(
                action = open.action,
                actions = lists.actions.value,
                index = open.index,
                languages = languages,
                models = models,
                defaultModel = defaultModel,
                onSave = { updated ->
                    lists.saveAction(open, updated)
                    dialogs.card.value = null
                },
                onCancel = { dialogs.card.value = null },
            )
        is OpenCard.QuickRow ->
            quickCard(
                item = open.item,
                onSave = { item ->
                    lists.saveQuick(open, item)
                    dialogs.card.value = null
                },
                onCancel = { dialogs.card.value = null },
            )
        is OpenCard.AppRow ->
            appCard(
                profile = open.profile,
                onSave = { profile ->
                    lists.saveApp(open, profile)
                    dialogs.card.value = null
                },
                onCancel = { dialogs.card.value = null },
            )
        null -> Unit
    }
    dialogs.deleteActionAt.value?.let { index ->
        confirmWindow(
            text =
                tr(
                    Ed.DEL_ACTION,
                    lists.actions.value
                        .getOrNull(index)
                        ?.name
                        .orEmpty(),
                ),
            yes = tr(Ed.YES),
            no = tr(Ed.NO),
            onYes = {
                dialogs.deleteActionAt.value = null
                lists.removeAction(index)
            },
            onNo = { dialogs.deleteActionAt.value = null },
        )
    }
    dialogs.deleteAppAt.value?.let { index ->
        confirmWindow(
            text =
                tr(
                    Ed.DEL_APP,
                    lists.apps.value
                        .getOrNull(index)
                        ?.name
                        .orEmpty(),
                ),
            yes = tr(Ed.YES),
            no = tr(Ed.NO),
            onYes = {
                dialogs.deleteAppAt.value = null
                lists.removeApp(index)
            },
            onNo = { dialogs.deleteAppAt.value = null },
        )
    }
    if (dialogs.confirmReset.value) {
        confirmWindow(
            text = tr(Ed.RESET_ASK),
            yes = tr(Ed.YES),
            no = tr(Ed.NO),
            onYes = {
                dialogs.confirmReset.value = false
                lists.resetToFactory()
            },
            onNo = { dialogs.confirmReset.value = false },
        )
    }
    dialogs.notice.value?.let { text -> noticeWindow(text, tr(Ed.OK)) { dialogs.notice.value = null } }
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
    AppTabs(
        labels = labels,
        selected = selected,
        onSelect = onSelect,
    )
}

@Composable
private fun wideButton(
    label: String,
    secondary: Boolean = false,
    danger: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val modifier = Modifier.fillMaxWidth()
    val contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
    when {
        danger ->
            AppDangerButton(label, onClick, modifier, enabled, contentPadding, 13.sp)
        secondary ->
            AppSecondaryButton(label, onClick, modifier, enabled, contentPadding, 13.sp)
        else ->
            AppPrimaryButton(label, onClick, modifier, enabled, contentPadding, 13.sp)
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
        AppSecondaryButton(
            text = tr(Ed.UP),
            onClick = onUp,
            enabled = upEnabled,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
            fontSize = 13.sp,
        )
        AppSecondaryButton(
            text = tr(Ed.DOWN),
            onClick = onDown,
            enabled = downEnabled,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
            fontSize = 13.sp,
        )
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
            Modifier.padding(start = 8.dp).width(160.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            wideButton(tr(Ed.ADD), onClick = onAdd)
            wideButton(tr(Ed.EDIT), secondary = true, enabled = chosen, onClick = { if (chosen) onOpen(selected) })
            wideButton(tr(Ed.DUP), secondary = true, enabled = chosen, onClick = onDuplicate)
            wideButton(tr(Ed.DELETE), danger = true, enabled = chosen, onClick = onDelete)
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
            Modifier.padding(start = 8.dp).width(160.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            wideButton(tr(Ed.ADD), onClick = onAdd)
            wideButton(tr(Ed.EDIT), secondary = true, enabled = chosen, onClick = { if (chosen) onOpen(selected) })
            wideButton(tr(Ed.DELETE), danger = true, enabled = chosen, onClick = onDelete)
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
            Modifier.padding(start = 8.dp).width(160.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            wideButton(tr(Ed.ADD), onClick = onAdd)
            wideButton(tr(Ed.EDIT), secondary = true, enabled = chosen, onClick = { if (chosen) onOpen(selected) })
            wideButton(tr(Ed.DELETE), danger = true, enabled = chosen, onClick = onDelete)
            Text(tr(Ed.APP_ORDER), fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
            Text(tr(Ed.APP_ORDER_HINT), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        }
    }
}
