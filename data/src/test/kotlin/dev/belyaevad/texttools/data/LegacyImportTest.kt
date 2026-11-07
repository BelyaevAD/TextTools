package dev.belyaevad.texttools.data

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import dev.belyaevad.texttools.data.db.TextToolsDatabase
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Import must not persist the API key. No earlier data test exists. */
class LegacyImportTest {
    @Test
    fun keyIsNotWrittenIntoActions() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        TextToolsDatabase.Schema.create(driver)
        val store = TextToolsStore(driver)
        val root = Files.createTempDirectory("texttools-import").toFile()
        java.io.File(root, "actions.ini").writeText(
            """
            [action1]
            id=proofread
            name=Вычитать
            task=TASK
            key=1
            model=-
            """.trimIndent(),
        )
        java.io.File(root, "TextTools.ini").writeText(
            """
            [api]
            api_key=secret-key
            proxy=socks5h://user:pass@127.0.0.1:1
            """.trimIndent(),
        )
        val imported = LegacyImport(store).importFolder(root)
        assertEquals("secret-key", imported.apiKey)
        val saved = store.actions().joinToString { it.name + it.task + it.model.orEmpty() }
        assertTrue("secret-key" !in saved)
        assertTrue("socks5h" !in saved)
    }
}
