package ai.hev.app.testing

import ai.hev.app.domain.decision.ChoiceOption
import ai.hev.app.domain.decision.DecisionRequest
import ai.hev.app.domain.decision.Question

/** Requests mirroring the TypeSafe API reference examples. */
object Requests {
    private const val TICKET = "Help! My payouts have been failing for 3 days."

    fun choice(model: String) = DecisionRequest(
        model = model,
        instructions = "Which team should handle this?",
        context = TICKET,
        question = Question.Choice(
            listOf(
                ChoiceOption("a", "Payments, invoicing, refunds"),
                ChoiceOption("b", "Bugs, outages, integrations"),
            ),
        ),
    )

    fun score(model: String) = DecisionRequest(
        model = model,
        instructions = "How frustrated is the customer?",
        context = TICKET,
        question = Question.Score(listOf("Calm", "Frustrated", "Very angry")),
    )

    /** No context: the question doubles as the state. */
    fun yesNo(model: String) = DecisionRequest(
        model = model,
        instructions = "Does this convey urgency?",
        context = null,
        question = Question.YesNo,
    )
}
