package ai.hev.app.data.remote

import ai.hev.app.data.remote.http.HttpTransport
import ai.hev.app.domain.decision.DecisionError
import ai.hev.app.domain.provider.Endpoints
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/** Lists the model ids the vendor behind a decisions endpoint offers. */
class ModelCatalog(private val transport: HttpTransport = HttpTransport()) {

    /** Model ids in server order from the first of [locations] that answers; empty when none does. */
    suspend fun fetch(endpoint: String, apiKey: String): List<String> {
        for (url in locations(endpoint)) {
            try {
                return parse(transport.get(url, apiKey))
            } catch (_: DecisionError) {
                // Try the next convention.
            }
        }
        return emptyList()
    }

    companion object {
        /**
         * Where vendors list models, resolved against [endpoint] like links: next to it
         * (`…/v1/systemone` → `…/v1/models`), then Ollama's `/api/tags`.
         */
        fun locations(endpoint: String): List<String> =
            listOf("models", "/api/tags").mapNotNull { Endpoints.resolve(endpoint, it) }

        /**
         * Reads `{"models": [...]}` (TypeSafe, Ollama) or `{"data": [...]}` (OpenAI style, OpenRouter).
         * Each entry's id is its `id`, else its `name`.
         */
        fun parse(raw: String): List<String> {
            val root = decodeWire(JsonElement.serializer(), raw) as? JsonObject
            val entries = (root?.get("models") ?: root?.get("data")) as? JsonArray
                ?: throw DecisionError.MalformedResponse("No model list")
            return entries
                .mapNotNull { entry -> (entry as? JsonObject)?.let { it.text("id") ?: it.text("name") } }
                .distinct()
        }

        private fun JsonObject.text(key: String): String? =
            (get(key) as? JsonPrimitive)?.contentOrNull?.trim()?.takeIf { it.isNotEmpty() }
    }
}
