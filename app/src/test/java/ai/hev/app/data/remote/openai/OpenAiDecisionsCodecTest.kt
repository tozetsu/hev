package ai.hev.app.data.remote.openai

import ai.hev.app.domain.decision.DecisionError
import ai.hev.app.domain.decision.DecisionKind
import ai.hev.app.domain.decision.DecisionOutcome
import ai.hev.app.domain.decision.TokenUsage
import ai.hev.app.testing.Fixtures
import ai.hev.app.testing.Requests
import ai.hev.app.testing.assertJsonEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

/**
 * Response fixtures: https://platform.openai.com/docs/guides/decisions (openai/) and
 * https://vercel.com/docs/ai-gateway/sdks-and-apis/openai-decisions (vercel/).
 */
class OpenAiDecisionsCodecTest {

    @Test
    fun `encodes choice options as values with descriptions`() {
        assertJsonEquals(
            Fixtures.read("golden/openai_choice.json"),
            OpenAiDecisionsCodec.encode(Requests.choice("gpt-6-luna")),
        )
    }

    @Test
    fun `encodes score levels as labels`() {
        assertJsonEquals(
            Fixtures.read("golden/openai_score.json"),
            OpenAiDecisionsCodec.encode(Requests.score("gpt-6-luna")),
        )
    }

    @Test
    fun `encodes yes-no as predicate`() {
        assertJsonEquals(
            Fixtures.read("golden/openai_yes_no.json"),
            OpenAiDecisionsCodec.encode(Requests.yesNo("gpt-6-luna")),
        )
    }

    @Test
    fun `decodes predicate answer`() {
        val result = OpenAiDecisionsCodec.decode(Fixtures.read("openai/response_predicate.json"), DecisionKind.YesNo)

        assertEquals(DecisionOutcome.YesNo(0.92), result.outcome)
        assertNull(result.model)
    }

    @Test
    fun `decodes choice probabilities array into a map`() {
        val result = OpenAiDecisionsCodec.decode(Fixtures.read("openai/response_choice.json"), DecisionKind.Choice)

        assertEquals(
            DecisionOutcome.Choice(
                choice = "billing",
                probabilities = mapOf("billing" to 0.95, "technical" to 0.02, "shipping" to 0.01, "other" to 0.02),
                confidence = 0.93,
            ),
            result.outcome,
        )
    }

    @Test
    fun `decodes score probabilities by level value`() {
        val result = OpenAiDecisionsCodec.decode(Fixtures.read("openai/response_score.json"), DecisionKind.Score)

        assertEquals(DecisionOutcome.Score(1.1, mapOf(0 to 0.1, 1 to 0.7, 2 to 0.2), 0.55), result.outcome)
    }

    @Test
    fun `picks the named answer from several`() {
        val raw = Fixtures.read("vercel/decisions_response.json")

        val result = OpenAiDecisionsCodec.decode(raw, DecisionKind.Score, questionName = "urgency")

        assertEquals(DecisionOutcome.Score(1.3, mapOf(0 to 0.08, 1 to 0.54, 2 to 0.38), 0.62), result.outcome)
        assertEquals("openai/gpt-6-luna-decisions", result.model)
        assertEquals(TokenUsage(96, 0), result.usage)
    }

    /** The docs describe the refusal answer shape but publish no full example. */
    @Test
    fun `refusal maps to refused for any kind`() {
        val raw = """{"answers": [{"type": "refusal", "name": "decision"}], "usage": {"input_tokens": 41}}"""
        DecisionKind.entries.forEach { kind ->
            assertEquals(DecisionOutcome.Refused, OpenAiDecisionsCodec.decode(raw, kind).outcome)
        }
    }

    @Test
    fun `answer of another kind is malformed`() {
        assertThrows(DecisionError.MalformedResponse::class.java) {
            OpenAiDecisionsCodec.decode(Fixtures.read("openai/response_predicate.json"), DecisionKind.Choice)
        }
    }

    @Test
    fun `empty answers are malformed`() {
        assertThrows(DecisionError.MalformedResponse::class.java) {
            OpenAiDecisionsCodec.decode("""{"answers": []}""", DecisionKind.YesNo)
        }
    }

    @Test
    fun `boolean choice values are read as text`() {
        val raw = """
            {"answers": [{"type": "choice", "name": "decision", "choice": true,
              "probabilities": [{"value": true, "probability": 0.7}, {"value": false, "probability": 0.3}]}]}
        """.trimIndent()

        val outcome = OpenAiDecisionsCodec.decode(raw, DecisionKind.Choice).outcome as DecisionOutcome.Choice

        assertEquals("true", outcome.choice)
        assertEquals(mapOf("true" to 0.7, "false" to 0.3), outcome.probabilities)
        assertNull(outcome.confidence)
    }
}
