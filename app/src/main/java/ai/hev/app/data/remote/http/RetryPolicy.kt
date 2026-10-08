package ai.hev.app.data.remote.http

import ai.hev.app.domain.decision.DecisionError
import java.time.Instant
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/** One retry for transient failures (429 / 503 / 529), waiting as the server asks within [maxDelay]. */
data class RetryPolicy(
    val defaultDelay: Duration = 1.seconds,
    val maxDelay: Duration = 10.seconds,
) {
    fun delayFor(error: DecisionError.Transient): Duration =
        (error.retryAfterSeconds?.seconds ?: defaultDelay).coerceIn(Duration.ZERO, maxDelay)

    companion object {
        /** `Retry-After` as delta-seconds or an HTTP date. */
        fun parseRetryAfter(value: String?, now: Instant = Instant.now()): Long? {
            val text = value?.trim()?.takeIf { it.isNotEmpty() } ?: return null
            text.toLongOrNull()?.let { return it.coerceAtLeast(0) }
            return runCatching {
                val date = ZonedDateTime.parse(text, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant()
                (date.epochSecond - now.epochSecond).coerceAtLeast(0)
            }.getOrNull()
        }
    }
}
