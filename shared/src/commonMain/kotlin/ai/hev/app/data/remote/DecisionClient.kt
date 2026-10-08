package ai.hev.app.data.remote

import ai.hev.app.data.remote.http.HttpTransport
import ai.hev.app.data.remote.openai.OpenAiDecisionsCodec
import ai.hev.app.data.remote.systemone.SystemOneCodec
import ai.hev.app.domain.decision.DecisionRequest
import ai.hev.app.domain.decision.DecisionResult
import ai.hev.app.domain.provider.DecisionProtocol

/** Sends a decision request in the provider's protocol and reads the typed answer. */
class DecisionClient(private val transport: HttpTransport = HttpTransport()) {

    suspend fun decide(
        protocol: DecisionProtocol,
        endpoint: String,
        apiKey: String,
        request: DecisionRequest,
    ): DecisionResult {
        val codec = codecFor(protocol)
        val raw = transport.post(endpoint, apiKey, codec.encode(request))
        return codec.decode(raw, request.kind)
    }

    private fun codecFor(protocol: DecisionProtocol): DecisionCodec = when (protocol) {
        DecisionProtocol.SystemOne -> SystemOneCodec
        DecisionProtocol.OpenAiDecisions -> OpenAiDecisionsCodec
    }
}
