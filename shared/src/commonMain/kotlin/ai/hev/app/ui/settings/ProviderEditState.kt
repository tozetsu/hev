package ai.hev.app.ui.settings

import ai.hev.app.domain.provider.DecisionProtocol
import ai.hev.app.domain.provider.Endpoints
import ai.hev.app.domain.provider.ProviderConfig
import ai.hev.app.domain.provider.ProviderIssue

data class ProviderEditState(
    val id: String,
    val isNew: Boolean,
    val name: String = "",
    val protocol: DecisionProtocol = DecisionProtocol.SystemOne,
    val endpoint: String = "",
    val apiKey: String = "",
    val model: String = "",
    /** Models the vendor lists, once loaded. */
    val fetchedModels: List<String> = emptyList(),
    val error: ProviderIssue? = null,
) {
    /** First problem that keeps this provider from being saved, or null. */
    fun validate(): ProviderIssue? = when {
        name.isBlank() -> ProviderIssue.MissingName
        !Endpoints.isValid(endpoint) -> ProviderIssue.InvalidEndpoint
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
    )

    companion object {
        fun of(config: ProviderConfig) = ProviderEditState(
            id = config.id,
            isNew = false,
            name = config.name,
            protocol = config.protocol,
            endpoint = config.endpoint,
            apiKey = config.apiKey,
            model = config.model,
        )
    }
}
