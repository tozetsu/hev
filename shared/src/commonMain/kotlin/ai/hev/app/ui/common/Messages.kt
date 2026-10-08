package ai.hev.app.ui.common

import ai.hev.app.domain.decision.DecisionError
import ai.hev.app.domain.decision.DecisionKind
import ai.hev.app.domain.decision.DraftIssue
import ai.hev.app.domain.provider.ProviderIssue
import ai.hev.app.resources.Res
import ai.hev.app.resources.error_api_key
import ai.hev.app.resources.error_endpoint_invalid
import ai.hev.app.resources.error_http_status
import ai.hev.app.resources.error_invalid_request
import ai.hev.app.resources.error_kind_unsupported
import ai.hev.app.resources.error_levels_empty
import ai.hev.app.resources.error_levels_range
import ai.hev.app.resources.error_malformed
import ai.hev.app.resources.error_model_required
import ai.hev.app.resources.error_name_required
import ai.hev.app.resources.error_network
import ai.hev.app.resources.error_options_empty
import ai.hev.app.resources.error_options_range
import ai.hev.app.resources.error_question
import ai.hev.app.resources.error_rate_limited
import ai.hev.app.resources.error_server
import ai.hev.app.resources.error_timeout
import ai.hev.app.resources.error_unauthorized
import ai.hev.app.resources.error_unavailable
import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource

/** Short headline plus the server's own message when there is one. */
@Composable
fun DecisionError.text(): String {
    val headline = stringResource(
        when (this) {
            is DecisionError.Unauthorized -> Res.string.error_unauthorized
            is DecisionError.InvalidRequest -> Res.string.error_invalid_request
            is DecisionError.RateLimited -> Res.string.error_rate_limited
            is DecisionError.Unavailable -> Res.string.error_unavailable
            is DecisionError.GatewayTimeout -> Res.string.error_timeout
            is DecisionError.Server -> Res.string.error_server
            is DecisionError.Network -> Res.string.error_network
            is DecisionError.MalformedResponse -> Res.string.error_malformed
        },
    )
    return when (this) {
        is DecisionError.Http -> "${stringResource(Res.string.error_http_status, headline, status)}\n$detail"
        else -> headline
    }
}

@Composable
fun DraftIssue.text(): String = when (this) {
    DraftIssue.UnsupportedKind -> stringResource(Res.string.error_kind_unsupported)
    DraftIssue.MissingQuestion -> stringResource(Res.string.error_question)
    is DraftIssue.ItemCount -> stringResource(
        if (kind == DecisionKind.Score) Res.string.error_levels_range else Res.string.error_options_range,
        range.first,
        range.last,
    )
    is DraftIssue.BlankItem -> stringResource(
        if (kind == DecisionKind.Score) Res.string.error_levels_empty else Res.string.error_options_empty,
    )
}

@Composable
fun ProviderIssue.text(): String = stringResource(
    when (this) {
        ProviderIssue.MissingName -> Res.string.error_name_required
        ProviderIssue.InvalidEndpoint -> Res.string.error_endpoint_invalid
        ProviderIssue.MissingApiKey -> Res.string.error_api_key
        ProviderIssue.MissingModel -> Res.string.error_model_required
    },
)
