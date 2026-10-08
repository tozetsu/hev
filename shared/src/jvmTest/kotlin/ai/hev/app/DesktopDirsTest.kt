package ai.hev.app

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.io.path.Path

class DesktopDirsTest {
    private val home = Path(System.getProperty("user.home"))

    @Test
    fun `xdg variables win when absolute`() {
        val dirs = DesktopDirs.fromEnvironment(mapOf("XDG_CONFIG_HOME" to "/cfg", "XDG_DATA_HOME" to "/data"))
        assertEquals(Path("/cfg/hev"), dirs.config)
        assertEquals(Path("/data/hev"), dirs.data)
    }

    @Test
    fun `missing or relative variables fall back to the home directory`() {
        val dirs = DesktopDirs.fromEnvironment(mapOf("XDG_CONFIG_HOME" to "relative"))
        assertEquals(home.resolve(".config/hev"), dirs.config)
        assertEquals(home.resolve(".local/share/hev"), dirs.data)
    }
}
