package ai.hev.app.data.local.secrets

import ai.hev.app.data.local.prefs.SecretBackend
import java.nio.file.Path
import kotlin.io.path.deleteExisting
import kotlin.io.path.exists

/** The secret store the desktop uses, and which kind it is. */
class DesktopSecrets(val store: SecretStore, val backend: SecretBackend) {
    companion object {
        /**
         * Uses [keyring] when there is one, else the file at [file].
         * Keys left in the file, from a session without a keyring, move into the keyring and the file is deleted.
         */
        fun open(file: Path, keyring: SecretStore?): DesktopSecrets {
            val fileStore = FileSecretStore(file)
            if (keyring == null) return DesktopSecrets(fileStore, SecretBackend.File)
            if (file.exists()) {
                keyring.save(keyring.load() + fileStore.load())
                file.deleteExisting()
            }
            return DesktopSecrets(keyring, SecretBackend.Keyring)
        }
    }
}
