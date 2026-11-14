package dev.belyaevad.texttools.platform.win

import kotlin.test.Test
import kotlin.test.assertTrue

/** A second process only exits. No process is stopped here. */
class InstanceHandoffTest {
    @Test
    fun aNonWindowsHostContinuesWithoutNativeCalls() {
        if (windowsHost()) return
        assertTrue(claimFirstInstance())
    }
}
