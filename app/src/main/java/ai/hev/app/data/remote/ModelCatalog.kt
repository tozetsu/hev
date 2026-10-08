package ai.hev.app.data.remote

import ai.hev.app.data.remote.http.HttpTransport
import ai.hev.app.domain.decision.DecisionError
import ai.hev.app.domain.provider.Endpoints
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/** Lists the model ids a vendor's models endpoint offers. */
class ModelCatalog(private val transport: HttpTransport = HttpTransport()) {

    /** Model ids in server order; empty when no URL can be formed. */
    suspend fun fetch(modelsUrl: String, endpoint: String, apiKey: String): List<String> {
        val url = resolve(modelsUrl, endpoint) ?: return emptyList()
        return parse(transport.get(url, apiKey))
    }

    companion object {
        /**
         * An absolute [modelsUrl] is used as is; a relative one (e.g. `/api/tags`) is resolved
         * against [endpoint] the way a browser resolves a link.
         */
        fun resolve(modelsUrl: String, endpoint: String): String? =
            modelsUrl.trim().ifEmpty { null }?.let { Endpoints.resolve(endpoint, it) }

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
