package ai.hev.app.ui.components

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import kotlin.time.Instant

/** Formats a 0–1 value as a whole percentage. */
fun formatPercent(value: Double): String = "${(value.coerceIn(0.0, 1.0) * 100).roundToInt()}%"

fun formatConfidence(value: Double?, na: String): String = value?.let(::formatPercent) ?: na

/** Whole scores without decimals, fractional scores with two. */
fun formatScore(value: Double): String =
    if (value % 1.0 == 0.0) value.toLong().toString() else formatFixed(value, decimals = 2)

/** [value] with exactly [decimals] fraction digits, rounded half away from zero. */
fun formatFixed(value: Double, decimals: Int): String {
    val unit = 10.0.pow(decimals).toLong()
    val scaled = (abs(value) * unit).roundToLong()
    val sign = if (value < 0 && scaled != 0L) "-" else ""
    val fraction = if (decimals == 0) "" else "." + (scaled % unit).toString().padStart(decimals, '0')
    return "$sign${scaled / unit}$fraction"
}

private val TimestampFormat = LocalDateTime.Format {
    monthNumber()
    char('-')
    day()
    char(' ')
    hour()
    char(':')
    minute()
}

/** `MM-dd HH:mm` in [zone]. */
fun formatTimestamp(epochMillis: Long, zone: TimeZone = TimeZone.currentSystemDefault()): String =
    Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(zone).format(TimestampFormat)
