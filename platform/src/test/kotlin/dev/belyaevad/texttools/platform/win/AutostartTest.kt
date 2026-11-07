package dev.belyaevad.texttools.platform.win

import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** The Run command is checked as text. This test does not write the registry. */
class AutostartTest {
    @Test
    fun commandQuotesTheInstalledExeAndLeavesTheRegistryAlone() {
        val before = runSnapshot()
        val exe = Path.of("C:\\Users\\John Doe\\AppData\\Local\\TextTools\\TextTools.exe")
        val command = installedAutostart(exe)
        assertEquals("HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Run", command.key)
        assertEquals("TextTools", command.valueName)
        assertEquals("\"${exe.toAbsolutePath().normalize()}\"", command.command)
        assertTrue(autostartMatchesInstall(command, exe))
        assertFalse(autostartMatchesInstall(command.copy(command = "\"C:\\Windows\\notepad.exe\""), exe))
        val after = runSnapshot()
        assertEquals(before, after, "autostart registry value changed")
    }

    @Test
    fun aScriptPathIsRejected() {
        assertFailsWith<IllegalArgumentException> {
            installedAutostart(Path.of("C:\\Users\\test\\AppData\\Local\\Scripts\\TextTools\\TextTools.ahk"))
        }
    }
}

private fun runSnapshot(): Int {
    val process =
        ProcessBuilder(
            "reg",
            "query",
            "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Run",
            "/v",
            "TextTools",
        ).redirectErrorStream(true).start()
    val bytes = process.inputStream.readBytes()
    val code = process.waitFor()
    return code * 31 + bytes.contentHashCode()
}
