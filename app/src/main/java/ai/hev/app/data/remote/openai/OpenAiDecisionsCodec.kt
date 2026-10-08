package ai.hev.app.data.remote.openai

import ai.hev.app.data.remote.DecisionCodec
import ai.hev.app.data.remote.WireJson
import ai.hev.app.data.remote.decodeWire
import ai.hev.app.data.remote.missingField
import ai.hev.app.domain.decision.DecisionError
import ai.hev.app.domain.decision.DecisionKind
import ai.hev.app.domain.decision.DecisionOutcome
import ai.hev.app.domain.decision.DecisionRequest
import ai.hev.app.domain.decision.DecisionResult
import ai.hev.app.domain.decision.Question
import ai.hev.app.domain.decision.TokenUsage
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull

/**
 * OpenAI Decisions wire format (`POST …/v1/decisions`), also served by Vercel AI Gateway.
 * Choice option ids travel as `value`, their labels as `description`.
 */
object OpenAiDecisionsCodec : DecisionCodec {

    override fun encode(request: DecisionRequest): String = WireJson.encodeToString(
        OpenAiDecisionsRequest.serializer(),
        OpenAiDecisionsRequest(
            model = request.model,
            input = request.state,
            questions = listOf(request.question.toWire(request.instructions)),
        ),
    )

    override fun decode(raw: String, kind: DecisionKind, questionName: String): DecisionResult {
        val response = decodeWire(OpenAiDecisionsResponse.serializer(), raw)
        val answer = response.answers.firstOrNull { it.name == questionName }
            ?: response.answers.singleOrNull()
            ?: missingField("answer")
        return DecisionResult(
            outcome = answer.toOutcome(kind),
            model = response.model,
            usage = TokenUsage(response.usage?.inputTokens, response.usage?.outputTokens),
            rawJson = raw,
        )
    }

    private fun Question.toWire(instructions: String) = OpenAiQuestion(
        type = kind.wireType,
        name = DecisionCodec.QUESTION_NAME,
        instructions = instructions,
        choices = (this as? Question.Choice)?.options?.map { OpenAiChoice(value = it.id, description = it.label) },
        levels = (this as? Question.Score)?.levels?.map(::OpenAiLevel),
    )

    private fun OpenAiAnswer.toOutcome(kind: DecisionKind): DecisionOutcome {
        if (type == REFUSAL) return DecisionOutcome.Refused
        if (type != kind.wireType) throw DecisionError.MalformedResponse("Unexpected answer type: $type")
        return when (kind) {
            DecisionKind.Choice -> DecisionOutcome.Choice(
                choice = choice?.contentOrNull ?: missingField("choice"),
                probabilities = probabilities.orEmpty().associate { it.value.content to it.probability },
                confidence = confidence,
            )
            DecisionKind.Score -> DecisionOutcome.Score(
                score = score ?: missingField("score"),
                probabilities = probabilities.orEmpty()
                    .mapNotNull { p -> p.value.levelIndex()?.let { it to p.probability } }
                    .toMap(),
                confidence = confidence,
            )
            DecisionKind.YesNo -> DecisionOutcome.YesNo(probability = probability ?: missingField("probability"))
        }
    }

    private fun JsonPrimitive.levelIndex(): Int? = intOrNull ?: content.toIntOrNull()

    private val DecisionKind.wireType: String
        get() = when (this) {
            DecisionKind.Choice -> "choice"
            DecisionKind.Score -> "score"
            DecisionKind.YesNo -> "predicate"
        }

    private const val REFUSAL = "refusal"
}
