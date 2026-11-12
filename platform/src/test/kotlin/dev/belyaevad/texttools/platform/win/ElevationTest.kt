package dev.belyaevad.texttools.platform.win

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Paste decision from token facts. Does not open a process or send input. */
class ElevationTest {
    @Test
    fun elevatedForeignWindowBlocksPaste() {
        assertTrue(blocksPaste(sameProcess = false, elevated = true))
    }

    @Test
    fun unreadableForeignWindowBlocksPaste() {
        assertTrue(blocksPaste(sameProcess = false, elevated = null))
    }

    @Test
    fun sameProcessAndMediumWindowAllowPaste() {
        assertFalse(blocksPaste(sameProcess = true, elevated = true))
        assertFalse(blocksPaste(sameProcess = false, elevated = false))
    }

    @Test
    fun maximizedWindowIsNotRestored() {
        assertFalse(shouldRestoreWindow(iconic = false))
        assertTrue(shouldRestoreWindow(iconic = true))
    }

    @Test
    fun tokenElevationFieldIsVisibleToJna() {
        val info = TokenElevationInfo()
        assertEquals(4, info.size())
    }

    @Test
    fun guiThreadInfoFieldsAreVisibleToJna() {
        val info = GuiThreadInfo()
        info.cbSize = info.size()
        assertTrue(info.cbSize > 4)
    }
}
