package dev.belyaevad.texttools.app.ui

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.unit.IntSize
import java.awt.Color
import java.awt.RenderingHints
import java.awt.Taskbar
import java.awt.image.BufferedImage
import java.util.concurrent.ConcurrentHashMap

/**
 * Purple rounded mark with a white T.
 * [Main.kt] uses it for the tray. Every [androidx.compose.ui.window.Window] and dialog passes it as `icon`.
 * No other file draws an application mark. The user asked to replace the Java icon on the tray, the application and every window.
 */
internal object AppIcon : Painter() {
    override val intrinsicSize: Size = Size(32f, 32f)

    override fun DrawScope.onDraw() {
        val pixels = size.minDimension.toInt().coerceAtLeast(1)
        val bitmap = iconBitmaps.getOrPut(pixels) { appIconImage(pixels).toComposeImageBitmap() }
        drawImage(bitmap, dstSize = IntSize(size.width.toInt().coerceAtLeast(1), size.height.toInt().coerceAtLeast(1)))
    }
}

/** Sets the process icon where the platform allows it. Window icons are set separately. */
internal fun installApplicationIcon() {
    val image = appIconImage(256)
    try {
        val taskbar = Taskbar.getTaskbar()
        if (taskbar.isSupported(Taskbar.Feature.ICON_IMAGE)) {
            taskbar.iconImage = image
        }
    } catch (_: UnsupportedOperationException) {
    } catch (_: SecurityException) {
    }
}

internal fun appIconImage(size: Int): BufferedImage {
    val image = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
    val graphics = image.createGraphics()
    graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    val inset = (size * 0.06f).toInt()
    val body = size - inset * 2
    val corner = (size * 0.44f).toInt().coerceAtLeast(1)
    graphics.color = Color(108, 77, 255)
    graphics.fillRoundRect(inset, inset, body, body, corner, corner)
    graphics.color = Color.WHITE
    val barLeft = (size * 0.26f).toInt()
    val barTop = (size * 0.28f).toInt()
    val barWidth = (size * 0.48f).toInt().coerceAtLeast(1)
    val barHeight = (size * 0.14f).toInt().coerceAtLeast(1)
    graphics.fillRoundRect(barLeft, barTop, barWidth, barHeight, barHeight, barHeight)
    val stemWidth = (size * 0.14f).toInt().coerceAtLeast(1)
    val stemLeft = (size - stemWidth) / 2
    val stemHeight = (size * 0.46f).toInt().coerceAtLeast(1)
    graphics.fillRoundRect(stemLeft, barTop, stemWidth, stemHeight, stemWidth, stemWidth)
    graphics.dispose()
    return image
}

private val iconBitmaps = ConcurrentHashMap<Int, ImageBitmap>()
