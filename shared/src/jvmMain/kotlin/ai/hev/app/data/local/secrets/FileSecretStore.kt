package ai.hev.app.data.local.secrets

import ai.hev.app.data.local.JsonFile
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import java.nio.file.Path

/** Keys in a JSON file only the user can read, for systems without a keyring. */
class FileSecretStore(path: Path) : SecretStore {
    private val file = JsonFile(path, MapSerializer(String.serializer(), String.serializer()), ownerOnly = true)

    override fun load(): Map<String, String> = file.read().orEmpty()

    override fun save(keys: Map<String, String>) = file.write(keys)
}
