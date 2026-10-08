package ai.hev.app.data.local.secrets

/** API keys by provider id, kept apart from the rest of the provider settings. */
interface SecretStore {
    fun load(): Map<String, String>

    /** Replaces every stored key with [keys]. */
    fun save(keys: Map<String, String>)
}
