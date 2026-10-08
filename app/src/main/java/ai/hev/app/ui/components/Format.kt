package ai.hev.app.ui.components

import java.util.Locale
import kotlin.math.roundToInt

/** Formats a 0–1 value as a whole percentage. */
fun formatPercent(value: Double): String = "${(value.coerceIn(0.0, 1.0) * 100).roundToInt()}%"

fun formatConfidence(value: Double?, na: String): String = value?.let(::formatPercent) ?: na

/** Whole scores without decimals, fractional scores with two. */
fun formatScore(value: Double): String =
    if (value % 1.0 == 0.0) value.toLong().toString() else String.format(Locale.US, "%.2f", value)
