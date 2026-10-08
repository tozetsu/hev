package ai.hev.app.ui.common

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon

/**
 * Whether the UI adds what a mouse and keyboard call for: tooltips, shortcuts, selectable text,
 * hand cursors, scrollbars and context menus. Only the desktop app does, for now.
 */
internal expect val desktopUi: Boolean

/** Desktop styles for scrollbars and context menus, taken from the Material theme around them. */
@Composable
internal expect fun DesktopStyles(content: @Composable () -> Unit)

/** A scrollbar for [state] on desktop; nothing elsewhere. */
@Composable
internal expect fun VerticalScrollbar(state: LazyListState, modifier: Modifier = Modifier)

/** Offers [actions] when [content] is right-clicked on desktop. */
@Composable
internal expect fun ContextMenu(actions: List<MenuAction>, content: @Composable () -> Unit)

internal class MenuAction(val label: String, val onClick: () -> Unit)

/** Shows [text] while the pointer rests on [content], on desktop. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun Tooltip(text: String, content: @Composable () -> Unit) {
    if (!desktopUi) return content()
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Below),
        tooltip = { PlainTooltip { Text(text) } },
        state = rememberTooltipState(isPersistent = true),
        content = content,
    )
}

/** Lets the text in [content] be selected and copied, on desktop. */
@Composable
internal fun SelectableText(content: @Composable () -> Unit) {
    if (desktopUi) SelectionContainer(content = content) else content()
}

/** The hand cursor over something clickable that does not look like a button, on desktop. */
internal fun Modifier.handCursor(): Modifier = if (desktopUi) pointerHoverIcon(PointerIcon.Hand) else this
