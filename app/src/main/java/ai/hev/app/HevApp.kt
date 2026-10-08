package ai.hev.app

import android.app.Application
import ai.hev.app.data.local.db.HevDatabase
import ai.hev.app.data.local.prefs.EncryptedProviderStorage
import ai.hev.app.data.local.prefs.SharedPreferencesThemeStorage

class HevApp : Application() {
    lateinit var graph: AppGraph
        private set

    override fun onCreate() {
        super.onCreate()
        graph = AppGraph(
            database = HevDatabase.get(this),
            providerStorage = EncryptedProviderStorage(this),
            themeStorage = SharedPreferencesThemeStorage(this),
        )
    }
}
