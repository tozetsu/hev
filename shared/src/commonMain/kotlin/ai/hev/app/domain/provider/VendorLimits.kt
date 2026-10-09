package ai.hev.app.domain.provider

import ai.hev.app.domain.decision.DecisionKind
import ai.hev.app.domain.decision.ModelCapabilities

/**
 * Limits vendors document for everything they serve, recognised by the endpoint's host or port.
 * Any other endpoint gets [ModelCapabilities.Lenient]; the server stays the final authority.
 */
internal object VendorLimits {
    private val DeepInfra = limits(choiceOptions = 2..52, scoreLevels = 2..10)
    private val Alibaba = limits(choiceOptions = 2..255, scoreLevels = 2..255)
    private val Ollama = limits(choiceOptions = 2..26, scoreLevels = 2..26)

    /** Ollama listens on this port wherever it runs. */
    private const val OLLAMA_PORT = 11434

    fun of(endpoint: String): ModelCapabilities {
        val authority = Endpoints.authority(endpoint) ?: return ModelCapabilities.Lenient
        return when {
            authority.host.isWithin("deepinfra.com") -> DeepInfra
            authority.host.isWithin("maas.aliyuncs.com") -> Alibaba
            authority.port == OLLAMA_PORT -> Ollama
            else -> ModelCapabilities.Lenient
        }
    }

    private fun String.isWithin(domain: String) = this == domain || endsWith(".$domain")

    private fun limits(choiceOptions: IntRange, scoreLevels: IntRange) =
        ModelCapabilities(DecisionKind.entries.toSet(), choiceOptions, scoreLevels)
}
