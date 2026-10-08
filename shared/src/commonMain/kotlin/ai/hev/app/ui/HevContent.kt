package ai.hev.app.ui

import ai.hev.app.AppGraph
import ai.hev.app.data.local.prefs.ThemeMode
import ai.hev.app.ui.common.LocalAppGraph
import ai.hev.app.ui.navigation.HevNavHost
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
import androidx.navigation.compose.rememberNavController

/** The whole app over [graph], themed by the user's choice. */
@Composable
fun HevContent(graph: AppGraph) {
    val theme by graph.themeStore.settings.collectAsStateWithLifecycle()
    val darkTheme = when (theme.mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }
    CompositionLocalProvider(LocalAppGraph provides graph) {
        HevTheme(darkTheme = darkTheme, accent = Color(theme.accentArgb)) {
            Surface(modifier = Modifier.fillMaxSize()) {
                HevNavHost(navController = rememberNavController())
            }
        }
    }
}
