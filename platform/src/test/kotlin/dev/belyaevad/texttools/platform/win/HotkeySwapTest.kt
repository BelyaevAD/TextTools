package dev.belyaevad.texttools.platform.win

import java.util.concurrent.ConcurrentHashMap
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Hotkey swap on a fake registrar. Does not call RegisterHotKey or SendInput. */
class HotkeySwapTest {
    @Test
    fun bothRefusalsKeepThePreviousPair() {
        val port = FakeHotkeyPort()
        val registration = HotkeyRegistration(port)
        val menu = chord("^!Space")
        val repeat = chord("^+Space")
        assertTrue(registration.replace(menu, repeat))
        val refusedMenu = chord("^#Space")
        val refusedRepeat = chord("^#Enter")
        port.refuse(setOf(refusedMenu, refusedRepeat))
        assertFalse(registration.replace(refusedMenu, refusedRepeat))
        assertEquals(menu, port.chord(MENU_HOTKEY_ID))
        assertEquals(repeat, port.chord(REPEAT_HOTKEY_ID))
    }

    @Test
    fun aSuccessfulSwapDropsTheOldPair() {
        val port = FakeHotkeyPort()
        val registration = HotkeyRegistration(port)
        assertTrue(registration.replace(chord("^!Space"), chord("^+Space")))
        val menu = chord("^#a")
        val repeat = chord("^+b")
        assertTrue(registration.replace(menu, repeat))
        assertEquals(menu, port.chord(MENU_HOTKEY_ID))
        assertEquals(repeat, port.chord(REPEAT_HOTKEY_ID))
    }

    @Test
    fun oneRefusalDoesNotRestoreTheOldPair() {
        val port = FakeHotkeyPort()
        val registration = HotkeyRegistration(port)
        assertTrue(registration.replace(chord("^!Space"), chord("^+Space")))
        val menu = chord("^#Space")
        val repeat = chord("^#Enter")
        port.refuse(setOf(repeat))
        assertFalse(registration.replace(menu, repeat))
        assertEquals(menu, port.chord(MENU_HOTKEY_ID))
        assertNull(port.chord(REPEAT_HOTKEY_ID))
    }

    @Test
    fun noPreviousPairStaysEmptyWhenBothAreRefused() {
        val port = FakeHotkeyPort()
        val registration = HotkeyRegistration(port)
        val menu = chord("^!Space")
        val repeat = chord("^+Space")
        port.refuse(setOf(menu, repeat))
        assertFalse(registration.replace(menu, repeat))
        assertTrue(port.isEmpty())
    }

    @Test
    fun startRegistersTheSavedChordBeforeTheFactoryPair() {
        if (!windowsHost()) return
        val port = FakeHotkeyPort()
        val hotkeys = WinHotkeys({}, port)
        try {
            assertTrue(hotkeys.start("^Space", "^+A"))
            assertEquals(chord("^Space"), port.chord(MENU_HOTKEY_ID))
            assertEquals(chord("^+A"), port.chord(REPEAT_HOTKEY_ID))
        } finally {
            hotkeys.stop()
        }
    }

    @Test
    fun aRefusedSavedChordFallsBackToTheFactoryPair() {
        if (!windowsHost()) return
        val port = FakeHotkeyPort()
        val hotkeys = WinHotkeys({}, port)
        port.refuse(setOf(chord("^Space"), chord("^+A")))
        try {
            assertTrue(hotkeys.start("^Space", "^+A"))
            assertEquals(chord("^!Space"), port.chord(MENU_HOTKEY_ID))
            assertEquals(chord("^+Space"), port.chord(REPEAT_HOTKEY_ID))
        } finally {
            hotkeys.stop()
        }
    }

    @Test
    fun anEmptyRepeatRegistersOnlyTheMenuChord() {
        if (!windowsHost()) return
        val port = FakeHotkeyPort()
        val hotkeys = WinHotkeys({}, port)
        try {
            assertTrue(hotkeys.start("^!Space", ""))
            assertNull(port.chord(REPEAT_HOTKEY_ID))
            assertTrue(hotkeys.swap("^!Enter", ""))
            assertEquals(chord("^!Enter"), port.chord(MENU_HOTKEY_ID))
            assertNull(port.chord(REPEAT_HOTKEY_ID))
        } finally {
            hotkeys.stop()
        }
    }

    @Test
    fun anEmptyRepeatDropsTheOldRepeatChord() {
        val port = FakeHotkeyPort()
        val registration = HotkeyRegistration(port)
        assertTrue(registration.replace(chord("^!Space"), chord("^+Space")))
        assertTrue(registration.replace(chord("^!Enter"), null))
        assertEquals(chord("^!Enter"), port.chord(MENU_HOTKEY_ID))
        assertNull(port.chord(REPEAT_HOTKEY_ID))
    }

    @Test
    fun aRefusedMenuWithoutRepeatRestoresThePreviousPair() {
        val port = FakeHotkeyPort()
        val registration = HotkeyRegistration(port)
        val menu = chord("^!Space")
        val repeat = chord("^+Space")
        assertTrue(registration.replace(menu, repeat))
        port.refuse(setOf(chord("^#Space")))
        assertFalse(registration.replace(chord("^#Space"), null))
        assertEquals(menu, port.chord(MENU_HOTKEY_ID))
        assertEquals(repeat, port.chord(REPEAT_HOTKEY_ID))
    }

    @Test
    fun theStartupFallbackKeepsAnEmptyRepeatEmpty() {
        if (!windowsHost()) return
        val port = FakeHotkeyPort()
        val hotkeys = WinHotkeys({}, port)
        port.refuse(setOf(chord("^Space")))
        try {
            assertTrue(hotkeys.start("^Space", ""))
            assertEquals(chord("^!Space"), port.chord(MENU_HOTKEY_ID))
            assertNull(port.chord(REPEAT_HOTKEY_ID))
        } finally {
            hotkeys.stop()
        }
    }

    @Test
    fun swapBeforeTheLoopDoesNotRegister() {
        val port = FakeHotkeyPort()
        val hotkeys = WinHotkeys({}, port)
        assertFalse(hotkeys.swap("^!Space", "^+Space"))
        assertTrue(port.isEmpty())
    }

    @Test
    fun invalidChordDoesNotChangeARunningPair() {
        if (!windowsHost()) return
        val port = FakeHotkeyPort()
        val hotkeys = WinHotkeys({}, port)
        try {
            assertTrue(hotkeys.start())
            assertFalse(hotkeys.swap("not a chord", "^+Space"))
            assertEquals(chord("^!Space"), port.chord(MENU_HOTKEY_ID))
            assertEquals(chord("^+Space"), port.chord(REPEAT_HOTKEY_ID))
        } finally {
            hotkeys.stop()
        }
    }

    @Test
    fun loopSwapRestoresThePreviousPairWhenBothAreRefused() {
        if (!windowsHost()) return
        val port = FakeHotkeyPort()
        val hotkeys = WinHotkeys({}, port)
        try {
            assertTrue(hotkeys.start())
            port.refuse(setOf(chord("^#Space"), chord("^#Enter")))
            assertFalse(hotkeys.swap("^#Space", "^#Enter"))
            assertEquals(chord("^!Space"), port.chord(MENU_HOTKEY_ID))
            assertEquals(chord("^+Space"), port.chord(REPEAT_HOTKEY_ID))
        } finally {
            hotkeys.stop()
        }
    }
}

private fun chord(spec: String): Chord = parseChord(spec) ?: error("chord was not parsed")

private class FakeHotkeyPort : HotkeyPort {
    private val live = ConcurrentHashMap<Int, Chord>()
    private val refused = mutableSetOf<Chord>()

    @Synchronized
    fun refuse(chords: Set<Chord>) {
        refused.clear()
        refused.addAll(chords)
    }

    @Synchronized
    override fun register(
        id: Int,
        chord: Chord,
    ): Boolean {
        if (chord in refused) return false
        live[id] = chord
        return true
    }

    @Synchronized
    override fun unregister(id: Int) {
        live.remove(id)
    }

    @Synchronized
    fun chord(id: Int): Chord? = live[id]

    @Synchronized
    fun isEmpty(): Boolean = live.isEmpty()
}
