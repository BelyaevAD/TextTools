package dev.belyaevad.texttools.app

import dev.belyaevad.texttools.data.TextToolsStore
import dev.belyaevad.texttools.data.UsageEvent
import io.sentry.MeasurementUnit
import io.sentry.Sentry
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicBoolean

const val TELEMETRY_HOUR_MS: Long = 60L * 60L * 1000L

/** Aggregates only completed hours. Texts, source windows and credentials are absent. */
data class UsageBatch(
    val bucketStartMs: Long,
    val kind: String,
    val model: String,
    val outcome: String,
    val events: List<UsageEvent>,
) {
    val count: Int get() = events.size
    val durationMs: Long get() = events.sumOf { it.durationMs }
    val inputChars: Long get() = events.sumOf { it.inputChars.toLong() }
    val outputChars: Long get() = events.sumOf { it.outputChars.toLong() }
    val usageCount: Int get() = events.count { it.promptTokens != null || it.completionTokens != null }
    val promptTokens: Long get() = events.sumOf { it.promptTokens?.toLong() ?: 0L }
    val completionTokens: Long get() = events.sumOf { it.completionTokens?.toLong() ?: 0L }
    val maxDurationMs: Long get() = events.maxOf { it.durationMs }
    val maxInputChars: Int get() = events.maxOf { it.inputChars }
    val maxOutputChars: Int get() = events.maxOf { it.outputChars }
}

fun usageBatches(
    events: List<UsageEvent>,
    nowMillis: Long,
): List<UsageBatch> =
    events
        .filter { it.sentryBucket == null && it.atEpochMs / TELEMETRY_HOUR_MS < nowMillis / TELEMETRY_HOUR_MS }
        .groupBy { UsageKey(it.atEpochMs / TELEMETRY_HOUR_MS * TELEMETRY_HOUR_MS, it.kind, it.model, it.outcome) }
        .map { (key, rows) -> UsageBatch(key.bucket, key.kind, key.model, key.outcome, rows) }
        .sortedBy { it.bucketStartMs }

private data class UsageKey(
    val bucket: Long,
    val kind: String,
    val model: String,
    val outcome: String,
)

fun interface UsageMetricSink {
    fun publish(batch: UsageBatch)
}

val SentryUsageMetricSink =
    UsageMetricSink { batch ->
        val tags =
            mapOf(
                "model" to batch.model,
                "kind" to batch.kind,
                "outcome" to batch.outcome,
                "bucket_start" to batch.bucketStartMs.toString(),
            )
        Sentry.metrics().increment("llm.requests", batch.count.toDouble(), null, tags)
        Sentry.metrics().distribution("llm.duration_ms.total", batch.durationMs.toDouble(), MeasurementUnit.Duration.MILLISECOND, tags)
        Sentry.metrics().distribution(
            "llm.duration_ms.mean",
            (batch.durationMs.toDouble() / batch.count),
            MeasurementUnit.Duration.MILLISECOND,
            tags,
        )
        Sentry.metrics().distribution("llm.duration_ms.max", batch.maxDurationMs.toDouble(), MeasurementUnit.Duration.MILLISECOND, tags)
        Sentry.metrics().distribution("llm.input_chars.total", batch.inputChars.toDouble(), null, tags)
        Sentry.metrics().distribution("llm.input_chars.mean", batch.inputChars.toDouble() / batch.count, null, tags)
        Sentry.metrics().distribution("llm.input_chars.max", batch.maxInputChars.toDouble(), null, tags)
        Sentry.metrics().distribution("llm.output_chars.total", batch.outputChars.toDouble(), null, tags)
        Sentry.metrics().distribution("llm.output_chars.mean", batch.outputChars.toDouble() / batch.count, null, tags)
        Sentry.metrics().distribution("llm.output_chars.max", batch.maxOutputChars.toDouble(), null, tags)
        if (batch.usageCount > 0) {
            Sentry.metrics().increment("llm.usage_reported", batch.usageCount.toDouble(), null, tags)
            Sentry.metrics().distribution("llm.prompt_tokens", batch.promptTokens.toDouble(), null, tags)
            Sentry.metrics().distribution("llm.completion_tokens", batch.completionTokens.toDouble(), null, tags)
        }
    }

/** One aggregate per hour/model/kind/outcome, never a Sentry event per model request. */
class UsageTelemetry(
    private val store: TextToolsStore,
    private val settings: () -> TelemetrySettings,
    private val sink: UsageMetricSink,
) : AutoCloseable {
    private val running = AtomicBoolean(true)
    private val gate = Mutex()

    suspend fun flushCompleted(nowMillis: Long = System.currentTimeMillis()): Int =
        gate.withLock {
            val current = settings()
            val allowed = current.traces && current.dsn.isNotBlank() && sentryConfigured.get()
            if (!running.get() || !allowed) return@withLock 0
            val batches = usageBatches(store.pendingUsageEvents(), nowMillis)
            batches.forEach(sink::publish)
            // Flush is not an HTTP acknowledgement. The batch remains pending for a safe retry.
            batches.size
        }

    override fun close() {
        try {
            runBlocking { flushCompleted() }
        } finally {
            running.set(false)
        }
    }
}

data class TelemetrySettings(
    val traces: Boolean,
    val dsn: String,
)
