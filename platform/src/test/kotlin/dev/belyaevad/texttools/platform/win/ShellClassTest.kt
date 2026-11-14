package dev.belyaevad.texttools.platform.win

import kotlin.test.Test
import kotlin.test.assertEquals
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

/** The profile match uses the file name, not the full path. */
class ExeFileNameTest {
    @Test
    fun keepsTheFileName() {
        assertEquals("chrome.exe", exeFileName("C:\\Program Files\\Google\\Chrome\\chrome.exe"))
        assertEquals("chrome.exe", exeFileName("/usr/bin/chrome.exe"))
        assertEquals("", exeFileName("  "))
        assertFalse(windowsHost("Linux"))
        assertTrue(windowsHost("Windows 11"))
    }
}

/** The invisible border is the outer rectangle minus the visible one. */
class FrameSlackTest {
    @Test
    fun dropsTheInvisibleBorder() {
        assertEquals(16 to 8, frameSlack(818, 640, 802, 632, 1.0, 1.0))
        assertEquals(0 to 0, frameSlack(100, 80, 120, 90, 1.0, 1.0))
    }

    @Test
    fun aScaledWindowGetsItsBorderInUserPixels() {
        assertEquals(11 to 6, frameSlack(1227, 960, 1211, 951, 1.5, 1.5))
    }
}
