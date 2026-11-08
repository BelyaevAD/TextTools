package dev.belyaevad.texttools.app.ui

import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals

/** App locale selection. No existing locale test covered the format category. */
class UiLocaleTest {
    @Test
    fun interfaceLanguageDoesNotReplaceTheFormatLocale() {
        val original = Locale.getDefault()
        val format = systemFormatLocale
        try {
            applyUiLocale("ru")
            assertEquals("ru", Locale.getDefault().language)
            assertEquals(format, Locale.getDefault(Locale.Category.FORMAT))
            applyUiLocale("en")
            assertEquals("en", Locale.getDefault().language)
            assertEquals(format, Locale.getDefault(Locale.Category.FORMAT))
            applyUiLocale("zz")
            assertEquals("en", Locale.getDefault().language)
            assertEquals(format, Locale.getDefault(Locale.Category.FORMAT))
        } finally {
            Locale.setDefault(original)
            Locale.setDefault(Locale.Category.FORMAT, format)
        }
    }
}
