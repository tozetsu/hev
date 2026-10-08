package ai.hev.app.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import ai.hev.app.R
import ai.hev.app.domain.decision.DecisionError
import ai.hev.app.domain.decision.DecisionKind
import ai.hev.app.domain.decision.DraftIssue
import ai.hev.app.domain.provider.ProviderIssue

/** Short headline plus the server's own message when there is one. */
@Composable
fun DecisionError.text(): String {
    val headline = stringResource(
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
        is DecisionError.Http -> "${stringResource(R.string.error_http_status, headline, status)}\n$detail"
        else -> headline
    }
}

@Composable
fun DraftIssue.text(): String = when (this) {
    DraftIssue.UnsupportedKind -> stringResource(R.string.error_kind_unsupported)
    DraftIssue.MissingQuestion -> stringResource(R.string.error_question)
    is DraftIssue.ItemCount -> stringResource(
        if (kind == DecisionKind.Score) R.string.error_levels_range else R.string.error_options_range,
        range.first,
        range.last,
    )
    is DraftIssue.BlankItem -> stringResource(
        if (kind == DecisionKind.Score) R.string.error_levels_empty else R.string.error_options_empty,
    )
}

@Composable
fun ProviderIssue.text(): String = stringResource(
    when (this) {
        ProviderIssue.MissingName -> R.string.error_name_required
        ProviderIssue.InvalidEndpoint -> R.string.error_endpoint_invalid
        ProviderIssue.MissingApiKey -> R.string.error_api_key
        ProviderIssue.MissingModel -> R.string.error_model_required
    },
)
