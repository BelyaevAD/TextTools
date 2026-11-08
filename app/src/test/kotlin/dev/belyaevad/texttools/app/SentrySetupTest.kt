package dev.belyaevad.texttools.app

import java.nio.file.Files
import java.util.Properties
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The DSN stays in the properties file. A blank file sends nothing. */
class SentrySetupTest {
    @Test
    fun blankFileHasNoDsn() {
        val file = parseSentryConfig(Properties())
        assertEquals("", file.dsn)
        assertEquals(0.0, file.tracesSampleRate)
    }

    @Test
    fun rateStaysInsideZeroAndOne() {
        val props = Properties()
        props.setProperty("dsn", "https://public@example/1")
        props.setProperty("environment", "local")
        props.setProperty("traces-sample-rate", "4")
        val file = parseSentryConfig(props)
        assertTrue(file.dsn.startsWith("https://"))
        assertEquals("local", file.environment)
        assertEquals(1.0, file.tracesSampleRate)
    }

    @Test
    fun moduleDirectoryUsesTheParentConfigFile() {
        val root = Files.createTempDirectory("tt-sentry")
        val appDir = Files.createDirectories(root.resolve("app"))
        val config = Files.createDirectories(root.resolve("config"))
        Files.writeString(config.resolve("sentry.properties"), "dsn=https://public@example/1\n")
        val found = sentryConfigFile(appDir, root.resolve("data"))
        assertEquals(config.resolve("sentry.properties"), found)
    }

    @Test
    fun scrubHidesBearerProxyPasswordAndKey() {
        val text = scrubSentryText("Bearer sk-or-v1-abcdefgh1234 https://user:secret@proxy.example:8080/x sk-live-abcdef1234")
        assertEquals("Bearer •••• https://••••@proxy.example:8080/x sk-••••", text)
        assertTrue(!text.contains("secret"))
        assertTrue(!text.contains("abcdefgh"))
    }
}
