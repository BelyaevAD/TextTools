package dev.belyaevad.texttools.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowScope
import dev.belyaevad.texttools.platform.win.ScreenPoint
import dev.belyaevad.texttools.platform.win.WorkArea
import dev.belyaevad.texttools.platform.win.decoratedFrameSlack
import java.awt.Dimension
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import javax.swing.SwingUtilities

/** Visible width of the action menu. The undecorated window uses this size directly. */
const val MENU_OUTER_WIDTH = 488

/** Visible width of the busy banner. */
const val BANNER_OUTER_WIDTH = 326

/** Visible height of the busy banner. */
const val BANNER_OUTER_HEIGHT = 120

private const val BANNER_MARGIN = 12

val ResultWidth = 818.dp
val SettingsWidth = 886.dp
val SettingsHeight = 780.dp
val EditorWidth = 798.dp
val EditorHeight = 545.dp
val HistoryWidth = 998.dp
val HistoryHeight = 456.dp
val ActionCardWidth = 663.dp
val ActionCardHeight = 720.dp
val AppCardWidth = 748.dp
val AppCardHeight = 440.dp
val QuickCardWidth = 658.dp
val QuickCardHeight = 380.dp
val KeyConflictWidth = 455.dp
val KeyConflictHeight = 222.dp

private val ResultFullHeight = 612.dp
private val ResultNoChipsHeight = 576.dp
private val ResultReportHeight = 563.dp

/**
 * Visible menu height for [actionCount] buttons in three columns.
 * One to three actions are one row. The result stays at most 760.
 * The 95 baseline keeps the original 87 px layout plus the compact field's 8 px growth.
 */
fun menuOuterHeight(actionCount: Int): Int {
    val rows = (actionCount.coerceAtLeast(1) + 2) / 3
    return (95 + rows * 41).coerceAtMost(760)
}

/**
 * Bottom-right corner of the work area that holds [anchor].
 * A point on that monitor's taskbar still selects it. No areas means 1280×720 at the origin.
 */
fun bannerCorner(
    anchor: ScreenPoint,
    areas: List<WorkArea>,
): Pair<Int, Int> {
    val area = bannerArea(anchor, areas)
    val x = (area.right - BANNER_OUTER_WIDTH - BANNER_MARGIN).coerceAtLeast(area.left)
    val y = (area.bottom - BANNER_OUTER_HEIGHT - BANNER_MARGIN).coerceAtLeast(area.top)
    return x to y
}

private fun bannerArea(
    anchor: ScreenPoint,
    areas: List<WorkArea>,
): WorkArea {
    if (areas.isEmpty()) return WorkArea(0, 0, 1280, 720)
    areas.firstOrNull { it.containsX(anchor.x) && it.containsY(anchor.y) }?.let { return it }
    val byX = areas.filter { it.containsX(anchor.x) }.minByOrNull { edgeGap(anchor.y, it.top, it.bottom) }
    if (byX != null) return byX
    val byY = areas.filter { it.containsY(anchor.y) }.minByOrNull { edgeGap(anchor.x, it.left, it.right) }
    return byY ?: areas.first()
}

private fun edgeGap(
    value: Int,
    start: Int,
    end: Int,
): Int =
    when {
        value < start -> start - value
        value >= end -> value - end + 1
        else -> 0
    }

/** Visible result height. Reports omit the dialogue. An empty chip row omits only the quick row. */
fun resultOuterHeight(
    hasConversation: Boolean,
    hasChips: Boolean,
): Dp =
    when {
        !hasConversation -> ResultReportHeight
        !hasChips -> ResultNoChipsHeight
        else -> ResultFullHeight
    }

/**
 * Grows a decorated window once so its visible frame matches the size already stored in [size].
 * The first Compose frame runs before the window is shown, and DWM reports no border then.
 * Measurement waits until the window is showing. Undecorated windows must not call this.
 * The fitted outer size becomes the window's minimum, so a later resize cannot hide the controls.
 * Compose 1.12.2 applies the dp number as pixels.
 */
@Composable
fun WindowScope.fitVisibleFrame(
    size: DpSize,
    onSize: (DpSize) -> Unit,
) {
    DisposableEffect(window) {
        var applied = false

        fun apply() {
            if (applied || !window.isShowing) return
            val (extraW, extraH) = decoratedFrameSlack(window)
            if (extraW == 0 && extraH == 0) return
            applied = true
            window.minimumSize = Dimension(window.width + extraW, window.height + extraH)
            onSize(DpSize(size.width + extraW.dp, size.height + extraH.dp))
        }
        val listener =
            object : WindowAdapter() {
                override fun windowOpened(event: WindowEvent) {
                    apply()
                    if (!applied) SwingUtilities.invokeLater { apply() }
                }
            }
        window.addWindowListener(listener)
        apply()
        onDispose { window.removeWindowListener(listener) }
    }
}
