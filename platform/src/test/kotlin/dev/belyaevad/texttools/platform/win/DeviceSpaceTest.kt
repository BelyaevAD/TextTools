package dev.belyaevad.texttools.platform.win

import kotlin.test.Test
import kotlin.test.assertEquals

/** Win32 device pixels to AWT user space. Screens follow the JDK rule: same origin, size divided by the scale. */
class DeviceSpaceTest {
    private fun screen(
        left: Int,
        top: Int,
        width: Int,
        height: Int,
        scale: Double,
    ) = ScreenScale(
        device = WorkArea(left, top, left + width, top + height),
        user = WorkArea(left, top, left + Math.round(width / scale).toInt(), top + Math.round(height / scale).toInt()),
        scaleX = scale,
        scaleY = scale,
    )

    private val plain = screen(0, 0, 1920, 1080, 1.0)
    private val scaledRight = screen(1920, 0, 2560, 1440, 1.5)

    @Test
    fun atScaleOneNothingMoves() {
        val box = WindowBounds(1500, 300, 1900, 360)
        assertEquals(box, toUserSpace(box, listOf(plain)))
        assertEquals(ScreenPoint(-5, 2000), toUserSpace(ScreenPoint(-5, 2000), listOf(plain)))
    }

    @Test
    fun aFieldOnAScreenAtOneAndAHalfShrinksByThatScale() {
        val screens = listOf(screen(0, 0, 2560, 1440, 1.5))
        assertEquals(WindowBounds(1000, 200, 1600, 240), toUserSpace(WindowBounds(1500, 300, 2400, 360), screens))
    }

    @Test
    fun eachOfTwoScreensMapsItsOwnPoints() {
        val screens = listOf(plain, scaledRight)
        assertEquals(ScreenPoint(100, 100), toUserSpace(ScreenPoint(100, 100), screens))
        assertEquals(ScreenPoint(1920 + 1000, 200), toUserSpace(ScreenPoint(1920 + 1500, 300), screens))
    }

    @Test
    fun aFieldAcrossTheSeamUsesTheScreenThatHoldsItsCentre() {
        val screens = listOf(plain, scaledRight)
        assertEquals(WindowBounds(1840, 200, 2040, 240), toUserSpace(WindowBounds(1800, 300, 2100, 360), screens))
        assertEquals(WindowBounds(1700, 300, 2000, 360), toUserSpace(WindowBounds(1700, 300, 2000, 360), screens))
    }

    @Test
    fun theSeamColumnBelongsToTheRightScreenWhenTheRoundedEdgeOverlapsIt() {
        // As currentScreenScales builds it: 2560 px at 150% is 1707 user px, and 1707 * 1.5 rounds to 2561.
        val scaled = ScreenScale(WorkArea(0, 0, 2561, 1440), WorkArea(0, 0, 1707, 960), 1.5, 1.5)
        val right = screen(2560, 0, 1920, 1080, 1.0)
        assertEquals(ScreenPoint(2560, 100), toUserSpace(ScreenPoint(2560, 100), listOf(scaled, right)))
    }

    @Test
    fun aScaledScreenLeftOfThePrimaryKeepsItsNegativeOrigin() {
        val screens = listOf(screen(-2560, 0, 2560, 1440, 1.5), plain)
        assertEquals(ScreenPoint(-1560, 200), toUserSpace(ScreenPoint(-1060, 300), screens))
        assertEquals(ScreenPoint(100, 100), toUserSpace(ScreenPoint(100, 100), screens))
    }

    @Test
    fun aScaledScreenAboveThePrimaryKeepsItsNegativeOrigin() {
        val screens = listOf(plain, screen(0, -1440, 2560, 1440, 1.5))
        assertEquals(ScreenPoint(1000, -1240), toUserSpace(ScreenPoint(1500, -1140), screens))
    }

    @Test
    fun aPointOffEveryScreenUsesTheNearestOne() {
        val screens = listOf(plain, scaledRight)
        assertEquals(ScreenPoint(3973, 200), toUserSpace(ScreenPoint(5000, 300), screens))
        assertEquals(ScreenPoint(7, 7), toUserSpace(ScreenPoint(7, 7), emptyList()))
    }
}
