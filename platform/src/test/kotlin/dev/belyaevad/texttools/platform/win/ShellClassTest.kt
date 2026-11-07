package dev.belyaevad.texttools.platform.win

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** The shell-class list is the same one the old tracker skipped. No earlier test covers it. */
class ShellClassTest {
    @Test
    fun taskbarAndDesktopAreShell() {
        assertTrue(isShellClass("Shell_TrayWnd"))
        assertTrue(isShellClass("Progman"))
        assertFalse(isShellClass("Notepad"))
    }
}
