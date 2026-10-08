package ai.hev.app.data.remote.systemone

import ai.hev.app.data.remote.DecisionCodec
import ai.hev.app.data.remote.WireJson
import ai.hev.app.data.remote.decodeWire
import ai.hev.app.domain.decision.DecisionError
import ai.hev.app.domain.decision.DecisionKind
import ai.hev.app.domain.decision.DecisionOutcome
import ai.hev.app.domain.decision.DecisionRequest
import ai.hev.app.domain.decision.DecisionResult
import ai.hev.app.domain.decision.Question
import ai.hev.app.domain.decision.TokenUsage
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * TypeSafe System One wire format (`POST …/v1/systemone`), also served by Perplexity,
 * Alibaba Model Studio, OpenRouter, Vercel, DeepInfra, Liquid and Ollama.
 *
 * Only `model`, `state` and `questions` are sent: some servers reject unknown fields.
 */
object SystemOneCodec : DecisionCodec {

    override fun encode(request: DecisionRequest): String = WireJson.encodeToString(
        SystemOneRequest.serializer(),
        SystemOneRequest(
            model = request.model,
            state = request.state,
            questions = mapOf(
                DecisionCodec.QUESTION_NAME to SystemOneQuestion(
                    type = request.kind.wireType,
                    instructions = request.instructions,
                    criteria = request.question.criteria(),
                ),
            ),
        ),
    )

    override fun decode(raw: String, kind: DecisionKind, questionName: String): DecisionResult {
        val response = decodeWire(SystemOneResponse.serializer(), raw)
        val answer = response.answers[questionName]
            ?: response.answers.values.singleOrNull()
            ?: throw DecisionError.MalformedResponse("Missing answer")
        return DecisionResult(
            outcome = answer.toOutcome(kind),
            model = response.model,
            usage = TokenUsage(response.usage?.inputTokens, response.usage?.outputTokens),
            rawJson = raw,
        )
    }

    private fun SystemOneAnswer.toOutcome(kind: DecisionKind): DecisionOutcome = when (kind) {
        DecisionKind.Choice -> DecisionOutcome.Choice(
            choice = choice ?: missing("choice"),
            probabilities = probabilities.orEmpty(),
            confidence = confidence,
        )
        DecisionKind.Score -> DecisionOutcome.Score(
            score = score ?: missing("score"),
            probabilities = probabilities.orEmpty().byLevelIndex(),
            confidence = confidence,
        )
        DecisionKind.YesNo -> DecisionOutcome.YesNo(probability = noul ?: missing("noul"))
    }

    private val DecisionKind.wireType: String
        get() = when (this) {
            DecisionKind.Choice -> "choice"
            DecisionKind.Score -> "score"
            DecisionKind.YesNo -> "noul"
        }

    private fun Question.criteria(): JsonElement? = when (this) {
        is Question.Choice -> JsonObject(options.associate { it.id to JsonPrimitive(it.label) })
        is Question.Score -> JsonArray(levels.map(::JsonPrimitive))
        Question.YesNo -> null
    }

    private fun Map<String, Double>.byLevelIndex(): Map<Int, Double> =
        entries.mapNotNull { (level, p) -> level.toIntOrNull()?.let { it to p } }.toMap()

    private fun missing(field: String): Nothing = throw DecisionError.MalformedResponse("Missing $field")
}
