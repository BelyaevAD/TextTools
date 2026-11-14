package dev.belyaevad.texttools.app

import dev.belyaevad.texttools.domain.Factory
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.util.Properties
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class UiSettingsTest {
    @Test
    fun anOlderFileKeepsStoredValuesAndGainsTheCurrentKeys() {
        val previous = Properties()
        previous.setProperty("baseUrl", "http://localhost:11434/v1")
        previous.setProperty("hotkey", "^!q")
        previous.setProperty("extra", "keep")
        val settings = settingsFromProperties(previous, proxy = "")
        val written = currentUiProperties(previous, settings, persistAutostart = false)
        assertEquals("1", written.getProperty("version"))
        assertEquals("http://localhost:11434/v1", written.getProperty("baseUrl"))
        assertEquals("^!q", written.getProperty("hotkey"))
        assertEquals("keep", written.getProperty("extra"))
        assertEquals(Factory.defaultSettings.defaultModel, written.getProperty("defaultModel"))
        assertEquals(Factory.defaultSettings.hotkeyRepeat, written.getProperty("hotkeyRepeat"))
        assertFalse(written.containsKey("autostart"))
    }

    @Test
    fun aRemovedSentryTracesKeyIsIgnoredOnReadAndKeptOnWrite() {
        val previous = Properties()
        previous.setProperty("sentryTraces", "true")
        val settings = settingsFromProperties(previous, proxy = "")
        assertEquals(Factory.defaultSettings, settings)
        assertEquals("true", currentUiProperties(previous, settings, persistAutostart = false).getProperty("sentryTraces"))
    }

    @Test
    fun aBlankMenuHotkeyBecomesTheFactoryChord() {
        val previous = Properties()
        previous.setProperty("hotkey", "  ")
        val settings = settingsFromProperties(previous, proxy = "http://proxy")
        assertEquals(Factory.defaultSettings.hotkey, settings.hotkey)
        assertEquals(true, settings.proxyOn)
    }

    @Test
    fun aCurrentFileIsNotStaleAndAnEmptyProxyFlagStaysUnset() {
        val props = Properties()
        props.setProperty("version", "1")
        props.setProperty("proxyOn", "")
        assertFalse(uiSettingsStale(props))
        assertNull(settingsFromProperties(props, proxy = "http://proxy").proxyOn)
    }

    @Test
    fun anUnreadableProxyKeepsTheProxyOn() {
        assertEquals(true, settingsFromProperties(Properties(), proxy = "", proxyUnreadable = true).proxyOn)
        val unset = Properties().apply { setProperty("proxyOn", "") }
        assertEquals(true, settingsFromProperties(unset, proxy = "", proxyUnreadable = true).proxyOn)
        val off = Properties().apply { setProperty("proxyOn", "false") }
        assertEquals(false, settingsFromProperties(off, proxy = "", proxyUnreadable = true).proxyOn)
    }

    @Test
    fun aBlankValueDoesNotOverwriteAnUnreadableSecret() {
        assertFalse(secretWritable("", storedUnreadable = true))
        assertTrue(secretWritable("new", storedUnreadable = true))
        assertTrue(secretWritable("", storedUnreadable = true, cleared = true))
        assertTrue(secretWritable("", storedUnreadable = false))
    }

    @Test
    fun replaceUiFileSwapsInTheFinishedText() {
        val dir = Files.createTempDirectory("ui-settings")
        try {
            val file = dir.resolve("ui.properties")
            Files.writeString(file, "old=1\n")
            val props = Properties()
            props.setProperty("baseUrl", "http://localhost:11434/v1")
            writeUiFile(file, props)
            val loaded = Properties()
            Files.newBufferedReader(file, StandardCharsets.UTF_8).use { loaded.load(it) }
            assertEquals("http://localhost:11434/v1", loaded.getProperty("baseUrl"))
            assertFalse(Files.exists(dir.resolve("ui.properties.tmp")))
        } finally {
            dir.toFile().deleteRecursively()
        }
    }
}
