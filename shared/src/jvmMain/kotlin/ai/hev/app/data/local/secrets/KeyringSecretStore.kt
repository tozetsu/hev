package ai.hev.app.data.local.secrets

import ai.hev.app.data.local.prefs.SecretStorageException
import org.freedesktop.dbus.DBusPath
import org.purejava.secret.api.Collection
import org.purejava.secret.api.DBusMessageHandler.DBusResult
import org.purejava.secret.api.EncryptedSession
import org.purejava.secret.api.Item
import org.purejava.secret.api.Service
import org.purejava.secret.api.Util

/** Keys as items in the default collection of the Secret Service keyring, one item per provider. */
class KeyringSecretStore private constructor(
    private val service: Service,
    private val collection: DBusPath,
) : SecretStore {
    private val session by lazy {
        EncryptedSession(service).takeIf { it.setupEncryptedSession() }
            ?: throw SecretStorageException("Could not open a Secret Service session")
    }

    override fun load(): Map<String, String> {
        val (unlocked, locked) = service.searchItems(SCHEMA).orThrow().let { it.a to it.b }
        val items = unlocked + unlock(locked)
        if (items.isEmpty()) return emptyMap()
        return service.getSecrets(items, session.session).orThrow()
            .mapKeys { (item, _) -> providerOf(item) }
            .mapValues { (_, secret) -> String(session.decrypt(secret)) }
    }

    override fun save(keys: Map<String, String>) {
        if (Collection(collection).isLocked().orThrow()) unlock(listOf(collection))
        keys.forEach { (provider, key) ->
            val properties = Item.createProperties(LABEL, SCHEMA + (PROVIDER to provider))
            Collection(collection).createItem(properties, session.encrypt(key), true).orThrow()
        }
        val stored = service.searchItems(SCHEMA).orThrow().let { it.a + it.b }
        stored.filter { providerOf(it) !in keys }.forEach { Item(it).delete().orThrow() }
    }

    /** Unlocks [objects], prompting the user if the keyring asks for it; refusing is an error. */
    private fun unlock(objects: List<DBusPath>): List<DBusPath> {
        if (objects.isEmpty()) return objects
        val result = service.unlock(objects).orThrow()
        val unlocked = if (result.b.path == NONE) result.a else Util.promptAndGetResultAsArrayList(result.b)
        if (unlocked.size < objects.size) throw SecretStorageException("The keyring is locked")
        return unlocked
    }

    private fun providerOf(item: DBusPath): String = Item(item).getAttributes().orThrow().getValue(PROVIDER)

    companion object {
        private const val LABEL = "HEV API key"
        private const val PROVIDER = "provider"
        /** The D-Bus null object path, meaning "no prompt" or "no collection". */
        private const val NONE = "/"
        private val SCHEMA = mapOf("xdg:schema" to "ai.hev.app.ApiKey")

        /** The keyring of this session, or null when there is no Secret Service with a default collection. */
        fun connect(): KeyringSecretStore? = runCatching {
            val service = Service()
            val collection = service.readAlias("default").orThrow()
            KeyringSecretStore(service, collection).takeIf { collection.path != NONE }
        }.getOrNull()
    }
}

private fun <T> DBusResult<T>.orThrow(): T = when (this) {
    is DBusResult.Success -> value()
    is DBusResult.Failure -> throw SecretStorageException(error().message ?: "Secret Service call failed", error())
}
