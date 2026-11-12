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
    fun noCaretUsesTheCursorEvenWhenTheWindowIsElsewhere() {
        val point =
            menuPosition(
                caret = null,
                cursor = ScreenPoint(10, 10),
                menuWidth = 500,
                menuHeight = 560,
                workAreas = listOf(desktop, right),
                window = WindowBounds(2000, 40, 2800, 900),
            )
        assertEquals(ScreenPoint(10, 10), point)
    }

    @Test
    fun aCaretOutsideTheInputWindowUsesTheCursor() {
        val point =
            menuPosition(
                caret = ScreenPoint(10, 10),
                cursor = ScreenPoint(20, 20),
                menuWidth = 500,
                menuHeight = 560,
                workAreas = listOf(desktop, right),
                window = WindowBounds(2000, 40, 2800, 900),
            )
        assertEquals(ScreenPoint(20, 20), point)
    }

    @Test
    fun aCaretInsideTheInputWindowStillDrops() {
        val point =
            menuPosition(
                caret = ScreenPoint(2100, 200),
                cursor = ScreenPoint(10, 10),
                menuWidth = 500,
                menuHeight = 560,
                workAreas = listOf(desktop, right),
                window = WindowBounds(2000, 40, 2800, 900),
            )
        assertEquals(ScreenPoint(2100, 224), point)
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

    @Test
    fun theResultSitsToTheRightOfTheSourceWindow() {
        val point =
            besideWindow(
                window = WindowBounds(100, 80, 500, 400),
                width = 200,
                height = 120,
                areas = listOf(desktop),
            )
        assertEquals(ScreenPoint(512, 80), point)
    }

    @Test
    fun theResultMovesToTheLeftWhenTheRightSideDoesNotFit() {
        val point =
            besideWindow(
                window = WindowBounds(1600, 80, 1900, 400),
                width = 200,
                height = 120,
                areas = listOf(desktop),
            )
        assertEquals(ScreenPoint(1388, 80), point)
    }

    @Test
    fun aLowWindowSlidesTheResultUpBesideIt() {
        val point =
            besideWindow(
                window = WindowBounds(100, 900, 500, 1040),
                width = 200,
                height = 300,
                areas = listOf(desktop),
            )
        assertEquals(ScreenPoint(512, 774), point)
    }

    @Test
    fun aCaretOnAMonitorAboveTheOriginStaysThere() {
        val above = WorkArea(-1920, -1080, 0, 0)
        val point =
            menuPosition(
                caret = ScreenPoint(-400, -200),
                cursor = ScreenPoint(40, 50),
                menuWidth = 100,
                menuHeight = 40,
                workAreas = listOf(desktop, above),
                window = WindowBounds(-500, -300, -100, -50),
            )
        assertEquals(ScreenPoint(-400, -176), point)
    }

    @Test
    fun aFieldOpensAboveWhenTheCursorIsNearItsTop() {
        val point =
            popupBeside(
                target = WindowBounds(100, 200, 500, 260),
                width = 200,
                height = 120,
                areas = listOf(desktop),
                cursor = ScreenPoint(300, 220),
            )
        assertEquals(ScreenPoint(300, 68), point)
    }

    @Test
    fun aPageSelectionOpensBesideTheCursor() {
        val cursor = ScreenPoint(80, 90)
        val point =
            popupBeside(
                target = anchorBox(null, null, cursor, null),
                width = 200,
                height = 80,
                areas = listOf(desktop),
                cursor = cursor,
            )
        assertEquals(ScreenPoint(93, 90), point)
    }

    @Test
    fun aPageSizedFieldUsesTheCaretInstead() {
        val window = WindowBounds(0, 0, 1000, 800)
        val box = anchorBox(window, ScreenPoint(40, 50), ScreenPoint(900, 700), window)
        assertEquals(WindowBounds(40, 50, 42, 70), box)
    }
}
