package dev.belyaevad.texttools.app.ui.editor

import dev.belyaevad.texttools.domain.Action
import dev.belyaevad.texttools.domain.AppProfile
import dev.belyaevad.texttools.domain.Factory
import dev.belyaevad.texttools.domain.KeyConflict
import dev.belyaevad.texttools.domain.OutputLanguage
import dev.belyaevad.texttools.domain.QuickRefine
import dev.belyaevad.texttools.domain.duplicateFactory
import dev.belyaevad.texttools.domain.yieldMenuKey

internal const val EMPTY_MARK = "\u2014"

internal const val APP_TITLE = "TextTools"

internal data class Labeled(
    val id: String,
    val label: String,
)

internal fun normalizedKey(key: String?): String? = key?.trim()?.lowercase()?.ifEmpty { null }

internal fun freeMenuKey(actions: List<Action>): String? {
    val used = actions.mapNotNull { normalizedKey(it.menuKey) }.toSet()
    return Factory.menuKeys.firstOrNull { it.lowercase() !in used }
}

internal fun occupantIndex(
    actions: List<Action>,
    key: String?,
    self: Int,
): Int {
    val wanted = normalizedKey(key) ?: return -1
    return actions.indices.firstOrNull { index ->
        index != self && normalizedKey(actions[index].menuKey) == wanted
    } ?: -1
}

/** The menu must keep one action. */
internal fun canDeleteAction(count: Int): Boolean = count > 1

internal fun <T> moveItem(
    items: List<T>,
    index: Int,
    delta: Int,
): Pair<List<T>, Int>? {
    val target = index + delta
    if (index !in items.indices || target !in items.indices) return null
    val next = items.toMutableList()
    val item = next.removeAt(index)
    next.add(target, item)
    return next to target
}

/** A duplicate drops the factory id, takes the next free key, and is inserted after the source. */
internal fun duplicated(
    actions: List<Action>,
    index: Int,
    suffix: String,
): List<Action>? {
    val source = actions.getOrNull(index) ?: return null
    val copy =
        duplicateFactory(source).copy(
            name = source.name + suffix,
            menuKey = freeMenuKey(actions),
        )
    val next = actions.toMutableList()
    next.add(index + 1, copy)
    return next
}

/**
 * Writes [edited] after yielding the occupant's key.
 * Swap with no key of its own returns null. Cancel and a missing occupant return null.
 * An [index] outside the list appends the row.
 */
internal fun commitAction(
    actions: List<Action>,
    index: Int,
    edited: Action,
    occupant: Int,
    oldKey: String?,
    mode: KeyConflict?,
): List<Action>? {
    val base =
        if (occupant < 0) {
            actions
        } else {
            val choice = mode ?: return null
            val swapWithoutKey = choice == KeyConflict.Swap && oldKey.isNullOrBlank()
            if (choice == KeyConflict.Cancel || swapWithoutKey) return null
            yieldMenuKey(actions, occupant, oldKey, choice) ?: return null
        }
    val next = base.toMutableList()
    if (index in next.indices) next[index] = edited else next.add(edited)
    return next
}

/** An empty name or instruction does not save. */
internal fun savedQuick(
    label: String,
    instruction: String,
): QuickRefine? {
    val name = label.trim()
    val text = instruction.trim()
    if (name.isEmpty() || text.isEmpty()) return null
    return QuickRefine(name, text)
}

/** An empty process does not save. A blank name is stored as the process. */
internal fun savedApp(
    proc: String,
    title: String,
    name: String,
    description: String,
    hint: String,
): AppProfile? {
    val process = proc.trim()
    if (process.isEmpty()) return null
    return AppProfile(
        process,
        title.trim(),
        name.trim().ifEmpty { process },
        description.trim(),
        hint.trim(),
    )
}

internal fun prefixForEdit(prefix: String): String = prefix.replace("\r\n", "\n").replace("\n", "\\n")

internal fun prefixFromEdit(field: String): String = field.replace("\\n", "\n")

internal fun languageOptions(
    action: Action,
    languages: List<OutputLanguage>,
    autoLabel: String,
    missingSuffix: String,
): List<Labeled> =
    buildList {
        add(Labeled("auto", autoLabel))
        languages.filter { it.enabled && it.code.isNotBlank() }.forEach { language ->
            if (none { it.id.equals(language.code, ignoreCase = true) }) {
                add(Labeled(language.code, language.name))
            }
        }
        val code = action.lang.trim()
        val known = any { it.id.equals(code, ignoreCase = true) }
        val custom = code.isNotEmpty() && !code.equals("auto", ignoreCase = true) && !known
        if (custom) {
            val label = languages.firstOrNull { it.code.equals(code, ignoreCase = true) }?.name ?: code
            add(Labeled(code, "$label $missingSuffix"))
        }
    }

internal fun selectedLanguageIndex(
    action: Action,
    options: List<Labeled>,
): Int {
    val current = action.lang.trim().ifEmpty { "auto" }
    val found = options.indexOfFirst { it.id.equals(current, ignoreCase = true) }
    return if (found < 0) 0 else found
}

internal fun modelChoices(
    action: Action,
    models: List<String>,
    defaultModel: String,
): List<String> {
    val fallback = defaultModel.trim()
    val extras =
        models
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.equals(fallback, ignoreCase = true) }
            .distinctBy { it.lowercase() }
    val current =
        action.model?.trim()?.takeIf { it.isNotEmpty() && !it.equals(fallback, ignoreCase = true) }
            ?: return extras
    return if (extras.none { it.equals(current, ignoreCase = true) }) extras + current else extras
}

internal fun selectedModelIndex(
    action: Action,
    extras: List<String>,
    defaultModel: String,
): Int {
    val current = action.model?.trim().orEmpty()
    if (current.isEmpty() || current.equals(defaultModel.trim(), ignoreCase = true)) return 0
    val found = extras.indexOfFirst { it.equals(current, ignoreCase = true) }
    return if (found < 0) 0 else found + 1
}

internal fun selectedKeyIndex(action: Action): Int {
    val key = normalizedKey(action.menuKey) ?: return 0
    val found = Factory.menuKeys.indexOfFirst { it.equals(key, ignoreCase = true) }
    return if (found < 0) 0 else found + 1
}

internal fun keyAt(index: Int): String? = if (index <= 0) null else Factory.menuKeys.getOrNull(index - 1)

internal fun actionListName(action: Action): String {
    val icon = action.icon.trim()
    return if (icon.isEmpty()) action.name else "$icon ${action.name}"
}

internal fun languageListLabel(
    code: String,
    languages: List<OutputLanguage>,
    autoLabel: String,
): String {
    if (code.isBlank() || code.equals("auto", ignoreCase = true)) return autoLabel
    return languages.firstOrNull { it.code.equals(code, ignoreCase = true) }?.name ?: code
}

internal fun modelListLabel(
    model: String?,
    defaultLabel: String,
): String = model?.trim()?.ifEmpty { null } ?: defaultLabel

internal fun keyListLabel(key: String?): String = normalizedKey(key)?.uppercase() ?: EMPTY_MARK

internal fun titleCell(title: String): String = title.trim().ifEmpty { EMPTY_MARK }

internal fun hintCell(
    hint: String,
    emptyLabel: String,
): String = hint.trim().ifEmpty { emptyLabel }
