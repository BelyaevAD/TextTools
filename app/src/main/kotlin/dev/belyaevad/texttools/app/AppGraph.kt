package dev.belyaevad.texttools.app

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import dev.belyaevad.texttools.data.AppWebSearch
import dev.belyaevad.texttools.data.LlmClient
import dev.belyaevad.texttools.data.PriceCatalogue
import dev.belyaevad.texttools.data.TextToolsStore
import dev.belyaevad.texttools.data.db.TextToolsDatabase
import dev.belyaevad.texttools.domain.AppException
import dev.belyaevad.texttools.domain.ErrorReason
import dev.belyaevad.texttools.domain.isLoopback
import dev.belyaevad.texttools.domain.normalizeProxy
import dev.belyaevad.texttools.platform.ClipboardProtocol
import dev.belyaevad.texttools.platform.win.ForegroundWatch
import dev.belyaevad.texttools.platform.win.GlobalChord
import dev.belyaevad.texttools.platform.win.SecretSlot
import dev.belyaevad.texttools.platform.win.SecretStoreException
import dev.belyaevad.texttools.platform.win.UserSecrets
import dev.belyaevad.texttools.platform.win.WinHotkeys
import dev.belyaevad.texttools.platform.win.openUserSecretStore
import dev.belyaevad.texttools.platform.win.userDataRoot
import dev.belyaevad.texttools.platform.win.windowsClipboard
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import okhttp3.Authenticator
import okhttp3.Credentials
import okhttp3.Dns
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.ProxySelector
import java.net.SocketAddress
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration

/**
 * Desktop wiring. Construction does not call [WinHotkeys.start], [ForegroundWatch.start], or open a window.
 * The API key and the proxy URL stay in [secrets]. Neither is written to the log or to SQLite.
 */
class AppGraph(
    onChord: (GlobalChord) -> Unit,
) : AutoCloseable {
    val secrets: UserSecrets = openUserSecretStore()

    private val driver: JdbcSqliteDriver = openDatabase()

    val store: TextToolsStore = TextToolsStore(driver)

    private val direct: HttpClient = directClient(secrets)

    private val proxied: HttpClient = proxiedClient({ readProxy(secrets) })

    val llm: LlmClient = LlmClient(http = direct, proxied = proxied)

    val search: AppWebSearch = AppWebSearch(http = direct, proxied = proxied)

    val catalogue: PriceCatalogue = PriceCatalogue(http = direct, proxied = proxied)

    val watch: ForegroundWatch = ForegroundWatch()

    val clipboard: ClipboardProtocol = ClipboardProtocol(windowsClipboard(watch))

    val hotkeys: WinHotkeys = WinHotkeys(onChord)

    /**
     * Settings probes: the real clients and route rules, with the unsaved [proxy] from the form.
     * A blank proxy goes through [direct], which still refuses while the saved proxy cannot be read.
     */
    suspend fun <T> probe(
        proxy: String,
        block: suspend (LlmClient) -> T,
    ): T = proxiedClient({ proxy }).use { block(LlmClient(http = direct, proxied = it)) }

    /** Closes the clients and the database. Stops the hotkey loop only when [WinHotkeys.start] was called later. */
    override fun close() {
        hotkeys.stop()
        direct.close()
        proxied.close()
        driver.close()
    }
}

private const val DATABASE_FILE = "texttools.db"

private fun openDatabase(): JdbcSqliteDriver {
    val file = userDataRoot().resolve(DATABASE_FILE)
    Files.createDirectories(file.parent)
    val driver = JdbcSqliteDriver(sqliteUrl(file))
    val schema = TextToolsDatabase.Schema
    val existing = userVersion(driver)
    when {
        existing == 0L -> {
            schema.create(driver)
            setUserVersion(driver, schema.version)
        }
        existing < schema.version -> {
            schema.migrate(driver, existing, schema.version)
            setUserVersion(driver, schema.version)
        }
    }
    return driver
}

private fun sqliteUrl(file: Path): String = "jdbc:sqlite:${file.toString().replace('\\', '/')}"

private fun userVersion(driver: JdbcSqliteDriver): Long =
    driver.getConnection().createStatement().use { statement ->
        statement.executeQuery("PRAGMA user_version").use { rows ->
            check(rows.next())
            rows.getLong(1)
        }
    }

private fun setUserVersion(
    driver: JdbcSqliteDriver,
    version: Long,
) {
    driver.getConnection().createStatement().use { statement ->
        statement.execute("PRAGMA user_version = $version")
    }
}

// shortcut: 180s with no response bytes, raise when a slow model still times out.
private val CHAT_READ_TIMEOUT: Duration = Duration.ofSeconds(180)

private fun directClient(secrets: UserSecrets): HttpClient =
    HttpClient(OkHttp) {
        engine {
            config {
                guardRoutes(this, { readProxy(secrets) }, direct = true)
                readTimeout(CHAT_READ_TIMEOUT)
            }
        }
    }

/** [proxyText] is read when a call is made. The value is not logged. [dns] is replaced only by tests. */
internal fun proxiedClient(
    proxyText: () -> String,
    dns: Dns = Dns.SYSTEM,
): HttpClient =
    HttpClient(OkHttp) {
        engine {
            config {
                dns(dns)
                guardRoutes(this, proxyText, direct = false)
                proxyAuthenticator(SecretProxyAuthenticator(proxyText))
                readTimeout(CHAT_READ_TIMEOUT)
            }
        }
    }

/**
 * Both clients choose their route here, so no caller can reach the network around an unreadable or bad proxy.
 * [proxyText] returns the stored proxy text and throws [AppException] when it cannot be read.
 */
internal fun guardRoutes(
    builder: OkHttpClient.Builder,
    proxyText: () -> String,
    direct: Boolean,
) {
    builder.addInterceptor(RouteGuard(proxyText, direct))
    builder.proxySelector(GuardedProxySelector(proxyText, direct))
}

/** Fails the call at once with the reason in the cause chain, before OkHttp plans any route. */
private class RouteGuard(
    private val proxyText: () -> String,
    private val direct: Boolean,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val uri = chain.request().url.toUri()
        // OkHttp skips the ProxySelector and connects directly when java.net.URI cannot read the host (e.g. "my_host").
        if (!direct && uri.host == null) throw IOException("proxy route refused", AppException(ErrorReason.INVALID_URL))
        try {
            route(proxyText, direct, uri)
        } catch (e: IllegalArgumentException) {
            throw IOException("proxy route refused", e as? AppException ?: AppException(ErrorReason.INVALID_PROXY, cause = e))
        }
        return chain.proceed(chain.request())
    }
}

/**
 * Backstop behind [RouteGuard]. select() must not throw: OkHttp reads an IllegalArgumentException as "no proxy",
 * and any other throw makes it retry route planning forever. A refused route is a proxy that cannot connect.
 */
private class GuardedProxySelector(
    private val proxyText: () -> String,
    private val direct: Boolean,
) : ProxySelector() {
    override fun select(uri: URI?): List<Proxy> =
        try {
            listOf(route(proxyText, direct, uri))
        } catch (_: IllegalArgumentException) {
            listOf(DEAD_ROUTE)
        }

    override fun connectFailed(
        uri: URI?,
        sa: SocketAddress?,
        ioe: IOException?,
    ) = Unit
}

private class SecretProxyAuthenticator(
    private val proxyText: () -> String,
) : Authenticator {
    override fun authenticate(
        route: Route?,
        response: Response,
    ): Request? {
        if (response.request.header("Proxy-Authorization") != null) return null
        val endpoint = proxyEndpoint(proxyText())
        val username = endpoint.username ?: return null
        val password = endpoint.password ?: return null
        val credential = Credentials.basic(username, password, StandardCharsets.UTF_8)
        return response.request
            .newBuilder()
            .header("Proxy-Authorization", credential)
            .build()
    }
}

private class ProxyEndpoint(
    val proxy: Proxy,
    val username: String?,
    val password: String?,
)

/** The stored proxy text. A proxy file that cannot be decrypted is an error, never "no proxy". */
internal fun readProxy(secrets: UserSecrets): String =
    try {
        secrets.get(SecretSlot.ProxyUrl)?.trim().orEmpty()
    } catch (e: SecretStoreException) {
        throw AppException(ErrorReason.SECRET_STORE_UNAVAILABLE, cause = e)
    }

// Port 0 is refused by OkHttp before any DNS lookup or connect.
private val DEAD_ROUTE = Proxy(Proxy.Type.HTTP, InetSocketAddress.createUnresolved("localhost", 0))

/** A direct route is refused while a saved proxy cannot be read. Loopback never leaves the machine. */
private fun route(
    proxyText: () -> String,
    direct: Boolean,
    uri: URI?,
): Proxy {
    if (!direct) {
        val proxy = proxyEndpoint(proxyText()).proxy
        // Callers pick this client only while a proxy is in force, so an empty one must not mean "direct".
        if (proxy == Proxy.NO_PROXY) badProxy()
        return proxy
    }
    if (uri == null || !isLoopback(uri.toString())) proxyText()
    return Proxy.NO_PROXY
}

private fun proxyEndpoint(raw: String): ProxyEndpoint {
    if (raw.isEmpty()) return ProxyEndpoint(Proxy.NO_PROXY, null, null)
    val value =
        try {
            normalizeProxy(raw)
        } catch (_: IllegalArgumentException) {
            return badProxy()
        }
    if (value.isEmpty()) return ProxyEndpoint(Proxy.NO_PROXY, null, null)
    val uri =
        try {
            URI.create(value)
        } catch (_: IllegalArgumentException) {
            return badProxy()
        }
    val host = uri.host ?: return badProxy()
    val scheme = uri.scheme?.lowercase().orEmpty()
    val port =
        when {
            uri.port != -1 -> uri.port
            else -> defaultProxyPort(scheme)
        }
    if (port == -1) return badProxy()
    val type = if (scheme.startsWith("socks")) Proxy.Type.SOCKS else Proxy.Type.HTTP
    val proxy = Proxy(type, InetSocketAddress.createUnresolved(host, port))
    val decoded = decodeUserInfo(uri.rawUserInfo)
    return ProxyEndpoint(proxy, decoded?.first, decoded?.second)
}

private fun badProxy(): Nothing = throw AppException(ErrorReason.INVALID_PROXY)

private fun decodeUserInfo(info: String?): Pair<String, String>? {
    if (info.isNullOrEmpty()) return null
    val separator = info.indexOf(':')
    val user = if (separator < 0) info else info.substring(0, separator)
    val password = if (separator < 0) "" else info.substring(separator + 1)
    return try {
        URLDecoder.decode(user, StandardCharsets.UTF_8) to URLDecoder.decode(password, StandardCharsets.UTF_8)
    } catch (_: IllegalArgumentException) {
        null
    }
}

private fun defaultProxyPort(scheme: String): Int =
    when (scheme) {
        "http" -> 80
        "https" -> 443
        "socks", "socks4", "socks4a", "socks5", "socks5h" -> 1080
        else -> -1
    }
