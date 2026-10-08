package ai.hev.app.domain.provider

data class ProviderConfig(
    val id: String,
    val name: String,
    val baseUrl: String,
    val apiKey: String,
    val model: String,
)
