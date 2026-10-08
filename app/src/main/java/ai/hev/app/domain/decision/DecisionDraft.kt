package ai.hev.app.domain.decision

/** What the user is composing on the home screen. */
data class DecisionDraft(
    val kind: DecisionKind,
    val instructions: String,
    val context: String,
    /** Choice options or score levels, depending on [kind]. */
    val items: List<ChoiceOption>,
) {
    /** First problem that keeps this draft from being sent, or null when it is ready. */
    fun validate(capabilities: ModelCapabilities): DraftIssue? {
        if (!capabilities.supports(kind)) return DraftIssue.UnsupportedKind
        if (instructions.isBlank()) return DraftIssue.MissingQuestion
        val range = capabilities.itemRange(kind) ?: return null
        return when {
            items.size !in range -> DraftIssue.ItemCount(kind, range)
            items.any { it.label.isBlank() } -> DraftIssue.BlankItem(kind)
            else -> null
        }
    }

    /** This draft with [kind]; items start over when the kind changes. */
    fun withKind(kind: DecisionKind): DecisionDraft =
        if (kind == this.kind) this else copy(kind = kind, items = DecisionItems.initial(kind))

    /** Moves to the first supported kind when [capabilities] do not accept the current one. */
    fun fittedTo(capabilities: ModelCapabilities): DecisionDraft =
        if (capabilities.supports(kind)) this else withKind(DecisionKind.entries.first(capabilities::supports))

    fun toRequest(model: String): DecisionRequest = DecisionRequest(
        model = model,
        instructions = instructions.trim(),
        context = context.trim().ifEmpty { null },
        question = when (kind) {
            DecisionKind.Choice -> Question.Choice(items.map { it.copy(label = it.label.trim()) })
            DecisionKind.Score -> Question.Score(items.map { it.label.trim() })
            DecisionKind.YesNo -> Question.YesNo
        },
    )
}

sealed interface DraftIssue {
    data object UnsupportedKind : DraftIssue
    data object MissingQuestion : DraftIssue
    data class ItemCount(val kind: DecisionKind, val range: IntRange) : DraftIssue
    data class BlankItem(val kind: DecisionKind) : DraftIssue
}
