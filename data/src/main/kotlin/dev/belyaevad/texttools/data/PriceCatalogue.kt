package dev.belyaevad.texttools.data

import dev.belyaevad.texttools.domain.effectiveProxy
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout

/**
 * OpenRouter model list used as the price catalogue.
 * A load older than a day is fetched again. A failed load keeps the previous body and does not throw.
 */
class PriceCatalogue(
    private val http: HttpClient,
    private val proxied: HttpClient = http,
) {
    /**
     * Stores a fresh catalogue when [force] is set or the saved one is missing or older than a day.
     * Returns true when a body is available afterwards.
     */
    suspend fun refresh(
        store: TextToolsStore,
        proxy: String = "",
        proxyOn: Boolean? = false,
        nowMillis: Long = System.currentTimeMillis(),
        force: Boolean = false,
    ): Boolean {
        val cached = store.prices()
        if (!force && fresh(cached, nowMillis)) return true
        val body = downloaded(proxy, proxyOn) ?: return cached != null
        store.savePrices(PriceCache(body, nowMillis))
        return true
    }

    private suspend fun downloaded(
        proxy: String,
        proxyOn: Boolean?,
    ): String? {
        val client = if (effectiveProxy(proxy, proxyOn).isNotEmpty()) proxied else http
        return try {
            val response = withTimeout(PRICE_TIMEOUT_MILLIS) { client.get(PRICE_URL) }
            val text = response.bodyAsText()
            if (!response.status.isSuccess() || !text.take(PRICE_SNIFF).contains("\"data\"")) {
                null
            } else {
                text
            }
        } catch (cancelled: CancellationException) {
            if (cancelled is TimeoutCancellationException) null else throw cancelled
        } catch (_: Exception) {
            null
        }
    }
}

private fun fresh(
    cache: PriceCache?,
    nowMillis: Long,
): Boolean {
    if (cache == null || cache.body.isBlank()) return false
    val age = nowMillis - cache.fetchedAtEpochMs
    return age in 0 until PRICE_MAX_AGE_MILLIS
}

private const val PRICE_URL = "https://openrouter.ai/api/v1/models"
private const val PRICE_MAX_AGE_MILLIS = 24L * 60L * 60L * 1000L
private const val PRICE_TIMEOUT_MILLIS = 20_000L
private const val PRICE_SNIFF = 400
