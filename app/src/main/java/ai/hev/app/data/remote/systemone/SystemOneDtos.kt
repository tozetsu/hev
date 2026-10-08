package ai.hev.app.data.remote.systemone

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
internal data class SystemOneRequest(
    val model: String,
    val state: String,
    val questions: Map<String, SystemOneQuestion>,
)

@Serializable
internal data class SystemOneQuestion(
    val type: String,
    val instructions: String,
    /** Choice: option → description; score: ordered level array; noul: omitted. */
    val criteria: JsonElement? = null,
)

@Serializable
internal data class SystemOneResponse(
    val model: String? = null,
    val answers: Map<String, SystemOneAnswer> = emptyMap(),
    val usage: SystemOneUsage? = null,
)

@Serializable
internal data class SystemOneAnswer(
    val type: String? = null,
    val choice: String? = null,
    val score: Double? = null,
    val noul: Double? = null,
    val probabilities: Map<String, Double>? = null,
    val confidence: Double? = null,
)

@Serializable
internal data class SystemOneUsage(
    @SerialName("input_tokens") val inputTokens: Int? = null,
    @SerialName("output_tokens") val outputTokens: Int? = null,
)
