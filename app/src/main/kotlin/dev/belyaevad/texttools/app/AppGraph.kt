package dev.belyaevad.texttools.app

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import dev.belyaevad.texttools.data.AppWebSearch
import dev.belyaevad.texttools.data.LlmClient
import dev.belyaevad.texttools.data.PriceCatalogue
import dev.belyaevad.texttools.data.TextToolsStore
import dev.belyaevad.texttools.data.db.TextToolsDatabase
import dev.belyaevad.texttools.domain.AppException
import dev.belyaevad.texttools.domain.ErrorReason
import dev.belyaevad.texttools.domain.normalizeProxy
import dev.belyaevad.texttools.platform.ClipboardProtocol
import dev.belyaevad.texttools.platform.win.ForegroundWatch
import dev.belyaevad.texttools.platform.win.GlobalChord
import dev.belyaevad.texttools.platform.win.SecretSlot
import dev.belyaevad.texttools.platform.win.UserSecrets
import dev.belyaevad.texttools.platform.win.WinHotkeys
import dev.belyaevad.texttools.platform.win.openUserSecretStore
import dev.belyaevad.texttools.platform.win.windowsClipboard
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import okhttp3.Authenticator
import okhttp3.Credentials
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

    private val direct: HttpClient = directClient()

    private val proxied: HttpClient = proxiedClient(secrets)

    val llm: LlmClient = LlmClient(http = direct, proxied = proxied)

    val search: AppWebSearch = AppWebSearch(http = direct, proxied = proxied)

    val catalogue: PriceCatalogue = PriceCatalogue(http = direct, proxied = proxied)

    val watch: ForegroundWatch = ForegroundWatch()

    val clipboard: ClipboardProtocol = ClipboardProtocol(windowsClipboard(watch))

    val hotkeys: WinHotkeys = WinHotkeys(onChord)

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
    val file = Path.of(localAppData(), "TextTools", DATABASE_FILE)
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

private fun localAppData(): String {
    val local = System.getenv("LOCALAPPDATA")
    if (local.isNullOrBlank()) {
        throw AppException(ErrorReason.SECRET_STORE_UNAVAILABLE)
    }
    return local
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

private fun directClient(): HttpClient =
    HttpClient(OkHttp) {
        engine {
            config {
                proxy(Proxy.NO_PROXY)
                readTimeout(CHAT_READ_TIMEOUT)
            }
        }
    }

/** The proxy slot is read when a call is made. The value is not logged. */
private fun proxiedClient(secrets: UserSecrets): HttpClient =
    HttpClient(OkHttp) {
        engine {
            config {
                proxySelector(SecretProxySelector(secrets))
                proxyAuthenticator(SecretProxyAuthenticator(secrets))
                readTimeout(CHAT_READ_TIMEOUT)
            }
        }
    }

private class SecretProxySelector(
    private val secrets: UserSecrets,
) : ProxySelector() {
    override fun select(uri: URI?): List<Proxy> = listOf(proxyEndpoint(secrets).proxy)

    override fun connectFailed(
        uri: URI?,
        sa: SocketAddress?,
        ioe: IOException?,
    ) = Unit
}

private class SecretProxyAuthenticator(
    private val secrets: UserSecrets,
) : Authenticator {
    override fun authenticate(
        route: Route?,
        response: Response,
    ): Request? {
        if (response.request.header("Proxy-Authorization") != null) return null
        val endpoint = proxyEndpoint(secrets)
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

private fun proxyEndpoint(secrets: UserSecrets): ProxyEndpoint {
    val raw = secrets.get(SecretSlot.ProxyUrl)?.trim().orEmpty()
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
            URI(value)
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
