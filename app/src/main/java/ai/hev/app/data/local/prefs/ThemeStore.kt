package ai.hev.app.data.local.prefs

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class ThemeMode {
    SYSTEM,
    DARK,
    LIGHT,
}

data class ThemeSettings(
    val mode: ThemeMode = ThemeMode.SYSTEM,
    val accentArgb: Int = DEFAULT_ACCENT_ARGB,
) {
    companion object {
        /** Matches ui.theme.Accent #5B8CFF */
        const val DEFAULT_ACCENT_ARGB: Int = 0xFF5B8CFF.toInt()
    }
}

/** Persists [ThemeSettings]. */
interface ThemeStorage {
    fun load(): ThemeSettings
    fun save(settings: ThemeSettings)
}

/** Theme choices as observable state, written through to [storage]. */
class ThemeStore(private val storage: ThemeStorage) {
    private val _settings = MutableStateFlow(storage.load())
    val settings: StateFlow<ThemeSettings> = _settings.asStateFlow()

    fun setThemeMode(mode: ThemeMode) = edit { it.copy(mode = mode) }

    fun setAccentArgb(argb: Int) = edit { it.copy(accentArgb = argb) }

    private fun edit(transform: (ThemeSettings) -> ThemeSettings) {
        _settings.update(transform)
        storage.save(_settings.value)
    }
}
