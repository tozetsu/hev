package ai.hev.app

import ai.hev.app.data.local.db.openHevDatabase
import ai.hev.app.data.local.prefs.FileProviderStorage
import ai.hev.app.data.local.prefs.FileThemeStorage
import ai.hev.app.data.local.secrets.FileSecretStore

/** The graph over desktop files: the database and secrets under [DesktopDirs.data], settings under [DesktopDirs.config]. */
fun AppGraph(dirs: DesktopDirs): AppGraph = AppGraph(
    database = openHevDatabase(dirs.create().data.resolve("hev.db")),
    providerStorage = FileProviderStorage(
        path = dirs.config.resolve("providers.json"),
        secrets = FileSecretStore(dirs.data.resolve("secrets.json")),
    ),
    themeStorage = FileThemeStorage(dirs.config.resolve("settings.json")),
)
