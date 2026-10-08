// The main class name becomes the X11 WM_CLASS, which the desktop entry's StartupWMClass matches.
@file:JvmName("hev")

import ai.hev.app.AppGraph
import ai.hev.app.DesktopDirs
import ai.hev.app.data.local.prefs.SecretStorageException
import ai.hev.app.ui.HevContent
import ai.hev.app.ui.common.Shortcuts
import ai.hev.app.ui.navigation.rememberHevNavigator
import ai.hev.desktop.rememberPortalDarkTheme
import ai.hev.desktop.rememberSavedWindowState
import ai.hev.desktop.resources.Res
import ai.hev.desktop.resources.hev
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import org.jetbrains.compose.resources.painterResource
import java.awt.Dimension
import javax.swing.JOptionPane

fun main() {
    val dirs = DesktopDirs.fromEnvironment()
    val graph = openGraph(dirs) ?: return
    application {
        val navigator = rememberHevNavigator()
        Window(
            onCloseRequest = ::exitApplication,
            state = rememberSavedWindowState(dirs.config.resolve("window.json")),
            title = APP_NAME,
            icon = painterResource(Res.drawable.hev),
            onKeyEvent = { event ->
                if (Shortcuts.Quit.matches(event)) {
                    exitApplication()
                    true
                } else {
                    navigator.handleShortcut(event)
                }
            },
        ) {
            LaunchedEffect(window) { window.minimumSize = Dimension(MIN_WIDTH, MIN_HEIGHT) }
            HevContent(graph, systemDark = rememberPortalDarkTheme() ?: isSystemInDarkTheme(), navigator = navigator)
        }
    }
}

/** Opens the graph, offering to retry while the keyring stays locked; null when the user quits instead. */
private fun openGraph(dirs: DesktopDirs): AppGraph? {
    while (true) {
        try {
            return AppGraph(dirs)
        } catch (e: SecretStorageException) {
            val choice = JOptionPane.showOptionDialog(
                null, e.message, APP_NAME, JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE,
                null, arrayOf(RETRY, QUIT), RETRY,
            )
            if (choice != 0) return null
        }
    }
}

private const val APP_NAME = "HEV"
private const val MIN_WIDTH = 360
private const val MIN_HEIGHT = 520
private const val RETRY = "Retry"
private const val QUIT = "Quit"
