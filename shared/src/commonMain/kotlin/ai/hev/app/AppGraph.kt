package ai.hev.app

import ai.hev.app.data.local.db.HevDatabase
import ai.hev.app.data.local.prefs.ProviderStorage
import ai.hev.app.data.local.prefs.ProviderStore
import ai.hev.app.data.local.prefs.ThemeStorage
import ai.hev.app.data.local.prefs.ThemeStore
import ai.hev.app.data.remote.DecisionClient
import ai.hev.app.data.remote.ModelCatalog
import ai.hev.app.data.remote.http.HttpTransport
import ai.hev.app.data.repository.HevRepository

/** The app's long-lived objects, wired from what each platform provides. */
class AppGraph internal constructor(
    database: HevDatabase,
    providerStorage: ProviderStorage,
    themeStorage: ThemeStorage,
) {
    private val transport = HttpTransport()

    val themeStore = ThemeStore(themeStorage)

    val repository = HevRepository(
        client = DecisionClient(transport),
        modelCatalog = ModelCatalog(transport),
        historyDao = database.historyDao(),
        providerStore = ProviderStore(providerStorage),
    )

    init {
        repository.ensureDefaultProvider()
    }
}
