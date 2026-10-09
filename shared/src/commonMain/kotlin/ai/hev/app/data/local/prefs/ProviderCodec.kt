package ai.hev.app.data.local.prefs

import ai.hev.app.data.local.StorageKeys
import ai.hev.app.domain.provider.DecisionProtocol
import ai.hev.app.domain.provider.ProviderConfig
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/**
 * Stored form of the provider list. Rows written before protocols existed carry no `protocol`;
 * they are System One providers. Keys older versions wrote, such as `presetId`, are ignored.
 */
internal object ProviderCodec {
    private val json = Json { ignoreUnknownKeys = true }
    private val serializer = ListSerializer(StoredProvider.serializer())

    fun encode(providers: List<ProviderConfig>): String = json.encodeToString(
        serializer,
        providers.map {
            StoredProvider(
                id = it.id,
                name = it.name,
                baseUrl = it.endpoint,
                apiKey = it.apiKey,
                model = it.model,
                protocol = StorageKeys.of(it.protocol),
            )
        },
    )

    /** Unreadable data yields an empty list rather than a crash. */
    fun decode(raw: String): List<ProviderConfig> =
        runCatching { json.decodeFromString(serializer, raw) }.getOrDefault(emptyList()).map { it.toConfig() }

    private fun StoredProvider.toConfig() = ProviderConfig(
        id = id,
        name = name,
        protocol = StorageKeys.protocol(protocol) ?: DecisionProtocol.SystemOne,
        endpoint = baseUrl,
        apiKey = apiKey,
        model = model,
    )

    /** `baseUrl` keeps its original key so older installs read their providers unchanged. */
    @Serializable
    private data class StoredProvider(
        val id: String,
        val name: String,
        val baseUrl: String,
        val apiKey: String,
        val model: String,
        val protocol: String? = null,
    )
}
