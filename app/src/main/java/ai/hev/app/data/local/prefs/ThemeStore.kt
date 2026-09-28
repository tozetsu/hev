package ai.hev.app.data.local.prefs

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode {
    SYSTEM,
    DARK,
    LIGHT,
}

class ThemeStore(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(loadThemeMode())
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _accentArgb = MutableStateFlow(
        prefs.getInt(KEY_ACCENT, DEFAULT_ACCENT_ARGB),
    )
    val accentArgb: StateFlow<Int> = _accentArgb.asStateFlow()

    fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
        _themeMode.value = mode
    }

    fun setAccentArgb(argb: Int) {
        prefs.edit().putInt(KEY_ACCENT, argb).apply()
        _accentArgb.value = argb
    }

    private fun loadThemeMode(): ThemeMode {
        val raw = prefs.getString(KEY_THEME_MODE, ThemeMode.SYSTEM.name)
        return runCatching { ThemeMode.valueOf(raw!!) }.getOrDefault(ThemeMode.SYSTEM)
    }

    companion object {
        private const val PREFS_NAME = "hev_theme_prefs"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_ACCENT = "accent_argb"
        /** Matches ui.theme.Accent #5B8CFF */
        const val DEFAULT_ACCENT_ARGB: Int = 0xFF5B8CFF.toInt()
    }
}
