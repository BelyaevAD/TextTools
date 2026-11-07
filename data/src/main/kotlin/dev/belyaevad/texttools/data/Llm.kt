package dev.belyaevad.texttools.data

import dev.belyaevad.texttools.domain.ChatMessage
import dev.belyaevad.texttools.domain.Usage
import dev.belyaevad.texttools.domain.effectiveProxy
import dev.belyaevad.texttools.domain.isLoopback
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/** OpenAI-compatible chat client. The key is a header, never a temp file. */
class LlmClient(
    private val http: HttpClient,
) {
    private val json = Json { ignoreUnknownKeys = true }

    /** One non-streaming completion. Empty content is an error. */
    suspend fun complete(
        baseUrl: String,
        apiKey: String,
        model: String,
        messages: List<ChatMessage>,
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
        val response =
            http.post(endpoint(baseUrl, "chat/completions")) {
                header("Authorization", "Bearer $apiKey")
                contentType(ContentType.Application.Json)
                setBody(body.toString())
            }
        val text = response.bodyAsText()
        if (text.isEmpty()) error("Пустой ответ от API")
        if (!response.status.isSuccess()) error(statusText(response.status.value))
        val root = json.parseToJsonElement(text).jsonObject
        val content =
            root["choices"]
                ?.jsonArray
                ?.firstOrNull()
                ?.jsonObject
                ?.get("message")
                ?.jsonObject
                ?.get("content")
                ?.jsonPrimitive
                ?.contentOrNull
                ?.trim()
                .orEmpty()
        if (content.isEmpty()) error("Модель вернула пустой ответ. Выделенный текст не изменён.")
        return content to usageOf(root)
    }

    /** True when this call should use the proxy. Loopback never does. */
    fun shouldProxy(
        baseUrl: String,
        proxy: String,
        proxyOn: Boolean?,
    ): Boolean {
        val value = effectiveProxy(proxy, proxyOn)
        return value.isNotEmpty() && !isLoopback(baseUrl)
    }

    private fun endpoint(
        baseUrl: String,
        path: String,
    ): String = baseUrl.trimEnd('/') + "/" + path

    private fun usageOf(root: JsonObject): Usage? {
        val usage = root["usage"]?.jsonObject ?: return null
        val prompt = usage["prompt_tokens"]?.jsonPrimitive?.intOrNull
        val completion = usage["completion_tokens"]?.jsonPrimitive?.intOrNull
        if (prompt == null && completion == null) return null
        return Usage(prompt ?: 0, completion ?: 0)
    }

    private fun statusText(status: Int): String =
        when (status) {
            401 -> "Ключ не принят (401)."
            402 -> "Не хватает средств (402)."
            404 -> "Адрес или модель не найдены (404)."
            429 -> "Слишком много запросов (429)."
            in 500..599 -> "Провайдер недоступен ($status)."
            else -> "HTTP $status"
        }
}
