package ai.hev.app.data.remote.systemone

import ai.hev.app.domain.decision.DecisionError
import ai.hev.app.domain.decision.DecisionKind
import ai.hev.app.domain.decision.DecisionOutcome
import ai.hev.app.domain.decision.TokenUsage
import ai.hev.app.testing.Fixtures
import ai.hev.app.testing.Requests
import ai.hev.app.testing.assertJsonEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

/** Response fixtures: https://docs.typesafe.ai/api */
class SystemOneCodecTest {

    @Test
    fun `encodes choice criteria as an option map`() {
        assertJsonEquals(
            Fixtures.read("golden/system_one_choice.json"),
            SystemOneCodec.encode(Requests.choice("jev-latest")),
        )
    }

    @Test
    fun `encodes score criteria as an ordered array`() {
        assertJsonEquals(
            Fixtures.read("golden/system_one_score.json"),
            SystemOneCodec.encode(Requests.score("jev-latest")),
        )
    }

    @Test
    fun `encodes yes-no as noul without criteria`() {
        assertJsonEquals(
            Fixtures.read("golden/system_one_yes_no.json"),
            SystemOneCodec.encode(Requests.yesNo("jev-latest")),
        )
    }

    @Test
    fun `decodes choice answer`() {
        val result = SystemOneCodec.decode(Fixtures.read("typesafe/response_choice.json"), DecisionKind.Choice)

        assertEquals(
            DecisionOutcome.Choice("billing", mapOf("billing" to 0.88, "technical" to 0.12, "sales" to 0.0), 0.81),
            result.outcome,
        )
        assertEquals("jev-1.13.0", result.model)
        assertEquals(TokenUsage(318, 34), result.usage)
    }

    @Test
    fun `decodes score answer keyed by level index`() {
        val result = SystemOneCodec.decode(Fixtures.read("typesafe/response_score.json"), DecisionKind.Score)

        assertEquals(DecisionOutcome.Score(1.05, mapOf(0 to 0.0, 1 to 0.95, 2 to 0.05), 0.92), result.outcome)
    }

    @Test
    fun `decodes noul answer as yes probability`() {
        val result = SystemOneCodec.decode(Fixtures.read("typesafe/response_noul.json"), DecisionKind.YesNo)

        assertEquals(DecisionOutcome.YesNo(0.95), result.outcome)
        assertEquals(TokenUsage(307, 20), result.usage)
    }

    @Test
    fun `keeps the raw response`() {
        val raw = Fixtures.read("typesafe/response_noul.json")
        assertEquals(raw, SystemOneCodec.decode(raw, DecisionKind.YesNo).rawJson)
    }

    @Test
    fun `answer missing the value for the asked kind is malformed`() {
        assertThrows(DecisionError.MalformedResponse::class.java) {
            SystemOneCodec.decode(Fixtures.read("typesafe/response_noul.json"), DecisionKind.Choice)
        }
    }

    @Test
    fun `several answers without ours is malformed`() {
        assertThrows(DecisionError.MalformedResponse::class.java) {
            SystemOneCodec.decode(Fixtures.read("perplexity/response.json"), DecisionKind.Choice)
        }
    }

    @Test
    fun `non json body is malformed`() {
        assertThrows(DecisionError.MalformedResponse::class.java) {
            SystemOneCodec.decode("<html>502</html>", DecisionKind.Choice)
        }
    }
}
