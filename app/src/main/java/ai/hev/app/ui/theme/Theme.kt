package ai.hev.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

private fun onAccent(accent: Color): Color =
    if (accent.luminance() > 0.55f) Ink else Color.White

private fun softAccent(accent: Color, dark: Boolean): Color {
    // primaryContainer / FAB surface — tinted from accent
    return if (dark) {
        Color(
            red = accent.red * 0.35f + 0.08f,
            green = accent.green * 0.35f + 0.10f,
            blue = accent.blue * 0.35f + 0.14f,
            alpha = 1f,
        )
    } else {
        Color(
            red = accent.red * 0.25f + 0.75f,
            green = accent.green * 0.25f + 0.75f,
            blue = accent.blue * 0.25f + 0.78f,
            alpha = 1f,
        )
    }
}

private fun hevDarkColors(accent: Color) = darkColorScheme(
    primary = accent,
    onPrimary = onAccent(accent),
    primaryContainer = softAccent(accent, dark = true),
    onPrimaryContainer = Color.White,
    secondary = accent,
    onSecondary = onAccent(accent),
    secondaryContainer = softAccent(accent, dark = true),
    onSecondaryContainer = Color.White,
    tertiary = accent,
    onTertiary = onAccent(accent),
    tertiaryContainer = softAccent(accent, dark = true),
    onTertiaryContainer = Color.White,
    background = Ink,
    onBackground = Paper,
    surface = InkElevated,
    onSurface = Paper,
    surfaceVariant = InkCard,
    onSurfaceVariant = Mist,
    error = Danger,
    onError = Color.White,
    outline = BarTrack,
)

private fun hevLightColors(accent: Color) = lightColorScheme(
    primary = accent,
    onPrimary = onAccent(accent),
    primaryContainer = softAccent(accent, dark = false),
    onPrimaryContainer = Ink,
    secondary = accent,
    onSecondary = onAccent(accent),
    secondaryContainer = softAccent(accent, dark = false),
    onSecondaryContainer = Ink,
    tertiary = accent,
    onTertiary = onAccent(accent),
    tertiaryContainer = softAccent(accent, dark = false),
    onTertiaryContainer = Ink,
    background = Color(0xFFF7F8FA),
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    surfaceVariant = Color(0xFFEEF1F5),
    onSurfaceVariant = Color(0xFF5A6A7A),
    error = Danger,
    onError = Color.White,
    outline = Color(0xFFD0D7E0),
)

@Composable
fun HevTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    accent: Color = Accent,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) hevDarkColors(accent) else hevLightColors(accent),
        typography = HevTypography,
        content = content,
    )
}
