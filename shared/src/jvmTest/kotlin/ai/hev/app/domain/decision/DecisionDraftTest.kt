package ai.hev.app.domain.decision

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DecisionDraftTest {
    private val narrow = ModelCapabilities(
        kinds = setOf(DecisionKind.Choice, DecisionKind.Score),
        choiceOptions = 2..3,
        scoreLevels = 2..4,
    )

    private fun choice(vararg labels: String) =
        DecisionDraft(DecisionKind.Choice, "Which?", "", DecisionItems.choiceOptions(labels.toList()))

    @Test
    fun `ready draft has no issue`() {
        assertNull(choice("x", "y").validate(narrow))
    }

    @Test
    fun `kind outside capabilities is rejected`() {
        val draft = DecisionDraft(DecisionKind.YesNo, "Is it?", "", emptyList())
        assertEquals(DraftIssue.UnsupportedKind, draft.validate(narrow))
    }

    @Test
    fun `blank question is rejected`() {
        assertEquals(DraftIssue.MissingQuestion, choice("x", "y").copy(instructions = " ").validate(narrow))
    }

    @Test
    fun `option count must fit the model range`() {
        assertEquals(
            DraftIssue.ItemCount(DecisionKind.Choice, 2..3),
            choice("a", "b", "c", "d").validate(narrow),
        )
    }

    @Test
    fun `blank level is rejected`() {
        val draft = DecisionDraft(DecisionKind.Score, "How?", "", DecisionItems.initial(DecisionKind.Score))
        assertEquals(DraftIssue.BlankItem(DecisionKind.Score), draft.validate(narrow))
    }

    @Test
    fun `yes-no needs no items`() {
        val draft = DecisionDraft(DecisionKind.YesNo, "Is it?", "", emptyList())
        assertNull(draft.validate(ModelCapabilities.Lenient))
    }

    @Test
    fun `request trims input and falls back to the question as state`() {
        val request = choice(" x ", "y").copy(instructions = " Which? ", context = "  ").toRequest("m")

        assertEquals("Which?", request.instructions)
        assertNull(request.context)
        assertEquals("Which?", request.state)
        assertEquals(Question.Choice(listOf(ChoiceOption("a", "x"), ChoiceOption("b", "y"))), request.question)
    }

    @Test
    fun `score request keeps level order`() {
        val draft = DecisionDraft(
            DecisionKind.Score, "How?", "ctx",
            listOf(ChoiceOption("0", "Low"), ChoiceOption("1", "High")),
        )
        val request = draft.toRequest("m")
        assertEquals(Question.Score(listOf("Low", "High")), request.question)
        assertEquals("ctx", request.state)
    }

    @Test
    fun `changing kind resets items, keeping it does not`() {
        val draft = choice("x", "y")
        assertEquals(draft, draft.withKind(DecisionKind.Choice))
        assertEquals(DecisionItems.initial(DecisionKind.Score), draft.withKind(DecisionKind.Score).items)
    }

    @Test
    fun `unsupported kind falls back to the first supported one`() {
        val yesNo = DecisionDraft(DecisionKind.YesNo, "Is it?", "ctx", emptyList())

        val fitted = yesNo.fittedTo(narrow)

        assertEquals(DecisionKind.Choice, fitted.kind)
        assertEquals("Is it?", fitted.instructions)
        assertEquals(yesNo, yesNo.fittedTo(ModelCapabilities.Lenient))
    }
}
