package ai.hev.app.domain.decision

import org.junit.Assert.assertEquals
import org.junit.Test

class DecisionItemsTest {
    @Test
    fun `choice ids are excel style`() {
        assertEquals(listOf("a", "z", "aa", "az", "ba", "iu"), listOf(0, 25, 26, 51, 52, 254).map(::choiceOptionId))
    }

    @Test
    fun `added choice option takes the first free id`() {
        val items = listOf(ChoiceOption("a", "x"), ChoiceOption("c", "y"))
        assertEquals("b", DecisionItems.added(DecisionKind.Choice, items).last().id)
    }

    @Test
    fun `removing a level renumbers the rest`() {
        val levels = DecisionItems.added(DecisionKind.Score, DecisionItems.initial(DecisionKind.Score))
        val ids = DecisionItems.removed(DecisionKind.Score, levels, "0").map { it.id }
        assertEquals(listOf("0", "1"), ids)
    }

    @Test
    fun `structured text drops prefixes and blanks and respects the cap`() {
        val text = "1. Alpha\n\n- Beta\n* Gamma\n• Delta\n2) Epsilon"
        assertEquals(listOf("Alpha", "Beta", "Gamma"), parseStructuredOptionLines(text, max = 3))
    }
}
