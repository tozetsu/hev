package ai.hev.app.ui.settings

import ai.hev.app.domain.provider.ProviderConfig
import ai.hev.app.resources.Res
import ai.hev.app.resources.cd_add
import ai.hev.app.resources.cd_select
import ai.hev.app.resources.providers_title
import ai.hev.app.ui.common.LocalAppGraph
import ai.hev.app.ui.common.handCursor
import ai.hev.app.ui.components.ActionIcon
import ai.hev.app.ui.components.BackButton
import ai.hev.app.ui.components.HevIcons
import ai.hev.app.ui.components.ScrollColumn
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProvidersScreen(
    onBack: () -> Unit,
    onEditProvider: (String) -> Unit,
    onAddProvider: () -> Unit,
) {
    val repo = LocalAppGraph.current.repository
    val providers by repo.providers.collectAsStateWithLifecycle()
    val activeId by repo.activeProviderId.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.providers_title)) },
                navigationIcon = { BackButton(onBack) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddProvider,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                elevation = FloatingActionButtonDefaults.elevation(),
            ) {
                Icon(HevIcons.Add, contentDescription = stringResource(Res.string.cd_add))
            }
        },
    ) { padding ->
        ScrollColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(providers, key = { it.id }) { p ->
                ProviderRow(
                    provider = p,
                    active = p.id == activeId,
                    onSelect = { repo.setActiveProvider(p.id) },
                    onEdit = { onEditProvider(p.id) },
                )
            }
        }
    }
}

@Composable
private fun ProviderRow(
    provider: ProviderConfig,
    active: Boolean,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit)
            .handCursor(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            ActionIcon(
                if (active) HevIcons.CheckCircle else HevIcons.RadioButtonUnchecked,
                stringResource(Res.string.cd_select),
                onClick = onSelect,
                tint = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(provider.name, fontWeight = FontWeight.Medium)
                Text(
                    text = provider.model,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = provider.endpoint,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(
                HevIcons.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
