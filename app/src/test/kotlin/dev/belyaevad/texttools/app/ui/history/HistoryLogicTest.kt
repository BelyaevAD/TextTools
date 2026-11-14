package dev.belyaevad.texttools.app.ui.history

import dev.belyaevad.texttools.app.ui.settings.catalogRow
import dev.belyaevad.texttools.data.UsageEvent
import dev.belyaevad.texttools.data.UsageRecord
import dev.belyaevad.texttools.domain.CatalogEntry
import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDate
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** History cells, the usage report, and the two clear operations. */
class HistoryLogicTest {
    @Test
    fun todayOmitsTheDate() {
        val zone = ZoneId.of("UTC")
        val today = LocalDate.of(2026, 11, 7)
        val noon =
            today
                .atTime(19, 16)
                .atZone(zone)
                .toInstant()
                .toEpochMilli()
        val yesterday =
            LocalDate
                .of(2026, 11, 6)
                .atTime(8, 5)
                .atZone(zone)
                .toInstant()
                .toEpochMilli()
        assertEquals("19:16", historyClock(noon, zone, today))
        assertEquals("06.11 08:05", historyClock(yesterday, zone, today))
    }

    @Test
    fun cellsMatchTheOldFormats() {
        assertEquals("a b", historySnip(" \ra\nb "))
        assertEquals("a".repeat(60), historySnip("a".repeat(60)))
        assertEquals("a".repeat(60) + "…", historySnip("a".repeat(61)))
        assertEquals("—", historyTokens(null, null))
        assertEquals("—", historyTokens(0, 0))
        assertEquals("128", historyTokens(100, 28))
        assertEquals("1 234 567", historyTokens(1_234_567, 0))
        assertEquals("—", historyMoney(null))
        assertEquals("—", historyMoney(-1.0))
        assertEquals("$2.50", historyMoney(2.5))
        assertEquals("$0.500", historyMoney(0.5))
        assertEquals("$0.00005", historyMoney(0.0000512))
        assertEquals("$0.00000", historyMoney(0.0))
        assertEquals("—", historyModel(""))
        assertEquals("m", historyModel("m"))
    }

    @Test
    fun actionRunsKeepACommaInTheName() {
        val log =
            listOf(
                "action: Translate, formal, selected 3 characters, model m",
                "action: Translate, casual (quick), model m",
                "action: Rewrite, model style, selected 3 characters, model m",
            ).joinToString("\n")
        assertEquals(
            listOf(
                ActionRun("Translate, formal", 1),
                ActionRun("Translate, casual (quick)", 1),
                ActionRun("Rewrite, model style", 1),
            ),
            actionRuns(log),
        )
    }

    @Test
    fun aPriceIsFoundAfterTheProviderChanges() {
        val catalog = listOf(CatalogEntry("openai/gpt-4.1-mini", 0.001, 0.002, 1000))
        assertNotNull(catalogRow(catalog, "gpt-4.1-mini", "https://openrouter.ai/api/v1"))
        assertNull(catalogRow(catalog, "gpt-5", "https://openrouter.ai/api/v1"))
        assertNull(catalogRow(catalog, "gpt-4.1-mini", "http://localhost:1234/v1"))
    }

    @Test
    fun actionRunsDropTheInstructionMarker() {
        val log =
            listOf(
                "2026-11-07 19:16:01  action: Summary (+instruction) (Shift: into window), selected 3 characters, model m",
                "x action: Rewrite, selected 10 characters",
                "action: Rewrite (custom), selected 1",
                "action finished",
            ).joinToString("\n")
        assertEquals(
            listOf(ActionRun("Rewrite", 2), ActionRun("Summary (Shift: into window)", 1)),
            actionRuns(log),
        )
    }

    @Test
    fun unknownPriceAddsAPlus() {
        val text =
            statsDocument(
                log = "action: Summary, selected 1",
                usage = listOf(UsageRecord(1L, "Summary", "m", 100, 28)),
                phrases = phrases(),
                price = { _, _, _ -> null },
            )
        assertTrue("| m | 1 | 100 | 28 | 128 | price unknown |" in text)
        assertTrue("| **Total** | **1** | **100** | **28** | **128** | **${historyMoney(0.0)} +** |" in text)
        assertTrue("> estimate catalogue invoice\n\n> marked with a plus\n" in text)
    }

    @Test
    fun knownPriceHasNoPlus() {
        val text =
            statsDocument(
                log = "action: Summary, selected 1",
                usage =
                    listOf(
                        UsageRecord(1L, "a", "small", 1, 1),
                        UsageRecord(2L, "b", "big", 1000, 500),
                        UsageRecord(3L, "b", "big", 500, 100),
                    ),
                phrases = phrases("downloaded"),
                price = { model, _, _ -> if (model == "big") 1.25 else 0.5 },
            )
        val big = text.indexOf("| big |")
        val small = text.indexOf("| small |")
        assertTrue(big in 0 until small)
        assertTrue("| big | 2 | 1 500 | 600 | 2 100 | ${historyMoney(1.25)} |" in text)
        assertTrue("| **Total** | **3** | **1 501** | **601** | **2 102** | **${historyMoney(1.75)}** |" in text)
        assertFalse(" +" in text)
        assertFalse("price unknown" in text)
        assertFalse("marked with a plus" in text)
        assertTrue("> estimate catalogue downloaded invoice\n" in text)
    }

    @Test
    fun clearHistoryLeavesTheLastResult() {
        withDir { dir ->
            Files.writeString(dir.resolve("TextTools_history.txt"), "row")
            Files.writeString(dir.resolve("TextTools_last.txt"), "keep")
            Files.writeString(dir.resolve("TextTools.log"), "log")
            clearHistoryFile(dir)
            assertFalse(Files.exists(dir.resolve("TextTools_history.txt")))
            assertEquals("keep", Files.readString(dir.resolve("TextTools_last.txt")))
            assertEquals("log", Files.readString(dir.resolve("TextTools.log")))
        }
    }

    @Test
    fun clearLogDeletesTheOldFile() {
        withDir { dir ->
            Files.writeString(dir.resolve("TextTools.log"), "log")
            Files.writeString(dir.resolve("TextTools.log.old"), "old")
            Files.writeString(dir.resolve("TextTools_last.txt"), "keep")
            Files.writeString(dir.resolve("TextTools_usage.log"), "usage")
            clearLogFiles(dir)
            assertFalse(Files.exists(dir.resolve("TextTools.log")))
            assertFalse(Files.exists(dir.resolve("TextTools.log.old")))
            assertEquals("keep", Files.readString(dir.resolve("TextTools_last.txt")))
            assertEquals("usage", Files.readString(dir.resolve("TextTools_usage.log")))
        }
    }

    @Test
    fun usageReportUsesMillisecondsAndLeavesUnknownTokensBlank() {
        val text =
            usageDocument(
                listOf(UsageEvent(0, "action", "m", "success", 1, 0, 1000, 1500, null, null)),
                phrases(),
            )
        assertTrue("| 1.5 s | 1 500 | 1 500 | — |" in text)
        assertTrue("1970-01-01" in text)
        assertFalse("1.5 s | 1.5 s" in text)
    }

    @Test
    fun clearOnAnEmptyDirectoryDoesNothing() {
        withDir { dir ->
            clearHistoryFile(dir)
            clearLogFiles(dir)
            assertTrue(Files.isDirectory(dir))
        }
    }
}

private fun phrases(stamp: String = ""): StatsPhrases =
    StatsPhrases(
        heading = "Usage statistics",
        runsTitle = "Action runs",
        byLog = "From TextTools.log.",
        actionColumn = "Action",
        runsColumn = "Runs",
        usageTitle = "Token usage",
        usageEmpty = "Empty so far",
        byModel = "Token usage by model",
        modelColumn = "Model",
        requestsColumn = "Requests",
        promptColumn = "Prompt",
        answerColumn = "Answer",
        totalColumn = "Total",
        costColumn = "Cost",
        priceUnknown = "price unknown",
        totalLabel = "Total",
        estimate = "estimate",
        catalogue = "catalogue",
        stamp = stamp,
        invoice = "invoice",
        unknownNote = "marked with a plus",
        eventTitle = "Model requests",
        failedColumn = "Failed",
        cancelledColumn = "Cancelled",
        inputColumn = "Input",
        outputColumn = "Output",
        timeColumn = "Total",
        meanColumn = "Mean",
        maxColumn = "Max",
        eventNote = "No texts stored",
        speedColumn = "Chars/s",
    )

private fun withDir(block: (Path) -> Unit) {
    val dir = Files.createTempDirectory("texttools-u7")
    try {
        block(dir)
    } finally {
        Files.list(dir).use { list -> list.forEach { Files.deleteIfExists(it) } }
        Files.deleteIfExists(dir)
    }
}
