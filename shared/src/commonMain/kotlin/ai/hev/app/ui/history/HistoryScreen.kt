package ai.hev.app.ui.history

import ai.hev.app.domain.decision.DecisionOutcome
import ai.hev.app.domain.history.HistoryEntry
import ai.hev.app.resources.Res
import ai.hev.app.resources.action_cancel
import ai.hev.app.resources.action_clear
import ai.hev.app.resources.action_delete
import ai.hev.app.resources.cd_clear
import ai.hev.app.resources.clear_history_message
import ai.hev.app.resources.clear_history_title
import ai.hev.app.resources.confidence_label
import ai.hev.app.resources.history
import ai.hev.app.resources.history_choice
import ai.hev.app.resources.history_empty
import ai.hev.app.resources.result_refused
import ai.hev.app.resources.score_label
import ai.hev.app.resources.value_na
import ai.hev.app.resources.yes_probability
import ai.hev.app.ui.common.ContextMenu
import ai.hev.app.ui.common.LocalAppGraph
import ai.hev.app.ui.common.MenuAction
import ai.hev.app.ui.common.handCursor
import ai.hev.app.ui.components.ActionIcon
import ai.hev.app.ui.components.BackButton
import ai.hev.app.ui.components.HevIcons
import ai.hev.app.ui.components.ScrollColumn
import ai.hev.app.ui.components.formatConfidence
import ai.hev.app.ui.components.formatPercent
import ai.hev.app.ui.components.formatScore
import ai.hev.app.ui.components.formatTimestamp
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/** A null [onBack] hides the back button, for when the navigation rail opened this screen. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onBack: (() -> Unit)?,
    onOpen: (Long) -> Unit,
) {
    val repo = LocalAppGraph.current.repository
    val items by repo.history.collectAsStateWithLifecycle(initialValue = emptyList())
    var confirmClear by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val deleteLabel = stringResource(Res.string.action_delete)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.history)) },
                navigationIcon = { onBack?.let { BackButton(it) } },
                actions = {
                    if (items.isNotEmpty()) {
                        ActionIcon(
                            HevIcons.DeleteSweep,
                            stringResource(Res.string.cd_clear),
                            onClick = { confirmClear = true },
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        if (items.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Text(stringResource(Res.string.history_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            ScrollColumn(
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(items, key = { it.id }) { entry ->
                    val delete: () -> Unit = { scope.launch { repo.deleteHistory(entry.id) } }
                    SwipeToDismissBox(
                        state = rememberSwipeToDismissBoxState(),
                        onDismiss = { delete() },
                        backgroundContent = {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 8.dp),
                                contentAlignment = Alignment.CenterEnd,
                            ) {
                                Text(stringResource(Res.string.action_delete), color = MaterialTheme.colorScheme.error)
                            }
                        },
                    ) {
                        ContextMenu(listOf(MenuAction(deleteLabel, delete))) {
                            HistoryRow(
                                entry = entry,
                                timeText = formatTimestamp(entry.createdAt),
                                onClick = { onOpen(entry.id) },
                            )
                        }
                    }
                }
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text(stringResource(Res.string.clear_history_title)) },
            text = { Text(stringResource(Res.string.clear_history_message)) },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { repo.clearHistory() }
                    confirmClear = false
                }) { Text(stringResource(Res.string.action_clear)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) { Text(stringResource(Res.string.action_cancel)) }
            },
        )
    }
}

@Composable
private fun HistoryRow(
    entry: HistoryEntry,
    timeText: String,
    onClick: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .handCursor(),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = entry.instructions,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = historySubtitle(entry),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = timeText,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun historySubtitle(entry: HistoryEntry): String {
    val na = stringResource(Res.string.value_na)
    return when (val outcome = entry.outcome) {
        is DecisionOutcome.Choice -> listOfNotNull(
            stringResource(Res.string.history_choice, outcome.choice),
            outcome.confidence?.let { stringResource(Res.string.confidence_label, formatConfidence(it, na)) },
        ).joinToString(" · ")
        is DecisionOutcome.Score -> listOfNotNull(
            stringResource(Res.string.score_label, formatScore(outcome.score)),
            outcome.confidence?.let { stringResource(Res.string.confidence_label, formatConfidence(it, na)) },
        ).joinToString(" · ")
        is DecisionOutcome.YesNo -> stringResource(Res.string.yes_probability, formatPercent(outcome.probability))
        DecisionOutcome.Refused -> stringResource(Res.string.result_refused)
        null -> na
    }
}
