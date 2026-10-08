package ai.hev.app.data.remote

import ai.hev.app.domain.decision.DecisionError
import ai.hev.app.domain.decision.DecisionKind
import ai.hev.app.domain.decision.DecisionRequest
import ai.hev.app.domain.decision.DecisionResult
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/** Maps hev's domain requests and answers to one wire protocol. Pure: no I/O. */
interface DecisionCodec {
    fun encode(request: DecisionRequest): String

    /** Reads the answer named [questionName], or the only answer when there is just one. */
    fun decode(raw: String, kind: DecisionKind, questionName: String = QUESTION_NAME): DecisionResult

    companion object {
        /** hev asks one question per request under this name. */
        const val QUESTION_NAME = "decision"
    }
}

internal val WireJson = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
}

internal fun <T> decodeWire(deserializer: DeserializationStrategy<T>, raw: String): T = try {
    WireJson.decodeFromString(deserializer, raw)
} catch (e: SerializationException) {
    throw DecisionError.MalformedResponse("Unreadable response", e)
} catch (e: IllegalArgumentException) {
    throw DecisionError.MalformedResponse("Unreadable response", e)
}
