package dev.belyaevad.texttools.data

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import dev.belyaevad.texttools.data.db.TextToolsDatabase
import dev.belyaevad.texttools.domain.Factory
import dev.belyaevad.texttools.domain.KnownModel
import dev.belyaevad.texttools.domain.OutputLanguage
import dev.belyaevad.texttools.domain.QuickRefine
import java.io.File
import java.nio.file.Files
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Import reads a fixture folder. The API key is returned to the caller and is not stored. */
class LegacyImportTest {
    @Test
    fun keyIsAbsentFromTheDatabaseFile() {
        val root = Files.createTempDirectory("tt-root").toFile()
        val dataDir = Files.createTempDirectory("tt-data").toFile()
        val db = Files.createTempFile("tt", ".db").toFile()
        writeRich(root, apiKey = FIXTURE_KEY, proxy = FIXTURE_PROXY)
        val before = snapshot(root)
        val driver = JdbcSqliteDriver(sqliteUrl(db))
        val connection = driver.getConnection()
        connection.createStatement().use { it.execute("PRAGMA journal_mode=DELETE") }
        TextToolsDatabase.Schema.create(driver)
        try {
            val store = TextToolsStore(driver)
            val imported = LegacyImport(store).importFolder(root, dataDir)
            assertTrue(imported.apiKey == FIXTURE_KEY)
            assertTrue(imported.proxyUrl == FIXTURE_PROXY)
            assertTrue(imported.proxyOn)
            assertEquals(ACTION_NAME, store.actions().single().name)
            assertEquals("line1\nline2", store.actions().single().task)
            assertEquals("note.exe", store.apps().single().proc)
        } finally {
            connection.close()
            driver.close()
        }
        assertEquals(before, snapshot(root))
        val stored = dbBlob(db).toString(Charsets.ISO_8859_1)
        assertTrue("note.exe" in stored)
        assertFalse(FIXTURE_KEY in stored)
        assertFalse(FIXTURE_PROXY in stored)
    }

    @Test
    fun unescapesNamesAndPrompts() {
        val root = Files.createTempDirectory("tt-esc").toFile()
        val dataDir = Files.createTempDirectory("tt-esc-data").toFile()
        writeRich(root, apiKey = "local", proxy = "http://127.0.0.1:9")
        val (driver, store) = memoryStore()
        try {
            LegacyImport(store).importFolder(root, dataDir)
            val action = store.actions().single()
            assertEquals(ACTION_NAME, action.name)
            assertEquals("a\r\nb", action.prefix)
            assertEquals("line1\nline2", action.task)
            assertEquals("\uD83D\uDE00", action.icon)
            assertEquals(null, action.factoryId)
            assertEquals(null, action.menuKey)
            assertEquals(null, action.model)
            assertFalse(action.styleOn)
            assertTrue(action.openInWindow)
            assertEquals(listOf(QuickRefine("Short", "Cut\nit")), store.quick())
            val app = store.apps().single()
            assertEquals("note.exe", app.proc)
            assertEquals("", app.title)
            assertEquals("Browser\nX", app.name)
            assertEquals("", app.description)
            assertEquals("", app.hint)
            assertEquals(OutputLanguage("de", "Deutsch", "German\nformal", false), store.languages().single())
            val history = store.history()
            assertEquals("New", history[0].actionName)
            assertEquals("a|b\nZ", history[0].result)
            assertEquals(12, history[0].promptTokens)
            assertEquals(0, history[0].completionTokens)
            assertEquals(0.5, history[0].cost)
            assertEquals("Old", history[1].actionName)
            assertEquals(null, history[1].promptTokens)
            assertEquals(null, history[1].cost)
            val at = LocalDateTime.of(2026, 1, 2, 3, 4).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            assertEquals(at, history[0].atEpochMs)
            val usage = store.usage().single()
            assertEquals("label", usage.label)
            assertEquals(3, usage.promptTokens)
            assertEquals(4, usage.completionTokens)
            assertEquals(
                listOf(KnownModel("picked", true), KnownModel("other", false)),
                store.models("https://models.example/v1"),
            )
            val pack = userPack()
            assertEquals(pack, File(dataDir, "lang/user/de.ini").readText())
            assertTrue("\\n" in pack)
            assertFalse(File(dataDir, "lang/user/README.txt").exists())
            assertFalse(File(dataDir, "lang/en.ini").exists())
            assertEquals("LAST\nline", File(dataDir, "TextTools_last.txt").readText())
        } finally {
            driver.close()
        }
    }

    @Test
    fun quickCountZeroDoesNotRestoreFactoryChips() {
        val root = Files.createTempDirectory("tt-quick0").toFile()
        File(root, "actions.ini").writeText(
            """
            [action1]
            name=Kept
            task=TASK
            [quick]
            count=0
            """.trimIndent(),
        )
        val (driver, store) = memoryStore()
        try {
            LegacyImport(store).importFolder(root, Files.createTempDirectory("tt-quick0-data").toFile())
            assertEquals("Kept", store.actions().single().name)
            assertTrue(store.quick().isEmpty())
            assertEquals("1", settingValue(driver, "quick_saved"))
            assertEquals(0, rowCount(driver, "quick_refine"))
        } finally {
            driver.close()
        }
    }

    @Test
    fun missingQuickCountKeepsFactoryChips() {
        val root = Files.createTempDirectory("tt-quick").toFile()
        File(root, "actions.ini").writeText(
            """
            [action1]
            name=Kept
            task=TASK
            """.trimIndent(),
        )
        val (driver, store) = memoryStore()
        try {
            LegacyImport(store).importFolder(root, Files.createTempDirectory("tt-quick-data").toFile())
            assertEquals(Factory.quick, store.quick())
            assertEquals(null, settingValue(driver, "quick_saved"))
            assertEquals(0, rowCount(driver, "quick_refine"))
        } finally {
            driver.close()
        }
    }

    @Test
    fun secondImportDoesNotDuplicate() {
        val root = Files.createTempDirectory("tt-twice").toFile()
        val dataDir = Files.createTempDirectory("tt-twice-data").toFile()
        writeRich(root, apiKey = "local", proxy = "http://127.0.0.1:9")
        val before = snapshot(root)
        val (driver, store) = memoryStore()
        try {
            val importer = LegacyImport(store)
            importer.importFolder(root, dataDir)
            importer.importFolder(root, dataDir)
            assertEquals(before, snapshot(root))
            assertEquals(1, store.actions().size)
            assertEquals(1, store.quick().size)
            assertEquals(1, store.apps().size)
            assertEquals(1, store.languages().size)
            assertEquals(2, store.history().size)
            assertEquals(1, store.usage().size)
            assertEquals(2, store.models("https://models.example/v1").size)
            assertEquals(listOf("de.ini"), File(dataDir, "lang/user").listFiles()?.map { it.name }?.sorted())
        } finally {
            driver.close()
        }
    }

    @Test
    fun brokenIniDoesNotWipeImportedRows() {
        val root = Files.createTempDirectory("tt-good").toFile()
        val dataDir = Files.createTempDirectory("tt-good-data").toFile()
        writeRich(root, apiKey = "local", proxy = "http://127.0.0.1:9")
        val (driver, store) = memoryStore()
        try {
            LegacyImport(store).importFolder(root, dataDir)
            val broken = Files.createTempDirectory("tt-broken").toFile()
            val junk = byteArrayOf(0xFF.toByte(), 0xFE.toByte())
            File(broken, "actions.ini").writeBytes(junk)
            File(broken, "apps.ini").writeBytes(junk)
            File(broken, "langs.ini").writeBytes(junk)
            File(broken, "TextTools.ini").writeBytes(junk)
            File(broken, "TextTools_history.txt").writeBytes(junk)
            File(broken, "TextTools_usage.log").writeBytes(junk)
            val before = snapshot(broken)
            val imported = LegacyImport(store).importFolder(broken, Files.createTempDirectory("tt-broken-data").toFile())
            assertEquals(before, snapshot(broken))
            assertTrue(imported.apiKey.isEmpty())
            assertTrue(imported.proxyUrl.isEmpty())
            assertFalse(imported.proxyOn)
            assertEquals(ACTION_NAME, store.actions().single().name)
            assertEquals(1, store.apps().size)
            assertEquals(1, store.languages().size)
            assertEquals(2, store.history().size)
            assertEquals(1, store.usage().size)
            assertEquals(2, store.models("https://models.example/v1").size)
            val garbage = Files.createTempDirectory("tt-garbage").toFile()
            File(garbage, "actions.ini").writeText("this is not an ini\n{{{")
            LegacyImport(store).importFolder(garbage, Files.createTempDirectory("tt-garbage-data").toFile())
            assertEquals(ACTION_NAME, store.actions().single().name)
        } finally {
            driver.close()
        }
    }

    @Test
    fun utf16ScriptFileIsImported() {
        val root = Files.createTempDirectory("tt-utf16").toFile()
        val dataDir = Files.createTempDirectory("tt-utf16-data").toFile()
        val body = "[action1]\r\nname=Вычитать\r\ntask=Keep\r\n".toByteArray(Charsets.UTF_16LE)
        File(root, "actions.ini").writeBytes(byteArrayOf(0xFF.toByte(), 0xFE.toByte()) + body)
        val (driver, store) = memoryStore()
        try {
            LegacyImport(store).importFolder(root, dataDir)
            assertEquals("Вычитать", store.actions().single().name)
        } finally {
            driver.close()
        }
    }
}

private const val FIXTURE_KEY: String = "tt-db7-key-9f3c2a7b"
private const val FIXTURE_PROXY: String = "socks5h://tt-db7-proxy-9f3c2a7b.invalid:9"
private const val ACTION_NAME: String = "A\"B\\C\tDE"

private fun writeRich(root: File, apiKey: String, proxy: String) {
    File(root, "actions.ini").writeText(
        """
        [action1]
        id=-
        name=A\"B\\C\tD\u0045
        icon=\uD83D\uDE00
        prefix=a\r\nb
        task=line1\nline2
        key=-
        model=-
        ru=0
        win=1
        [quick1]
        label=Short
        instr=Cut\nit
        """.trimIndent(),
    )
    File(root, "apps.ini").writeText(
        """
        [app1]
        proc=note.exe
        title=-
        name=Browser\nX
        desc=-
        hint=-
        """.trimIndent(),
    )
    File(root, "langs.ini").writeText(
        """
        [lang1]
        code=de
        name=Deutsch
        prompt=German\nformal
        on=0
        """.trimIndent(),
    )
    File(root, "TextTools.ini").writeText(
        """
        [api]
        base_url=https://models.example/v1
        api_key=$apiKey
        proxy=$proxy
        model=anthropic/claude-haiku-4.5
        models=picked
        models_known=picked, other
        models_url=https://models.example/v1
        """.trimIndent(),
    )
    File(root, "TextTools_history.txt").writeText(
        "2026-01-02 03:04|New|model-x|12|0.5|src|a\\u007Cb\\nZ\n" +
            "2026-01-01 00:00|Old|model-x|0|-1|s1|r1\n",
    )
    File(root, "TextTools_usage.log").writeText("2026-01-02 03:04:05|label|model-x|3|4\n")
    File(root, "TextTools_last.txt").writeText("LAST\nline")
    File(root, "lang/user").mkdirs()
    File(root, "lang/en.ini").writeText("[meta]\nname=English\n")
    File(root, "lang/user/README.txt").writeText("leave me")
    File(root, "lang/user/de.ini").writeText(userPack())
}

private fun userPack(): String =
    """
    [meta]
    name=Deutsch
    [ui]
    hello=Hallo\nWelt
    """.trimIndent() + "\n"

private fun memoryStore(): Pair<JdbcSqliteDriver, TextToolsStore> {
    val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    TextToolsDatabase.Schema.create(driver)
    return driver to TextToolsStore(driver)
}

private fun sqliteUrl(file: File): String = "jdbc:sqlite:${file.absolutePath.replace('\\', '/')}"

private fun snapshot(root: File): Map<String, List<Byte>> {
    val base = root.absoluteFile.toPath()
    return root.walkTopDown().filter { it.isFile }.associate { file ->
        base.relativize(file.absoluteFile.toPath()).toString() to file.readBytes().toList()
    }
}

private fun dbBlob(file: File): ByteArray {
    val out = java.io.ByteArrayOutputStream()
    for (part in listOf(file, File(file.path + "-wal"), File(file.path + "-shm"))) {
        if (part.isFile) out.write(part.readBytes())
    }
    return out.toByteArray()
}

private fun settingValue(driver: JdbcSqliteDriver, key: String): String? =
    driver.getConnection().prepareStatement("SELECT value FROM setting WHERE key = ?").use { statement ->
        statement.setString(1, key)
        statement.executeQuery().use { rows -> if (rows.next()) rows.getString(1) else null }
    }

private fun rowCount(driver: JdbcSqliteDriver, table: String): Int =
    driver.getConnection().createStatement().use { statement ->
        statement.executeQuery("SELECT COUNT(*) FROM $table").use { rows ->
            rows.next()
            rows.getInt(1)
        }
    }
