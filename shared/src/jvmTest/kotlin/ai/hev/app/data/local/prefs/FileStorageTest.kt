package ai.hev.app.data.local.prefs

import ai.hev.app.data.local.secrets.FileSecretStore
import ai.hev.app.domain.provider.DecisionProtocol
import ai.hev.app.domain.provider.ProviderConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.file.attribute.PosixFilePermissions
import kotlin.io.path.getPosixFilePermissions
import kotlin.io.path.readText

class FileStorageTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val config get() = folder.root.toPath().resolve("config")
    private val data get() = folder.root.toPath().resolve("data")

    @Test
    fun `providers round-trip with keys kept only in the private secrets file`() {
        val provider = ProviderConfig("p1", "TypeSafe", DecisionProtocol.SystemOne, "https://api.typesafe.ai/v1/systemone", "sk-secret", "jev-latest")
        val storage = FileProviderStorage(config.resolve("providers.json"), FileSecretStore(data.resolve("secrets.json")))
        storage.saveProviders(listOf(provider))
        storage.saveActiveId("p1")

        val reopened = FileProviderStorage(config.resolve("providers.json"), FileSecretStore(data.resolve("secrets.json")))
        assertEquals(listOf(provider), reopened.loadProviders())
        assertEquals("p1", reopened.loadActiveId())
        assertFalse("sk-secret" in config.resolve("providers.json").readText())
        assertEquals("rw-------", PosixFilePermissions.toString(data.resolve("secrets.json").getPosixFilePermissions()))
        assertEquals("rwx------", PosixFilePermissions.toString(data.getPosixFilePermissions()))
    }

    @Test
    fun `theme settings round-trip and default when missing`() {
        val storage = FileThemeStorage(config.resolve("settings.json"))
        assertEquals(ThemeSettings(), storage.load())

        val settings = ThemeSettings(ThemeMode.DARK, 0xFF00FF00.toInt())
        storage.save(settings)
        assertEquals(settings, FileThemeStorage(config.resolve("settings.json")).load())
    }
}
