package ai.hev.app.ui

import ai.hev.app.AppGraph
import ai.hev.app.data.local.prefs.ThemeMode
import ai.hev.app.ui.common.LocalAppGraph
import ai.hev.app.ui.navigation.HevNavDisplay
import ai.hev.app.ui.navigation.HevNavigator
import ai.hev.app.ui.navigation.rememberHevNavigator
import ai.hev.app.ui.theme.HevTheme
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * The whole app over [graph], themed by the user's choice; [systemDark] is what "System" follows.
 * [navigator] is hoisted for windows that handle keyboard shortcuts.
 */
@Composable
fun HevContent(
    graph: AppGraph,
    systemDark: Boolean = isSystemInDarkTheme(),
    navigator: HevNavigator = rememberHevNavigator(),
) {
    val theme by graph.themeStore.settings.collectAsStateWithLifecycle()
    val darkTheme = when (theme.mode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }
    CompositionLocalProvider(LocalAppGraph provides graph) {
        HevTheme(darkTheme = darkTheme, accent = Color(theme.accentArgb)) {
            Surface(modifier = Modifier.fillMaxSize()) {
                HevNavDisplay(navigator)
            }
        }
    }
}
