package ai.hev.app.data.local

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import java.nio.file.Path
import java.nio.file.StandardCopyOption.ATOMIC_MOVE
import java.nio.file.StandardCopyOption.REPLACE_EXISTING
import java.nio.file.attribute.PosixFilePermissions
import java.nio.file.attribute.PosixFilePermissions.asFileAttribute
import kotlin.io.path.createDirectories
import kotlin.io.path.createFile
import kotlin.io.path.deleteIfExists
import kotlin.io.path.exists
import kotlin.io.path.moveTo
import kotlin.io.path.readText
import kotlin.io.path.writeText

/**
 * One value stored as JSON at [path]. Writes go through a temporary file and an atomic rename, so
 * a crash never leaves half a file. The directory is private to the user; so is the file when
 * [ownerOnly] is set.
 */
internal class JsonFile<T>(
    private val path: Path,
    private val serializer: KSerializer<T>,
    private val ownerOnly: Boolean = false,
) {
    /** Null when the file does not exist yet; throws if it exists but cannot be read. */
    fun read(): T? = if (path.exists()) json.decodeFromString(serializer, path.readText()) else null

    /** Like [read], but an unreadable file counts as missing. */
    fun readOrNull(): T? = runCatching { read() }.getOrNull()

    fun write(value: T) {
        val dir = path.parent.createDirectories(OWNER_DIR)
        val temp = dir.resolve("${path.fileName}.tmp").apply { deleteIfExists() }
        if (ownerOnly) temp.createFile(OWNER_FILE)
        temp.writeText(json.encodeToString(serializer, value))
        temp.moveTo(path, ATOMIC_MOVE, REPLACE_EXISTING)
    }

    private companion object {
        val json = Json { ignoreUnknownKeys = true; prettyPrint = true }
        val OWNER_DIR = asFileAttribute(PosixFilePermissions.fromString("rwx------"))
        val OWNER_FILE = asFileAttribute(PosixFilePermissions.fromString("rw-------"))
    }
}
