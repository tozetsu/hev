package ai.hev.app.domain.provider

import ai.hev.app.domain.decision.ModelCapabilities

/**
 * A known vendor setup. Presets only prefill a provider; the saved provider keeps its own copy
 * of every field. [modelsUrl] may be relative to the endpoint (e.g. `/api/tags`).
 * [capabilities] are the vendor's documented limits; the server remains the final authority.
 */
data class ProviderPreset(
    val id: String,
    val name: String,
    val protocol: DecisionProtocol,
    val endpoint: String,
    val models: List<String>,
    val modelsUrl: String? = null,
    val apiKeyRequired: Boolean = true,
    val capabilities: ModelCapabilities = ModelCapabilities.Lenient,
) {
    init {
        require(models.isNotEmpty()) { "Preset $id needs at least one model" }
    }

    fun newProvider(id: String) = ProviderConfig(
        id = id,
        name = name,
        protocol = protocol,
        endpoint = endpoint,
        apiKey = "",
        model = models.first(),
        presetId = this.id,
        modelsUrl = modelsUrl,
    )
}
