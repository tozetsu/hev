package ai.hev.app.ui.result

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ai.hev.app.HevApp
import ai.hev.app.R
import ai.hev.app.domain.decision.DecisionOutcome
import ai.hev.app.domain.history.HistoryEntry
import ai.hev.app.ui.components.MetaChip
import ai.hev.app.ui.components.ProbabilityBar
import ai.hev.app.ui.components.formatConfidence
import ai.hev.app.ui.components.formatPercent
import ai.hev.app.ui.components.formatScore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreen(
    historyId: Long,
    onBack: () -> Unit,
    onOpenHistory: () -> Unit,
) {
    val app = LocalContext.current.applicationContext as HevApp
    var entry by remember { mutableStateOf<HistoryEntry?>(null) }

    LaunchedEffect(historyId) {
        entry = app.container.repository.getHistory(historyId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.result)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                },
                actions = {
                    IconButton(onClick = onOpenHistory) {
                        Icon(Icons.Outlined.History, contentDescription = stringResource(R.string.cd_history))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        val e = entry
        if (e == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(20.dp),
            ) {
                Text(stringResource(R.string.loading), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            ResultContent(entry = e, modifier = Modifier.padding(padding))
        }
    }
}

@Composable
fun ResultContent(entry: HistoryEntry, modifier: Modifier = Modifier) {
    val labels = remember(entry.items) { entry.items.associate { it.id to it.label } }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Text(
                text = entry.instructions,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            if (!entry.context.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = entry.context,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        when (val outcome = entry.outcome) {
            is DecisionOutcome.Choice -> {
                item { ConfidenceChip(outcome.confidence) }
                item {
                    ProbabilityCard {
                        if (outcome.probabilities.isEmpty()) {
                            Text(
                                stringResource(R.string.no_probability_data),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        outcome.probabilities.entries.sortedByDescending { it.value }.forEach { (id, p) ->
                            ProbabilityBar(
                                label = "$id  ${labels[id] ?: ""}".trimEnd(),
                                probability = p,
                                highlighted = id == outcome.choice,
                            )
                        }
                    }
                }
            }
            is DecisionOutcome.Score -> {
                item { Headline(stringResource(R.string.score_label, formatScore(outcome.score))) }
                item { ConfidenceChip(outcome.confidence) }
                if (outcome.probabilities.isNotEmpty()) {
                    item {
                        ProbabilityCard {
                            outcome.probabilities.entries.sortedByDescending { it.value }.forEach { (level, p) ->
                                ProbabilityBar(
                                    label = labels[level.toString()] ?: level.toString(),
                                    probability = p,
                                    highlighted = false,
                                )
                            }
                        }
                    }
                }
            }
            is DecisionOutcome.YesNo -> {
                item { Headline(stringResource(R.string.yes_probability, formatPercent(outcome.probability))) }
                item {
                    ProbabilityCard {
                        ProbabilityBar(label = "", probability = outcome.probability, highlighted = true)
                    }
                }
            }
            DecisionOutcome.Refused -> item { Headline(stringResource(R.string.result_refused)) }
            null -> item { Headline(stringResource(R.string.value_na)) }
        }

        item {
            val meta = listOfNotNull(
                entry.providerName?.takeIf { it.isNotBlank() },
                entry.model?.takeIf { it.isNotBlank() },
                entry.inputTokens?.let { pluralStringResource(R.plurals.input_tokens, it, it) },
            )
            if (meta.isNotEmpty()) {
                Text(
                    text = meta.joinToString(" · "),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun Headline(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.displaySmall,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
private fun ConfidenceChip(confidence: Double?) {
    if (confidence != null) {
        MetaChip(stringResource(R.string.confidence_label, formatConfidence(confidence, "")))
    }
}

@Composable
private fun ProbabilityCard(content: @Composable () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            content = { content() },
        )
    }
}
