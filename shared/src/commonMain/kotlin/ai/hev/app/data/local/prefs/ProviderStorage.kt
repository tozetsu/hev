package ai.hev.app.data.local.prefs

import ai.hev.app.domain.provider.ProviderConfig

/** Persists the provider list, API keys included, and which provider is active. */
interface ProviderStorage {
    fun loadProviders(): List<ProviderConfig>
    fun saveProviders(providers: List<ProviderConfig>)
    fun loadActiveId(): String?
    fun saveActiveId(id: String?)
}

/** Where API keys are kept, for platforms that have more than one place. */
enum class SecretBackend { Keyring, File }

/** API keys could not be read or written, for example because the user would not unlock the keyring. */
class SecretStorageException(message: String, cause: Throwable? = null) : Exception(message, cause)
