package ai.hev.app.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import ai.hev.app.domain.model.HistoryEntry
import ai.hev.app.domain.model.QuestionType

@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val createdAt: Long,
    val questionType: String = QuestionType.Choice.apiValue,
    val question: String,
    val state: String?,
    val optionsJson: String,
    val probabilitiesJson: String,
    val confidence: Double?,
    val choice: String?,
    val score: Double? = null,
    val noul: Double? = null,
    val model: String?,
    val providerName: String?,
    val rawJson: String,
)

fun HistoryEntity.toDomain() = HistoryEntry(
    id = id,
    createdAt = createdAt,
    questionType = QuestionType.from(questionType),
    question = question,
    state = state,
    optionsJson = optionsJson,
    probabilitiesJson = probabilitiesJson,
    confidence = confidence,
    choice = choice,
    score = score,
    noul = noul,
    model = model,
    providerName = providerName,
    rawJson = rawJson,
)

fun HistoryEntry.toEntity() = HistoryEntity(
    id = id,
    createdAt = createdAt,
    questionType = questionType.apiValue,
    question = question,
    state = state,
    optionsJson = optionsJson,
    probabilitiesJson = probabilitiesJson,
    confidence = confidence,
    choice = choice,
    score = score,
    noul = noul,
    model = model,
    providerName = providerName,
    rawJson = rawJson,
)
