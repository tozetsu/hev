package ai.hev.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import ai.hev.app.data.local.prefs.ThemeMode
import ai.hev.app.ui.common.LocalAppGraph
import ai.hev.app.ui.navigation.HevNavHost
import ai.hev.app.ui.theme.HevTheme

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val graph = (application as HevApp).graph
        setContent {
            val theme by graph.themeStore.settings.collectAsStateWithLifecycle()
            val systemDark = isSystemInDarkTheme()
            val darkTheme = when (theme.mode) {
                ThemeMode.SYSTEM -> systemDark
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
            }
            CompositionLocalProvider(LocalAppGraph provides graph) {
                HevTheme(darkTheme = darkTheme, accent = Color(theme.accentArgb)) {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        val navController = rememberNavController()
                        HevNavHost(navController = navController)
                    }
                }
            }
        }
    }
}
