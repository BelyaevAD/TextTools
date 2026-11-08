package dev.belyaevad.texttools.data

import dev.belyaevad.texttools.domain.ChatMessage
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking

/** The proxy client is used only when the domain rule says the call is proxied. */
class LlmClientTest {
    @Test
    fun loopbackSkipsTheProxyClient() = runBlocking {
        var direct = 0
        var viaProxy = 0
        val client = LlmClient(http = counter { direct += 1 }, proxied = counter { viaProxy += 1 })
        client.complete(
            baseUrl = "http://127.0.0.1:11434/v1",
            apiKey = "k",
            model = "m",
            messages = listOf(ChatMessage("user", "hi")),
            proxy = "socks5h://proxy:1",
            proxyOn = true,
        )
        assertEquals(1, direct)
        assertEquals(0, viaProxy)
    }

    @Test
    fun remoteCallUsesTheProxyClient() = runBlocking {
        var direct = 0
        var viaProxy = 0
        val client = LlmClient(http = counter { direct += 1 }, proxied = counter { viaProxy += 1 })
        client.complete(
            baseUrl = "https://api.openai.com/v1",
            apiKey = "k",
            model = "m",
            messages = listOf(ChatMessage("user", "hi")),
            proxy = "http://proxy:1",
            proxyOn = true,
        )
        assertEquals(0, direct)
        assertEquals(1, viaProxy)
    }
}

private fun counter(hit: () -> Unit): HttpClient =
    HttpClient(MockEngine) {
        engine {
            addHandler {
                hit()
                respond(
                    content = """{"choices":[{"message":{"content":"ok"}}],"usage":{"prompt_tokens":1,"completion_tokens":1}}""",
                    status = HttpStatusCode.OK,
                    headers = headersOf("Content-Type", "application/json"),
                )
            }
        }
    }
