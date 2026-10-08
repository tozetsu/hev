package ai.hev.app.data.local.db

import ai.hev.app.domain.decision.ChoiceOption
import ai.hev.app.domain.decision.DecisionKind
import ai.hev.app.domain.decision.DecisionOutcome
import ai.hev.app.domain.history.HistoryEntry
import ai.hev.app.domain.provider.DecisionProtocol
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HistoryMapperTest {
    private fun entry(kind: DecisionKind, outcome: DecisionOutcome?, items: List<ChoiceOption> = emptyList()) =
        HistoryEntry(
            id = 7,
            createdAt = 1_000,
            kind = kind,
            instructions = "Q?",
            context = "ctx",
            items = items,
            outcome = outcome,
            model = "m",
            providerName = "P",
            rawJson = "{}",
        )

    @Test
    fun `entries survive a round trip`() {
        listOf(
            entry(
                DecisionKind.Choice,
                DecisionOutcome.Choice("a", mapOf("a" to 0.9, "b" to 0.1), 0.8),
                listOf(ChoiceOption("a", "Yes"), ChoiceOption("b", "No")),
            ),
            entry(
                DecisionKind.Score,
                DecisionOutcome.Score(1.2, mapOf(0 to 0.1, 1 to 0.6, 2 to 0.3), null),
                listOf(ChoiceOption("0", "L"), ChoiceOption("1", "M"), ChoiceOption("2", "H")),
            ),
            entry(DecisionKind.YesNo, DecisionOutcome.YesNo(0.4)),
            entry(DecisionKind.Choice, DecisionOutcome.Refused, listOf(ChoiceOption("a", "A"), ChoiceOption("b", "B"))),
            entry(DecisionKind.YesNo, DecisionOutcome.YesNo(0.7)).copy(
                providerId = "p1",
                protocol = DecisionProtocol.OpenAiDecisions,
                inputTokens = 96,
            ),
        ).forEach { original ->
            assertEquals(original, HistoryMapper.toDomain(HistoryMapper.toEntity(original)))
        }
    }

    @Test
    fun `legacy rows read with defaults`() {
        val legacy = HistoryEntity(
            createdAt = 1,
            questionType = "",
            question = "Q?",
            state = null,
            optionsJson = "",
            probabilitiesJson = "not json",
            confidence = null,
            choice = null,
            model = null,
            providerName = null,
            rawJson = "",
        )
        val entry = HistoryMapper.toDomain(legacy)
        assertEquals(DecisionKind.Choice, entry.kind)
        assertEquals(emptyList<ChoiceOption>(), entry.items)
        assertNull(entry.outcome)
        assertNull(entry.providerId)
        assertNull(entry.protocol)
        assertNull(entry.inputTokens)
    }

    @Test
    fun `refusal and provider details are stored in their own columns`() {
        val entity = HistoryMapper.toEntity(
            entry(DecisionKind.Score, DecisionOutcome.Refused).copy(
                providerId = "p1",
                protocol = DecisionProtocol.SystemOne,
                inputTokens = 12,
            ),
        )

        assertEquals(true, entity.refused)
        assertEquals("score", entity.questionType)
        assertEquals("system_one", entity.protocol)
        assertEquals("p1", entity.providerId)
        assertEquals(12, entity.inputTokens)
        assertNull(entity.score)
    }

    @Test
    fun `yes-no is stored under its own key`() {
        assertEquals("yes_no", HistoryMapper.toEntity(entry(DecisionKind.YesNo, DecisionOutcome.YesNo(0.4))).questionType)
    }
}
