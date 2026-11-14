package dev.belyaevad.texttools.platform

import dev.belyaevad.texttools.domain.clipWaitSeconds
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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
    fun pasteShowsAWindowWhenTheSourceHasNoInputCaret() {
        val io = FakeClip(text = "old", inputCaret = false)
        assertIs<PasteAttempt.ShowInWindow>(ClipboardProtocol(io).paste("result"))
        assertEquals("old", io.text)
        assertEquals(0, io.pastes)
    }

    @Test
    fun anEmptyClipboardStillCopiesTheSelection() {
        val io = FakeClip(text = "")
        io.afterCopy = "from the page"
        assertEquals("from the page", ClipboardProtocol(io).capture(50))
        assertEquals("", io.text)
        assertEquals(1, io.copies)
    }

    @Test
    fun aLockedReadDoesNotStartACapture() {
        val io = FakeClip(text = null)
        val protocol = ClipboardProtocol(io)
        assertEquals("", protocol.capture(50))
        assertEquals(0, io.copies)
        assertEquals(0, io.writes)
        assertTrue(protocol.takeBusyNotice())
        assertFalse(protocol.takeBusyNotice())
    }

    @Test
    fun aRefusedClearingWriteReportsABusyClipboard() {
        val io = FakeClip(text = "old", writable = false)
        val protocol = ClipboardProtocol(io)
        assertEquals("", protocol.capture(20))
        assertTrue(protocol.takeBusyNotice())
    }

    @Test
    fun automaticPasteUsesTheCapturedSourceAndForgetsIt() {
        val io = FakeClip(text = "old")
        io.afterCopy = "selected"
        val protocol = ClipboardProtocol(io)
        assertEquals("selected", protocol.capture(50))
        assertFalse(protocol.takeBusyNotice())
        assertTrue(io.remembersSource)
        assertIs<PasteAttempt.Sent>(protocol.paste("result", captured = true))
        assertEquals("result", io.text)
        assertEquals(true, io.activatedCaptured)
        assertFalse(io.remembersSource)
    }

    @Test
    fun aButtonPasteAfterACaptureTargetsTheLiveWindow() {
        val io = FakeClip(text = "old")
        io.afterCopy = "selected"
        val protocol = ClipboardProtocol(io)
        protocol.capture(50)
        assertIs<PasteAttempt.Sent>(protocol.paste("result"))
        assertEquals(false, io.activatedCaptured)
        assertFalse(io.remembersSource)
    }

    @Test
    fun aCapturedPasteWhoseSourceWasForgottenShowsTheWindow() {
        val io = FakeClip(text = "old")
        io.afterCopy = "selected"
        val protocol = ClipboardProtocol(io)
        protocol.capture(50)
        assertIs<PasteAttempt.Sent>(protocol.paste("button"))
        val writes = io.writes
        assertIs<PasteAttempt.ShowInWindow>(protocol.paste("answer", captured = true))
        assertEquals(1, io.pastes)
        assertEquals(writes, io.writes)
    }

    @Test
    fun aFallbackPasteAlsoForgetsTheCapturedSource() {
        val io = FakeClip(text = "old", canActivate = false)
        io.afterCopy = "selected"
        val protocol = ClipboardProtocol(io)
        protocol.capture(50)
        assertIs<PasteAttempt.ShowInWindow>(protocol.paste("result", captured = true))
        assertFalse(io.remembersSource)
    }

    @Test
    fun anEmptyCaptureLeavesNoSourceBehind() {
        val io = FakeClip(text = "old")
        assertEquals("", ClipboardProtocol(io).capture(20))
        assertFalse(io.remembersSource)
    }

    @Test
    fun aPasteThatCannotHappenLeavesAnImageClipboardAlone() {
        val noWindow = FakeClip(text = "", imageOrFiles = true, canActivate = false)
        assertIs<PasteAttempt.ShowInWindow>(ClipboardProtocol(noWindow).paste("result"))
        assertEquals(0, noWindow.writes)
        val noField = FakeClip(text = "", imageOrFiles = true, inputCaret = false)
        assertIs<PasteAttempt.ShowInWindow>(ClipboardProtocol(noField).paste("result"))
        assertEquals(0, noField.writes)
    }

    @Test
    fun aFailedPasteWritePutsTheSavedTextBack() {
        val io = FakeClip(text = "old", writable = false)
        assertIs<PasteAttempt.ShowInWindow>(ClipboardProtocol(io).paste("result"))
        assertEquals(listOf("result", "old"), io.written)
        assertEquals(0, io.pastes)
    }

    @Test
    fun lockedReadDoesNotWrite() {
        val io = FakeClip(text = null)
        assertIs<PasteAttempt.ShowInWindow>(ClipboardProtocol(io).paste("result"))
        assertEquals(null, io.text)
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
    var inputCaret: Boolean = true,
) : ClipIo {
    var afterCopy: String = ""
    var writes: Int = 0
    var copies: Int = 0
    var pastes: Int = 0
    var remembersSource = false
    var activatedCaptured: Boolean? = null
    val written = mutableListOf<String>()

    override fun readText(): String? = text

    override fun writeText(text: String): Boolean {
        writes++
        written += text
        if (!writable) return false
        this.text = text
        return true
    }

    override fun sendCopy() {
        copies++
        text = afterCopy
        remembersSource = true
    }

    override fun sendPaste() {
        pastes++
    }

    override fun activateSource(): Boolean {
        activatedCaptured = remembersSource
        return canActivate
    }

    override fun forgetSource() {
        remembersSource = false
    }

    override fun hasCapturedSource(): Boolean = remembersSource

    override fun canAcceptText(): Boolean = inputCaret

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
