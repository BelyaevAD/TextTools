package dev.belyaevad.texttools.data

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import dev.belyaevad.texttools.data.db.TextToolsDatabase
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Catalogue age, a rejected body, and a failed load. No live network call. */
class PriceCatalogueTest {
    @Test
    fun aFreshCacheIsNotDownloaded() =
        runBlocking {
            val store = memoryStore()
            store.savePrices(PriceCache("kept", 1_000L))
            var hits = 0
            val http = countingClient { hits += 1 }
            val catalogue = PriceCatalogue(http)
            assertTrue(catalogue.refresh(store, nowMillis = 1_000L + 60_000L))
            assertEquals(0, hits)
            assertEquals("kept", store.prices()?.body)
            http.close()
        }

    @Test
    fun aStaleCacheTakesTheNewBody() =
        runBlocking {
            val store = memoryStore()
            store.savePrices(PriceCache("old", 0L))
            val http = bodyClient("""{"data":[{"id":"m"}]}""")
            val catalogue = PriceCatalogue(http)
            val now = DAY_MILLIS + 5
            assertTrue(catalogue.refresh(store, nowMillis = now))
            assertEquals("""{"data":[{"id":"m"}]}""", store.prices()?.body)
            assertEquals(now, store.prices()?.fetchedAtEpochMs)
            http.close()
        }

    @Test
    fun aBodyWithoutTheCatalogueIsRejected() =
        runBlocking {
            val store = memoryStore()
            store.savePrices(PriceCache("kept", 0L))
            var hits = 0
            val http = bodyClient("hello") { hits += 1 }
            val catalogue = PriceCatalogue(http)
            assertTrue(catalogue.refresh(store, nowMillis = DAY_MILLIS + 5))
            assertEquals(1, hits)
            assertEquals("kept", store.prices()?.body)
            http.close()
        }

    @Test
    fun aFailedLoadKeepsThePreviousBody() =
        runBlocking {
            val store = memoryStore()
            store.savePrices(PriceCache("kept", 0L))
            var direct = 0
            var viaProxy = 0
            val plain = countingClient { direct += 1 }
            val proxied = countingClient { viaProxy += 1 }
            val catalogue = PriceCatalogue(http = plain, proxied = proxied)
            assertTrue(
                catalogue.refresh(
                    store,
                    proxy = "http://proxy:1",
                    proxyOn = true,
                    nowMillis = DAY_MILLIS + 5,
                ),
            )
            assertEquals("kept", store.prices()?.body)
            assertEquals(0, direct)
            assertEquals(1, viaProxy)
            plain.close()
            proxied.close()
        }

    @Test
    fun theFirstFailedLoadLeavesTheStoreEmpty() =
        runBlocking {
            val store = memoryStore()
            val http = failingClient()
            val catalogue = PriceCatalogue(http)
            assertFalse(catalogue.refresh(store, nowMillis = 0L))
            assertNull(store.prices())
            http.close()
        }
}

private const val DAY_MILLIS = 24L * 60L * 60L * 1000L

private fun memoryStore(): TextToolsStore {
    val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    TextToolsDatabase.Schema.create(driver)
    return TextToolsStore(driver)
}

private fun bodyClient(
    body: String,
    hit: () -> Unit = {},
): HttpClient =
    HttpClient(MockEngine) {
        engine {
            addHandler {
                hit()
                respond(body, HttpStatusCode.OK, headersOf("Content-Type", "application/json"))
            }
        }
    }

private fun failingClient(): HttpClient =
    HttpClient(MockEngine) {
        engine {
            addHandler { throw IOException("down") }
        }
    }

private fun countingClient(hit: () -> Unit): HttpClient =
    HttpClient(MockEngine) {
        engine {
            addHandler {
                hit()
                throw IOException("down")
            }
        }
    }
