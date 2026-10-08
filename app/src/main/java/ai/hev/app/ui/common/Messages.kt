package ai.hev.app.ui.common

import android.content.Context
import ai.hev.app.R
import ai.hev.app.domain.decision.DecisionError
import ai.hev.app.domain.decision.DecisionKind
import ai.hev.app.domain.decision.DraftIssue

/** Short headline plus the server's own message when there is one. */
fun DecisionError.message(context: Context): String {
    val headline = context.getString(
        when (this) {
            is DecisionError.Unauthorized -> R.string.error_unauthorized
            is DecisionError.InvalidRequest -> R.string.error_invalid_request
            is DecisionError.RateLimited -> R.string.error_rate_limited
            is DecisionError.Unavailable -> R.string.error_unavailable
            is DecisionError.GatewayTimeout -> R.string.error_timeout
            is DecisionError.Server -> R.string.error_server
            is DecisionError.Network -> R.string.error_network
            is DecisionError.MalformedResponse -> R.string.error_malformed
        },
    )
    return when (this) {
        is DecisionError.Http -> "${context.getString(R.string.error_http_status, headline, status)}\n$detail"
        else -> headline
    }
}

fun DraftIssue.message(context: Context): String = when (this) {
    DraftIssue.UnsupportedKind -> context.getString(R.string.error_kind_unsupported)
    DraftIssue.MissingQuestion -> context.getString(R.string.error_question)
    is DraftIssue.ItemCount -> context.getString(
        if (kind == DecisionKind.Score) R.string.error_levels_range else R.string.error_options_range,
        range.first,
        range.last,
    )
    is DraftIssue.BlankItem -> context.getString(
        if (kind == DecisionKind.Score) R.string.error_levels_empty else R.string.error_options_empty,
    )
}
