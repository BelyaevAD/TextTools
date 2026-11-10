package dev.belyaevad.texttools.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Parsing and storage of an automatically added application profile. */
class AppProfilingTest {
    @Test
    fun unknownKeepsTheProfileNeutral() {
        assertNull(parseAppDescription("UNKNOWN"))
        assertNull(parseAppDescription("UNKNOWN\nDESC: invented"))
        assertNull(parseAppDescription("not the expected shape"))
        assertNull(parseAppDescription("   "))
    }

    @Test
    fun twoLinesBecomeTheDescriptionAndTheHint() {
        val parsed =
            parseAppDescription(
                "DESC: Редактор кода.\nHINT: Keep identifiers byte-for-byte.",
            )
        assertEquals("Редактор кода.", parsed?.description)
        assertEquals("Keep identifiers byte-for-byte.", parsed?.hint)
    }

    @Test
    fun webNotesAreOmittedWhenTheSearchIsEmpty() {
        val bare = profileMessages("note.exe", "", "")
        assertTrue("Web notes:" !in bare[1].content)
        assertTrue("Window title: (none)" in bare[1].content)
        val noted = profileMessages("note.exe", "Note", "- Note: a text editor")
        assertTrue("Web notes:\n- Note: a text editor" in noted[1].content)
        assertTrue("Window class: Notepad" in profileMessages("note.exe", "Note", "", "Notepad")[1].content)
    }

    @Test
    fun openRouterSearchesAndABlankChoiceUsesTheDefaultModel() {
        assertEquals("anthropic/claude-haiku-4.5", profilingModel("", "anthropic/claude-haiku-4.5"))
        assertEquals("openai/gpt-4.1-mini", profilingModel(" openai/gpt-4.1-mini ", "other"))
        assertEquals(
            "openai/gpt-4.1-mini:online",
            searchModel("openai/gpt-4.1-mini", "https://openrouter.ai/api/v1"),
        )
        assertEquals(
            "openai/gpt-4.1-mini:online",
            searchModel("openai/gpt-4.1-mini:online", "https://openrouter.ai/api/v1"),
        )
        assertEquals("qwen3:4b", searchModel("qwen3:4b", "http://127.0.0.1:11434/v1"))
    }

    @Test
    fun aKnownProcessIsNotAddedTwice() {
        val apps = listOf(AppProfile("note.exe", "", "Note", "editor", "keep lines"))
        assertEquals(apps, withNeutralApp(apps, "NOTE.EXE", "Note", "Добавлено автоматически"))
        val added = withNeutralApp(apps, "paint.exe", "Paint", "Добавлено автоматически")
        assertEquals("paint.exe", added.last().proc)
        assertEquals("", added.last().title)
        assertEquals("", added.last().hint)
        val filled =
            applyAppDescription(
                added,
                "paint.exe",
                AppDescription("Графический редактор.", "Keep short labels."),
            )
        assertEquals("Графический редактор.", filled.last().description)
        assertEquals("Keep short labels.", filled.last().hint)
        assertEquals("keep lines", filled.first().hint)
    }

    @Test
    fun anAutomaticRowStaysPendingUntilItIsSettled() {
        val pending = listOf(AppProfile("paint.exe", "", "paint.exe", "Added automatically", ""))
        assertTrue(profilePending(pending, "paint.exe", "Paint", "Added automatically"))
        assertEquals(false, profilePending(pending, "paint.exe", "Paint", "Factory"))
        val named = listOf(AppProfile("notepad.exe", "", "Notepad", "notes", ""))
        assertEquals(false, profilePending(named, "notepad.exe", "notes.txt", "Added automatically"))
        val settled = settleUnidentified(pending, "paint.exe", "Not identified")
        assertEquals("Not identified", settled.single().description)
        assertEquals(false, profilePending(settled, "paint.exe", "Paint", "Added automatically"))
    }
}
