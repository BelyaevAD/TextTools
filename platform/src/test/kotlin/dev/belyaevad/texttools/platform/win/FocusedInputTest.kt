package dev.belyaevad.texttools.platform.win

import com.sun.jna.platform.win32.OaIdl
import com.sun.jna.platform.win32.OleAuto
import com.sun.jna.platform.win32.Variant
import com.sun.jna.platform.win32.WTypes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
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

    @Test
    fun aSafeArrayOfFourDoublesBecomesBounds() =
        withBoundsVariant(100.0, 200.0, 300.0, 50.0) { assertEquals(WindowBounds(100, 200, 400, 250), boundsOf(it)) }

    @Test
    fun aNonFiniteValueGivesNoBounds() = withBoundsVariant(100.0, Double.NaN, 300.0, 50.0) { assertNull(boundsOf(it)) }

    private fun withBoundsVariant(
        vararg values: Double,
        check: (Variant.VARIANT) -> Unit,
    ) {
        if (!windowsHost()) return
        val array = OaIdl.SAFEARRAY.createSafeArray(WTypes.VARTYPE(Variant.VT_R8), 4)
        values.forEachIndexed { index, value -> array.putElement(value, index) }
        val sent = Variant.VARIANT(array)
        sent.write()
        // Like the UIA reply: a VARIANT filled by native code, then read back.
        val received = Variant.VARIANT()
        received.pointer.write(0, sent.pointer.getByteArray(0, sent.size()), 0, sent.size())
        received.read()
        try {
            check(received)
        } finally {
            received.autoRead = false
            OleAuto.INSTANCE.VariantClear(received)
        }
    }
}
