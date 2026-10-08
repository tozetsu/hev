package ai.hev.app

import android.content.Context
import ai.hev.app.data.local.db.openHevDatabase
import ai.hev.app.data.local.prefs.EncryptedProviderStorage
import ai.hev.app.data.local.prefs.SharedPreferencesThemeStorage

/** The graph over Android storage: the app database, encrypted provider prefs and theme prefs. */
fun AppGraph(context: Context): AppGraph = AppGraph(
    database = openHevDatabase(context),
    providerStorage = EncryptedProviderStorage(context),
    themeStorage = SharedPreferencesThemeStorage(context),
)
