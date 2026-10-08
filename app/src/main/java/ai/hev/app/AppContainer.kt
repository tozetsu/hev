package ai.hev.app

import android.content.Context
import ai.hev.app.data.local.db.HevDatabase
import ai.hev.app.data.local.prefs.ProviderStore
import ai.hev.app.data.local.prefs.ThemeStore
import ai.hev.app.data.remote.DecisionClient
import ai.hev.app.data.repository.HevRepository

class AppContainer(context: Context) {
    private val db = HevDatabase.get(context)
    private val providerStore = ProviderStore(context)

    val themeStore = ThemeStore(context)

    val repository = HevRepository(
        client = DecisionClient(),
        historyDao = db.historyDao(),
        providerStore = providerStore,
    )

    init {
        repository.ensureDefaultProvider()
    }
}
