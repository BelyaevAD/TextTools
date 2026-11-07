package dev.belyaevad.texttools.domain

/** How a taken menu key is resolved. Cancel is the dialog choice that changes nothing. */
enum class KeyConflict {
    Cancel,
    Swap,
    Take,
}

/**
 * Yields a key the way YieldMenuKey does.
 * Swap gives the occupant the incoming action's old key. Take clears the occupant.
 * The card being edited is updated by the caller. A bad row returns null.
 */
fun yieldMenuKey(
    actions: List<Action>,
    occupantIndex: Int,
    incomingOldKey: String?,
    mode: KeyConflict,
): List<Action>? {
    if (occupantIndex !in actions.indices || mode == KeyConflict.Cancel) {
        return null
    }
    val next = actions.toMutableList()
    val occupant = next[occupantIndex]
    next[occupantIndex] =
        when (mode) {
            KeyConflict.Cancel -> occupant
            KeyConflict.Swap -> occupant.copy(menuKey = incomingOldKey?.ifEmpty { null })
            KeyConflict.Take -> occupant.copy(menuKey = null)
        }
    return next
}
