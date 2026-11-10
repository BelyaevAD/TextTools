package dev.belyaevad.texttools.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowScope
import dev.belyaevad.texttools.platform.win.WorkArea
import dev.belyaevad.texttools.platform.win.currentWorkAreas
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.Properties

/** Outer window frame in screen pixels. */
internal data class SavedFrame(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
)

/** Values for rememberWindowState or rememberDialogState. */
internal data class Placement(
    val position: WindowPosition,
    val width: Dp,
    val height: Dp,
    val restored: Boolean,
)

/**
 * Remembers where a window was left.
 * Called from SettingsWindow, EditorWindow, HistoryWindow, ResultWindow and framedDialog.
 * A missing record keeps the caller's size and lets the platform choose the first position.
 * The file is windows.properties: id.x, id.y, id.w, id.h.
 */
@Composable
internal fun rememberPlacement(
    id: String,
    width: Dp,
    height: Dp,
    atLeastDesign: Boolean = false,
): Placement {
    val saved =
        remember(id, width, height, atLeastDesign) {
            WindowFrames.read(id)?.let { frame ->
                val floored =
                    if (atLeastDesign) {
                        frame.copy(
                            width = maxOf(frame.width, width.value.toInt()),
                            height = maxOf(frame.height, height.value.toInt()),
                        )
                    } else {
                        frame
                    }
                placeOnScreen(floored, currentWorkAreas())
            }
        }
    return if (saved == null) {
        Placement(WindowPosition.PlatformDefault, width, height, restored = false)
    } else {
        Placement(WindowPosition(saved.x.dp, saved.y.dp), saved.width.dp, saved.height.dp, restored = true)
    }
}

/** Writes the live outer frame while the window is showing, including the last position at close. */
@Composable
internal fun WindowScope.keepWindowFrame(id: String) {
    DisposableEffect(window, id) {
        fun write() {
            if (!window.isShowing || window.width < 80 || window.height < 40) return
            WindowFrames.save(id, SavedFrame(window.x, window.y, window.width, window.height))
        }
        val listener =
            object : ComponentAdapter() {
                override fun componentMoved(event: ComponentEvent) = write()

                override fun componentResized(event: ComponentEvent) = write()
            }
        window.addComponentListener(listener)
        onDispose {
            write()
            window.removeComponentListener(listener)
        }
    }
}

/** Moves [frame] fully inside the nearest work area. A huge frame is shrunk to that area. */
internal fun placeOnScreen(
    frame: SavedFrame,
    areas: List<WorkArea>,
): SavedFrame {
    val area = areas.minByOrNull { distanceTo(frame, it) } ?: return frame
    val limitW = (area.right - area.left - 12).coerceAtLeast(80)
    val limitH = (area.bottom - area.top - 12).coerceAtLeast(40)
    val width = frame.width.coerceIn(80, limitW)
    val height = frame.height.coerceIn(40, limitH)
    val maxX = (area.right - width).coerceAtLeast(area.left)
    val maxY = (area.bottom - height).coerceAtLeast(area.top)
    return SavedFrame(frame.x.coerceIn(area.left, maxX), frame.y.coerceIn(area.top, maxY), width, height)
}

private fun distanceTo(
    frame: SavedFrame,
    area: WorkArea,
): Long {
    val dx =
        when {
            frame.x + frame.width <= area.left -> area.left - (frame.x + frame.width)
            frame.x >= area.right -> frame.x - area.right
            else -> 0
        }
    val dy =
        when {
            frame.y + frame.height <= area.top -> area.top - (frame.y + frame.height)
            frame.y >= area.bottom -> frame.y - area.bottom
            else -> 0
        }
    return dx.toLong() + dy.toLong()
}

/** `%LOCALAPPDATA%\TextTools\windows.properties`. Numbers only. */
internal object WindowFrames {
    private val file: Path =
        Path.of(
            System.getenv("LOCALAPPDATA")?.takeIf { it.isNotBlank() }
                ?: Path.of(System.getProperty("user.home"), "AppData", "Local").toString(),
            "TextTools",
            "windows.properties",
        )
    private val props = Properties()

    init {
        synchronized(props) { reload() }
    }

    fun read(id: String): SavedFrame? =
        synchronized(props) {
            val x = props.getProperty("$id.x")?.toIntOrNull() ?: return null
            val y = props.getProperty("$id.y")?.toIntOrNull() ?: return null
            val width = props.getProperty("$id.w")?.toIntOrNull() ?: return null
            val height = props.getProperty("$id.h")?.toIntOrNull() ?: return null
            if (width < 80 || height < 40) return null
            SavedFrame(x, y, width, height)
        }

    fun save(
        id: String,
        frame: SavedFrame,
    ) {
        synchronized(props) {
            props.setProperty("$id.x", frame.x.toString())
            props.setProperty("$id.y", frame.y.toString())
            props.setProperty("$id.w", frame.width.toString())
            props.setProperty("$id.h", frame.height.toString())
            try {
                Files.createDirectories(file.parent)
                Files.newBufferedWriter(file, StandardCharsets.UTF_8).use { writer ->
                    props.store(writer, "TextTools window frames")
                }
            } catch (_: IOException) {
            }
        }
    }

    private fun reload() {
        if (!Files.isRegularFile(file)) return
        try {
            Files.newBufferedReader(file, StandardCharsets.UTF_8).use { reader -> props.load(reader) }
        } catch (_: IOException) {
        }
    }
}
