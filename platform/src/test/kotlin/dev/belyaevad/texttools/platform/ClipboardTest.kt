package dev.belyaevad.texttools.platform

import dev.belyaevad.texttools.domain.clipWaitSeconds
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

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
    fun restoreAfterPastePutsTheOldTextBackOnlyWhileTheResultIsStillThere() {
        val io = FakeClip(text = "old")
        val sent = assertIs<PasteAttempt.Sent>(ClipboardProtocol(io).paste("result"))
        assertEquals("result", io.text)
        ClipboardProtocol(io).restoreAfterPaste(sent.saved, sent.pasted)
        assertEquals("old", io.text)
        io.text = "user copied this"
        ClipboardProtocol(io).restoreAfterPaste("old", "result")
        assertEquals("user copied this", io.text)
    }

    @Test
    fun lockedReadDoesNotWrite() {
        val io = FakeClip(text = null)
        assertIs<PasteAttempt.ShowInWindow>(ClipboardProtocol(io).paste("result"))
        assertEquals(null, io.text)
    }

    @Test
    fun waylandIsNotX11() {
        assertEquals(DesktopSession.Wayland, desktopSession("Linux", "wayland-0", ":0"))
        assertEquals(DesktopSession.X11, desktopSession("Linux", null, ":0"))
    }

    @Test
    fun imageOrFilesWarnOnceAndLeaveTheClipboardAlone() {
        val io = FakeClip(text = "", imageOrFiles = true)
        val protocol = ClipboardProtocol(io)
        assertEquals("", protocol.capture(200))
        assertEquals(NON_TEXT_CLIPBOARD_WARNING, protocol.takeNonTextWarning())
        assertEquals("", protocol.capture(200))
        assertNull(protocol.takeNonTextWarning())
        assertEquals("", io.text)
        assertEquals(0, io.writes)
        assertEquals(0, io.copies)
        assertEquals(0, io.pastes)
        assertTrue(io.imageOrFiles)
    }

    @Test
    fun captureSleepsForTheCallerClipWait() {
        val clock = ManualClock()
        val io = SleepyClip(clock)
        val protocol = ClipboardProtocol(io, clock)
        val seconds = clipWaitSeconds(1.5)
        assertEquals("selected", protocol.captureSeconds(seconds))
        assertTrue(clock.slept.isNotEmpty())
        assertTrue(clock.slept.all { it in 1..20 })
        assertTrue(clock.now < (seconds * 1000).toLong())
        assertEquals("old", io.text)
    }

    @Test
    fun emptyCopyWaitsTheFullCallerDuration() {
        val clock = ManualClock()
        val io = FakeClip(text = "old")
        val seconds = clipWaitSeconds(9.0)
        assertEquals("", ClipboardProtocol(io, clock).captureSeconds(seconds))
        assertEquals(5_000L, clock.now)
        assertTrue(clock.slept.isNotEmpty())
        assertTrue(clock.slept.all { it in 1..20 })
        assertEquals("old", io.text)
    }

    @Test
    fun elevatedForeignWindowDoesNotPaste() {
        val io = FakeClip(text = "old", elevated = true)
        assertIs<PasteAttempt.ShowInWindow>(ClipboardProtocol(io).paste("result"))
        assertEquals("old", io.text)
        assertEquals(0, io.pastes)
        assertEquals(0, io.writes)
    }
}

private class FakeClip(
    var text: String?,
    var writable: Boolean = true,
    var canActivate: Boolean = true,
    var imageOrFiles: Boolean = false,
    var elevated: Boolean = false,
) : ClipIo {
    var afterCopy: String = ""
    var writes: Int = 0
    var copies: Int = 0
    var pastes: Int = 0

    override fun readText(): String? = text

    override fun writeText(text: String): Boolean {
        writes++
        if (!writable) return false
        this.text = text
        return true
    }

    override fun sendCopy() {
        copies++
        text = afterCopy
    }

    override fun sendPaste() {
        pastes++
    }

    override fun activateSource(): Boolean = canActivate

    override fun hasImageOrFiles(): Boolean = imageOrFiles

    override fun elevatedForeign(): Boolean = elevated
}

/** Copy text appears only after the clock has slept, so a spin would not finish. */
private class SleepyClip(
    private val clock: ManualClock,
) : ClipIo {
    var text: String? = "old"
    private var waiting = false

    override fun readText(): String? {
        if (!waiting) return text
        return if (clock.slept.size >= 2) "selected" else ""
    }

    override fun writeText(text: String): Boolean {
        this.text = text
        waiting = false
        return true
    }

    override fun sendCopy() {
        waiting = true
    }

    override fun sendPaste() = Unit

    override fun activateSource(): Boolean = true
}

private class ManualClock : WaitClock {
    var now: Long = 0
    val slept = mutableListOf<Long>()
    private var spins = 0

    override fun nowMillis(): Long {
        if (slept.isEmpty()) {
            spins++
            if (spins > 1_000) error("busy spin")
        }
        return now
    }

    override fun sleepMillis(millis: Long) {
        if (millis <= 0L) error("sleep must be positive")
        slept += millis
        now += millis
        if (slept.size > 10_000) error("wait did not stop")
    }
}
