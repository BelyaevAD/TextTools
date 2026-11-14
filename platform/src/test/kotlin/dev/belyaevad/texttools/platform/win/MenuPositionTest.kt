package dev.belyaevad.texttools.platform.win

import kotlin.test.Test
import kotlin.test.assertEquals

/** Menu coordinates only. No window is opened. */
class MenuPositionTest {
    private val desktop = WorkArea(0, 0, 1920, 1080)
    private val right = WorkArea(1920, 0, 3840, 1080)

    // Expected values below were derived from popupBeside's current behaviour.
    @Test
    fun aCursorOutsideEveryWorkAreaMovesOntoTheNearestOne() {
        val point =
            popupBeside(
                target = WindowBounds(-60, -60, -10, -10),
                width = 200,
                height = 120,
                areas = listOf(desktop),
                cursor = ScreenPoint(-20, -30),
            )
        assertEquals(ScreenPoint(6, 6), point)
    }

    @Test
    fun aCursorOnTheSecondMonitorKeepsThePopupThere() {
        val point =
            popupBeside(
                target = WindowBounds(2100, 200, 2500, 260),
                width = 200,
                height = 120,
                areas = listOf(desktop, right),
                cursor = ScreenPoint(2000, 100),
            )
        assertEquals(ScreenPoint(2000, 68), point)
    }

    @Test
    fun aMonitorAboveTheOriginKeepsThePopupThere() {
        val above = WorkArea(-1920, -1080, 0, 0)
        val point =
            popupBeside(
                target = WindowBounds(-500, -300, -100, -250),
                width = 100,
                height = 40,
                areas = listOf(desktop, above),
                cursor = ScreenPoint(-400, -200),
            )
        assertEquals(ScreenPoint(-400, -238), point)
    }

    @Test
    fun aCaretAtTheOriginIsNoCaret() {
        val cursor = ScreenPoint(80, 90)
        assertEquals(WindowBounds(80, 90, 81, 91), anchorBox(null, ScreenPoint(0, 0), cursor, null))
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
