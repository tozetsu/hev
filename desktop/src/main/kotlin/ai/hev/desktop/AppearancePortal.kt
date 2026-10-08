package ai.hev.desktop

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import org.freedesktop.dbus.annotations.DBusInterfaceName
import org.freedesktop.dbus.annotations.DBusMemberName
import org.freedesktop.dbus.connections.impl.DBusConnection
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder
import org.freedesktop.dbus.interfaces.DBusInterface
import org.freedesktop.dbus.interfaces.DBusSigHandler
import org.freedesktop.dbus.messages.DBusSignal
import org.freedesktop.dbus.types.UInt32
import org.freedesktop.dbus.types.Variant

/**
 * Whether the desktop prefers a dark appearance, read from the XDG settings portal and kept
 * current. Compose does not report this on Linux (CMP-10466). Null when there is no portal or
 * the desktop states no preference.
 */
@Composable
fun rememberPortalDarkTheme(): Boolean? {
    val dark by produceState<Boolean?>(null) {
        val portal = withContext(Dispatchers.IO) { AppearancePortal.connect() } ?: return@produceState
        portal.use {
            value = withContext(Dispatchers.IO) { it.prefersDark() }
            it.changes().collect { prefersDark -> value = prefersDark }
        }
    }
    return dark
}

private class AppearancePortal(private val connection: DBusConnection) : AutoCloseable {
    private val settings = connection.getRemoteObject(BUS_NAME, OBJECT_PATH, PortalSettings::class.java)

    fun prefersDark(): Boolean? = runCatching { settings.readOne(NAMESPACE, KEY).value }.getOrNull().toPrefersDark()

    fun changes(): Flow<Boolean?> = callbackFlow {
        val handler = DBusSigHandler<PortalSettings.SettingChanged> {
            if (it.namespace == NAMESPACE && it.key == KEY) trySend(it.value.value.toPrefersDark())
        }
        connection.addSigHandler(PortalSettings.SettingChanged::class.java, handler)
        awaitClose { connection.removeSigHandler(PortalSettings.SettingChanged::class.java, handler) }
    }

    override fun close() = connection.close()

    companion object {
        private const val BUS_NAME = "org.freedesktop.portal.Desktop"
        private const val OBJECT_PATH = "/org/freedesktop/portal/desktop"
        private const val NAMESPACE = "org.freedesktop.appearance"
        private const val KEY = "color-scheme"

        fun connect(): AppearancePortal? =
            runCatching { AppearancePortal(DBusConnectionBuilder.forSessionBus().build()) }.getOrNull()
    }
}

/** `color-scheme` is 1 for dark, 2 for light and 0 for no preference. */
private fun Any?.toPrefersDark(): Boolean? = when ((this as? UInt32)?.toInt()) {
    1 -> true
    2 -> false
    else -> null
}

@DBusInterfaceName("org.freedesktop.portal.Settings")
internal interface PortalSettings : DBusInterface {
    @DBusMemberName("ReadOne")
    fun readOne(namespace: String, key: String): Variant<*>

    class SettingChanged(path: String, val namespace: String, val key: String, val value: Variant<*>) :
        DBusSignal(path, namespace, key, value)
}
