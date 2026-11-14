package dev.belyaevad.texttools.data

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import dev.belyaevad.texttools.data.db.TextToolsDatabase
import java.nio.file.Files
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Usage rows from an action and a profiling call must both remain. */
class StoreTest {
    @Test
    fun concurrentAppendsKeepEveryRow() {
        val file = Files.createTempFile("texttools-usage", ".db")
        val driver = JdbcSqliteDriver("jdbc:sqlite:${file.toAbsolutePath().toString().replace('\\', '/')}")
        TextToolsDatabase.Schema.create(driver)
        val store = TextToolsStore(driver)
        val start = CountDownLatch(1)
        val done = CountDownLatch(2)
        repeat(2) { thread ->
            Thread {
                start.await()
                repeat(20) { index ->
                    store.appendUsage(UsageRecord(index.toLong(), "t$thread", "m", 1, 1))
                }
                done.countDown()
            }.start()
        }
        start.countDown()
        assertTrue(done.await(10, TimeUnit.SECONDS))
        assertEquals(40, store.usage().size)
        driver.close()
        Files.deleteIfExists(file)
    }

    @Test
    fun appendLeavesStoredRowsInPlace() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        TextToolsDatabase.Schema.create(driver)
        val store = TextToolsStore(driver)
        driver.execute(null, "INSERT INTO usage_log VALUES (5, 1, 'a', 'm', 1, 1), (9, 2, 'b', 'm', 1, 1)", 0)
        store.appendUsage(UsageRecord(3, "c", "m", 1, 1))
        assertEquals(listOf("a", "b", "c"), store.usage().map { it.label })
        // A rewrite of the whole table would renumber the stored rows 0, 1, 2.
        val positions =
            driver
                .executeQuery(null, "SELECT position FROM usage_log ORDER BY position", { cursor ->
                    val found = mutableListOf<Long>()
                    while (cursor.next().value) found += cursor.getLong(0) ?: -1
                    QueryResult.Value(found)
                }, 0)
                .value
        assertEquals(listOf(5L, 9L, 10L), positions)
        driver.close()
    }

    @Test
    fun storePreservesMeasurementsWithoutText() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        TextToolsDatabase.Schema.create(driver)
        try {
            val store = TextToolsStore(driver)
            store.appendUsageEvent(event(42, "failure"))
            store.appendUsageEvent(event(43, "success"))
            val saved = store.usageEvents()
            assertEquals(2, saved.size)
            assertEquals("failure", saved[0].outcome)
            assertEquals(20, saved[0].promptTokens)
        } finally {
            driver.close()
        }
    }

    @Test
    fun aMigratedDatabaseMatchesAFreshOne() {
        val fresh = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        val migrated = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        try {
            TextToolsDatabase.Schema.create(fresh)
            // Version 1 is the current schema without usage_event: 1.sqm only adds that table.
            TextToolsDatabase.Schema.create(migrated)
            migrated.getConnection().createStatement().use { it.execute("DROP TABLE usage_event") }
            TextToolsDatabase.Schema.migrate(migrated, 1, TextToolsDatabase.Schema.version)
            assertEquals(schemaOf(fresh), schemaOf(migrated))
        } finally {
            fresh.close()
            migrated.close()
        }
    }

    private fun event(
        at: Long,
        outcome: String,
    ): UsageEvent = UsageEvent(at, "action", "m", outcome, 10, 0, 20, 30, 20, 5)

    private fun schemaOf(driver: JdbcSqliteDriver): List<String> =
        driver.getConnection().createStatement().use { statement ->
            statement.executeQuery("SELECT name, sql FROM sqlite_master ORDER BY name").use { rows ->
                buildList { while (rows.next()) add(rows.getString(1) + ": " + rows.getString(2)) }
            }
        }
}
