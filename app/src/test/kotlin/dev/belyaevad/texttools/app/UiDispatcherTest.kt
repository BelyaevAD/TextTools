package dev.belyaevad.texttools.app

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.jetbrains.skiko.MainUIDispatcher
import kotlin.test.Test

class UiDispatcherTest {
    @Test
    fun uiDispatcherRunsACoroutine() {
        runBlocking { withContext(MainUIDispatcher) { } }
    }
}
