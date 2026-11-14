package dev.belyaevad.texttools.platform.win

import java.awt.GraphicsEnvironment
import kotlin.math.roundToInt

/**
 * One monitor in device pixels (Win32, UI Automation) and in AWT user space (MouseInfo, work areas, Compose).
 * Right and bottom are exclusive. The JDK keeps the device origin and divides the size by the scale
 * (`SunGraphicsEnvironment.getGCDeviceBounds`), so each screen maps on its own and no global division fits.
 */
internal data class ScreenScale(
    val device: WorkArea,
    val user: WorkArea,
    val scaleX: Double,
    val scaleY: Double,
)

/** A device-pixel point in user space, mapped by the screen that holds it, or the nearest one. Identity at scale 1. */
internal fun toUserSpace(
    point: ScreenPoint,
    screens: List<ScreenScale>,
): ScreenPoint = screenAt(point.x, point.y, screens)?.map(point) ?: point

/**
 * A device-pixel rectangle in user space. The screen that holds its centre maps both corners,
 * so a field across the seam of two screens keeps one scale and stays a rectangle.
 */
internal fun toUserSpace(
    box: WindowBounds,
    screens: List<ScreenScale>,
): WindowBounds {
    val screen = screenAt((box.left + box.right) / 2, (box.top + box.bottom) / 2, screens) ?: return box
    val topLeft = screen.map(ScreenPoint(box.left, box.top))
    val bottomRight = screen.map(ScreenPoint(box.right, box.bottom))
    return WindowBounds(topLeft.x, topLeft.y, bottomRight.x, bottomRight.y)
}

/** Every attached screen as AWT maps it now. */
internal fun currentScreenScales(): List<ScreenScale> =
    GraphicsEnvironment.getLocalGraphicsEnvironment().screenDevices.map { device ->
        val config = device.defaultConfiguration
        val bounds = config.bounds
        val transform = config.defaultTransform
        ScreenScale(
            device =
                WorkArea(
                    left = bounds.x,
                    top = bounds.y,
                    right = bounds.x + (bounds.width * transform.scaleX).roundToInt(),
                    bottom = bounds.y + (bounds.height * transform.scaleY).roundToInt(),
                ),
            user = WorkArea(bounds.x, bounds.y, bounds.x + bounds.width, bounds.y + bounds.height),
            scaleX = transform.scaleX,
            scaleY = transform.scaleY,
        )
    }

internal fun userSpace(box: WindowBounds): WindowBounds = toUserSpace(box, currentScreenScales())

internal fun userSpace(point: ScreenPoint): ScreenPoint = toUserSpace(point, currentScreenScales())

private fun screenAt(
    x: Int,
    y: Int,
    screens: List<ScreenScale>,
): ScreenScale? {
    // Origins are exact and far edges are rounded estimates, so on an overlap the later-starting screen wins.
    val holding = screens.filter { it.device.containsX(x) && it.device.containsY(y) }
    if (holding.isNotEmpty()) return holding.maxWith(compareBy({ it.device.left }, { it.device.top }))
    val area = nearestWorkArea(x, y, screens.map { it.device }) ?: return null
    return screens.first { it.device == area }
}

private fun ScreenScale.map(point: ScreenPoint): ScreenPoint =
    ScreenPoint(
        user.left + ((point.x - device.left) / scaleX).roundToInt(),
        user.top + ((point.y - device.top) / scaleY).roundToInt(),
    )
