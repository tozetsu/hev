package ai.hev.app.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Column names are kept from schema v2; [HistoryMapper] translates to the domain model. */
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
)
