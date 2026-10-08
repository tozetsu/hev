package ai.hev.app.domain.decision

import java.io.IOException

/** Why a decision request failed. */
sealed class DecisionError(message: String, cause: Throwable? = null) : Exception(message, cause) {

    /** The server answered with an error status. [detail] is its message, or the raw body. */
    sealed class Http(val status: Int, val detail: String) : DecisionError("HTTP $status: $detail")

    /** Worth one retry after [retryAfterSeconds] (or a short default). */
    sealed class Transient(status: Int, detail: String, val retryAfterSeconds: Long?) : Http(status, detail)

    class Unauthorized(status: Int, detail: String) : Http(status, detail)
    class InvalidRequest(status: Int, detail: String) : Http(status, detail)
    class RateLimited(status: Int, detail: String, retryAfterSeconds: Long?) :
        Transient(status, detail, retryAfterSeconds)
    class Unavailable(status: Int, detail: String, retryAfterSeconds: Long?) :
        Transient(status, detail, retryAfterSeconds)
    class GatewayTimeout(status: Int, detail: String) : Http(status, detail)
    class Server(status: Int, detail: String) : Http(status, detail)

    class Network(cause: IOException) : DecisionError(cause.message ?: cause.javaClass.simpleName, cause)

    /** The response did not match the protocol. */
    class MalformedResponse(detail: String, cause: Throwable? = null) : DecisionError(detail, cause)

    companion object {
        fun forStatus(status: Int, detail: String, retryAfterSeconds: Long? = null): Http = when (status) {
            401, 403 -> Unauthorized(status, detail)
            429 -> RateLimited(status, detail, retryAfterSeconds)
            503, 529 -> Unavailable(status, detail, retryAfterSeconds)
            504 -> GatewayTimeout(status, detail)
            in 400..499 -> InvalidRequest(status, detail)
            else -> Server(status, detail)
        }
    }
}
