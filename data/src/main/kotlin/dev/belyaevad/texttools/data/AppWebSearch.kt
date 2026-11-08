package dev.belyaevad.texttools.data

import dev.belyaevad.texttools.domain.effectiveProxy
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout

/** Looks up an executable on the public web. A failure returns an empty note, not an error. */
class AppWebSearch(
    private val http: HttpClient,
    private val proxied: HttpClient = http,
) {
    /** Short search hits for [proc]. Blank when the search does not answer. */
    suspend fun notes(
        proc: String,
        title: String,
        proxy: String = "",
        proxyOn: Boolean? = false,
    ): String {
        val name = proc.trim()
        if (name.isEmpty()) return ""
        val query =
            buildString {
                append(name)
                append(" windows application")
                val short = title.trim().take(TITLE_LIMIT)
                if (short.isNotEmpty()) {
                    append(' ')
                    append(short)
                }
            }
        return try {
            val client = if (effectiveProxy(proxy, proxyOn).isNotEmpty()) proxied else http
            val response =
                withTimeout(SEARCH_TIMEOUT_MILLIS) {
                    client.get(SEARCH_URL) {
                        parameter("q", query)
                        header("User-Agent", USER_AGENT)
                    }
                }
            if (!response.status.isSuccess()) "" else duckSnippets(response.bodyAsText())
        } catch (_: TimeoutCancellationException) {
            ""
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            ""
        }
    }
}

/** Titles and snippets from a DuckDuckGo HTML result page. At most four hits. */
fun duckSnippets(html: String): String {
    val titles = pieces(html, TITLE_PATTERN)
    val snippets = pieces(html, SNIPPET_PATTERN)
    val count = maxOf(titles.size, snippets.size).coerceAtMost(HIT_LIMIT)
    return (0 until count).joinToString("\n") { index ->
        val title = titles.getOrElse(index) { "" }
        val snippet = snippets.getOrElse(index) { "" }
        when {
            title.isEmpty() -> "- $snippet"
            snippet.isEmpty() -> "- $title"
            else -> "- $title: $snippet"
        }
    }
}

private fun pieces(
    html: String,
    pattern: Regex,
): List<String> =
    pattern
        .findAll(html)
        .map { cleanSnippet(it.groupValues[1]) }
        .filter { it.isNotEmpty() }
        .toList()

private fun cleanSnippet(raw: String): String =
    raw
        .replace(TAG, "")
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .replace("&nbsp;", " ")
        .replace(WHITESPACE, " ")
        .trim()

private const val SEARCH_URL: String = "https://html.duckduckgo.com/html/"
private const val USER_AGENT: String = "Mozilla/5.0 TextTools"
private const val SEARCH_TIMEOUT_MILLIS: Long = 8_000
private const val TITLE_LIMIT: Int = 80
private const val HIT_LIMIT: Int = 4

private val TITLE_PATTERN =
    Regex("""class="result__a"[^>]*>(.*?)</a>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
private val SNIPPET_PATTERN =
    Regex(
        """class="result__snippet"[^>]*>(.*?)</(?:a|td|div|span)>""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
    )
private val TAG = Regex("<[^>]+>")
private val WHITESPACE = Regex("\\s+")
