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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ai.hev.app.HevApp
import ai.hev.app.R
import ai.hev.app.domain.model.HistoryEntry
import ai.hev.app.domain.model.QuestionType
import ai.hev.app.ui.components.MetaChip
import ai.hev.app.ui.components.ProbabilityBar
import ai.hev.app.ui.components.formatConfidence
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.math.roundToInt

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
    val probs = parseProbabilities(entry.probabilitiesJson)
    val labels = parseOptionLabels(entry.optionsJson)
    val levelList = parseLevelList(entry.optionsJson)
    val sorted = probs.entries.sortedByDescending { it.value }
    val na = stringResource(R.string.value_na)

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Text(
                text = entry.question,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            if (!entry.state.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = entry.state,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        when (entry.questionType) {
            QuestionType.Choice -> {
                item {
                    MetaChip(stringResource(R.string.confidence_label, formatConfidence(entry.confidence, na)))
                }
                item {
                    ProbabilityCard {
                        if (sorted.isEmpty()) {
                            Text(stringResource(R.string.no_probability_data), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            sorted.forEach { (key, value) ->
                                val optionText = labels[key] ?: key
                                ProbabilityBar(
                                    label = "$key  $optionText",
                                    probability = value,
                                    highlighted = key == entry.choice,
                                )
                            }
                        }
                    }
                }
            }
            QuestionType.Score -> {
                item {
                    Text(
                        text = stringResource(
                            R.string.score_label,
                            entry.score?.let { formatScore(it) } ?: na,
                        ),
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                item {
                    MetaChip(stringResource(R.string.confidence_label, formatConfidence(entry.confidence, na)))
                }
                if (sorted.isNotEmpty()) {
                    item {
                        ProbabilityCard {
                            sorted.forEach { (key, value) ->
                                val levelText = resolveLevelLabel(key, labels, levelList)
                                ProbabilityBar(
                                    label = levelText,
                                    probability = value,
                                    highlighted = false,
                                )
                            }
                        }
                    }
                }
            }
            QuestionType.Noul -> {
                item {
                    val pct = entry.noul?.let { formatPercent(it) } ?: na
                    Text(
                        text = stringResource(R.string.noul_label, pct),
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                entry.noul?.let { noul ->
                    item {
                        ProbabilityCard {
                            ProbabilityBar(
                                label = "",
                                probability = noul,
                                highlighted = true,
                            )
                        }
                    }
                }
                if (entry.confidence != null) {
                    item {
                        MetaChip(stringResource(R.string.confidence_label, formatConfidence(entry.confidence, na)))
                    }
                }
            }
        }

        item {
            val meta = listOfNotNull(
                entry.providerName?.takeIf { it.isNotBlank() },
                entry.model?.takeIf { it.isNotBlank() },
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

private fun formatScore(value: Double): String {
    return if (value == value.toLong().toDouble()) {
        value.toLong().toString()
    } else {
        String.format("%.2f", value)
    }
}

private fun formatPercent(value: Double): String {
    val pct = (value * 100).coerceIn(0.0, 100.0)
    return "${pct.roundToInt()}%"
}

private fun resolveLevelLabel(
    key: String,
    labels: Map<String, String>,
    levelList: List<String>,
): String {
    labels[key]?.let { return it }
    val index = key.toIntOrNull()
    if (index != null && index in levelList.indices) return levelList[index]
    return key
}

private val json = Json { ignoreUnknownKeys = true }

fun parseProbabilities(raw: String): Map<String, Double> {
    if (raw.isBlank()) return emptyMap()
    return runCatching {
        val obj = json.parseToJsonElement(raw).jsonObject
        obj.mapValues { (_, v) -> v.jsonPrimitive.doubleOrNull ?: 0.0 }
    }.getOrElse { emptyMap() }
}

fun parseOptionLabels(raw: String): Map<String, String> {
    if (raw.isBlank()) return emptyMap()
    return runCatching {
        val arr = json.parseToJsonElement(raw).jsonArray
        arr.mapNotNull { el ->
            val o = el.jsonObject
            val id = o["id"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
            val label = o["label"]?.jsonPrimitive?.contentOrNull ?: id
            id to label
        }.toMap()
    }.getOrElse { emptyMap() }
}

fun parseLevelList(raw: String): List<String> {
    if (raw.isBlank()) return emptyList()
    return runCatching {
        val arr = json.parseToJsonElement(raw).jsonArray
        arr.mapNotNull { el ->
            val o = el.jsonObject
            o["label"]?.jsonPrimitive?.contentOrNull
        }
    }.getOrElse { emptyList() }
}
