package ai.hev.app.ui.settings

import ai.hev.app.data.local.prefs.ThemeMode
import ai.hev.app.resources.Res
import ai.hev.app.resources.about_app
import ai.hev.app.resources.about_author
import ai.hev.app.resources.about_version
import ai.hev.app.resources.app_author
import ai.hev.app.resources.app_name
import ai.hev.app.resources.app_version
import ai.hev.app.resources.cd_back
import ai.hev.app.resources.label_accent
import ai.hev.app.resources.label_appearance
import ai.hev.app.resources.manage_providers
import ai.hev.app.resources.section_about
import ai.hev.app.resources.section_providers
import ai.hev.app.resources.section_theme
import ai.hev.app.resources.settings
import ai.hev.app.resources.theme_dark
import ai.hev.app.resources.theme_light
import ai.hev.app.resources.theme_system
import ai.hev.app.ui.common.LocalAppGraph
import ai.hev.app.ui.components.HevIcons
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource

private data class AccentSwatch(val color: Color)

private val AccentSwatches = listOf(
    AccentSwatch(Color(0xFF5B8CFF)),
    AccentSwatch(Color(0xFF2DD4BF)),
    AccentSwatch(Color(0xFFA78BFA)),
    AccentSwatch(Color(0xFFFB7185)),
    AccentSwatch(Color(0xFFE879A9)),
    AccentSwatch(Color(0xFFF472B6)),
    AccentSwatch(Color(0xFFF59E0B)),
    AccentSwatch(Color(0xFF34D399)),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenProviders: () -> Unit,
) {
    val themeStore = LocalAppGraph.current.themeStore
    val theme by themeStore.settings.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(HevIcons.ArrowBack, contentDescription = stringResource(Res.string.cd_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                SectionLabel(icon = HevIcons.Cloud, title = stringResource(Res.string.section_providers))
                Spacer(Modifier.height(8.dp))
                SettingsNavCard(
                    title = stringResource(Res.string.manage_providers),
                    onClick = onOpenProviders,
                )
            }
            item {
                SectionLabel(icon = HevIcons.Palette, title = stringResource(Res.string.section_theme))
                Spacer(Modifier.height(8.dp))
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Text(
                            text = stringResource(Res.string.label_appearance),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            ThemeModeChip(
                                label = stringResource(Res.string.theme_dark),
                                selected = theme.mode == ThemeMode.DARK,
                                onClick = { themeStore.setThemeMode(ThemeMode.DARK) },
                                modifier = Modifier.weight(1f),
                            )
                            ThemeModeChip(
                                label = stringResource(Res.string.theme_light),
                                selected = theme.mode == ThemeMode.LIGHT,
                                onClick = { themeStore.setThemeMode(ThemeMode.LIGHT) },
                                modifier = Modifier.weight(1f),
                            )
                            ThemeModeChip(
                                label = stringResource(Res.string.theme_system),
                                selected = theme.mode == ThemeMode.SYSTEM,
                                onClick = { themeStore.setThemeMode(ThemeMode.SYSTEM) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        Text(
                            text = stringResource(Res.string.label_accent),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            AccentSwatches.take(4).forEach { swatch ->
                                AccentDot(
                                    color = swatch.color,
                                    selected = theme.accentArgb == swatch.color.toArgb(),
                                    onClick = { themeStore.setAccentArgb(swatch.color.toArgb()) },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            AccentSwatches.drop(4).forEach { swatch ->
                                AccentDot(
                                    color = swatch.color,
                                    selected = theme.accentArgb == swatch.color.toArgb(),
                                    onClick = { themeStore.setAccentArgb(swatch.color.toArgb()) },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                }
            }
            item {
                SectionLabel(icon = HevIcons.Info, title = stringResource(Res.string.section_about))
                Spacer(Modifier.height(8.dp))
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        AboutRow(label = stringResource(Res.string.about_app), value = stringResource(Res.string.app_name))
                        AboutRow(label = stringResource(Res.string.about_version), value = stringResource(Res.string.app_version))
                        AboutRow(label = stringResource(Res.string.about_author), value = stringResource(Res.string.app_author))
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(icon: ImageVector, title: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun SettingsNavCard(
    title: String,
    onClick: () -> Unit,
    subtitle: String? = null,
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Medium)
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Icon(
                HevIcons.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ThemeModeChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(label, style = MaterialTheme.typography.labelLarge)
            }
        },
        modifier = modifier,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
        ),
    )
}

@Composable
private fun AccentDot(
    color: Color,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(color)
                .then(
                    if (selected) {
                        Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                    } else {
                        Modifier
                    },
                )
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Icon(
                    HevIcons.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun AboutRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
        )
    }
}
