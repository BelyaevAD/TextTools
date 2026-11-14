package dev.belyaevad.texttools.data

import dev.belyaevad.texttools.domain.AppException
import dev.belyaevad.texttools.domain.ChatMessage
import dev.belyaevad.texttools.domain.ErrorReason
import dev.belyaevad.texttools.domain.Usage
import dev.belyaevad.texttools.domain.effectiveProxy
import dev.belyaevad.texttools.domain.isLoopback
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.IllegalHeaderValueException
import io.ktor.http.Url
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import java.io.IOException
import java.net.URI
import java.net.URISyntaxException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.put

/** OpenAI-compatible chat client. The key is a header, never a temp file. */
class LlmClient(
    private val http: HttpClient,
    private val proxied: HttpClient = http,
) {
    private val json = Json { ignoreUnknownKeys = true }

    /** One non-streaming completion. Empty content is an error. */
    suspend fun complete(
        baseUrl: String,
        apiKey: String,
        model: String,
        messages: List<ChatMessage>,
        proxy: String = "",
        proxyOn: Boolean? = false,
    ): Pair<String, Usage?> {
        val body =
            buildJsonObject {
                put("model", model)
                put("stream", false)
                put(
                    "messages",
                    buildJsonArray {
                        messages.forEach { message ->
                            add(
                                buildJsonObject {
                                    put("role", message.role)
                                    put("content", message.content)
                                },
                            )
                        }
                    },
                )
            }
        val client = if (shouldProxy(baseUrl, proxy, proxyOn)) proxied else http
        val response = send(client, endpoint(baseUrl, "chat/completions"), apiKey, body.toString())
        val text = response.bodyAsText()
        if (!response.status.isSuccess()) throw httpError(response.status.value)
        val root = replyRoot(text)
        val message = ((root["choices"] as? JsonArray)?.firstOrNull() as? JsonObject)?.get("message") as? JsonObject
        val content = (message?.get("content") as? JsonPrimitive)?.contentOrNull?.trim().orEmpty()
        if (content.isEmpty()) throw AppException(ErrorReason.EMPTY_RESPONSE)
        return content to usageOf(root)
    }

    /** `GET models`: ids from a bare array, `data[]` or `models[]`, with the same status handling as [complete]. */
    suspend fun modelIds(
        baseUrl: String,
        apiKey: String,
        proxy: String = "",
        proxyOn: Boolean? = false,
    ): List<String> {
        val url = endpoint(baseUrl, "models")
        val base = if (shouldProxy(baseUrl, proxy, proxyOn)) proxied else http
        // A redirect could lead to another host around the proxy rule, so a 3xx is an HTTP error here.
        val (status, text) =
            base.config { followRedirects = false }.use { client ->
                val response = send(client, url, apiKey, body = null)
                response.status.value to response.bodyAsText()
            }
        if (status !in HTTP_OK_RANGE) throw httpError(status)
        val root =
            try {
                json.parseToJsonElement(text)
            } catch (_: SerializationException) {
                null
            }
        val rows =
            root as? JsonArray
                ?: (root as? JsonObject)?.let { it["data"] as? JsonArray ?: it["models"] as? JsonArray }
                ?: throw AppException(ErrorReason.EMPTY_RESPONSE)
        return rows
            .mapNotNull { row -> ((row as? JsonObject)?.get("id") as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotEmpty() } }
            .distinct()
    }

    /** A null [body] is a GET. */
    private suspend fun send(
        client: HttpClient,
        url: String,
        apiKey: String,
        body: String?,
    ): HttpResponse =
        try {
            if (body == null) {
                client.get(url) { header("Authorization", "Bearer $apiKey") }
            } else {
                client.post(url) {
                    header("Authorization", "Bearer $apiKey")
                    contentType(ContentType.Application.Json)
                    setBody(body)
                }
            }
        } catch (e: IOException) {
            // OkHttp wraps a refused proxy route in an IOException; its own reason is the one to show.
            throw generateSequence<Throwable>(e) { it.cause }.filterIsInstance<AppException>().firstOrNull()
                ?: AppException(ErrorReason.PROVIDER_UNAVAILABLE, cause = e)
        } catch (e: IllegalArgumentException) {
            // OkHttp refuses a host both parsers accepted ("Invalid URL host: ..."); that text holds the address.
            throw if (e is AppException || e is IllegalHeaderValueException) e else AppException(ErrorReason.INVALID_URL)
        }

    /** Empty or non-JSON text is an empty reply; the text itself never goes into the error. */
    private fun replyRoot(text: String): JsonObject =
        try {
            json.parseToJsonElement(text) as? JsonObject
        } catch (_: SerializationException) {
            null
        } ?: throw AppException(ErrorReason.EMPTY_RESPONSE)

    /** True when this call should use the proxy. Loopback never does. */
    fun shouldProxy(
        baseUrl: String,
        proxy: String,
        proxyOn: Boolean?,
    ): Boolean {
        val value = effectiveProxy(proxy, proxyOn)
        return value.isNotEmpty() && !isLoopback(baseUrl)
    }

    /** Ktor reads an address without a scheme or host as a path on localhost, so it is refused here. */
    private fun endpoint(
        baseUrl: String,
        path: String,
    ): String {
        val base = baseUrl.trim().trimEnd('/')
        if (base.isEmpty()) throw AppException(ErrorReason.EMPTY_URL)
        if (!usableAddress(base)) throw AppException(ErrorReason.INVALID_URL)
        return "$base/$path"
    }

    /** The address text never goes into an error. */
    private fun usableAddress(base: String): Boolean {
        val scheme = base.substringBefore("://", "").lowercase()
        val authority = base.substringAfter("://").substringBefore('/').substringBefore('?').substringBefore('#')
        val host = authority.substringAfterLast('@')
        // Ktor fills an empty host, as in "http://:8080", with localhost.
        if (scheme !in listOf("http", "https") || host.substringBefore(':').isEmpty()) return false
        // Ktor refuses a bad port; java.net.URI refuses spaces and a broken IPv6 literal, which OkHttp would throw on.
        return try {
            URI(base)
            Url(base).host.isNotEmpty()
        } catch (_: URISyntaxException) {
            false
        } catch (_: IllegalArgumentException) {
            false
        } catch (_: IllegalStateException) {
            false
        }
    }

    private fun usageOf(root: JsonObject): Usage? {
        val usage = root["usage"] as? JsonObject ?: return null
        val prompt = (usage["prompt_tokens"] as? JsonPrimitive)?.intOrNull
        val completion = (usage["completion_tokens"] as? JsonPrimitive)?.intOrNull
        if (prompt == null && completion == null) return null
        return Usage(prompt ?: 0, completion ?: 0)
    }

}

private val HTTP_OK_RANGE = 200..299

private fun httpError(status: Int): AppException {
    val reason =
        when (status) {
            401 -> ErrorReason.UNAUTHORIZED
            402 -> ErrorReason.INSUFFICIENT_FUNDS
            404 -> ErrorReason.NOT_FOUND
            429 -> ErrorReason.RATE_LIMITED
            in 500..599 -> ErrorReason.PROVIDER_UNAVAILABLE
            else -> ErrorReason.HTTP_ERROR
        }
    return AppException(reason, status, "HTTP $status")
}
