package dev.belyaevad.texttools.platform

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/** Clipboard order without touching the real OS clipboard. No earlier test file exists. */
class ClipboardTest {
    @Test
    fun captureRestoresThePreviousText() {
        val io = FakeClip(text = "old")
        io.afterCopy = "selected"
        assertEquals("selected", ClipboardProtocol(io).capture(50))
        assertEquals("old", io.text)
    }

    @Test
    fun lockedClipboardDoesNotStart() {
        val io = FakeClip(text = "old", writable = false)
        assertEquals("", ClipboardProtocol(io).capture(20))
        assertEquals("old", io.text)
    }

    @Test
    fun pasteFallsBackWhenTheWindowWillNotActivate() {
        val io = FakeClip(text = "old", canActivate = false)
        assertIs<PasteAttempt.ShowInWindow>(ClipboardProtocol(io).paste("result"))
        assertEquals("old", io.text)
    }

    @Test
    fun waylandIsNotX11() {
        assertEquals(DesktopSession.Wayland, desktopSession("Linux", "wayland-0", ":0"))
        assertEquals(DesktopSession.X11, desktopSession("Linux", null, ":0"))
    }
}

private class FakeClip(
    var text: String?,
    var writable: Boolean = true,
    var canActivate: Boolean = true,
) : ClipIo {
    var afterCopy: String = ""

    override fun readText(): String? = text

    override fun writeText(value: String): Boolean {
        if (!writable) return false
        text = value
        return true
    }

    override fun sendCopy() {
        text = afterCopy
    }

    override fun sendPaste() = Unit

    override fun activateSource(): Boolean = canActivate
}
