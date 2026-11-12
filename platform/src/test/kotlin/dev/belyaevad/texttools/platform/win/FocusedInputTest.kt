package dev.belyaevad.texttools.platform.win

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Which focused controls can take a paste. No window is opened. */
class FocusedInputTest {
    @Test
    fun anEditTakesTheResult() {
        assertTrue(acceptsTypedText(50004, enabled = true, focusable = true, readOnly = false))
    }

    @Test
    fun anEditWithoutAReadOnlyFlagStillTakesTheResult() {
        assertTrue(acceptsTypedText(50004, enabled = true, focusable = null, readOnly = null))
    }

    @Test
    fun aReadOnlyEditDoesNot() {
        assertFalse(acceptsTypedText(50004, enabled = true, focusable = true, readOnly = true))
    }

    @Test
    fun aPageTextSelectionDoesNot() {
        assertFalse(acceptsTypedText(50020, enabled = true, focusable = true, readOnly = null))
    }

    @Test
    fun aDocumentTakesTheResultOnlyWhenItSaysItIsWritable() {
        assertTrue(acceptsTypedText(50030, enabled = true, focusable = true, readOnly = false))
        assertFalse(acceptsTypedText(50030, enabled = true, focusable = true, readOnly = null))
    }
}
