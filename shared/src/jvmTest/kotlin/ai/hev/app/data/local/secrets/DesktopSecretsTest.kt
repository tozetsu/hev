package ai.hev.app.data.local.secrets

import ai.hev.app.data.local.prefs.SecretBackend
import ai.hev.app.data.local.prefs.SecretStorageException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import kotlin.io.path.exists

class DesktopSecretsTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val file get() = folder.root.toPath().resolve("secrets.json")

    @Test
    fun `without a keyring keys stay in the file`() {
        val secrets = DesktopSecrets.open(file, keyring = null)
        secrets.store.save(mapOf("p1" to "sk-1"))

        assertEquals(SecretBackend.File, secrets.backend)
        assertEquals(mapOf("p1" to "sk-1"), FileSecretStore(file).load())
    }

    @Test
    fun `keys left in the file move into the keyring`() {
        FileSecretStore(file).save(mapOf("p1" to "sk-new", "p2" to "sk-2"))
        val keyring = MemorySecretStore(mapOf("p1" to "sk-old", "p3" to "sk-3"))

        val secrets = DesktopSecrets.open(file, keyring)

        assertEquals(SecretBackend.Keyring, secrets.backend)
        assertSame(keyring, secrets.store)
        assertEquals(mapOf("p1" to "sk-new", "p2" to "sk-2", "p3" to "sk-3"), keyring.load())
        assertFalse(file.exists())
    }

    @Test
    fun `a locked keyring fails without losing the file`() {
        FileSecretStore(file).save(mapOf("p1" to "sk-1"))

        assertThrows(SecretStorageException::class.java) { DesktopSecrets.open(file, LockedSecretStore) }
        assertTrue(file.exists())
    }

    private class MemorySecretStore(private var keys: Map<String, String>) : SecretStore {
        override fun load() = keys

        override fun save(keys: Map<String, String>) {
            this.keys = keys
        }
    }

    private object LockedSecretStore : SecretStore {
        override fun load() = throw SecretStorageException("The keyring is locked")

        override fun save(keys: Map<String, String>) = throw SecretStorageException("The keyring is locked")
    }
}
