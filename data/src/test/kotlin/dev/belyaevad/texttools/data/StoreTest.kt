package dev.belyaevad.texttools.data

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
}
