package ai.hev.app.ui.common

import ai.hev.app.AppGraph
import androidx.compose.runtime.staticCompositionLocalOf

/** The [AppGraph] screens read their dependencies from. */
val LocalAppGraph = staticCompositionLocalOf<AppGraph> { error("No AppGraph provided") }
