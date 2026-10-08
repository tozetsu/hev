package ai.hev.app.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class SystemOneRequest(
    val model: String,
    val state: String,
    val questions: Map<String, QuestionDto>,
)

@Serializable
data class QuestionDto(
    val type: String = "choice",
    val instructions: String,
    /** Choice: object map; score: string array; noul: omitted. */
    val criteria: JsonElement? = null,
)

@Serializable
data class SystemOneResponse(
    val answers: Map<String, AnswerDto>? = null,
    val response: ResponseMeta? = null,
    val usage: UsageDto? = null,
    val model: String? = null,
)

@Serializable
data class AnswerDto(
    val type: String? = null,
    val choice: String? = null,
    val score: Double? = null,
    val noul: Double? = null,
    val probabilities: Map<String, Double>? = null,
    val confidence: Double? = null,
    /** Score: level index → level text. */
    val legend: Map<String, String>? = null,
)

@Serializable
data class ResponseMeta(
    val model: String? = null,
)

@Serializable
data class UsageDto(
    @SerialName("input_tokens") val inputTokens: Int? = null,
    @SerialName("output_tokens") val outputTokens: Int? = null,
)

@Serializable
data class ModelsListResponse(
    val data: List<ModelItemDto>? = null,
)

@Serializable
data class ModelItemDto(
    val id: String,
    @SerialName("owned_by") val ownedBy: String? = null,
)
