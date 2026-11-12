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

    /** True when the clipboard holds an image or a file list and no usable text is required to notice it. */
    fun hasImageOrFiles(): Boolean = false

    /**
     * True when the source field can take the result.
     * A Win32 caret counts. So does a focused editable control that does not publish one.
     * Tests paste unless they override this.
     */
    fun canAcceptText(): Boolean = true

    /**
     * True when the source window belongs to another process and is elevated,
     * or its token cannot be read. Paste must not be sent.
     */
    fun elevatedForeign(): Boolean = false
}

/** Shown once per [ClipboardProtocol] when a capture finds an image or files and no text. */
const val NON_TEXT_CLIPBOARD_WARNING =
    "The clipboard has no text (an image or files). TextTools keeps text only."

/** Result of trying to put text back into the source window. */
sealed class PasteAttempt {
    /**
     * The paste chord was sent.
     * [saved] is the clipboard from before the paste. [pasted] is the text we put there.
     * The pasted text stays on the clipboard.
     */
    data class Sent(
        val saved: String,
        val pasted: String,
    ) : PasteAttempt()

    /** The source window could not take the text, so the UI must show it. */
    data object ShowInWindow : PasteAttempt()
}

/**
 * The AHK clipboard order: remember, clear, copy, wait, read, restore.
 * A locked clipboard does not start an action.
 * The wait sleeps. An image or a file list is left in place and warned about once.
 */
class ClipboardProtocol internal constructor(
    private val io: ClipIo,
    private val wait: WaitClock,
) {
    constructor(io: ClipIo) : this(io, SystemWaitClock)

    private var warnedNonText = false
    private var pendingNonText: String? = null

    /**
     * Reads the selection.
     * [waitSeconds] is the caller's `clipWaitSeconds` value, already clamped to 0.2–5.
     * Returns an empty string when the copy did not arrive.
     */
    fun captureSeconds(waitSeconds: Double): String = capture(secondsToMillis(waitSeconds))

    /** Reads the selection. [waitMillis] is the caller's wait, in milliseconds. */
    fun capture(waitMillis: Long): String {
        val nonText = io.hasImageOrFiles()
        val saved = io.readText()
        if ((saved == null || saved.isEmpty()) && nonText) {
            warnNonTextOnce()
            return ""
        }
        if (!io.writeText("")) {
            if (saved != null) io.writeText(saved)
            return ""
        }
        io.sendCopy()
        val text = waitForCopy(waitMillis)
        io.writeText(saved ?: "")
        return text
    }

    /**
     * The one-shot notice for an image or a file list, or null when this session already delivered it.
     * The text is fixed. Clipboard contents are not included.
     */
    fun takeNonTextWarning(): String? {
        val notice = pendingNonText
        pendingNonText = null
        return notice
    }

    /**
     * Puts the result into the source window, or asks the UI to show it.
     * A locked clipboard is left untouched. An elevated foreign window is not pasted into.
     */
    fun paste(text: String): PasteAttempt {
        val saved = io.readText() ?: return PasteAttempt.ShowInWindow
        if (io.elevatedForeign()) return PasteAttempt.ShowInWindow
        if (!io.writeText(text)) return PasteAttempt.ShowInWindow
        if (!io.activateSource()) return PasteAttempt.ShowInWindow
        if (!io.canAcceptText()) {
            io.writeText(saved)
            return PasteAttempt.ShowInWindow
        }
        io.sendPaste()
        return PasteAttempt.Sent(saved, text)
    }

    /**
     * Restores [saved] only while the clipboard still holds [pasted].
     * Matches the AHK timer: if the user copied something else, that copy stays.
     */
    fun restoreAfterPaste(
        saved: String,
        pasted: String,
    ) {
        val current = io.readText() ?: return
        if (current == pasted) {
            io.writeText(saved)
        }
    }

    private fun warnNonTextOnce() {
        if (warnedNonText) return
        warnedNonText = true
        pendingNonText = NON_TEXT_CLIPBOARD_WARNING
    }

    private fun waitForCopy(waitMillis: Long): String {
        val limit = waitMillis.coerceAtLeast(0)
        val deadline = wait.nowMillis() + limit
        while (true) {
            val current = io.readText()
            if (!current.isNullOrEmpty()) return current
            val now = wait.nowMillis()
            if (now >= deadline) return ""
            val slice = minOf(CLIP_POLL_MILLIS, deadline - now)
            if (slice <= 0L) return ""
            wait.sleepMillis(slice)
        }
    }
}

/** Clock for the copy wait. Production sleeps. Tests advance time only inside [sleepMillis]. */
internal interface WaitClock {
    fun nowMillis(): Long

    fun sleepMillis(millis: Long)
}

private const val CLIP_POLL_MILLIS = 20L

private object SystemWaitClock : WaitClock {
    override fun nowMillis(): Long = System.currentTimeMillis()

    override fun sleepMillis(millis: Long) {
        Thread.sleep(millis)
    }
}

private fun secondsToMillis(waitSeconds: Double): Long {
    if (waitSeconds <= 0.0) return 0L
    val millis = waitSeconds * 1000.0
    if (millis >= Long.MAX_VALUE) return Long.MAX_VALUE
    return millis.toLong()
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
