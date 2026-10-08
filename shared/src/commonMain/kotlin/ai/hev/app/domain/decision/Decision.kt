package ai.hev.app.domain.decision

/** One option of a choice question, or one level of a score question (id = level index). */
data class ChoiceOption(
    val id: String,
    val label: String,
)

/** What a request asks; each kind carries only the inputs it needs. */
sealed interface Question {
    val kind: DecisionKind

    data class Choice(val options: List<ChoiceOption>) : Question {
        override val kind get() = DecisionKind.Choice
    }

    /** Levels ordered low → high; the index of a level is its score. */
    data class Score(val levels: List<String>) : Question {
        override val kind get() = DecisionKind.Score
    }

    data object YesNo : Question {
        override val kind get() = DecisionKind.YesNo
    }
}

data class DecisionRequest(
    val model: String,
    val instructions: String,
    /** User-supplied context; null when left empty. */
    val context: String?,
    val question: Question,
) {
    val kind: DecisionKind get() = question.kind

    /** Every protocol requires a state; an empty context falls back to the question text. */
    val state: String get() = context ?: instructions
}

/** A typed answer. Probabilities and confidence are as reported by the provider. */
sealed interface DecisionOutcome {
    data class Choice(
        val choice: String,
        /** Option id → probability. */
        val probabilities: Map<String, Double>,
        val confidence: Double?,
    ) : DecisionOutcome

    data class Score(
        /** Probability-weighted level index; may fall between levels. */
        val score: Double,
        /** Level index → probability. */
        val probabilities: Map<Int, Double>,
        val confidence: Double?,
    ) : DecisionOutcome

    data class YesNo(
        /** Probability that the answer is yes, 0–1. */
        val probability: Double,
    ) : DecisionOutcome

    /** The model declined to answer. */
    data object Refused : DecisionOutcome
}

data class TokenUsage(
    val inputTokens: Int? = null,
    val outputTokens: Int? = null,
)

data class DecisionResult(
    val outcome: DecisionOutcome,
    /** Model that answered, as echoed by the provider. */
    val model: String?,
    val usage: TokenUsage,
    val rawJson: String,
)
