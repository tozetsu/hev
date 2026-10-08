package ai.hev.app.data.local.prefs

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import ai.hev.app.domain.provider.ProviderConfig
import ai.hev.app.domain.provider.ProviderPresets
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class ProviderStore(context: Context) {
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
        val def = ProviderPresets.TypeSafe.newProvider(UUID.randomUUID().toString())
        upsert(def)
        setActive(def.id)
    }

    private fun persist(list: List<ProviderConfig>) {
        prefs.edit().putString(KEY_PROVIDERS, ProviderCodec.encode(list)).apply()
        _providers.value = list
    }

    private fun loadProviders(): List<ProviderConfig> =
        prefs.getString(KEY_PROVIDERS, null)?.let(ProviderCodec::decode).orEmpty()

    companion object {
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
