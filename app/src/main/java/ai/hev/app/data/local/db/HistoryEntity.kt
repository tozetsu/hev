package ai.hev.app.data.local.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A stored decision. Column names predate the domain model (`question` holds the instructions,
 * `state` the context, `noul` the yes probability); [HistoryMapper] translates.
 * Schema changes need a migration in [HevMigrations] and an exported schema.
 */
@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val createdAt: Long,
    val questionType: String,
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
    val providerId: String? = null,
    val protocol: String? = null,
    @ColumnInfo(defaultValue = "0") val refused: Boolean = false,
    val inputTokens: Int? = null,
)
