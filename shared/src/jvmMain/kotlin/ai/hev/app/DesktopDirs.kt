package ai.hev.app

import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermissions
import kotlin.io.path.Path
import kotlin.io.path.createDirectories

/** Where the desktop app keeps its files, following the XDG base directory spec. */
class DesktopDirs(val config: Path, val data: Path) {
    /** Creates both directories, private to the user, where they are missing. */
    fun create(): DesktopDirs = apply {
        val ownerOnly = PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------"))
        config.createDirectories(ownerOnly)
        data.createDirectories(ownerOnly)
    }

    companion object {
        fun fromEnvironment(env: Map<String, String> = System.getenv()): DesktopDirs {
            val home = Path(System.getProperty("user.home"))
            fun base(variable: String, fallback: String): Path =
                env[variable]?.let(::Path)?.takeIf { it.isAbsolute } ?: home.resolve(fallback)
            return DesktopDirs(
                config = base("XDG_CONFIG_HOME", ".config").resolve("hev"),
                data = base("XDG_DATA_HOME", ".local/share").resolve("hev"),
            )
        }
    }
}
