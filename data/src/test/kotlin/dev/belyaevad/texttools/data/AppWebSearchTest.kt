package dev.belyaevad.texttools.data

import kotlin.test.Test
import kotlin.test.assertEquals

/** The search page is reduced to a few plain lines before it is sent to the model. */
class AppWebSearchTest {
    @Test
    fun snippetsKeepTheTitleAndTheText() {
        val html =
            """
            <a class="result__a" href="https://example.test">Windows &amp; Note</a>
            <a class="result__snippet" href="https://example.test">A plain <b>text</b> editor.</a>
            <a class="result__a" href="https://other.test">Other</a>
            <td class="result__snippet">Second hit</td>
            """.trimIndent()
        assertEquals(
            "- Windows & Note: A plain text editor.\n- Other: Second hit",
            duckSnippets(html),
        )
    }

    @Test
    fun anEmptyPageAddsNothing() {
        assertEquals("", duckSnippets("<html></html>"))
    }
}
