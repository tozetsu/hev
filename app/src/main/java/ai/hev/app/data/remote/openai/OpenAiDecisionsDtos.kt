package ai.hev.app.data.remote.openai

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonPrimitive

@Serializable
internal data class OpenAiDecisionsRequest(
    val model: String,
    val input: String,
    val questions: List<OpenAiQuestion>,
)

@Serializable
internal data class OpenAiQuestion(
    val type: String,
    val name: String,
    val instructions: String,
    val choices: List<OpenAiChoice>? = null,
    val levels: List<OpenAiLevel>? = null,
)

@Serializable
internal data class OpenAiChoice(
    val value: String,
    val description: String,
)

@Serializable
internal data class OpenAiLevel(
    val label: String,
)

@Serializable
internal data class OpenAiDecisionsResponse(
    val model: String? = null,
    val answers: List<OpenAiAnswer> = emptyList(),
    val usage: OpenAiUsage? = null,
)

@Serializable
internal data class OpenAiAnswer(
    val type: String,
    val name: String? = null,
    /** A supplied choice value: string or boolean. */
    val choice: JsonPrimitive? = null,
    val probability: Double? = null,
    val score: Double? = null,
    val confidence: Double? = null,
    val probabilities: List<OpenAiProbability>? = null,
)

@Serializable
internal data class OpenAiProbability(
    /** Choice value (string or boolean) or score level index. */
    val value: JsonPrimitive,
    val probability: Double,
)

@Serializable
internal data class OpenAiUsage(
    @SerialName("input_tokens") val inputTokens: Int? = null,
    @SerialName("output_tokens") val outputTokens: Int? = null,
)
