package ai.hev.app.data.local.prefs

import ai.hev.app.data.local.JsonFile
import kotlinx.serialization.Serializable
import java.nio.file.Path

/** Theme choices in a small JSON settings file. */
class FileThemeStorage(path: Path) : ThemeStorage {
    private val file = JsonFile(path, Stored.serializer())

    override fun load(): ThemeSettings {
        val stored = file.readOrNull() ?: return ThemeSettings()
        return ThemeSettings(
            mode = ThemeMode.entries.firstOrNull { it.name == stored.themeMode } ?: ThemeMode.SYSTEM,
            accentArgb = stored.accentArgb,
        )
    }

    override fun save(settings: ThemeSettings) = file.write(Stored(settings.mode.name, settings.accentArgb))

    @Serializable
    private data class Stored(
        val themeMode: String = ThemeMode.SYSTEM.name,
        val accentArgb: Int = ThemeSettings.DEFAULT_ACCENT_ARGB,
    )
}
