package ai.hev.app.testing

import ai.hev.app.domain.provider.DecisionProtocol
import ai.hev.app.domain.provider.DecisionProtocol.OpenAiDecisions
import ai.hev.app.domain.provider.DecisionProtocol.SystemOne

/** A vendor as its docs describe it: protocol, endpoint, and a model it serves. */
data class Vendor(
    val id: String,
    val protocol: DecisionProtocol,
    val endpoint: String,
    val model: String,
    val apiKeyRequired: Boolean = true,
)

/** The vendors the fixtures come from. */
object Vendors {
    val TypeSafe = Vendor("typesafe", SystemOne, "https://api.typesafe.ai/v1/systemone", "jev-latest")
    val Perplexity = Vendor("perplexity", SystemOne, "https://api.perplexity.ai/v1/decisions", "pplx-decider-v1.1-27b")
    val AlibabaBeijing = Vendor(
        "alibaba-beijing", SystemOne,
        "https://ws-1.cn-beijing.maas.aliyuncs.com/compatible-mode/v1/systemone", "decision-model-preview",
    )
    val AlibabaSingapore = Vendor(
        "alibaba-singapore", SystemOne,
        "https://ws-1.ap-southeast-1.maas.aliyuncs.com/compatible-mode/v1/systemone", "decision-model-preview",
    )
    val OpenRouter = Vendor("openrouter", SystemOne, "https://openrouter.ai/api/v1/systemone", "typesafe/jev-1.13")
    val VercelSystemOne = Vendor("vercel-systemone", SystemOne, "https://ai-gateway.vercel.sh/typesafe/v1/systemone", "typesafe-ai/jev")
    val DeepInfra = Vendor("deepinfra", SystemOne, "https://api.deepinfra.com/v1/decisions", "typesafe/jev")
    val Liquid = Vendor("liquid", SystemOne, "https://api.liquid.ai/decisions/v1/systemone", "d1")
    val Ollama = Vendor("ollama", SystemOne, "http://localhost:11434/v1/systemone", "nimble", apiKeyRequired = false)
    val OpenAi = Vendor("openai", OpenAiDecisions, "https://api.openai.com/v1/decisions", "gpt-6-luna")
    val VercelDecisions = Vendor("vercel-decisions", OpenAiDecisions, "https://ai-gateway.vercel.sh/v1/decisions", "openai/gpt-6-luna-decisions")

    val all = listOf(
        TypeSafe, Perplexity, AlibabaBeijing, AlibabaSingapore, OpenRouter, VercelSystemOne,
        DeepInfra, Liquid, Ollama, OpenAi, VercelDecisions,
    )
}
