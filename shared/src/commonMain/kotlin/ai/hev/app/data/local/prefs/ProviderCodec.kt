package ai.hev.app.data.local.prefs

import ai.hev.app.data.local.StorageKeys
import ai.hev.app.domain.provider.DecisionProtocol
import ai.hev.app.domain.provider.ProviderConfig
import ai.hev.app.domain.provider.ProviderPresets
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/**
 * Stored form of the provider list. Rows written before protocols existed carry no `protocol`;
 * they are System One providers, linked to the preset whose endpoint they match.
 */
internal object ProviderCodec {
    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }
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
                presetId = it.presetId,
                modelsUrl = it.modelsUrl,
            )
        },
    )

    /** Unreadable data yields an empty list rather than a crash. */
    fun decode(raw: String): List<ProviderConfig> =
        runCatching { json.decodeFromString(serializer, raw) }.getOrDefault(emptyList()).map { it.toConfig() }

    private fun StoredProvider.toConfig(): ProviderConfig {
        if (protocol == null) {
            val preset = ProviderPresets.matching(DecisionProtocol.SystemOne, baseUrl)
            return ProviderConfig(
                id = id,
                name = name,
                protocol = DecisionProtocol.SystemOne,
                endpoint = baseUrl,
                apiKey = apiKey,
                model = model,
                presetId = preset?.id,
                modelsUrl = preset?.modelsUrl,
            )
        }
        return ProviderConfig(
            id = id,
            name = name,
            protocol = StorageKeys.protocol(protocol) ?: DecisionProtocol.SystemOne,
            endpoint = baseUrl,
            apiKey = apiKey,
            model = model,
            presetId = presetId?.takeIf { ProviderPresets.byId(it) != null },
            modelsUrl = modelsUrl,
        )
    }

    /** `baseUrl` keeps its original key so older installs read their providers unchanged. */
    @Serializable
    private data class StoredProvider(
        val id: String,
        val name: String,
        val baseUrl: String,
        val apiKey: String,
        val model: String,
        val protocol: String? = null,
        val presetId: String? = null,
        val modelsUrl: String? = null,
    )
}
