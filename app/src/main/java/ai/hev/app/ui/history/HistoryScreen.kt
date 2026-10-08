package ai.hev.app.ui.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ai.hev.app.HevApp
import ai.hev.app.R
import ai.hev.app.domain.decision.DecisionOutcome
import ai.hev.app.domain.history.HistoryEntry
import ai.hev.app.ui.components.formatConfidence
import ai.hev.app.ui.components.formatPercent
import ai.hev.app.ui.components.formatScore
import ai.hev.app.ui.components.formatTimestamp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onBack: () -> Unit,
    onOpen: (Long) -> Unit,
) {
    val repo = (LocalContext.current.applicationContext as HevApp).container.repository
    val items by repo.history.collectAsStateWithLifecycle(initialValue = emptyList())
    var confirmClear by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.history)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                },
                actions = {
                    if (items.isNotEmpty()) {
                        IconButton(onClick = { confirmClear = true }) {
                            Icon(Icons.Outlined.DeleteSweep, contentDescription = stringResource(R.string.cd_clear))
                        }
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
                Text(stringResource(R.string.history_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(items, key = { it.id }) { entry ->
                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = { value ->
                            if (value == SwipeToDismissBoxValue.EndToStart ||
                                value == SwipeToDismissBoxValue.StartToEnd
                            ) {
                                scope.launch { repo.deleteHistory(entry.id) }
                                true
                            } else false
                        }
                    )
                    SwipeToDismissBox(
                        state = dismissState,
                        backgroundContent = {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 8.dp),
                                contentAlignment = Alignment.CenterEnd,
                            ) {
                                Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error)
                            }
                        },
                    ) {
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

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text(stringResource(R.string.clear_history_title)) },
            text = { Text(stringResource(R.string.clear_history_message)) },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { repo.clearHistory() }
                    confirmClear = false
                }) { Text(stringResource(R.string.action_clear)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) { Text(stringResource(R.string.action_cancel)) }
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
            .clickable(onClick = onClick),
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
    val na = stringResource(R.string.value_na)
    return when (val outcome = entry.outcome) {
        is DecisionOutcome.Choice -> listOfNotNull(
            stringResource(R.string.history_choice, outcome.choice),
            outcome.confidence?.let { stringResource(R.string.confidence_label, formatConfidence(it, na)) },
        ).joinToString(" · ")
        is DecisionOutcome.Score -> listOfNotNull(
            stringResource(R.string.score_label, formatScore(outcome.score)),
            outcome.confidence?.let { stringResource(R.string.confidence_label, formatConfidence(it, na)) },
        ).joinToString(" · ")
        is DecisionOutcome.YesNo -> stringResource(R.string.yes_probability, formatPercent(outcome.probability))
        DecisionOutcome.Refused -> stringResource(R.string.result_refused)
        null -> na
    }
}
