package dev.belyaevad.texttools.app.ui

import androidx.compose.ui.unit.dp
import dev.belyaevad.texttools.platform.win.ScreenPoint
import dev.belyaevad.texttools.platform.win.WorkArea
import kotlin.test.Test
import kotlin.test.assertEquals

/** Visible menu height uses the Material 3 field baseline and keeps the row step. */
class WindowMetricsTest {
    @Test
    fun menuHeightFollowsTheRows() {
        assertEquals(341, menuOuterHeight(16))
        assertEquals(136, menuOuterHeight(1))
        assertEquals(136, menuOuterHeight(3))
        assertEquals(177, menuOuterHeight(4))
        assertEquals(760, menuOuterHeight(100))
    }

    @Test
    fun resultHeightFollowsTheRefineField() {
        assertEquals(612.dp, resultOuterHeight(hasConversation = true, hasChips = true))
        assertEquals(576.dp, resultOuterHeight(hasConversation = true, hasChips = false))
        assertEquals(563.dp, resultOuterHeight(hasConversation = false, hasChips = false))
    }

    @Test
    fun bannerUsesTheMonitorOfTheAnchor() {
        val left = WorkArea(0, 0, 1920, 1080)
        val right = WorkArea(1920, 0, 3840, 1080)
        assertEquals(1582 to 948, bannerCorner(ScreenPoint(10, 10), listOf(left, right)))
        assertEquals(3502 to 948, bannerCorner(ScreenPoint(2000, 100), listOf(left, right)))
        assertEquals(3502 to 948, bannerCorner(ScreenPoint(2000, 1100), listOf(left, right)))
        assertEquals(1582 to 948, bannerCorner(ScreenPoint(10, 100), listOf(right, WorkArea(40, 0, 1920, 1080))))
        assertEquals(1582 to 948, bannerCorner(ScreenPoint(-40, -40), listOf(left, right)))
        assertEquals(942 to 588, bannerCorner(ScreenPoint(0, 0), emptyList()))
    }

    @Test
    fun aSavedFrameOffTheDesktopComesBackOntoIt() {
        val desktop = WorkArea(0, 0, 1920, 1080)
        val moved = placeOnScreen(SavedFrame(-400, -200, 800, 600), listOf(desktop))
        assertEquals(SavedFrame(0, 0, 800, 600), moved)
    }
}
