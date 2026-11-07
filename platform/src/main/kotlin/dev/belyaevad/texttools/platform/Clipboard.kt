package dev.belyaevad.texttools.platform

/** What the OS clipboard and key injection can do. Tests use a fake. */
interface ClipIo {
    /** Current text, or null when the clipboard is locked. */
    fun readText(): String?

    /** Writes text. False when the clipboard is locked. */
    fun writeText(text: String): Boolean

    /** Sends the copy chord into the previous window. */
    fun sendCopy()

    /** Sends the paste chord. */
    fun sendPaste()

    /** True when the remembered source window became active. */
    fun activateSource(): Boolean
}

/** Result of trying to put text back into the source window. */
sealed class PasteAttempt {
    /** The paste chord was sent. */
    data object Sent : PasteAttempt()

    /** The source window could not take the text, so the UI must show it. */
    data object ShowInWindow : PasteAttempt()
}

/**
 * The AHK clipboard order: remember, clear, copy, wait, read, restore.
 * A locked clipboard does not start an action.
 */
class ClipboardProtocol(
    private val io: ClipIo,
) {
    /** Reads the selection. Returns an empty string when the copy did not arrive. */
    fun capture(waitMillis: Long): String {
        val saved = io.readText() ?: return ""
        if (!io.writeText("")) {
            io.writeText(saved)
            return ""
        }
        io.sendCopy()
        val started = System.currentTimeMillis()
        var text = ""
        while (System.currentTimeMillis() - started < waitMillis) {
            val current = io.readText()
            if (!current.isNullOrEmpty()) {
                text = current
                break
            }
        }
        io.writeText(saved)
        return text
    }

    /** Puts the result into the source window, or asks the UI to show it. */
    fun paste(text: String): PasteAttempt {
        val saved = io.readText() ?: ""
        if (!io.writeText(text)) return PasteAttempt.ShowInWindow
        if (!io.activateSource()) {
            io.writeText(saved)
            return PasteAttempt.ShowInWindow
        }
        io.sendPaste()
        return PasteAttempt.Sent
    }
}

/** Which desktop session the process is in. Wayland is not treated as X11. */
enum class DesktopSession { Windows, Mac, X11, Wayland, Unknown }

/** Reads the session from the OS name and display variables. */
fun desktopSession(
    osName: String,
    waylandDisplay: String?,
    display: String?,
): DesktopSession {
    val os = osName.lowercase()
    return when {
        os.contains("win") -> DesktopSession.Windows
        os.contains("mac") -> DesktopSession.Mac
        !waylandDisplay.isNullOrEmpty() -> DesktopSession.Wayland
        !display.isNullOrEmpty() -> DesktopSession.X11
        else -> DesktopSession.Unknown
    }
}
