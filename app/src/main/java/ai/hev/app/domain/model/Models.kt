package ai.hev.app.domain.model

data class ProviderConfig(
    val id: String,
    val name: String,
    val baseUrl: String,
    val apiKey: String,
    val model: String,
)

enum class QuestionType(val apiValue: String) {
    Choice("choice"),
    Score("score"),
    Noul("noul");

    companion object {
        fun from(value: String?): QuestionType =
            entries.firstOrNull { it.apiValue.equals(value, ignoreCase = true) } ?: Choice
    }
}

data class ChoiceOption(
    val id: String,
    val label: String,
)

data class ChoiceRequest(
    val question: String,
    val options: List<ChoiceOption>,
    val state: String? = null,
    val model: String,
)

data class ScoreRequest(
    val question: String,
    val levels: List<String>,
    val state: String? = null,
    val model: String,
)

data class NoulRequest(
    val question: String,
    val state: String? = null,
    val model: String,
)

data class DecideResult(
    val questionType: QuestionType,
    val choice: String? = null,
    val score: Double? = null,
    val noul: Double? = null,
    val probabilities: Map<String, Double> = emptyMap(),
    val confidence: Double? = null,
    val model: String? = null,
    val inputTokens: Int? = null,
    val outputTokens: Int? = null,
    val rawJson: String = "",
)

data class HistoryEntry(
    val id: Long = 0,
    val createdAt: Long,
    val questionType: QuestionType = QuestionType.Choice,
    val question: String,
    val state: String?,
    val optionsJson: String,
    val probabilitiesJson: String,
    val confidence: Double?,
    val choice: String?,
    val score: Double? = null,
    val noul: Double? = null,
    val model: String?,
    val providerName: String?,
    val rawJson: String,
)

data class ModelInfo(
    val id: String,
    val ownedBy: String? = null,
)
