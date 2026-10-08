// The main class name becomes the X11 WM_CLASS, which the desktop entry's StartupWMClass matches.
@file:JvmName("hev")

import ai.hev.app.AppGraph
import ai.hev.app.DesktopDirs
import ai.hev.app.ui.HevContent
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

fun main() {
    val dirs = DesktopDirs.fromEnvironment()
    val graph = AppGraph(dirs)
    application {
        Window(
            onCloseRequest = ::exitApplication,
            state = rememberSavedWindowState(dirs.config.resolve("window.json")),
            title = APP_NAME,
            icon = painterResource(Res.drawable.hev),
        ) {
            LaunchedEffect(window) { window.minimumSize = Dimension(MIN_WIDTH, MIN_HEIGHT) }
            HevContent(graph, systemDark = rememberPortalDarkTheme() ?: isSystemInDarkTheme())
        }
    }
}

private const val APP_NAME = "HEV"
private const val MIN_WIDTH = 360
private const val MIN_HEIGHT = 520
