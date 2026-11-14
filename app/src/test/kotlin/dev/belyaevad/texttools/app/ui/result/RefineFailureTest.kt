package dev.belyaevad.texttools.app.ui.result

import dev.belyaevad.texttools.domain.AppException
import dev.belyaevad.texttools.domain.ChatMessage
import dev.belyaevad.texttools.domain.ErrorReason
import dev.belyaevad.texttools.domain.Usage
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

/** A refine request that fails or comes back empty leaves a reason for the window to show. */
class RefineFailureTest {
    private fun refine(complete: suspend (String, List<ChatMessage>) -> Pair<String, Usage?>): ResultLive {
        val live = ResultLive("old", emptyList(), raw = false)
        runBlocking {
            val job = kotlinx.coroutines.Job()
            val scope = kotlinx.coroutines.CoroutineScope(job)
            launchRefine(scope, live, "m", "shorter", "", RefineCall(complete, {}, {}))
            job.children.forEach { it.join() }
        }
        return live
    }

    @Test
    fun failedRequestKeepsItsReason() {
        val live = refine { _, _ -> throw AppException(ErrorReason.UNAUTHORIZED, 401) }
        assertEquals(ErrorReason.UNAUTHORIZED, (live.failure as AppException).reason)
        assertFalse(live.busy)
        assertEquals("old", live.text)
    }

    @Test
    fun emptyAnswerReportsEmptyResponse() {
        val live = refine { _, _ -> "  " to null }
        assertEquals(ErrorReason.EMPTY_RESPONSE, (live.failure as AppException).reason)
    }

    @Test
    fun successClearsNoFailureAndSetsText() {
        val live = refine { _, _ -> "new" to null }
        assertNull(live.failure)
        assertEquals("new", live.text)
    }
}
