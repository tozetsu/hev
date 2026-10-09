package ai.hev.app.domain.provider

import ai.hev.app.domain.decision.ModelCapabilities

/** A configured decisions endpoint, used exactly as entered. */
data class ProviderConfig(
    val id: String,
    val name: String,
    val protocol: DecisionProtocol,
    val endpoint: String,
    val apiKey: String,
    val model: String,
) {
    /** Documented limits of the vendor behind [endpoint]; lenient when there are none. */
    val capabilities: ModelCapabilities get() = VendorLimits.of(endpoint)
}
