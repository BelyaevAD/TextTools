package dev.belyaevad.texttools.app.ui.run

import dev.belyaevad.texttools.domain.AppProfile
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals

/** A second action must not start another lookup while the first one is still asking. */
class AppProfilerTest {
    @Test
    fun aSecondProfilerWaitsOutTheLookupAlreadyRunning() =
        runBlocking {
            val calls = AtomicInteger(0)
            val rows = mutableListOf<List<AppProfile>>()
            val started = CompletableDeferred<Unit>()
            val release = CompletableDeferred<Unit>()

            fun profiler() =
                AppProfiler(
                    apps = { rows.lastOrNull().orEmpty() },
                    save = { rows += it },
                    search = { _, _ -> "" },
                    complete = { _, _ ->
                        calls.incrementAndGet()
                        started.complete(Unit)
                        release.await()
                        "UNKNOWN" to null
                    },
                    background = { block -> launch { block() } },
                    profilingModel = "m",
                )

            val first = profiler()
            val second = profiler()
            try {
                first.consider(
                    proc = "zz-audit.exe",
                    title = "Audit",
                    windowClass = "AuditClass",
                    enabled = true,
                    description = "Added automatically",
                    addedPrefix = "Added automatically",
                    unknownDescription = "Not identified",
                )
                started.await()
                second.consider(
                    proc = "zz-audit.exe",
                    title = "Audit",
                    windowClass = "AuditClass",
                    enabled = true,
                    description = "Added automatically",
                    addedPrefix = "Added automatically",
                    unknownDescription = "Not identified",
                )
                assertEquals(1, calls.get())
            } finally {
                release.complete(Unit)
            }
        }
}
