package ai.hev.app.ui.common

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

internal actual val desktopUi = false

@Composable
internal actual fun DesktopStyles(content: @Composable () -> Unit) = content()

@Composable
internal actual fun VerticalScrollbar(state: LazyListState, modifier: Modifier) = Unit

@Composable
internal actual fun ContextMenu(actions: List<MenuAction>, content: @Composable () -> Unit) = content()
