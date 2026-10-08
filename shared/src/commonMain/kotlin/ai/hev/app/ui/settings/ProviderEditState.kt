package ai.hev.app.ui.settings

import ai.hev.app.domain.provider.DecisionProtocol
import ai.hev.app.domain.provider.Endpoints
import ai.hev.app.domain.provider.ProviderConfig
import ai.hev.app.domain.provider.ProviderIssue
import ai.hev.app.domain.provider.ProviderPreset

data class ProviderEditState(
    val id: String,
    val isNew: Boolean,
    /** Null when the provider is custom. */
    val preset: ProviderPreset?,
    val name: String,
    val protocol: DecisionProtocol,
    val endpoint: String,
    val modelsUrl: String,
    val apiKey: String,
    val model: String,
    /** Models listed by the vendor's models endpoint, once loaded. */
    val fetchedModels: List<String> = emptyList(),
    val error: ProviderIssue? = null,
) {
    val modelOptions: List<String> get() = (fetchedModels + preset?.models.orEmpty()).distinct()

    val isCustom: Boolean get() = preset == null
    val apiKeyRequired: Boolean get() = preset?.apiKeyRequired ?: false

    /** Prefills from [preset]; null keeps the current values as a custom provider. */
    fun withPreset(preset: ProviderPreset?): ProviderEditState {
        if (preset == null) return copy(preset = null)
        val autoName = name.isBlank() || name == this.preset?.name
        return copy(
            preset = preset,
            name = if (autoName) preset.name else name,
            protocol = preset.protocol,
            endpoint = preset.endpoint,
            modelsUrl = preset.modelsUrl.orEmpty(),
            model = preset.models.first(),
            fetchedModels = emptyList(),
        )
    }

    /** First problem that keeps this provider from being saved, or null. */
    fun validate(): ProviderIssue? = when {
        name.isBlank() -> ProviderIssue.MissingName
        !Endpoints.isValid(endpoint) -> ProviderIssue.InvalidEndpoint
        apiKeyRequired && apiKey.isBlank() -> ProviderIssue.MissingApiKey
        model.isBlank() -> ProviderIssue.MissingModel
        else -> null
    }

    fun toConfig() = ProviderConfig(
        id = id,
        name = name.trim(),
        protocol = protocol,
        endpoint = endpoint.trim(),
        apiKey = apiKey.trim(),
        model = model.trim(),
        presetId = preset?.id,
        modelsUrl = modelsUrl.trim().ifEmpty { null },
    )

    companion object {
        fun of(config: ProviderConfig, isNew: Boolean) = ProviderEditState(
            id = config.id,
            isNew = isNew,
            preset = config.preset,
            name = config.name,
            protocol = config.protocol,
            endpoint = config.endpoint,
            modelsUrl = config.modelsUrl.orEmpty(),
            apiKey = config.apiKey,
            model = config.model,
        )
    }
}
