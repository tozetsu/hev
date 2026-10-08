package ai.hev.app.data.remote.http

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/** Pulls the human-readable message out of the error bodies decision providers send. */
internal object ErrorBody {
    private const val MAX_RAW_LENGTH = 2_000

    /** The server's message, else the trimmed raw body, else [fallback]. */
    fun detail(body: String, fallback: String): String =
        message(body) ?: body.trim().take(MAX_RAW_LENGTH).ifEmpty { fallback }

    /**
     * Recognised shapes:
     * - `{"error": {"message": "…"}}` (OpenAI, Perplexity, Vercel)
     * - `{"error": "…"}` (Ollama)
     * - `{"message": "…", "error_type": "…"}` (TypeSafe)
     * - `{"detail": "…"}` and `{"detail": [{"loc": […], "msg": "…"}]}` (FastAPI servers, e.g. DeepInfra)
     */
    fun message(body: String): String? {
        val root = runCatching { Json.parseToJsonElement(body) }.getOrNull() as? JsonObject ?: return null
        return root["error"]?.let { error ->
            (error as? JsonObject)?.get("message")?.text() ?: error.text()
        } ?: root["message"]?.text() ?: root["detail"]?.let(::detailMessage)
    }

    private fun detailMessage(detail: JsonElement): String? = when (detail) {
        is JsonArray -> detail.mapNotNull { item ->
            val obj = item as? JsonObject ?: return@mapNotNull null
            val msg = obj["msg"]?.text() ?: return@mapNotNull null
            val location = (obj["loc"] as? JsonArray)?.mapNotNull { it.text() }?.joinToString(".")
            if (location.isNullOrEmpty()) msg else "$location: $msg"
        }.joinToString("\n").ifEmpty { null }
        else -> detail.text()
    }

    private fun JsonElement.text(): String? =
        (this as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() }
}
