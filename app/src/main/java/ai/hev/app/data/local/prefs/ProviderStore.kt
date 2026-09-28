package ai.hev.app.data.local.prefs

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import ai.hev.app.domain.model.ProviderConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

class ProviderStore(context: Context) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val prefs: SharedPreferences = createEncryptedPrefs(context)

    private val _providers = MutableStateFlow(loadProviders())
    val providers: StateFlow<List<ProviderConfig>> = _providers.asStateFlow()

    private val _activeId = MutableStateFlow(prefs.getString(KEY_ACTIVE, null))
    val activeId: StateFlow<String?> = _activeId.asStateFlow()

    val activeProvider: ProviderConfig?
        get() {
            val id = _activeId.value
            return _providers.value.firstOrNull { it.id == id } ?: _providers.value.firstOrNull()
        }

    fun upsert(config: ProviderConfig) {
        val list = _providers.value.toMutableList()
        val idx = list.indexOfFirst { it.id == config.id }
        if (idx >= 0) list[idx] = config else list.add(config)
        persist(list)
        if (_activeId.value == null) setActive(config.id)
    }

    fun delete(id: String) {
        val list = _providers.value.filterNot { it.id == id }
        persist(list)
        if (_activeId.value == id) {
            setActive(list.firstOrNull()?.id)
        }
    }

    fun setActive(id: String?) {
        prefs.edit().putString(KEY_ACTIVE, id).apply()
        _activeId.value = id
    }

    fun createDefaultIfEmpty() {
        if (_providers.value.isNotEmpty()) return
        val def = ProviderConfig(
            id = UUID.randomUUID().toString(),
            name = "TypeSafe",
            baseUrl = DEFAULT_BASE,
            apiKey = "",
            model = "jev-latest",
        )
        upsert(def)
        setActive(def.id)
    }

    private fun persist(list: List<ProviderConfig>) {
        val stored = list.map {
            StoredProvider(it.id, it.name, it.baseUrl, it.apiKey, it.model)
        }
        prefs.edit().putString(KEY_PROVIDERS, json.encodeToString(stored)).apply()
        _providers.value = list
    }

    private fun loadProviders(): List<ProviderConfig> {
        val raw = prefs.getString(KEY_PROVIDERS, null) ?: return emptyList()
        return runCatching {
            json.decodeFromString<List<StoredProvider>>(raw).map {
                ProviderConfig(it.id, it.name, it.baseUrl, it.apiKey, it.model)
            }
        }.getOrElse { emptyList() }
    }

    @Serializable
    private data class StoredProvider(
        val id: String,
        val name: String,
        val baseUrl: String,
        val apiKey: String,
        val model: String,
    )

    companion object {
        const val DEFAULT_BASE = "https://api.typesafe.ai/v1/systemone"
        private const val PREFS_NAME = "hev_secure_prefs"
        private const val KEY_PROVIDERS = "providers_json"
        private const val KEY_ACTIVE = "active_provider_id"

        private fun createEncryptedPrefs(context: Context): SharedPreferences {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            return EncryptedSharedPreferences.create(
                context,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
        }
    }
}
