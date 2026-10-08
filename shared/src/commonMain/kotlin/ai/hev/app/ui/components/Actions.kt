package ai.hev.app.ui.components

import ai.hev.app.resources.Res
import ai.hev.app.resources.cd_back
import ai.hev.app.ui.common.Shortcut
import ai.hev.app.ui.common.Shortcuts
import ai.hev.app.ui.common.Tooltip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.graphics.vector.ImageVector
import org.jetbrains.compose.resources.stringResource

/** An icon button named by [description], which desktop also shows on hover, with the [shortcut] if any. */
@Composable
internal fun ActionIcon(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    tint: Color = Color.Unspecified,
    shortcut: Shortcut? = null,
) {
    Tooltip(text = shortcut?.let { "$description (${it.label})" } ?: description) {
        IconButton(onClick = onClick, enabled = enabled) {
            Icon(icon, contentDescription = description, tint = tint.takeOrElse { LocalContentColor.current })
        }
    }
}

@Composable
internal fun BackButton(onBack: () -> Unit) =
    ActionIcon(HevIcons.ArrowBack, stringResource(Res.string.cd_back), onBack, shortcut = Shortcuts.Back)
