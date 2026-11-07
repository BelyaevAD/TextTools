package dev.belyaevad.texttools.domain

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Behavior carried over from tests-logic.ahk. There is no older Kotlin suite. */
class BehaviorTest {
    @Test
    fun promptOrderPutsBrevityAfterTheTask() {
        val proof = Factory.actions.first { it.factoryId == "proofread" }
        val text = buildInstruction(proof, Factory.languages, "jira hint")
        val brevity = text.indexOf("BREVITY:")
        val task = text.indexOf("TASK:")
        val context = text.indexOf("CONTEXT:")
        val source = text.indexOf("SOURCE APPLICATION:")
        assertTrue(context < source && source < task && task < brevity)
        assertTrue(text.indexOf("STYLE:") in 0 until task)
        val russian = buildInstruction(Factory.actions.first { it.factoryId == "to_ru" }, Factory.languages, "")
        assertTrue(russian.indexOf("STYLE (Russian)") < russian.indexOf("TASK:"))
        val noStyle = buildInstruction(proof.copy(styleOn = false), Factory.languages, "")
        assertTrue("STYLE" !in noStyle)
        assertTrue("SOURCE APPLICATION" !in noStyle)
    }

    @Test
    fun yieldTakeClearsTheOccupantAndSwapMovesTheOldKey() {
        val rows = Factory.actions
        val proof = rows.indexOfFirst { it.factoryId == "proofread" }
        val taken = yieldMenuKey(rows, proof, rows[1].menuKey, KeyConflict.Take)
        assertEquals(null, taken?.get(proof)?.menuKey)
        val swapped = yieldMenuKey(rows, proof, "q", KeyConflict.Swap)
        assertEquals("q", swapped?.get(proof)?.menuKey)
        assertNull(yieldMenuKey(rows, -1, "1", KeyConflict.Take))
        assertNull(yieldMenuKey(rows, proof, "1", KeyConflict.Cancel))
    }

    @Test
    fun proxyRules() {
        assertEquals("", effectiveProxy("http://p:1", false))
        assertEquals("http://p:1", effectiveProxy("http://p:1", null))
        assertEquals("socks5h://h:1", normalizeProxy("socks5h://h:1"))
        assertEquals("http://h:1", normalizeProxy("h:1"))
        val error = assertFailsWith<IllegalArgumentException> { normalizeProxy("ftp://h") }
        assertTrue(error.message!!.contains("socks5h://"))
        assertTrue(isLoopback("http://127.0.0.1:11434/v1"))
        assertEquals(0.2, clipWaitSeconds(0.01))
        assertEquals(5.0, clipWaitSeconds(9.0))
    }

    @Test
    fun modelListBelongsToTheAddressAndRetargetSkipsCopies() {
        val replaced = replaceModels(listOf("b", "a", "whisper-1"))
        assertEquals(listOf("a", "b"), replaced.map { it.id })
        assertTrue(replaced.none { it.selected })
        val merged = mergeModels(listOf(KnownModel("kept", true), KnownModel("old", false)), listOf("new"))
        assertEquals(true, merged.first { it.id == "kept" }.selected)
        assertTrue(merged.none { it.id == "old" })
        providerUrls().forEach { (url, key) -> assertEquals(key, providerOf(url)) }
        assertEquals("ollama", providerOf("http://127.0.0.1:11434/v1"))
        val copy = duplicateFactory(Factory.actions.first())
        assertNull(copy.factoryId)
        val retargeted = retarget(Factory.actions + copy.copy(model = "mine"), "https://api.openai.com/v1")
        assertEquals("gpt-4.1-nano", retargeted.first { it.factoryId == "proofread" }.model)
        assertNull(retargeted.first { it.factoryId == "rewrite" }.model)
        assertEquals("mine", retargeted.last().model)
    }

    @Test
    fun pricesDoNotBorrowANeighbourAndHistoryKeepsThePipe() {
        val priced = CatalogEntry("m", 0.001, 0.002, 1000)
        val bare = CatalogEntry("m", null, 0.002, null)
        assertEquals(0.001 * 10 + 0.002 * 5, costOf(priced, 10, 5))
        assertNull(costOf(bare, 10, 5))
        assertNull(contextLength(bare))
        val row = HistoryEntry(0, "A|B", "model", 1, 2, "s", "r")
        assertEquals("A|B", appendHistory(emptyList(), row).single().actionName)
        assertEquals(TEXT_LIMIT, trimStored("x".repeat(9000)).length)
    }

    @Test
    fun conversationTailStartsWithUser() {
        val messages =
            listOf(ChatMessage("system", "s")) +
                (1..10).map { ChatMessage(if (it % 2 == 0) "assistant" else "user", "$it") }
        val trimmed = trimConversation(messages)
        assertEquals("system", trimmed.first().role)
        assertEquals("user", trimmed[1].role)
        val short = messages.take(9)
        assertEquals(short, trimConversation(short))
    }

    @Test
    fun markdownSubset() {
        val table = parseMarkdown("| a | b |\n|---|---|\n| 1 | 2 |")
        assertIs<MdBlock.Table>(table.single())
        val notTable = parseMarkdown("| a | b |\n| 1 | 2 |")
        assertTrue(notTable.none { it is MdBlock.Table })
        val code = parseMarkdown("```\n**не жирный** < >\n```")
        val block = assertIs<MdBlock.Code>(code.single())
        assertTrue(block.text.contains("**не жирный**"))
        assertIs<MdBlock.Code>(parseMarkdown("```\nabc").single())
        assertEquals(2, parseMarkdown("- раз\n- два\n\n1. один\n2. два").size)
    }

    @Test
    fun runnerHoldsBusyAndRejectsEmpty() =
        runBlocking {
            val empty =
                ActionRunner(
                    model = { _, _ -> "  " to Usage(1, 1) },
                    deliver = { _, _ -> true },
                )
            val action = Factory.actions.first()
            assertIs<RunOutcome.Failed>(
                empty.run(action, "hi", "", Factory.defaultSettings, Factory.languages, "", true, false),
            )
            assertEquals(false, empty.busy)
            var calls = 0
            val ok =
                ActionRunner(
                    model = { _, _ ->
                        calls += 1
                        "done" to Usage(2, 3)
                    },
                    deliver = { _, delivery -> delivery == Delivery.Paste },
                )
            assertIs<RunOutcome.Pasted>(
                ok.run(action, "hi", "", Factory.defaultSettings, Factory.languages, "", true, false),
            )
            val window = ok.run(action, "hi", "", Factory.defaultSettings, Factory.languages, "", true, true)
            assertIs<RunOutcome.InWindow>(window)
            assertIs<RunOutcome.NeedsKey>(
                ok.run(action, "hi", "", Factory.defaultSettings, Factory.languages, "", false, false),
            )
            assertEquals(2, calls)
        }

    @Test
    fun factoryCarriesAhkIconsPromptsAndProfiles() {
        val proof = Factory.actions.first { it.factoryId == "proofread" }
        val summary = Factory.actions.first { it.factoryId == "summary" }
        val concise = Factory.actions.first { it.factoryId == "concise" }
        assertEquals("\u270F", proof.icon)
        assertTrue(proof.icon.isNotEmpty())
        assertTrue(proof.task.lines().size > 1)
        assertEquals(23, Factory.apps.size)
        assertNull(summary.role)
        assertEquals("mech", concise.role)
        assertEquals("mech", proof.role)
        assertEquals(
            listOf("keypoints", "table", "spec", "code", "questions", "reply", "reqreview"),
            Factory.actions.filter { it.role == "deep" }.map { it.factoryId },
        )
        assertTrue((Factory.actions + Factory.custom).all { it.model == null })
    }

    @Test
    fun domainGradleDoesNotDependOnUiOrStorage() {
        val text = java.io.File("build.gradle.kts").readText()
        assertTrue("ktor" !in text)
        assertTrue("sqldelight" !in text)
        assertTrue("compose" !in text)
    }
}
