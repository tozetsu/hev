package ai.hev.app.domain.provider

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
}
