package ai.hev.app.domain.history

import ai.hev.app.domain.decision.ChoiceOption
import ai.hev.app.domain.decision.DecisionKind
import ai.hev.app.domain.decision.DecisionOutcome
import ai.hev.app.domain.provider.DecisionProtocol

data class HistoryEntry(
    val id: Long = 0,
    val createdAt: Long,
    val kind: DecisionKind,
    val instructions: String,
    val context: String?,
    /** Choice options or score levels as sent. */
    val items: List<ChoiceOption>,
    /** Null only for legacy rows saved without an answer value. */
    val outcome: DecisionOutcome?,
    val model: String?,
    val providerName: String?,
    val rawJson: String,
    /** Provider details are unknown for entries saved before they were recorded. */
    val providerId: String? = null,
    val protocol: DecisionProtocol? = null,
    val inputTokens: Int? = null,
)
