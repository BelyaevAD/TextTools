package dev.belyaevad.texttools.platform.win

import kotlin.test.Test
import kotlin.test.assertEquals

/** Second process focuses the first and exits. The fake does not stop any process. */
class InstanceHandoffTest {
    @Test
    fun secondProcessFocusesAndExits() {
        var focused = 0
        var exited = 0
        runInstanceAction(instanceAction(alreadyRunning = true), { focused++ }, { exited++ })
        assertEquals(1, focused)
        assertEquals(1, exited)
    }

    @Test
    fun firstProcessStays() {
        var focused = 0
        var exited = 0
        runInstanceAction(instanceAction(alreadyRunning = false), { focused++ }, { exited++ })
        assertEquals(0, focused)
        assertEquals(0, exited)
    }
}
