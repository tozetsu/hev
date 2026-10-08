package ai.hev.app.data.remote

import ai.hev.app.domain.model.ChoiceRequest
import ai.hev.app.domain.model.DecideResult
import ai.hev.app.domain.model.ModelInfo
import ai.hev.app.domain.model.NoulRequest
import ai.hev.app.domain.model.QuestionType
import ai.hev.app.domain.model.ScoreRequest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class JevApiClient(
    private val http: OkHttpClient = defaultClient(),
    private val json: Json = defaultJson(),
) {
    fun choice(
        endpointUrl: String,
        apiKey: String,
        request: ChoiceRequest,
    ): DecideResult {
        val criteria = JsonObject(
            request.options.associate { it.id to JsonPrimitive(it.label.ifBlank { it.id }) }
        )
        return postDecide(
            endpointUrl = endpointUrl,
            apiKey = apiKey,
            model = request.model,
            state = request.state,
            question = request.question,
            type = QuestionType.Choice,
            criteria = criteria,
        )
    }

    fun score(
        endpointUrl: String,
        apiKey: String,
        request: ScoreRequest,
    ): DecideResult {
        val criteria = JsonArray(request.levels.map { JsonPrimitive(it) })
        return postDecide(
            endpointUrl = endpointUrl,
            apiKey = apiKey,
            model = request.model,
            state = request.state,
            question = request.question,
            type = QuestionType.Score,
            criteria = criteria,
        )
    }

    fun noul(
        endpointUrl: String,
        apiKey: String,
        request: NoulRequest,
    ): DecideResult {
        return postDecide(
            endpointUrl = endpointUrl,
            apiKey = apiKey,
            model = request.model,
            state = request.state,
            question = request.question,
            type = QuestionType.Noul,
            criteria = null,
        )
    }

    fun listModels(endpointUrl: String, apiKey: String): List<ModelInfo> {
        val url = endpointUrl.trim()
        val httpRequest = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $apiKey")
            .header("Accept", "application/json")
            .get()
            .build()
        http.newCall(httpRequest).execute().use { response ->
            val raw = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw ApiException(response.code, raw.ifBlank { response.message })
            }
            val parsed = json.decodeFromString<ModelsListResponse>(raw)
            return parsed.data.orEmpty().map { ModelInfo(id = it.id, ownedBy = it.ownedBy) }
        }
    }

    private fun postDecide(
        endpointUrl: String,
        apiKey: String,
        model: String,
        state: String?,
        question: String,
        type: QuestionType,
        criteria: kotlinx.serialization.json.JsonElement?,
    ): DecideResult {
        val body = SystemOneRequest(
            model = model,
            // API requires state; fall back to the question text when context is empty.
            state = state?.takeIf { it.isNotBlank() } ?: question,
            questions = mapOf(
                "q" to QuestionDto(
                    type = type.apiValue,
                    instructions = question,
                    criteria = criteria,
                )
            ),
        )
        val payload = json.encodeToString(body)
        val url = endpointUrl.trim()
        val httpRequest = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $apiKey")
            .header("Accept", "application/json")
            .header("Content-Type", "application/json")
            .post(payload.toRequestBody(JSON))
            .build()

        http.newCall(httpRequest).execute().use { response ->
            val raw = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw ApiException(response.code, raw.ifBlank { response.message })
            }
            return parseDecideResult(raw, type)
        }
    }

    private fun parseDecideResult(raw: String, type: QuestionType): DecideResult {
        val parsed = json.decodeFromString<SystemOneResponse>(raw)
        val answer = parsed.answers?.get("q")
            ?: parsed.answers?.values?.firstOrNull()
        val model = parsed.response?.model ?: parsed.model
        return DecideResult(
            questionType = type,
            choice = answer?.choice,
            score = answer?.score,
            noul = answer?.noul,
            probabilities = answer?.probabilities.orEmpty(),
            confidence = answer?.confidence,
            model = model,
            inputTokens = parsed.usage?.inputTokens,
            outputTokens = parsed.usage?.outputTokens,
            rawJson = raw,
        )
    }

    companion object {
        private val JSON = "application/json; charset=utf-8".toMediaType()

        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()

        fun defaultJson(): Json = Json {
            ignoreUnknownKeys = true
            isLenient = true
            explicitNulls = false
            encodeDefaults = true
        }
    }
}

class ApiException(val code: Int, override val message: String) : Exception("HTTP $code: $message")
