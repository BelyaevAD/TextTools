package dev.belyaevad.texttools.app

import dev.belyaevad.texttools.data.LlmClient
import dev.belyaevad.texttools.domain.AppException
import dev.belyaevad.texttools.domain.ErrorReason
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import kotlinx.coroutines.runBlocking
import okhttp3.Dns
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.net.InetAddress
import java.net.ServerSocket
import java.net.UnknownHostException
import java.time.Duration
import kotlin.concurrent.thread
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/** A refused route fails the call at once and never resolves or connects to anything. */
class ProxyRouteTest {
    @Test
    fun anUnreadableProxyRefusesTheDirectClient() {
        assertRefused(direct = true, ErrorReason.SECRET_STORE_UNAVAILABLE) { throw AppException(ErrorReason.SECRET_STORE_UNAVAILABLE) }
    }

    @Test
    fun aProxyThatParsesButCannotBeUsedRefusesTheProxiedClient() {
        assertRefused(direct = false, ErrorReason.INVALID_PROXY) { "http://proxy:99999" }
    }

    @Test
    fun anEmptyProxyRefusesTheProxiedClient() {
        assertRefused(direct = false, ErrorReason.INVALID_PROXY) { "" }
        assertRefused(direct = false, ErrorReason.INVALID_PROXY) { "\"\"" }
    }

    @Test
    fun aSettingsProbeWithAnUnusableDraftProxyFailsWithoutALookup() =
        runBlocking {
            val asked = mutableListOf<String>()
            val draft = "socks5://user:secret@proxy:99999"
            val direct = HttpClient(OkHttp) { engine { config { dns(RecordingDns(asked)) } } }
            val proxied = proxiedClient({ draft }, RecordingDns(asked))
            val start = System.nanoTime()
            val error =
                assertFailsWith<AppException> {
                    LlmClient(http = direct, proxied = proxied).modelIds("https://api.example.com/v1", "k", draft, proxyOn = true)
                }
            val millis = (System.nanoTime() - start) / 1_000_000
            direct.close()
            proxied.close()
            assertEquals(ErrorReason.INVALID_PROXY, error.reason)
            assertTrue(millis < 2_000, "took $millis ms")
            assertEquals(emptyList(), asked)
            assertTrue("secret" !in error.stackTraceToString())
        }

    @Test
    fun aHostJavaUriCannotReadIsRefusedByTheProxiedClient() =
        runBlocking {
            val asked = mutableListOf<String>()
            val proxied = proxiedClient({ "http://proxy.example:3128" }, RecordingDns(asked))
            val start = System.nanoTime()
            val error =
                assertFailsWith<AppException> {
                    LlmClient(http = proxied, proxied = proxied).modelIds("http://my_host.test/v1", "k", "http://proxy.example:3128", true)
                }
            val millis = (System.nanoTime() - start) / 1_000_000
            proxied.close()
            assertEquals(ErrorReason.INVALID_URL, error.reason)
            assertTrue(millis < 2_000, "took $millis ms")
            assertEquals(emptyList(), asked)
        }

    @Test
    fun theDirectClientStillReachesAnUnderscoreHost() {
        val asked = mutableListOf<String>()
        val builder = OkHttpClient.Builder().dns(RecordingDns(asked))
        guardRoutes(builder, { "" }, direct = true)
        assertFailsWith<IOException> {
            val request = Request.Builder().url("http://my_host.test/v1/models").build()
            val call = builder.build().newCall(request)
            call.execute().close()
        }
        assertEquals(listOf("my_host.test"), asked)
    }

    @Test
    fun aSocksDraftProxyIsWhereTheProbeConnects() =
        runBlocking {
            ServerSocket(0, 1, InetAddress.getLoopbackAddress()).use { listener ->
                val firsts = java.util.Collections.synchronizedList(mutableListOf<Int>())
                // Every connection, including an OkHttp retry, gets its first byte read and is closed.
                val accept =
                    thread {
                        runCatching { while (true) listener.accept().use { firsts += it.getInputStream().read() } }
                    }
                val asked = mutableListOf<String>()
                val draft = "socks5://127.0.0.1:${listener.localPort}"
                val direct = HttpClient(OkHttp) { engine { config { dns(RecordingDns(asked)) } } }
                val proxied = proxiedClient({ draft }, RecordingDns(asked))
                val error =
                    assertFailsWith<AppException> {
                        LlmClient(http = direct, proxied = proxied).modelIds("https://api.example.com/v1", "k", draft, proxyOn = true)
                    }
                direct.close()
                proxied.close()
                listener.close()
                accept.join(5_000)
                assertTrue(firsts.isNotEmpty() && firsts.all { it == 0x05 }, "first bytes $firsts")
                assertEquals(ErrorReason.PROVIDER_UNAVAILABLE, error.reason)
                assertEquals(emptyList(), asked)
            }
        }

    private fun assertRefused(
        direct: Boolean,
        reason: ErrorReason,
        proxyText: () -> String,
    ) {
        val asked = mutableListOf<String>()
        val builder =
            OkHttpClient
                .Builder()
                .dns(RecordingDns(asked))
                .callTimeout(Duration.ofSeconds(3))
        guardRoutes(builder, proxyText, direct)
        val client = builder.build()
        val guarded = timed { client.call() }
        val cause = generateSequence<Throwable>(guarded.first) { it.cause }.filterIsInstance<AppException>().firstOrNull()
        assertEquals(reason, cause?.reason)
        // The selector alone, as if the interceptor were bypassed, must also fail fast without a lookup.
        val bare = client.newBuilder().apply { interceptors().clear() }.build()
        val backstop = timed { bare.call() }
        assertTrue(guarded.second < 2_000 && backstop.second < 2_000, "took ${guarded.second} / ${backstop.second} ms")
        assertEquals(emptyList(), asked)
    }
}

private fun OkHttpClient.call() {
    newCall(Request.Builder().url("https://api.example.com/v1/models").build()).execute().close()
}

private fun timed(block: () -> Unit): Pair<IOException, Long> {
    val start = System.nanoTime()
    val error = assertFailsWith<IOException> { block() }
    return error to (System.nanoTime() - start) / 1_000_000
}

private class RecordingDns(
    private val asked: MutableList<String>,
) : Dns {
    override fun lookup(hostname: String): List<InetAddress> {
        asked += hostname
        throw UnknownHostException("lookup is not allowed in this test")
    }
}
