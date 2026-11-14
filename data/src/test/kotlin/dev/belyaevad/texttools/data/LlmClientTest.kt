package dev.belyaevad.texttools.data

import dev.belyaevad.texttools.domain.AppException
import dev.belyaevad.texttools.domain.ChatMessage
import dev.belyaevad.texttools.domain.ErrorReason
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlinx.coroutines.CancellationException
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

    @Test
    fun unauthorizedKeepsReasonAndStatus() = runBlocking {
        val error = assertFailsWith<AppException> { ask(replying("""{"error":{"message":"bad key"}}""", HttpStatusCode.Unauthorized)) }
        assertEquals(ErrorReason.UNAUTHORIZED, error.reason)
        assertEquals(401, error.httpStatus)
    }

    @Test
    fun emptyErrorBodyKeepsStatus() = runBlocking {
        val error = assertFailsWith<AppException> { ask(replying("", HttpStatusCode.TooManyRequests)) }
        assertEquals(ErrorReason.RATE_LIMITED, error.reason)
        assertEquals(429, error.httpStatus)
    }

    @Test
    fun aNullUsageStillReturnsTheAnswer() = runBlocking {
        val (text, usage) = ask(replying("""{"choices":[{"message":{"content":"ok"}}],"usage":null}"""))
        assertEquals("ok", text)
        assertNull(usage)
    }

    @Test
    fun aNonJsonBodyIsATypedErrorWithoutTheBody() = runBlocking {
        val error = assertFailsWith<AppException> { ask(replying("<html>secret answer</html>")) }
        assertEquals(ErrorReason.EMPTY_RESPONSE, error.reason)
        assertTrue("secret" !in error.message.orEmpty())
    }

    @Test
    fun aTransportFailureIsProviderUnavailable() = runBlocking {
        val error = assertFailsWith<AppException> { ask(failing(IOException("reset"))) }
        assertEquals(ErrorReason.PROVIDER_UNAVAILABLE, error.reason)
    }

    @Test
    fun aRefusedProxyRouteKeepsItsReason() = runBlocking {
        val refused = IOException("proxy route refused", AppException(ErrorReason.INVALID_PROXY))
        val error = assertFailsWith<AppException> { ask(failing(refused)) }
        assertEquals(ErrorReason.INVALID_PROXY, error.reason)
    }

    @Test
    fun modelIdsReadsTheDataArray() = runBlocking {
        var seen: String? = null
        val http =
            HttpClient(MockEngine) {
                engine {
                    addHandler { request ->
                        seen = "${request.method.value} ${request.url} ${request.headers["Authorization"]}"
                        val body = """{"data":[{"id":"a"},{"id":"b"},{"id":"a"},{"x":1}]}"""
                        respond(body, HttpStatusCode.OK, headersOf("Content-Type", "application/json"))
                    }
                }
            }
        assertEquals(listOf("a", "b"), LlmClient(http = http).modelIds("https://api.openai.com/v1/", "k"))
        assertEquals("GET https://api.openai.com/v1/models Bearer k", seen)
    }

    @Test
    fun modelIdsKeepsTheHttpReason() = runBlocking {
        val client = LlmClient(http = replying("""{"error":"bad key"}""", HttpStatusCode.Unauthorized))
        val error = assertFailsWith<AppException> { client.modelIds("https://api.openai.com/v1", "k") }
        assertEquals(ErrorReason.UNAUTHORIZED, error.reason)
        assertEquals(401, error.httpStatus)
    }

    @Test
    fun modelIdsReadsABareArrayAndTheModelsKey() = runBlocking {
        assertEquals(listOf("a"), LlmClient(http = replying("""[{"id":"a"}]""")).modelIds("https://h.example/v1", "k"))
        assertEquals(listOf("b"), LlmClient(http = replying("""{"models":[{"id":"b"}]}""")).modelIds("https://h.example/v1", "k"))
        assertEquals(emptyList(), LlmClient(http = replying("""{"data":[]}""")).modelIds("https://h.example/v1", "k"))
        val odd = LlmClient(http = replying("""{"items":[]}"""))
        val error = assertFailsWith<AppException> { odd.modelIds("https://h.example/v1", "k") }
        assertEquals(ErrorReason.EMPTY_RESPONSE, error.reason)
    }

    @Test
    fun modelIdsDoesNotFollowARedirect() = runBlocking {
        var calls = 0
        val http =
            HttpClient(MockEngine) {
                engine {
                    addHandler {
                        calls += 1
                        respond("", HttpStatusCode.Found, headersOf("Location", "https://elsewhere.example/models"))
                    }
                }
            }
        val error = assertFailsWith<AppException> { LlmClient(http = http).modelIds("http://127.0.0.1:11434/v1", "k") }
        assertEquals(ErrorReason.HTTP_ERROR, error.reason)
        assertEquals(302, error.httpStatus)
        assertEquals(1, calls)
    }

    @Test
    fun badAddressesAreRefusedBeforeAnyRequest() = runBlocking {
        var calls = 0
        val client = LlmClient(http = replying { calls += 1 })
        assertEquals(ErrorReason.EMPTY_URL, assertFailsWith<AppException> { client.modelIds(" ", "k") }.reason)
        val refused =
            listOf(
                "api.openai.com/v1",
                "ftp://h.example/v1",
                "http:///v1",
                "http://exa mple.com/v1",
                "http://h.example:99999/v1",
                "https://[::1/v1",
                "http://:8080/v1",
                "http://user@:8080/v1",
            )
        for (address in refused) {
            val error = assertFailsWith<AppException>(address) { client.modelIds(address, "k") }
            assertEquals(ErrorReason.INVALID_URL, error.reason, address)
            assertTrue("example" !in error.stackTraceToString() && "::1" !in error.stackTraceToString(), address)
        }
        assertEquals(0, calls)
        val accepted =
            listOf("HTTP://API.OPENAI.COM/V1", "  https://api.openai.com/v1/  ", "http://[::1]:11434/v1", "http://my_box.test:8080/v1")
        for (address in accepted) assertEquals(emptyList(), client.modelIds(address, "k"), address)
        assertEquals(accepted.size, calls)
    }

    @Test
    fun aHostOkHttpRefusesIsATypedErrorWithoutTheAddress() = runBlocking {
        val error = assertFailsWith<AppException> { ask(throwing(IllegalArgumentException("Invalid URL host: \"x\""))) }
        assertEquals(ErrorReason.INVALID_URL, error.reason)
        assertTrue("Invalid URL host" !in error.stackTraceToString())
    }

    @Test
    fun anAppErrorFromTheEnginePassesThroughUnchanged() = runBlocking {
        val own = AppException(ErrorReason.SECRET_STORE_UNAVAILABLE)
        assertSame(own, assertFailsWith<AppException> { ask(throwing(own)) })
    }

    @Test
    fun cancellationIsNotTurnedIntoAnAppError() = runBlocking {
        val stopped = CancellationException("stopped")
        val client = HttpClient(MockEngine) { engine { addHandler { throw stopped } } }
        val error = assertFailsWith<CancellationException> { ask(client) }
        assertEquals("stopped", error.message)
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

private suspend fun ask(http: HttpClient) =
    LlmClient(http = http).complete("https://api.openai.com/v1", "k", "m", listOf(ChatMessage("user", "hi")))

private fun failing(error: IOException): HttpClient = throwing(error)

private fun throwing(error: Throwable): HttpClient = HttpClient(MockEngine) { engine { addHandler { throw error } } }

private fun replying(
    body: String = """{"data":[]}""",
    status: HttpStatusCode = HttpStatusCode.OK,
    hit: () -> Unit = {},
): HttpClient =
    HttpClient(MockEngine) {
        engine {
            addHandler {
                hit()
                respond(body, status, headersOf("Content-Type", "application/json"))
            }
        }
    }
