package ai.hev.app.domain.provider

import ai.hev.app.domain.decision.ModelCapabilities

/**
 * A configured decisions endpoint. [endpoint] and [modelsUrl] are used exactly as entered;
 * [presetId] is null for a custom provider.
 */
data class ProviderConfig(
    val id: String,
    val name: String,
    val protocol: DecisionProtocol,
    val endpoint: String,
    val apiKey: String,
    val model: String,
    val presetId: String? = null,
    val modelsUrl: String? = null,
) {
    val preset: ProviderPreset? get() = ProviderPresets.byId(presetId)

    val requiresApiKey: Boolean get() = preset?.apiKeyRequired ?: false

    /** Known limits for this vendor; lenient for custom providers. */
    val capabilities: ModelCapabilities get() = preset?.capabilities ?: ModelCapabilities.Lenient
}
