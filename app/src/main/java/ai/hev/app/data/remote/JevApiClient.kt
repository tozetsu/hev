package ai.hev.app.data.remote

import ai.hev.app.domain.decision.DecisionError
import ai.hev.app.domain.decision.DecisionKind
import ai.hev.app.domain.decision.DecisionOutcome
import ai.hev.app.domain.decision.DecisionRequest
import ai.hev.app.domain.decision.DecisionResult
import ai.hev.app.domain.decision.Question
import ai.hev.app.domain.decision.TokenUsage
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

class JevApiClient(
    private val http: OkHttpClient = defaultClient(),
    private val json: Json = defaultJson(),
) {
    fun decide(endpointUrl: String, apiKey: String, request: DecisionRequest): DecisionResult {
        val body = SystemOneRequest(
            model = request.model,
            state = request.state,
            questions = mapOf(
                QUESTION_KEY to QuestionDto(
                    type = request.kind.wireType,
                    instructions = request.instructions,
                    criteria = request.question.criteria(),
                ),
            ),
        )
        val httpRequest = Request.Builder()
            .url(endpointUrl.trim())
            .header("Authorization", "Bearer $apiKey")
            .header("Accept", "application/json")
            .post(json.encodeToString(body).toRequestBody(JSON))
            .build()

        val raw = try {
            http.newCall(httpRequest).execute().use { response ->
                val text = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    throw DecisionError.forStatus(response.code, text.ifBlank { response.message })
                }
                text
            }
        } catch (e: IOException) {
            throw DecisionError.Network(e)
        }
        return parse(raw, request.kind)
    }

    private fun parse(raw: String, kind: DecisionKind): DecisionResult {
        val parsed = try {
            json.decodeFromString<SystemOneResponse>(raw)
        } catch (e: SerializationException) {
            throw DecisionError.MalformedResponse("Unreadable response", e)
        }
        val answer = parsed.answers?.get(QUESTION_KEY)
            ?: parsed.answers?.values?.singleOrNull()
            ?: throw DecisionError.MalformedResponse("Missing answer")
        val outcome = when (kind) {
            DecisionKind.Choice -> answer.choice?.let {
                DecisionOutcome.Choice(it, answer.probabilities.orEmpty(), answer.confidence)
            }
            DecisionKind.Score -> answer.score?.let { score ->
                DecisionOutcome.Score(
                    score = score,
                    probabilities = answer.probabilities.orEmpty()
                        .mapNotNull { (k, v) -> k.toIntOrNull()?.let { it to v } }
                        .toMap(),
                    confidence = answer.confidence,
                )
            }
            DecisionKind.YesNo -> answer.noul?.let { DecisionOutcome.YesNo(it) }
        } ?: throw DecisionError.MalformedResponse("Missing $kind value")
        return DecisionResult(
            outcome = outcome,
            model = parsed.model,
            usage = TokenUsage(parsed.usage?.inputTokens, parsed.usage?.outputTokens),
            rawJson = raw,
        )
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

    companion object {
        private const val QUESTION_KEY = "decision"
        private val JSON = "application/json; charset=utf-8".toMediaType()

        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()

        fun defaultJson(): Json = Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        }
    }
}
