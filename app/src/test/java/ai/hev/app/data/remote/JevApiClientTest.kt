package ai.hev.app.data.remote

import ai.hev.app.domain.decision.DecisionError
import ai.hev.app.domain.decision.DecisionOutcome
import ai.hev.app.domain.decision.DecisionRequest
import ai.hev.app.domain.decision.Question
import ai.hev.app.testing.Fixtures
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test

/** Fixtures: https://docs.typesafe.ai/api (response examples). */
class JevApiClientTest {
    private val server = MockWebServer()
    private val client = JevApiClient()

    @Before fun setUp() = server.start()
    @After fun tearDown() = server.shutdown()

    private fun request(question: Question) =
        DecisionRequest(model = "jev-latest", instructions = "Q?", context = null, question = question)

    @Test
    fun `parses yes-no answer and usage`() {
        server.enqueue(MockResponse().setBody(Fixtures.read("typesafe/response_noul.json")))

        val result = client.decide(server.url("/v1/systemone").toString(), "key", request(Question.YesNo))

        assertEquals(DecisionOutcome.YesNo(0.95), result.outcome)
        assertEquals(307, result.usage.inputTokens)
        assertEquals("jev-1.13.0", result.model)
    }

    @Test
    fun `parses score answer keyed by level index`() {
        server.enqueue(MockResponse().setBody(Fixtures.read("typesafe/response_score.json")))

        val result = client.decide(
            server.url("/v1/systemone").toString(),
            "key",
            request(Question.Score(listOf("Calm", "Frustrated", "Very angry"))),
        )

        val outcome = result.outcome as DecisionOutcome.Score
        assertEquals(1.05, outcome.score, 1e-9)
        assertEquals(0.95, outcome.probabilities.getValue(1), 1e-9)
    }

    @Test
    fun `maps error status to typed error`() {
        server.enqueue(MockResponse().setResponseCode(401).setBody("bad key"))

        val error = assertThrows(DecisionError.Unauthorized::class.java) {
            client.decide(server.url("/v1/systemone").toString(), "key", request(Question.YesNo))
        }
        assertEquals("bad key", error.detail)
    }
}
