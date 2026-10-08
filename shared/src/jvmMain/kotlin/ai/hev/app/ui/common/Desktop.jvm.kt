package ai.hev.app.ui.common

import androidx.compose.foundation.ContextMenuArea
import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.DefaultContextMenuRepresentation
import androidx.compose.foundation.LocalContextMenuRepresentation
import androidx.compose.foundation.LocalScrollbarStyle
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.VerticalScrollbar as DesktopScrollbar

internal actual val desktopUi = true

@Composable
internal actual fun DesktopStyles(content: @Composable () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val contextMenu = remember(colors) {
        DefaultContextMenuRepresentation(
            backgroundColor = colors.surfaceContainer,
            textColor = colors.onSurface,
            itemHoverColor = colors.onSurface.copy(alpha = HOVER_ALPHA),
        )
    }
    CompositionLocalProvider(
        LocalContextMenuRepresentation provides contextMenu,
        LocalScrollbarStyle provides LocalScrollbarStyle.current.copy(
            shape = RoundedCornerShape(4.dp),
            unhoverColor = colors.onSurface.copy(alpha = 0.16f),
            hoverColor = colors.onSurface.copy(alpha = 0.48f),
        ),
        content = content,
    )
}

@Composable
internal actual fun VerticalScrollbar(state: LazyListState, modifier: Modifier) =
    DesktopScrollbar(rememberScrollbarAdapter(state), modifier)

@Composable
internal actual fun ContextMenu(actions: List<MenuAction>, content: @Composable () -> Unit) =
    ContextMenuArea(items = { actions.map { ContextMenuItem(it.label, it.onClick) } }, content = content)

private const val HOVER_ALPHA = 0.08f
