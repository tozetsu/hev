package ai.hev.app.data.local.db

import ai.hev.app.data.local.StorageKeys
import ai.hev.app.domain.decision.ChoiceOption
import ai.hev.app.domain.decision.DecisionKind
import ai.hev.app.domain.decision.DecisionOutcome
import ai.hev.app.domain.history.HistoryEntry
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

/** Converts between [HistoryEntry] and its stored row. */
object HistoryMapper {
    private val json = Json { ignoreUnknownKeys = true }
    private val itemsSerializer = ListSerializer(StoredItem.serializer())
    private val probabilitiesSerializer = MapSerializer(String.serializer(), Double.serializer())

    fun toEntity(entry: HistoryEntry): HistoryEntity {
        val outcome = entry.outcome
        return HistoryEntity(
            id = entry.id,
            createdAt = entry.createdAt,
            questionType = StorageKeys.of(entry.kind),
            question = entry.instructions,
            state = entry.context,
            optionsJson = json.encodeToString(itemsSerializer, entry.items.map { StoredItem(it.id, it.label) }),
            probabilitiesJson = json.encodeToString(probabilitiesSerializer, outcome.probabilities()),
            confidence = (outcome as? DecisionOutcome.Choice)?.confidence
                ?: (outcome as? DecisionOutcome.Score)?.confidence,
            choice = (outcome as? DecisionOutcome.Choice)?.choice,
            score = (outcome as? DecisionOutcome.Score)?.score,
            noul = (outcome as? DecisionOutcome.YesNo)?.probability,
            model = entry.model,
            providerName = entry.providerName,
            rawJson = entry.rawJson,
            providerId = entry.providerId,
            protocol = entry.protocol?.let(StorageKeys::of),
            refused = outcome == DecisionOutcome.Refused,
            inputTokens = entry.inputTokens,
        )
    }

    fun toDomain(entity: HistoryEntity): HistoryEntry {
        val kind = StorageKeys.kind(entity.questionType) ?: DecisionKind.Choice
        val probabilities = decodeOrDefault(entity.probabilitiesJson, emptyMap()) {
            json.decodeFromString(probabilitiesSerializer, it)
        }
        val outcome = if (entity.refused) DecisionOutcome.Refused else when (kind) {
            DecisionKind.Choice -> entity.choice?.let {
                DecisionOutcome.Choice(it, probabilities, entity.confidence)
            }
            DecisionKind.Score -> entity.score?.let { score ->
                DecisionOutcome.Score(
                    score = score,
                    probabilities = probabilities
                        .mapNotNull { (k, v) -> k.toIntOrNull()?.let { it to v } }
                        .toMap(),
                    confidence = entity.confidence,
                )
            }
            DecisionKind.YesNo -> entity.noul?.let { DecisionOutcome.YesNo(it) }
        }
        return HistoryEntry(
            id = entity.id,
            createdAt = entity.createdAt,
            kind = kind,
            instructions = entity.question,
            context = entity.state,
            items = decodeOrDefault(entity.optionsJson, emptyList()) {
                json.decodeFromString(itemsSerializer, it).map { item -> ChoiceOption(item.id, item.label) }
            },
            outcome = outcome,
            model = entity.model,
            providerName = entity.providerName,
            rawJson = entity.rawJson,
            providerId = entity.providerId,
            protocol = StorageKeys.protocol(entity.protocol),
            inputTokens = entity.inputTokens,
        )
    }

    private fun DecisionOutcome?.probabilities(): Map<String, Double> = when (this) {
        is DecisionOutcome.Choice -> probabilities
        is DecisionOutcome.Score -> probabilities.mapKeys { it.key.toString() }
        else -> emptyMap()
    }

    private inline fun <T> decodeOrDefault(raw: String, default: T, decode: (String) -> T): T =
        if (raw.isBlank()) default else runCatching { decode(raw) }.getOrDefault(default)

    @Serializable
    private data class StoredItem(val id: String, val label: String = id)
}
