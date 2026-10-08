package ai.hev.app

import ai.hev.app.data.local.db.openHevDatabase
import ai.hev.app.data.local.prefs.FileProviderStorage
import ai.hev.app.data.local.prefs.FileThemeStorage
import ai.hev.app.data.local.secrets.DesktopSecrets
import ai.hev.app.data.local.secrets.KeyringSecretStore

/**
 * The graph over desktop files: the database under [DesktopDirs.data], settings under [DesktopDirs.config],
 * and API keys in the keyring, or in [DesktopDirs.data] without one.
 *
 * @throws ai.hev.app.data.local.prefs.SecretStorageException when the keyring stays locked.
 */
fun AppGraph(dirs: DesktopDirs): AppGraph {
    dirs.create()
    val secrets = DesktopSecrets.open(dirs.data.resolve("secrets.json"), KeyringSecretStore.connect())
    return AppGraph(
        database = openHevDatabase(dirs.data.resolve("hev.db")),
        providerStorage = FileProviderStorage(dirs.config.resolve("providers.json"), secrets.store),
        themeStorage = FileThemeStorage(dirs.config.resolve("settings.json")),
        secretBackend = secrets.backend,
    )
}
