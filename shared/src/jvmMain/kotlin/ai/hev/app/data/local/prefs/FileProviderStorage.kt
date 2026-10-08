package ai.hev.app.data.local.prefs

import ai.hev.app.data.local.JsonFile
import ai.hev.app.data.local.secrets.SecretStore
import ai.hev.app.domain.provider.ProviderConfig
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonArray
import java.nio.file.Path

/** Providers in a JSON file without their API keys, which live in [secrets]. */
class FileProviderStorage(path: Path, private val secrets: SecretStore) : ProviderStorage {
    private val file = JsonFile(path, Stored.serializer())

    override fun loadProviders(): List<ProviderConfig> {
        val keys = secrets.load()
        return ProviderCodec.decode(stored().providers.toString()).map { it.copy(apiKey = keys[it.id].orEmpty()) }
    }

    override fun saveProviders(providers: List<ProviderConfig>) {
        secrets.save(providers.filter { it.apiKey.isNotEmpty() }.associate { it.id to it.apiKey })
        val withoutKeys = ProviderCodec.encode(providers.map { it.copy(apiKey = "") })
        file.write(stored().copy(providers = Json.parseToJsonElement(withoutKeys).jsonArray))
    }

    override fun loadActiveId(): String? = stored().activeProviderId

    override fun saveActiveId(id: String?) = file.write(stored().copy(activeProviderId = id))

    private fun stored(): Stored = file.readOrNull() ?: Stored()

    @Serializable
    private data class Stored(
        val activeProviderId: String? = null,
        val providers: JsonArray = JsonArray(emptyList()),
    )
}
