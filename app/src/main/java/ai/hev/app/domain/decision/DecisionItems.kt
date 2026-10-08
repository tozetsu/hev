package ai.hev.app.domain.decision

/**
 * Item lists for the home editor: choice options use Excel-style ids (a, b, … z, aa, …);
 * score levels use their index as id.
 */
object DecisionItems {
    fun initial(kind: DecisionKind): List<ChoiceOption> = when (kind) {
        DecisionKind.Choice -> List(2) { ChoiceOption(choiceOptionId(it), "") }
        DecisionKind.Score -> List(2) { ChoiceOption(it.toString(), "") }
        DecisionKind.YesNo -> emptyList()
    }

    fun added(kind: DecisionKind, items: List<ChoiceOption>): List<ChoiceOption> = when (kind) {
        DecisionKind.Choice -> items + ChoiceOption(nextChoiceOptionId(items.mapTo(HashSet()) { it.id }), "")
        DecisionKind.Score -> items + ChoiceOption(items.size.toString(), "")
        DecisionKind.YesNo -> items
    }

    fun removed(kind: DecisionKind, items: List<ChoiceOption>, id: String): List<ChoiceOption> {
        val remaining = items.filterNot { it.id == id }
        return if (kind == DecisionKind.Score) {
            remaining.mapIndexed { index, level -> level.copy(id = index.toString()) }
        } else {
            remaining
        }
    }

    /** Choice options built from labels, ids assigned in order. */
    fun choiceOptions(labels: List<String>): List<ChoiceOption> =
        labels.mapIndexed { index, label -> ChoiceOption(choiceOptionId(index), label) }
}

fun choiceOptionId(index: Int): String {
    require(index >= 0)
    var n = index + 1
    val sb = StringBuilder()
    while (n > 0) {
        n--
        sb.append('a' + n % 26)
        n /= 26
    }
    return sb.reverse().toString()
}

private fun nextChoiceOptionId(used: Set<String>): String =
    generateSequence(0) { it + 1 }.map(::choiceOptionId).first { it !in used }

/**
 * Parses one option per line, stripping numbered (`1.` / `1)`) and bullet (`-` / `*` / `•`) prefixes.
 * Blank lines are dropped; the result is capped at [max].
 */
fun parseStructuredOptionLines(text: String, max: Int): List<String> {
    val prefix = Regex("""^(\d+[.)]\s+|[-*•]\s+)""")
    return text.lineSequence()
        .map { prefix.replaceFirst(it.trim(), "").trim() }
        .filter { it.isNotEmpty() }
        .take(max)
        .toList()
}
