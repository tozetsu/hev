package ai.hev.app.domain.choice

import ai.hev.app.domain.model.ChoiceOption

/** API max for choice criteria map size (TypeSafe System One). */
const val MIN_CHOICE_OPTIONS = 2
const val MAX_CHOICE_OPTIONS = 255

const val MIN_SCORE_LEVELS = 2
const val MAX_SCORE_LEVELS = 10

/**
 * Excel-style lowercase ids: a, b, … z, aa, ab, … — enough for [MAX_CHOICE_OPTIONS].
 */
fun choiceOptionId(index: Int): String {
    require(index >= 0)
    var n = index + 1
    val sb = StringBuilder()
    while (n > 0) {
        n--
        sb.append(('a' + n % 26).toChar())
        n /= 26
    }
    return sb.reverse().toString()
}

fun nextChoiceOptionId(used: Set<String>): String {
    var i = 0
    while (true) {
        val id = choiceOptionId(i)
        if (id !in used) return id
        i++
    }
}

/**
 * Parse structured text into option labels.
 * Supports one option per line; strips numbered (`1.` / `1)`) and bullet (`-` / `*` / `•`) prefixes.
 * Empties dropped; result capped at [max].
 */
fun parseStructuredOptionLines(text: String, max: Int = MAX_CHOICE_OPTIONS): List<String> {
    val prefix = Regex("""^(\d+[.)]\s+|[-*•]\s+)""")
    return text.lineSequence()
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .map { line -> prefix.replaceFirst(line, "").trim() }
        .filter { it.isNotEmpty() }
        .take(max)
        .toList()
}

fun optionsFromLabels(labels: List<String>): List<ChoiceOption> =
    labels.mapIndexed { index, label -> ChoiceOption(id = choiceOptionId(index), label = label) }
