package ai.hev.app.data.remote

import ai.hev.app.domain.model.NoulRequest
import ai.hev.app.domain.model.ScoreRequest
import ai.hev.app.testing.Fixtures
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/** Fixtures: https://docs.typesafe.ai/api (response examples). */
class JevApiClientTest {
    private val server = MockWebServer()
    private val client = JevApiClient()

    @Before fun setUp() = server.start()
    @After fun tearDown() = server.shutdown()

    @Test
    fun `parses usage from input and output tokens`() {
        server.enqueue(MockResponse().setBody(Fixtures.read("typesafe/response_noul.json")))

        val result = client.noul(server.url("/v1/systemone").toString(), "key", NoulRequest("Urgent?", model = "jev-latest"))

        assertEquals(0.95, result.noul!!, 1e-9)
        assertEquals(307, result.inputTokens)
        assertEquals(20, result.outputTokens)
        assertEquals("jev-1.13.0", result.model)
    }

    @Test
    fun `parses score answer with level probabilities`() {
        server.enqueue(MockResponse().setBody(Fixtures.read("typesafe/response_score.json")))

        val result = client.score(
            server.url("/v1/systemone").toString(),
            "key",
            ScoreRequest("How frustrated?", listOf("Calm", "Frustrated", "Very angry"), model = "jev-latest"),
        )

        assertEquals(1.05, result.score!!, 1e-9)
        assertEquals(0.95, result.probabilities.getValue("1"), 1e-9)
        assertEquals(0.92, result.confidence!!, 1e-9)
    }
}
