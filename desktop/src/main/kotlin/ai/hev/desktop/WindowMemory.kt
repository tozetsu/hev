package ai.hev.desktop

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.rememberWindowState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.readText
import kotlin.io.path.writeText

/**
 * A window state that reopens where the window was left: size, position and whether it was
 * maximized, kept in [file]. A maximized window remembers the size it had before.
 */
@Composable
fun rememberSavedWindowState(file: Path): WindowState {
    val saved = remember(file) { runCatching { json.decodeFromString<Bounds>(file.readText()) }.getOrNull() }
    val state = rememberWindowState(
        placement = if (saved?.maximized == true) WindowPlacement.Maximized else WindowPlacement.Floating,
        position = saved?.position ?: WindowPosition(Alignment.Center),
        size = saved?.let { DpSize(it.width.dp, it.height.dp) } ?: DefaultSize,
    )
    LaunchedEffect(state) {
        var floating = saved ?: Bounds.of(DefaultSize, WindowPosition.PlatformDefault)
        snapshotFlow { Triple(state.placement, state.size, state.position) }.collectLatest { (placement, size, position) ->
            delay(SAVE_DELAY_MILLIS)
            if (placement == WindowPlacement.Floating) floating = Bounds.of(size, position)
            val bounds = floating.copy(maximized = placement == WindowPlacement.Maximized)
            withContext(Dispatchers.IO) {
                file.parent.createDirectories()
                file.writeText(json.encodeToString(bounds))
            }
        }
    }
    return state
}

@Serializable
private data class Bounds(
    val width: Float,
    val height: Float,
    val x: Float? = null,
    val y: Float? = null,
    val maximized: Boolean = false,
) {
    val position: WindowPosition?
        get() = if (x != null && y != null) WindowPosition(x.dp, y.dp) else null

    companion object {
        fun of(size: DpSize, position: WindowPosition) = Bounds(
            width = size.width.value,
            height = size.height.value,
            x = position.x.value.takeIf { position.isSpecified },
            y = position.y.value.takeIf { position.isSpecified },
        )
    }
}

private val DefaultSize = DpSize(1100.dp, 720.dp)
private const val SAVE_DELAY_MILLIS = 500L
private val json = Json { ignoreUnknownKeys = true }
