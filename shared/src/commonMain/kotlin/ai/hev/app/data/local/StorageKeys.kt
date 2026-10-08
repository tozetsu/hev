package ai.hev.app.data.local

import ai.hev.app.domain.decision.DecisionKind
import ai.hev.app.domain.provider.DecisionProtocol

/** Stable on-disk names, independent of Kotlin identifiers. Never change an existing key. */
internal object StorageKeys {

    fun of(kind: DecisionKind): String = when (kind) {
        DecisionKind.Choice -> "choice"
        DecisionKind.Score -> "score"
        DecisionKind.YesNo -> "yes_no"
    }

    fun of(protocol: DecisionProtocol): String = when (protocol) {
        DecisionProtocol.SystemOne -> "system_one"
        DecisionProtocol.OpenAiDecisions -> "openai_decisions"
    }

    fun kind(key: String?): DecisionKind? = DecisionKind.entries.firstOrNull { of(it) == key }

    fun protocol(key: String?): DecisionProtocol? = DecisionProtocol.entries.firstOrNull { of(it) == key }
}
