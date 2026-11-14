package dev.belyaevad.texttools.app

import dev.belyaevad.texttools.domain.Settings
import io.sentry.Sentry
import io.sentry.SentryEvent
import io.sentry.SentryOptions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.Properties
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Connection values from a properties file. An empty [dsn] means nothing is sent.
 * The file is not created here and its values are not logged.
 */
data class SentryFile(
    val dsn: String = "",
    val environment: String = "",
)

/**
 * Reads `config/sentry.properties` beside [workDir], then beside its parent, then [dataDir].
 * `:app:run` uses the app module as [workDir], so the repo file is the parent one.
 */
fun sentryConfigFile(
    workDir: Path,
    dataDir: Path,
): Path? {
    val candidates =
        listOfNotNull(
            workDir.resolve("config").resolve(SENTRY_FILE),
            workDir.parent?.resolve("config")?.resolve(SENTRY_FILE),
            dataDir.resolve(SENTRY_FILE),
        )
    return candidates.firstOrNull { Files.isRegularFile(it) }
}

/** Parses the file. A missing or unreadable file is an empty config. */
fun loadSentryFile(path: Path?): SentryFile {
    if (path == null || !Files.isRegularFile(path)) return SentryFile()
    return try {
        val props = Properties()
        Files.newBufferedReader(path, StandardCharsets.UTF_8).use { props.load(it) }
        parseSentryConfig(props)
    } catch (_: IOException) {
        SentryFile()
    }
}

/** Parses keys `dsn` and `environment`. */
fun parseSentryConfig(props: Properties): SentryFile =
    SentryFile(
        dsn = props.getProperty("dsn").orEmpty().trim(),
        environment = props.getProperty("environment").orEmpty().trim(),
    )

/**
 * Turns the SDK on only when the file has a DSN and the error switch is set.
 * The switch off, or a blank DSN, closes the client.
 * The error switch also sends failures passed to [reportFailure].
 */
fun applySentry(
    file: SentryFile,
    settings: Settings,
) {
    val sendErrors = file.dsn.isNotEmpty() && settings.sentryErrors
    reportErrors.set(sendErrors)
    if (!sendErrors) {
        if (Sentry.isEnabled()) Sentry.close()
        return
    }
    Sentry.init { options -> configureSentryOptions(options, file) }
}

internal fun configureSentryOptions(
    options: SentryOptions,
    file: SentryFile,
) {
    options.dsn = file.dsn
    options.environment = file.environment.ifBlank { null }
    options.isEnableUncaughtExceptionHandler = true
    options.isAttachServerName = false // the machine name is not in the documented list of what is sent
    options.beforeSend = SentryOptions.BeforeSendCallback { event, _ -> scrubEvent(event) }
}

/** Sends [error] when the error switch is on. Cancellation is ignored. A timeout is sent. */
fun reportFailure(error: Throwable) {
    if (!reportErrors.get()) return
    if (error is CancellationException && error !is TimeoutCancellationException) return
    if (!Sentry.isEnabled()) return
    Sentry.captureException(error)
}

/** Blocks until the queued event is sent, or the timeout passes. */
fun flushSentry() {
    if (Sentry.isEnabled()) Sentry.flush(FLUSH_MILLIS)
}

/** Hides a bearer token, an `sk-` key, and a proxy userinfo. */
internal fun scrubSentryText(text: String): String =
    text.replace(BEARER, "Bearer ••••").replace(PROXY_USER, "://••••@").replace(SK_KEY, "sk-••••")

private fun scrubEvent(event: SentryEvent): SentryEvent {
    event.message?.let { message ->
        message.formatted = message.formatted?.let(::scrubSentryText)
        message.message = message.message?.let(::scrubSentryText)
    }
    event.exceptions?.forEach { item -> item.value = item.value?.let(::scrubSentryText) }
    event.request = null
    return event
}

private val reportErrors = AtomicBoolean(false)

private val BEARER = Regex("(?i)bearer\\s+\\S+")
private val PROXY_USER = Regex("://[^\\s/@]+:[^\\s/@]+@")
private val SK_KEY = Regex("\\bsk-[A-Za-z0-9_\\-]{8,}")

private const val SENTRY_FILE = "sentry.properties"
private const val FLUSH_MILLIS = 2_000L
