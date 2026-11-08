package dev.belyaevad.texttools.platform.win

import kotlin.test.Test
import kotlin.test.assertEquals

/** Menu coordinates only. No window is opened. */
class MenuPositionTest {
    private val desktop = WorkArea(0, 0, 1920, 1080)
    private val right = WorkArea(1920, 0, 3840, 1080)

    @Test
    fun caretDropsTwentyFourPixels() {
        val point =
            menuPosition(
                caret = ScreenPoint(100, 200),
                cursor = ScreenPoint(1, 1),
                menuWidth = 500,
                menuHeight = 560,
                workAreas = listOf(desktop),
            )
        assertEquals(ScreenPoint(100, 224), point)
    }

    @Test
    fun missingCaretUsesTheCursorWithoutTheDrop() {
        val point =
            menuPosition(
                caret = null,
                cursor = ScreenPoint(80, 90),
                menuWidth = 500,
                menuHeight = 560,
                workAreas = listOf(desktop),
            )
        assertEquals(ScreenPoint(80, 90), point)
    }

    @Test
    fun caretAtTheOriginUsesTheCursor() {
        val point =
            menuPosition(
                caret = ScreenPoint(0, 0),
                cursor = ScreenPoint(40, 50),
                menuWidth = 500,
                menuHeight = 560,
                workAreas = listOf(desktop),
            )
        assertEquals(ScreenPoint(40, 50), point)
    }

    @Test
    fun caretOnTheLeftEdgeStillDrops() {
        val point =
            menuPosition(
                caret = ScreenPoint(0, 50),
                cursor = ScreenPoint(400, 400),
                menuWidth = 500,
                menuHeight = 560,
                workAreas = listOf(desktop),
            )
        assertEquals(ScreenPoint(6, 74), point)
    }

    @Test
    fun pointIsClampedInsideTheWorkArea() {
        val point =
            menuPosition(
                caret = null,
                cursor = ScreenPoint(1900, 1000),
                menuWidth = 500,
                menuHeight = 560,
                workAreas = listOf(desktop),
            )
        assertEquals(ScreenPoint(1414, 514), point)
    }

    @Test
    fun theMonitorThatContainsThePointWins() {
        val point =
            menuPosition(
                caret = ScreenPoint(2000, 100),
                cursor = ScreenPoint(10, 10),
                menuWidth = 500,
                menuHeight = 560,
                workAreas = listOf(desktop, right),
            )
        assertEquals(ScreenPoint(2000, 124), point)
    }

    @Test
    fun aPointOutsideEveryWorkAreaMovesOntoTheNearest() {
        val point =
            menuPosition(
                caret = null,
                cursor = ScreenPoint(-20, -30),
                menuWidth = 500,
                menuHeight = 560,
                workAreas = listOf(desktop),
            )
        assertEquals(ScreenPoint(6, 6), point)
    }

    @Test
    fun aPointPastTheRightMonitorUsesThatMonitor() {
        val point =
            menuPosition(
                caret = null,
                cursor = ScreenPoint(4000, 1000),
                menuWidth = 500,
                menuHeight = 560,
                workAreas = listOf(desktop, right),
            )
        assertEquals(ScreenPoint(3334, 514), point)
    }
}
