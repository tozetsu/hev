package ai.hev.app.domain.decision

/** What a model accepts. Drives which kinds and how many items the UI offers. */
data class ModelCapabilities(
    val kinds: Set<DecisionKind>,
    val choiceOptions: IntRange,
    val scoreLevels: IntRange,
) {
    init {
        require(kinds.isNotEmpty())
        require(choiceOptions.first >= 2 && !choiceOptions.isEmpty())
        require(scoreLevels.first >= 2 && !scoreLevels.isEmpty())
    }

    fun supports(kind: DecisionKind): Boolean = kind in kinds

    /** Allowed number of options or levels; null for kinds without items. */
    fun itemRange(kind: DecisionKind): IntRange? = when (kind) {
        DecisionKind.Choice -> choiceOptions
        DecisionKind.Score -> scoreLevels
        DecisionKind.YesNo -> null
    }

    companion object {
        /** For models without known limits; the server stays the final authority. */
        val Lenient = ModelCapabilities(
            kinds = DecisionKind.entries.toSet(),
            choiceOptions = 2..255,
            scoreLevels = 2..10,
        )
    }
}
