package ai.hev.app.data.local.prefs

import ai.hev.app.domain.provider.ProviderConfig

/** Persists the provider list, API keys included, and which provider is active. */
interface ProviderStorage {
    fun loadProviders(): List<ProviderConfig>
    fun saveProviders(providers: List<ProviderConfig>)
    fun loadActiveId(): String?
    fun saveActiveId(id: String?)
}
