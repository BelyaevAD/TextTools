package dev.belyaevad.texttools.app.ui.editor

import dev.belyaevad.texttools.domain.Action
import dev.belyaevad.texttools.domain.KeyConflict
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class EditorLogicTest {
    private val actions = listOf(action("1"), action("2"))
    private val edited = actions[0].copy(menuKey = "2")

    @Test
    fun swapGivesTheOccupantTheOldKey() {
        val saved = commitAction(actions, 0, edited, occupant = 1, oldKey = "1", mode = KeyConflict.Swap)
        assertEquals(listOf("2", "1"), saved?.map { it.menuKey })
    }

    @Test
    fun takeClearsTheOccupantKey() {
        val saved = commitAction(actions, 0, edited, occupant = 1, oldKey = "1", mode = KeyConflict.Take)
        assertEquals(listOf("2", null), saved?.map { it.menuKey })
    }

    @Test
    fun cancelAndSwapWithoutAKeyChangeNothing() {
        assertNull(commitAction(actions, 0, edited, occupant = 1, oldKey = "1", mode = KeyConflict.Cancel))
        assertNull(commitAction(actions, 0, edited, occupant = 1, oldKey = null, mode = KeyConflict.Swap))
    }

    private fun action(key: String) = Action(null, "a$key", "", "", "", "", false, false, key, null, null)
}
