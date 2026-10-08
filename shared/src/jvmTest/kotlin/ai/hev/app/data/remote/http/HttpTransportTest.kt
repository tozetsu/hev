package ai.hev.app.data.remote.http

import ai.hev.app.domain.decision.DecisionError
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import okhttp3.Headers.Companion.headersOf
import mockwebserver3.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class HttpTransportTest {
    private val server = MockWebServer()
    private val transport = HttpTransport()
    private val url get() = server.url("/v1/systemone").toString()

    @Before fun setUp() = server.start()
    @After fun tearDown() = server.close()

    private suspend inline fun <reified T : DecisionError> expectError(block: () -> Unit): T {
        try {
            block()
        } catch (e: DecisionError) {
            assertTrue("Expected ${T::class.simpleName}, got $e", e is T)
            return e as T
        }
        fail("Expected ${T::class.simpleName}")
        throw AssertionError()
    }

    @Test
    fun `posts json with bearer auth to the url as given`() = runTest {
        server.enqueue(MockResponse(body = "{}"))

        assertEquals("{}", transport.post("$url?x=1", " key ", """{"a":1}"""))

        val recorded = server.takeRequest()
        assertEquals("POST", recorded.method)
        assertEquals("/v1/systemone?x=1", recorded.target)
        assertEquals("Bearer key", recorded.headers["Authorization"])
        assertEquals("""{"a":1}""", recorded.body?.utf8())
        assertTrue(recorded.headers["Content-Type"]!!.startsWith("application/json"))
    }

    @Test
    fun `omits auth when no key is set`() = runTest {
        server.enqueue(MockResponse(body = "{}"))

        transport.get(url, "")

        assertNull(server.takeRequest().headers["Authorization"])
    }

    @Test
    fun `retries once after the requested delay`() = runTest {
        server.enqueue(MockResponse(code = 429, headers = headersOf("Retry-After", "3")))
        server.enqueue(MockResponse(body = """{"ok":true}"""))

        assertEquals("""{"ok":true}""", transport.post(url, "k", "{}"))

        assertEquals(2, server.requestCount)
        assertEquals(3_000, testScheduler.currentTime)
    }

    @Test
    fun `caps long retry delays`() = runTest {
        server.enqueue(MockResponse(code = 503, headers = headersOf("Retry-After", "120")))
        server.enqueue(MockResponse(body = "{}"))

        transport.post(url, "k", "{}")

        assertEquals(10_000, testScheduler.currentTime)
    }

    @Test
    fun `uses a short default delay without retry-after`() = runTest {
        server.enqueue(MockResponse(code = 529))
        server.enqueue(MockResponse(body = "{}"))

        transport.post(url, "k", "{}")

        assertEquals(2, server.requestCount)
        assertEquals(1_000, testScheduler.currentTime)
    }

    @Test
    fun `second transient failure is surfaced`() = runTest {
        repeat(2) {
            server.enqueue(MockResponse(code = 429, body = """{"error":{"message":"slow down"}}"""))
        }

        val error = expectError<DecisionError.RateLimited> { transport.post(url, "k", "{}") }

        assertEquals("slow down", error.detail)
        assertEquals(2, server.requestCount)
    }

    @Test
    fun `client errors are not retried`() = runTest {
        server.enqueue(MockResponse(code = 422, body = """{"detail":"bad"}"""))

        val error = expectError<DecisionError.InvalidRequest> { transport.post(url, "k", "{}") }

        assertEquals(422, error.status)
        assertEquals("bad", error.detail)
        assertEquals(1, server.requestCount)
    }

    @Test
    fun `status codes map to error kinds`() = runTest {
        mapOf(
            401 to DecisionError.Unauthorized::class,
            403 to DecisionError.Unauthorized::class,
            404 to DecisionError.InvalidRequest::class,
            500 to DecisionError.Server::class,
            502 to DecisionError.Server::class,
            504 to DecisionError.GatewayTimeout::class,
        ).forEach { (status, type) ->
            server.enqueue(MockResponse(code = status, body = "e$status"))
            val error = expectError<DecisionError.Http> { transport.post(url, "k", "{}") }
            assertEquals(type, error::class)
            assertEquals("e$status", error.detail)
        }
    }

    @Test
    fun `connection failures become network errors`() = runTest {
        val deadUrl = url
        server.close()

        expectError<DecisionError.Network> { transport.post(deadUrl, "k", "{}") }
    }

    @Test
    fun `retry-after accepts seconds and http dates`() {
        val now = Instant.parse("2026-10-08T07:00:00Z")
        assertEquals(5L, RetryPolicy.parseRetryAfter("5", now))
        assertEquals(30L, RetryPolicy.parseRetryAfter("Thu, 08 Oct 2026 07:00:30 GMT", now))
        assertEquals(0L, RetryPolicy.parseRetryAfter("Thu, 08 Oct 2026 06:00:00 GMT", now))
        assertNull(RetryPolicy.parseRetryAfter("soon", now))
        assertNull(RetryPolicy.parseRetryAfter(null, now))
    }
}
