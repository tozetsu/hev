package ai.hev.app.ui.settings

import androidx.annotation.StringRes
import ai.hev.app.R
import ai.hev.app.domain.provider.DecisionProtocol
import ai.hev.app.domain.provider.Endpoints
import ai.hev.app.domain.provider.ProviderConfig
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
    @StringRes val error: Int? = null,
) {
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
        )
    }

    /** First problem that keeps this provider from being saved, or null. */
    @StringRes
    fun validate(): Int? = when {
        name.isBlank() -> R.string.error_name_required
        !Endpoints.isValid(endpoint) -> R.string.error_endpoint_invalid
        apiKeyRequired && apiKey.isBlank() -> R.string.error_api_key
        model.isBlank() -> R.string.error_model_required
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
