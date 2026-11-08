package dev.belyaevad.texttools.app

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import dev.belyaevad.texttools.data.TextToolsStore
import dev.belyaevad.texttools.data.UsageEvent
import dev.belyaevad.texttools.data.db.TextToolsDatabase
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UsageTelemetryTest {
    @Test
    fun completedHoursGroupByModelAndOutcome() {
        val rows =
            listOf(
                event(0, "m", "success", input = 100, output = 200, duration = 100),
                event(100, "m", "success", input = 200, output = 300, duration = 200),
                event(200, "m", "failure", input = 300, output = 0, duration = 30),
                event(TELEMETRY_HOUR_MS, "other", "success", input = 400, output = 50, duration = 40),
            )
        val batches = usageBatches(rows, TELEMETRY_HOUR_MS + 1)
        assertEquals(2, batches.size)
        val success = batches.single { it.outcome == "success" }
        assertEquals(2, success.count)
        assertEquals(300, success.durationMs)
        assertEquals(300, success.inputChars)
        assertEquals(500, success.outputChars)
        assertEquals(40, success.promptTokens)
        assertEquals(2, success.usageCount)
        assertTrue(batches.none { it.model == "other" })
    }

    @Test
    fun alreadySentEventsAreNotReused() {
        val sent = event(0, "m", "success").copy(sentryBucket = 0)
        assertTrue(usageBatches(listOf(sent), TELEMETRY_HOUR_MS * 2).isEmpty())
    }

    @Test
    fun storePreservesMeasurementsWithoutText() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        TextToolsDatabase.Schema.create(driver)
        try {
            val store = TextToolsStore(driver)
            store.appendUsageEvent(event(42, "m", "failure"))
            store.appendUsageEvent(event(43, "m", "success"))
            val saved = store.usageEvents()
            assertEquals(2, saved.size)
            assertEquals("failure", saved[0].outcome)
            assertEquals(20, saved[0].promptTokens)
            assertEquals(2, store.pendingUsageEvents().size)
            store.markUsageEventsSent(saved.map { it.id }, 0)
            assertTrue(store.pendingUsageEvents().isEmpty())
        } finally {
            driver.close()
        }
    }

    @Test
    fun closePublishesACompletedHourAndKeepsItPending() =
        runBlocking {
            sentryConfigured.set(true)
            val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
            TextToolsDatabase.Schema.create(driver)
            try {
                val store = TextToolsStore(driver)
                store.appendUsageEvent(event(0, "m", "success"))
                val seen = mutableListOf<UsageBatch>()
                val telemetry =
                    UsageTelemetry(
                        store,
                        { TelemetrySettings(traces = true, dsn = "https://example.invalid") },
                        object : UsageMetricSink {
                            override fun publish(batch: UsageBatch) {
                                seen += batch
                            }
                        },
                    )
                telemetry.close()
                assertEquals(1, seen.size)
                assertEquals(1, store.pendingUsageEvents().size)
                telemetry.close()
                assertEquals(1, seen.size)
            } finally {
                sentryConfigured.set(false)
                driver.close()
            }
        }

    @Test
    fun closeSendsNothingWhenTheSwitchIsOff() =
        runBlocking {
            sentryConfigured.set(true)
            val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
            TextToolsDatabase.Schema.create(driver)
            try {
                val store = TextToolsStore(driver)
                store.appendUsageEvent(event(0, "m", "success"))
                var sent = 0
                UsageTelemetry(
                    store,
                    { TelemetrySettings(traces = false, dsn = "https://example.invalid") },
                    object : UsageMetricSink {
                        override fun publish(batch: UsageBatch) {
                            sent += 1
                        }
                    },
                ).close()
                assertEquals(0, sent)
                assertEquals(1, store.pendingUsageEvents().size)
            } finally {
                sentryConfigured.set(false)
                driver.close()
            }
        }
}

private fun event(
    at: Long,
    model: String,
    outcome: String,
    input: Int = 10,
    output: Int = 20,
    duration: Long = 30,
): UsageEvent = UsageEvent(at, "action", model, outcome, input, 0, output, duration, 20, 5)
