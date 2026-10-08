package ai.hev.app.ui.result

import ai.hev.app.domain.decision.DecisionOutcome
import ai.hev.app.domain.history.HistoryEntry
import ai.hev.app.resources.Res
import ai.hev.app.resources.cd_back
import ai.hev.app.resources.cd_history
import ai.hev.app.resources.confidence_label
import ai.hev.app.resources.input_tokens
import ai.hev.app.resources.loading
import ai.hev.app.resources.no_probability_data
import ai.hev.app.resources.result
import ai.hev.app.resources.result_refused
import ai.hev.app.resources.score_label
import ai.hev.app.resources.value_na
import ai.hev.app.resources.yes_probability
import ai.hev.app.ui.common.LocalAppGraph
import ai.hev.app.ui.components.HevIcons
import ai.hev.app.ui.components.MetaChip
import ai.hev.app.ui.components.ProbabilityBar
import ai.hev.app.ui.components.formatConfidence
import ai.hev.app.ui.components.formatPercent
import ai.hev.app.ui.components.formatScore
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreen(
    historyId: Long,
    onBack: () -> Unit,
    onOpenHistory: () -> Unit,
) {
    val repo = LocalAppGraph.current.repository
    var entry by remember { mutableStateOf<HistoryEntry?>(null) }

    LaunchedEffect(historyId) {
        entry = repo.getHistory(historyId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.result)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(HevIcons.ArrowBack, contentDescription = stringResource(Res.string.cd_back))
                    }
                },
                actions = {
                    IconButton(onClick = onOpenHistory) {
                        Icon(HevIcons.History, contentDescription = stringResource(Res.string.cd_history))
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
                Text(stringResource(Res.string.loading), color = MaterialTheme.colorScheme.onSurfaceVariant)
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
            entry.context?.takeIf { it.isNotBlank() }?.let { context ->
                Spacer(Modifier.height(8.dp))
                Text(
                    text = context,
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
                                stringResource(Res.string.no_probability_data),
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
                item { Headline(stringResource(Res.string.score_label, formatScore(outcome.score))) }
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
                item { Headline(stringResource(Res.string.yes_probability, formatPercent(outcome.probability))) }
                item {
                    ProbabilityCard {
                        ProbabilityBar(label = "", probability = outcome.probability, highlighted = true)
                    }
                }
            }
            DecisionOutcome.Refused -> item { Headline(stringResource(Res.string.result_refused)) }
            null -> item { Headline(stringResource(Res.string.value_na)) }
        }

        item {
            val meta = listOfNotNull(
                entry.providerName?.takeIf { it.isNotBlank() },
                entry.model?.takeIf { it.isNotBlank() },
                entry.inputTokens?.let { pluralStringResource(Res.plurals.input_tokens, it, it) },
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
        MetaChip(stringResource(Res.string.confidence_label, formatConfidence(confidence, "")))
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
