package ai.hev.app.data.local

import ai.hev.app.data.local.db.HistoryEntity
import ai.hev.app.data.local.db.HistoryMapper
import ai.hev.app.domain.decision.ChoiceOption
import ai.hev.app.domain.decision.DecisionKind
import ai.hev.app.domain.decision.DecisionOutcome
import ai.hev.app.domain.history.HistoryEntry
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
    }
}
