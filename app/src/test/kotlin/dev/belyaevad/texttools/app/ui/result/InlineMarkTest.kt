package dev.belyaevad.texttools.app.ui.result

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Inline markers that are not markup stay literal text. */
class InlineMarkTest {
    @Test
    fun loneAsterisksAndUnderscoresStayText() {
        assertNull(markAt("2 * 3 * 4", 2))
        assertNull(markAt("*.kt and *.xml", 0))
        assertNull(markAt("call __init__ first", 5))
        assertEquals(InlineKind.Italic, markAt("*курсив*", 0)?.kind)
        assertEquals(InlineKind.Bold, markAt("**жирный**", 0)?.kind)
    }
}
