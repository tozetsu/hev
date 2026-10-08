package ai.hev.app.data.local.prefs

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import ai.hev.app.domain.provider.ProviderConfig

/** Providers and their keys in EncryptedSharedPreferences, under the names every release has used. */
class EncryptedProviderStorage(context: Context) : ProviderStorage {
    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    override fun loadProviders(): List<ProviderConfig> =
        prefs.getString(KEY_PROVIDERS, null)?.let(ProviderCodec::decode).orEmpty()

    override fun saveProviders(providers: List<ProviderConfig>) =
        prefs.edit { putString(KEY_PROVIDERS, ProviderCodec.encode(providers)) }

    override fun loadActiveId(): String? = prefs.getString(KEY_ACTIVE, null)

    override fun saveActiveId(id: String?) = prefs.edit { putString(KEY_ACTIVE, id) }

    private companion object {
        const val PREFS_NAME = "hev_secure_prefs"
        const val KEY_PROVIDERS = "providers_json"
        const val KEY_ACTIVE = "active_provider_id"
    }
}
