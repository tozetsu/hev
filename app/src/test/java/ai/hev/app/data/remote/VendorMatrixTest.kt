package ai.hev.app.data.remote

import ai.hev.app.data.remote.http.HttpTransport
import ai.hev.app.domain.decision.DecisionError
import ai.hev.app.domain.decision.DecisionKind
import ai.hev.app.domain.decision.DecisionOutcome
import ai.hev.app.domain.decision.DecisionRequest
import ai.hev.app.domain.decision.TokenUsage
import ai.hev.app.domain.provider.DecisionProtocol
import ai.hev.app.domain.provider.ProviderPreset
import ai.hev.app.domain.provider.ProviderPresets
import ai.hev.app.testing.Fixtures
import ai.hev.app.testing.Requests
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

/**
 * Every documented vendor response and error body, sent through [DecisionClient] against each
 * preset's own path. Multi-question examples are narrowed to one answer, renamed to the question
 * name the client sends, the way the server would echo it.
 */
class VendorMatrixTest {
    private val server = MockWebServer()
    private val client = DecisionClient(HttpTransport())

    @Before fun setUp() = server.start()
    @After fun tearDown() = server.shutdown()

    private data class AnswerCase(
        val preset: ProviderPreset,
        val fixture: String,
        val answer: String?,
        val kind: DecisionKind,
        val expected: DecisionOutcome,
    )

    private val answers = listOf(
        AnswerCase(
            ProviderPresets.TypeSafe, "typesafe/response_choice.json", null, DecisionKind.Choice,
            DecisionOutcome.Choice("billing", mapOf("billing" to 0.88, "technical" to 0.12, "sales" to 0.0), 0.81),
        ),
        AnswerCase(
            ProviderPresets.TypeSafe, "typesafe/response_score.json", null, DecisionKind.Score,
            DecisionOutcome.Score(1.05, mapOf(0 to 0.0, 1 to 0.95, 2 to 0.05), 0.92),
        ),
        AnswerCase(
            ProviderPresets.TypeSafe, "typesafe/response_noul.json", null, DecisionKind.YesNo,
            DecisionOutcome.YesNo(0.95),
        ),
        AnswerCase(
            ProviderPresets.Perplexity, "perplexity/response.json", "defect", DecisionKind.YesNo,
            DecisionOutcome.YesNo(0.9424522889347015),
        ),
        AnswerCase(
            ProviderPresets.Perplexity, "perplexity/response.json", "sentiment", DecisionKind.Choice,
            DecisionOutcome.Choice(
                "mixed",
                mapOf("positive" to 0.020649883775315993, "mixed" to 0.9503497962668123, "negative" to 0.02900031995787183),
                0.9255246944002182,
            ),
        ),
        AnswerCase(
            ProviderPresets.Perplexity, "perplexity/response.json", "severity", DecisionKind.Score,
            DecisionOutcome.Score(
                1.7838686319784252,
                mapOf(0 to 0.008423954913615923, 1 to 0.199283458194343, 2 to 0.7922925868920411),
                0.7838686319784252,
            ),
        ),
        AnswerCase(
            ProviderPresets.AlibabaBeijing, "alibaba/response.json", "department", DecisionKind.Choice,
            DecisionOutcome.Choice("billing", mapOf("billing" to 0.94, "technical" to 0.06), 0.88),
        ),
        AnswerCase(
            ProviderPresets.AlibabaSingapore, "alibaba/response.json", "escalate", DecisionKind.YesNo,
            DecisionOutcome.YesNo(0.99),
        ),
        AnswerCase(
            ProviderPresets.AlibabaBeijing, "alibaba/response.json", "severity", DecisionKind.Score,
            DecisionOutcome.Score(2.25, mapOf(0 to 0.0, 1 to 0.01, 2 to 0.73, 3 to 0.26), 0.91),
        ),
        AnswerCase(
            ProviderPresets.Ollama, "ollama/response_choice.json", null, DecisionKind.Choice,
            DecisionOutcome.Choice("bug", mapOf("billing" to 0.0125, "bug" to 0.9781, "account" to 0.0093), 0.8906),
        ),
        AnswerCase(
            ProviderPresets.Ollama, "ollama/response_image.json", "has_ollama", DecisionKind.YesNo,
            DecisionOutcome.YesNo(0.959),
        ),
        AnswerCase(
            ProviderPresets.Ollama, "ollama/response_image.json", "app", DecisionKind.Choice,
            DecisionOutcome.Choice(
                "vscode",
                mapOf("vscode" to 0.966, "other" to 0.017, "browser" to 0.010, "terminal" to 0.007),
                0.868,
            ),
        ),
        AnswerCase(
            ProviderPresets.VercelSystemOne, "vercel/typesafe_response.json", null, DecisionKind.YesNo,
            DecisionOutcome.YesNo(0.98),
        ),
        AnswerCase(
            ProviderPresets.OpenAi, "openai/response_predicate.json", null, DecisionKind.YesNo,
            DecisionOutcome.YesNo(0.92),
        ),
        AnswerCase(
            ProviderPresets.OpenAi, "openai/response_choice.json", null, DecisionKind.Choice,
            DecisionOutcome.Choice(
                "billing",
                mapOf("billing" to 0.95, "technical" to 0.02, "shipping" to 0.01, "other" to 0.02),
                0.93,
            ),
        ),
        AnswerCase(
            ProviderPresets.OpenAi, "openai/response_score.json", null, DecisionKind.Score,
            DecisionOutcome.Score(1.1, mapOf(0 to 0.1, 1 to 0.7, 2 to 0.2), 0.55),
        ),
        AnswerCase(
            ProviderPresets.VercelDecisions, "vercel/decisions_response.json", "damaged", DecisionKind.YesNo,
            DecisionOutcome.YesNo(0.95),
        ),
        AnswerCase(
            ProviderPresets.VercelDecisions, "vercel/decisions_response.json", "queue", DecisionKind.Choice,
            DecisionOutcome.Choice("shipping", mapOf("billing" to 0.19, "shipping" to 0.81), 0.81),
        ),
        AnswerCase(
            ProviderPresets.VercelDecisions, "vercel/decisions_response.json", "urgency", DecisionKind.Score,
            DecisionOutcome.Score(1.3, mapOf(0 to 0.08, 1 to 0.54, 2 to 0.38), 0.62),
        ),
    )

    @Test
    fun `documented answers decode for every preset`() = runTest {
        answers.forEach { case ->
            val label = "${case.preset.id} ${case.fixture} ${case.answer ?: ""}"
            server.enqueue(MockResponse().setBody(narrowed(case.preset.protocol, Fixtures.read(case.fixture), case.answer)))
            val request = request(case.kind, case.preset.models.first())
            val apiKey = if (case.preset.apiKeyRequired) "test-key" else ""

            val result = client.decide(case.preset.protocol, urlFor(case.preset), apiKey, request)

            assertEquals(label, case.expected, result.outcome)
            val recorded = server.takeRequest()
            assertEquals(label, pathOf(case.preset), recorded.path)
            assertEquals(label, if (apiKey.isEmpty()) null else "Bearer test-key", recorded.getHeader("Authorization"))
            assertSentQuestion(case.preset.protocol, case.kind, case.preset.models.first(), recorded.body.readUtf8(), label)
        }
    }

    @Test
    fun `usage and model come from the response`() = runTest {
        server.enqueue(MockResponse().setBody(narrowed(DecisionProtocol.SystemOne, Fixtures.read("perplexity/response.json"), "defect")))
        server.enqueue(MockResponse().setBody(narrowed(DecisionProtocol.SystemOne, Fixtures.read("alibaba/response.json"), "escalate")))

        val perplexity = client.decide(
            DecisionProtocol.SystemOne, urlFor(ProviderPresets.Perplexity), "k", Requests.yesNo("pplx-decider-v1.1-27b"),
        )
        val alibaba = client.decide(
            DecisionProtocol.SystemOne, urlFor(ProviderPresets.AlibabaBeijing), "k", Requests.yesNo("decision-model-preview"),
        )

        assertEquals("pplx-decider-v1.1-27b", perplexity.model)
        assertEquals(TokenUsage(367, 3), perplexity.usage)
        assertEquals("decision-model-preview", alibaba.model)
        assertEquals(TokenUsage(125, null), alibaba.usage)
    }

    /** The docs describe the refusal answer but publish no full example. */
    @Test
    fun `refusal decodes as refused`() = runTest {
        server.enqueue(MockResponse().setBody("""{"answers": [{"type": "refusal", "name": "decision"}]}"""))

        val result = client.decide(
            DecisionProtocol.OpenAiDecisions, urlFor(ProviderPresets.OpenAi), "k", Requests.choice("gpt-6-luna"),
        )

        assertEquals(DecisionOutcome.Refused, result.outcome)
    }

    private data class ErrorCase(
        val preset: ProviderPreset,
        val status: Int,
        val body: String,
        val expected: Class<out DecisionError.Http>,
        val detail: String,
    )

    private val errors = listOf(
        ErrorCase(
            ProviderPresets.VercelDecisions, 400, Fixtures.read("vercel/decisions_error.json"),
            DecisionError.InvalidRequest::class.java,
            "Image input isn't supported on AI Gateway's Decisions API yet. Send text input.",
        ),
        ErrorCase(
            ProviderPresets.VercelSystemOne, 400, Fixtures.read("vercel/typesafe_error.json"),
            DecisionError.InvalidRequest::class.java,
            "questions.refund.type: expected one of 'noul', 'choice', 'score'",
        ),
        ErrorCase(
            ProviderPresets.Perplexity, 400, Fixtures.read("perplexity/error_noul.json"),
            DecisionError.InvalidRequest::class.java,
            "Noul question must have criteria or instructions",
        ),
        ErrorCase(
            ProviderPresets.Perplexity, 400, Fixtures.read("perplexity/error_invalid_model.json"),
            DecisionError.InvalidRequest::class.java,
            "Invalid model 'pplx-decider-v1.1-27b-latest'. Permitted models can be found in the documentation at " +
                "https://docs.perplexity.ai/docs/getting-started/models.",
        ),
        ErrorCase(
            ProviderPresets.Perplexity, 401, Fixtures.read("perplexity/error_invalid_api_key.json"),
            DecisionError.Unauthorized::class.java,
            "Invalid API key provided. You can find your API key at https://console.perplexity.ai.",
        ),
        ErrorCase(
            ProviderPresets.Perplexity, 404, "",
            DecisionError.InvalidRequest::class.java,
            "Client Error",
        ),
        // Perplexity documents that a 504 body may be an HTML page; this one is illustrative.
        ErrorCase(
            ProviderPresets.Perplexity, 504, "<html><body><h1>504 Gateway Time-out</h1></body></html>",
            DecisionError.GatewayTimeout::class.java,
            "<html><body><h1>504 Gateway Time-out</h1></body></html>",
        ),
        ErrorCase(
            ProviderPresets.Ollama, 413, Fixtures.read("ollama/error_too_large.json"),
            DecisionError.InvalidRequest::class.java,
            "request body must not exceed 64 KiB without images",
        ),
        ErrorCase(
            ProviderPresets.DeepInfra, 422, Fixtures.read("deepinfra/error_validation.json"),
            DecisionError.InvalidRequest::class.java,
            "body.questions.decision.criteria: Dictionary should have at most 52 items after validation, not 60",
        ),
    )

    @Test
    fun `documented error bodies map to typed errors`() = runTest {
        errors.forEach { case ->
            server.enqueue(MockResponse().setResponseCode(case.status).setBody(case.body))

            val error = decideExpectingError(case.preset)

            assertEquals(case.preset.id, case.expected, error.javaClass)
            assertEquals(case.preset.id, case.status, error.status)
            assertEquals(case.preset.id, case.detail, error.detail)
        }
        assertEquals(errors.size, server.requestCount)
    }

    @Test
    fun `rate limit is retried once, then surfaced`() = runTest {
        repeat(2) {
            server.enqueue(
                MockResponse().setResponseCode(429).setHeader("Retry-After", "2")
                    .setBody(Fixtures.read("perplexity/error_rate_limited.json")),
            )
        }

        val error = decideExpectingError(ProviderPresets.Perplexity)

        assertTrue(error is DecisionError.RateLimited)
        assertEquals(2L, (error as DecisionError.RateLimited).retryAfterSeconds)
        assertEquals("Request rate limit exceeded, please try again later.", error.detail)
        assertEquals(2, server.requestCount)
    }

    @Test
    fun `temporary unavailability recovers on retry`() = runTest {
        server.enqueue(MockResponse().setResponseCode(503))
        server.enqueue(MockResponse().setBody(Fixtures.read("typesafe/response_noul.json")))

        val result = client.decide(
            DecisionProtocol.SystemOne, urlFor(ProviderPresets.TypeSafe), "k", Requests.yesNo("jev-latest"),
        )

        assertEquals(DecisionOutcome.YesNo(0.95), result.outcome)
        assertEquals(2, server.requestCount)
    }

    private suspend fun decideExpectingError(preset: ProviderPreset): DecisionError.Http {
        try {
            client.decide(preset.protocol, urlFor(preset), "k", request(DecisionKind.Choice, preset.models.first()))
        } catch (e: DecisionError.Http) {
            return e
        }
        fail("Expected an HTTP error for ${preset.id}")
        throw AssertionError()
    }

    private fun request(kind: DecisionKind, model: String): DecisionRequest = when (kind) {
        DecisionKind.Choice -> Requests.choice(model)
        DecisionKind.Score -> Requests.score(model)
        DecisionKind.YesNo -> Requests.yesNo(model)
    }

    /** The preset's path and query, served by the mock server. */
    private fun pathOf(preset: ProviderPreset): String = "/" + preset.endpoint.substringAfter("://").substringAfter('/')

    private fun urlFor(preset: ProviderPreset): String = server.url(pathOf(preset)).toString()

    private fun assertSentQuestion(protocol: DecisionProtocol, kind: DecisionKind, model: String, body: String, label: String) {
        val json = Json.parseToJsonElement(body).jsonObject
        assertEquals(label, model, json.getValue("model").jsonPrimitive.content)
        val type = when (protocol) {
            DecisionProtocol.SystemOne -> json.getValue("questions").jsonObject.getValue("decision").jsonObject["type"]
            DecisionProtocol.OpenAiDecisions -> json.getValue("questions").jsonArray.single().jsonObject["type"]
        }
        val expected = when (kind) {
            DecisionKind.Choice -> "choice"
            DecisionKind.Score -> "score"
            DecisionKind.YesNo -> if (protocol == DecisionProtocol.SystemOne) "noul" else "predicate"
        }
        assertEquals(label, expected, type?.jsonPrimitive?.content)
    }

    /** Keeps only [answer] from a multi-question example, renamed to the client's question name. */
    private fun narrowed(protocol: DecisionProtocol, raw: String, answer: String?): String {
        if (answer == null) return raw
        val root = Json.parseToJsonElement(raw).jsonObject
        val answers = root.getValue("answers")
        val kept = when (protocol) {
            DecisionProtocol.SystemOne -> JsonObject(mapOf(QUESTION to answers.jsonObject.getValue(answer)))
            DecisionProtocol.OpenAiDecisions -> JsonArray(
                answers.jsonArray
                    .map { it.jsonObject }
                    .filter { it["name"]?.jsonPrimitive?.content == answer }
                    .map { JsonObject(it + ("name" to JsonPrimitive(QUESTION))) },
            )
        }
        return JsonObject(root + ("answers" to kept)).toString()
    }

    private companion object {
        const val QUESTION = DecisionCodec.QUESTION_NAME
    }
}
