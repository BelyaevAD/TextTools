package dev.belyaevad.texttools.app

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

class AppVersionTest {
    @Test
    fun shownVersionMatchesTheInstaller() {
        val text = File("build.gradle.kts").readText()
        val version = Regex("""packageVersion = "([^"]+)"""").find(text)?.groupValues?.get(1)
        assertEquals(version, APP_VERSION)
    }
}
