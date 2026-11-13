package dev.belyaevad.texttools.app.ui.run

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import dev.belyaevad.texttools.data.PriceCache
import dev.belyaevad.texttools.data.TextToolsStore
import dev.belyaevad.texttools.data.db.TextToolsDatabase
import dev.belyaevad.texttools.domain.AppProfile
import dev.belyaevad.texttools.domain.ChatMessage
import dev.belyaevad.texttools.domain.Factory
import dev.belyaevad.texttools.domain.RequestMetric
import dev.belyaevad.texttools.domain.RunOutcome
import dev.belyaevad.texttools.domain.Usage
import dev.belyaevad.texttools.platform.ClipIo
import dev.belyaevad.texttools.platform.ClipboardProtocol
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Paste, window, key, busy, and the instruction field. No secret values. */
class RunActionTest {
    @Test
    fun pasteReturnsBeforeTheClipboardRestore() =
        runBlocking {
            var now = 1_000L
            var waited = -1L
            lateinit var running: RunAction
            val scene =
                scenario(
                    clock = { now },
                    pause = { waited = it },
                    complete = { _, _ ->
                        assertTrue(running.busy)
                        assertEquals(RunPhase.Preparing, running.banner()?.phase)
                        assertEquals(true, running.banner()?.usage?.approximate)
                        assertTrue((running.banner()?.usage?.promptTokens ?: 0) > 0)
                        assertEquals(0L, running.banner()?.elapsedMillis)
                        now = 2_600L
                        assertEquals(1600L, running.banner()?.elapsedMillis)
                        "MODEL-OUT" to Usage(2, 3)
                    },
                )
            running = scene.action
            scene.use { opened ->
                opened.store.savePrices(
                    PriceCache(
                        """
                        {"data":[
                        {"id":"other","pricing":{"prompt":"9","completion":"9"}},
                        {"id":"anthropic/claude-haiku-4.5","pricing":{"prompt": "1","completion": "2"}}
                        ]}
                        """.trimIndent(),
                        0L,
                    ),
                )
                val outcome = opened.action.run(proof(), "SEL-MARKER", "", false, "", "")
                assertIs<RunOutcome.Pasted>(outcome)
                assertFalse(opened.action.busy)
                assertEquals(-1L, waited)
                assertEquals("MODEL-OUT", opened.clip.text)
                now = 9_000L
                assertEquals(RunPhase.Done, opened.action.banner()?.phase)
                val spent = opened.action.banner()?.usage
                assertEquals(2, spent?.promptTokens)
                assertEquals(3, spent?.completionTokens)
                assertEquals(8.0, spent?.cost)
                assertEquals(false, spent?.approximate)
                assertEquals(1600L, opened.action.banner()?.elapsedMillis)
                assertEquals("0.0", statusSeconds(0))
                assertEquals("1.5", statusSeconds(1_500))
                assertEquals("0.0", statusSeconds(-5))
                val row = opened.store.history().single()
                assertEquals("Proofread", row.actionName)
                assertEquals("SEL-MARKER", row.source)
                assertEquals("MODEL-OUT", row.result)
                assertEquals(Factory.DEFAULT_MODEL, row.model)
                assertEquals(2, row.promptTokens)
                assertEquals(3, row.completionTokens)
                assertEquals(8.0, row.cost)
                val spend = opened.store.usage().single()
                assertEquals("Proofread", spend.label)
                assertEquals(2, spend.promptTokens)
                assertEquals(3, spend.completionTokens)
                assertEquals("MODEL-OUT", Files.readString(opened.dir.resolve("TextTools_last.txt")))
                val log = Files.readString(opened.dir.resolve("TextTools.log"))
                assertTrue("action: Proofread, selected" in log)
                assertTrue("spend: prompt 2, completion 3" in log)
                assertFalse("SEL-MARKER" in log)
                assertFalse("(+instruction)" in log)
                assertTrue(opened.queued.isEmpty())
                assertEquals("MODEL-OUT", opened.clip.text)
            }
        }

    @Test
    fun emptyAnswerAndErrorsStayOutOfHistory() =
        runBlocking {
            var mode = "blank"
            val seen = mutableListOf<RunPhase?>()
            val scene =
                scenario(
                    onStatus = { seen += it?.phase },
                    complete = { _, _ ->
                        if (mode == "blank") " \n" to Usage(9, 9) else error("boom")
                    },
                )
            scene.use { opened ->
                val blank = opened.action.run(proof(), "SEL-MARKER", "", false, "", "")
                assertIs<RunOutcome.Failed>(blank)
                mode = "boom"
                val failed = opened.action.run(proof(), "SEL-MARKER", "", false, "", "")
                assertIs<RunOutcome.Failed>(failed)
                assertEquals("boom", failed.message)
                assertEquals(0, opened.store.history().size)
                assertEquals(0, opened.store.usage().size)
                assertEquals(0, opened.clip.pastes)
                assertEquals("saved", opened.clip.text)
                assertFalse(Files.exists(opened.dir.resolve("TextTools_last.txt")))
                assertNull(opened.action.banner())
                assertFalse(RunPhase.Done in seen)
                val log = opened.dir.resolve("TextTools.log")
                if (Files.exists(log)) {
                    val text = Files.readString(log)
                    assertFalse("action:" in text)
                    assertFalse("SEL-MARKER" in text)
                }
            }
        }

    @Test
    fun missingKeyDoesNotCallTheModel() =
        runBlocking {
            var calls = 0
            val scene =
                scenario(
                    hasKey = { false },
                    complete = { _, _ ->
                        calls += 1
                        "x" to null
                    },
                )
            scene.use { opened ->
                val outcome = opened.action.run(proof(), "Hi", "extra", false, "chrome.exe", "Jira")
                assertIs<RunOutcome.NeedsKey>(outcome)
                assertEquals(0, calls)
                assertEquals(0, opened.store.history().size)
                assertEquals(0, opened.clip.pastes)
                assertTrue(opened.queued.isEmpty())
                assertNull(opened.action.banner())
                assertFalse(opened.action.busy)
            }
        }

    @Test
    fun shiftOpenInWindowAndFailedPasteShowAWindow() =
        runBlocking {
            var n = 0
            val scene =
                scenario(
                    complete = { _, _ ->
                        n += 1
                        "out-$n" to Usage(1, 1)
                    },
                )
            scene.use { opened ->
                val shift = opened.action.run(proof(), "SECRET-SEL", "", true, "", "")
                assertIs<RunOutcome.InWindow>(shift)
                val summary = Factory.actions.first { it.factoryId == "summary" }
                assertTrue(summary.openInWindow)
                val window = opened.action.run(summary, "SECRET-SEL", "", false, "", "")
                assertIs<RunOutcome.InWindow>(window)
                opened.clip.canActivate = false
                val failedPaste = opened.action.run(proof(), "SECRET-SEL", "", false, "", "")
                assertIs<RunOutcome.InWindow>(failedPaste)
                assertEquals(0, opened.clip.pastes)
                assertTrue(opened.queued.isEmpty())
                assertEquals("saved", opened.clip.text)
                assertEquals(listOf("out-3", "out-2", "out-1"), opened.store.history().map { it.result })
                val log = Files.readString(opened.dir.resolve("TextTools.log"))
                assertTrue("action: Proofread (Shift: into window), selected" in log)
                assertTrue("action: Summary, selected" in log)
                assertFalse("action: Summary (Shift" in log)
                assertFalse("SECRET-SEL" in log)
            }
        }

    @Test
    fun enterRunsCustomOnlyWhenTheFieldHasText() =
        runBlocking {
            var calls = 0
            var user = ""
            var system = ""
            val scene =
                scenario(
                    complete = { _, messages ->
                        calls += 1
                        system = messages.first { it.role == "system" }.content
                        user = messages.first { it.role == "user" }.content
                        "custom-out" to Usage(4, 5)
                    },
                )
            scene.use { opened ->
                assertNull(opened.action.enter(" \t", "Hello", false, "Chrome.exe", "Ticket in Jira"))
                assertEquals(0, calls)
                val shown = opened.action.enter(" shorter ", "Hello", true, "Chrome.exe", "Ticket in Jira")
                assertIs<RunOutcome.InWindow>(shown)
                assertEquals(1, calls)
                assertEquals(0, opened.clip.pastes)
                assertTrue("The change to make: shorter" in user)
                assertTrue("Apply the user's instruction" in user)
                assertTrue("issue keys" in system)
                assertEquals(
                    "Custom",
                    opened.store
                        .history()
                        .single()
                        .actionName,
                )
                opened.action.run(proof(), "Hello", "keep names", false, "", "")
                assertTrue("Additional instruction from the user" in user)
                assertTrue("keep names" in user)
                assertFalse("The change to make" in user)
                assertEquals(
                    "Proofread",
                    opened.store
                        .history()
                        .first()
                        .actionName,
                )
            }
        }

    @Test
    fun aStoredProfileWinsAndAnEmptyTableUsesTheFactory() =
        runBlocking {
            var system = ""
            val scene =
                scenario(
                    complete = { _, messages ->
                        system = messages.first { it.role == "system" }.content
                        "out" to null
                    },
                )
            scene.use { opened ->
                opened.action.run(proof(), "Hello", "", false, "chrome.exe", "Jira")
                assertTrue("issue keys" in system)
                assertEquals("Jira (в браузере)", opened.action.banner()?.source)
                opened.store.saveApps(listOf(AppProfile("note.exe", "Mine", "Note", "", "STORED HINT")))
                opened.action.run(proof(), "Hello", "", false, "NOTE.EXE", "x Mine y")
                assertTrue("STORED HINT" in system)
                assertEquals("Note", opened.action.banner()?.source)
                opened.action.run(proof(), "Hello", "", false, "chrome.exe", "Jira")
                assertFalse("SOURCE APPLICATION:" in system)
                assertFalse("issue keys" in system)
                assertEquals(null, opened.action.banner()?.source)
                assertEquals(0, opened.store.usage().size)
            }
        }

    @Test
    fun aSecondCallWhileBusyReturnsBusy() =
        runBlocking {
            val started = CompletableDeferred<Unit>()
            val release = CompletableDeferred<Unit>()
            var calls = 0
            lateinit var running: RunAction
            val scene =
                scenario(
                    complete = { _, _ ->
                        calls += 1
                        started.complete(Unit)
                        release.await()
                        "done" to Usage(1, 1)
                    },
                )
            running = scene.action
            scene.use { opened ->
                val first = async { opened.action.run(proof(), "Hi", "", false, "", "") }
                started.await()
                assertTrue(running.busy)
                assertEquals(RunPhase.Preparing, running.banner()?.phase)
                assertNull(opened.action.enter("  ", "Hi", false, "", ""))
                assertIs<RunOutcome.Busy>(opened.action.enter("go", "Hi", false, "", ""))
                assertIs<RunOutcome.Busy>(opened.action.run(proof(), "Hi", "", false, "", ""))
                assertEquals(1, calls)
                assertEquals(RunPhase.Preparing, running.banner()?.phase)
                release.complete(Unit)
                assertIs<RunOutcome.Pasted>(first.await())
                assertFalse(running.busy)
                assertEquals(RunPhase.Done, running.banner()?.phase)
                assertEquals(1, opened.store.history().size)
                assertEquals(
                    "Proofread",
                    opened.store
                        .history()
                        .single()
                        .actionName,
                )
                assertEquals(1, calls)
            }
        }

    @Test
    fun measuredInputIsTheSelectionNotTheActionPrefix() =
        runBlocking {
            val seen = mutableListOf<RequestMetric>()
            val scene = scenario(complete = { _, _ -> "ok" to null }, onRequest = { seen += it })
            scene.use { opened ->
                opened.action.run(proof(), "SEL", "NOTE", false, "", "")
            }
            val metric = seen.single()
            assertEquals(3, metric.inputChars)
            assertEquals(4, metric.extraChars)
            assertEquals("success", metric.outcome)
            assertTrue(metric.inputChars < proof().prefix.length)
        }
}

private fun proof() = Factory.actions.first { it.factoryId == "proofread" }

private class Scene(
    val action: RunAction,
    val clip: MemoryClip,
    val store: TextToolsStore,
    val driver: JdbcSqliteDriver,
    val dir: Path,
    val queued: MutableList<suspend () -> Unit>,
) : AutoCloseable {
    override fun close() {
        action.close()
        driver.close()
        if (Files.exists(dir)) {
            Files.walk(dir).use { walk ->
                walk.sorted(compareByDescending { it.nameCount }).forEach { Files.deleteIfExists(it) }
            }
        }
    }
}

private fun scenario(
    complete: suspend (String, List<ChatMessage>) -> Pair<String, Usage?>,
    hasKey: () -> Boolean = { true },
    clock: () -> Long = { 0L },
    pause: suspend (Long) -> Unit = { _ -> },
    onStatus: (RunStatus?) -> Unit = { _ -> },
    onRequest: (RequestMetric) -> Unit = {},
    clip: MemoryClip = MemoryClip(),
): Scene {
    val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    TextToolsDatabase.Schema.create(driver)
    val store = TextToolsStore(driver)
    val dir = Files.createTempDirectory("texttools-r1-")
    val queued = mutableListOf<suspend () -> Unit>()
    val action =
        RunAction(
            complete = complete,
            clipboard = ClipboardProtocol(clip),
            store = store,
            dataDir = dir,
            settings = { Factory.defaultSettings },
            hasKey = hasKey,
            hooks =
                RunHooks(
                    clock = clock,
                    schedule = { block -> queued += block },
                    pause = pause,
                    onStatus = onStatus,
                ),
            onRequest = onRequest,
        )
    return Scene(action, clip, store, driver, dir, queued)
}

private class MemoryClip : ClipIo {
    var text: String? = "saved"
    var canActivate = true
    var pastes = 0

    override fun readText(): String? = text

    override fun writeText(text: String): Boolean {
        this.text = text
        return true
    }

    override fun sendCopy() = Unit

    override fun sendPaste() {
        pastes += 1
    }

    override fun activateSource(): Boolean = canActivate
}
