package ai.hev.app.data.local.prefs

import android.content.Context
import androidx.core.content.edit

/** Theme choices in plain SharedPreferences, under the names every release has used. */
class SharedPreferencesThemeStorage(context: Context) : ThemeStorage {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun load() = ThemeSettings(
        mode = ThemeMode.entries.firstOrNull { it.name == prefs.getString(KEY_THEME_MODE, null) } ?: ThemeMode.SYSTEM,
        accentArgb = prefs.getInt(KEY_ACCENT, ThemeSettings.DEFAULT_ACCENT_ARGB),
    )

    override fun save(settings: ThemeSettings) = prefs.edit {
        putString(KEY_THEME_MODE, settings.mode.name)
        putInt(KEY_ACCENT, settings.accentArgb)
    }

    private companion object {
        const val PREFS_NAME = "hev_theme_prefs"
        const val KEY_THEME_MODE = "theme_mode"
        const val KEY_ACCENT = "accent_argb"
    }
}
