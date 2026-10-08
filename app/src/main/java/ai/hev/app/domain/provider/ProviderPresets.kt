package ai.hev.app.domain.provider

import ai.hev.app.domain.provider.DecisionProtocol.OpenAiDecisions
import ai.hev.app.domain.provider.DecisionProtocol.SystemOne

/** Built-in vendor presets, in display order. */
object ProviderPresets {

    val TypeSafe = ProviderPreset(
        id = "typesafe",
        name = "TypeSafe",
        protocol = SystemOne,
        endpoint = "https://api.typesafe.ai/v1/systemone",
        modelsUrl = "https://api.typesafe.ai/v1/models",
        models = listOf("jev-latest", "jev-preview", "jev-1.13.0"),
    )

    val Perplexity = ProviderPreset(
        id = "perplexity",
        name = "Perplexity",
        protocol = SystemOne,
        endpoint = "https://api.perplexity.ai/v1/decisions",
        models = listOf("pplx-decider-v1.1-27b", "pplx-decider-v1-27b"),
    )

    val AlibabaBeijing = ProviderPreset(
        id = "alibaba-beijing",
        name = "Alibaba Beijing",
        protocol = SystemOne,
        endpoint = "https://{WorkspaceId}.cn-beijing.maas.aliyuncs.com/compatible-mode/v1/systemone",
        models = listOf("decision-model-preview"),
    )

    val AlibabaSingapore = ProviderPreset(
        id = "alibaba-singapore",
        name = "Alibaba Singapore",
        protocol = SystemOne,
        endpoint = "https://{WorkspaceId}.ap-southeast-1.maas.aliyuncs.com/compatible-mode/v1/systemone",
        models = listOf("decision-model-preview"),
    )

    val OpenRouter = ProviderPreset(
        id = "openrouter",
        name = "OpenRouter",
        protocol = SystemOne,
        endpoint = "https://openrouter.ai/api/v1/systemone",
        modelsUrl = "https://openrouter.ai/api/v1/models?output_modalities=decisions",
        models = listOf("typesafe/jev-1.13", "~typesafe/jev-latest", "liquid/d1"),
    )

    val VercelSystemOne = ProviderPreset(
        id = "vercel-systemone",
        name = "Vercel System One",
        protocol = SystemOne,
        endpoint = "https://ai-gateway.vercel.sh/typesafe/v1/systemone",
        modelsUrl = "https://ai-gateway.vercel.sh/typesafe/v1/models",
        models = listOf("typesafe-ai/jev"),
    )

    val DeepInfra = ProviderPreset(
        id = "deepinfra",
        name = "DeepInfra",
        protocol = SystemOne,
        endpoint = "https://api.deepinfra.com/v1/decisions",
        modelsUrl = "https://api.deepinfra.com/typesafe/v1/models",
        models = listOf("typesafe/jev"),
    )

    val Liquid = ProviderPreset(
        id = "liquid",
        name = "Liquid",
        protocol = SystemOne,
        endpoint = "https://api.liquid.ai/decisions/v1/systemone",
        models = listOf("d1", "d1:free"),
    )

    val Ollama = ProviderPreset(
        id = "ollama",
        name = "Ollama",
        protocol = SystemOne,
        endpoint = "http://localhost:11434/v1/systemone",
        modelsUrl = "/api/tags",
        models = listOf("nimble", "tev1", "clef", "clef-flash"),
        apiKeyRequired = false,
    )

    val OpenAi = ProviderPreset(
        id = "openai",
        name = "OpenAI",
        protocol = OpenAiDecisions,
        endpoint = "https://api.openai.com/v1/decisions",
        models = listOf("gpt-6-luna"),
    )

    val VercelDecisions = ProviderPreset(
        id = "vercel-decisions",
        name = "Vercel Decisions",
        protocol = OpenAiDecisions,
        endpoint = "https://ai-gateway.vercel.sh/v1/decisions",
        models = listOf("openai/gpt-6-luna-decisions", "typesafe-ai/jev"),
    )

    val all: List<ProviderPreset> = listOf(
        TypeSafe, Perplexity, AlibabaBeijing, AlibabaSingapore, OpenRouter, VercelSystemOne,
        DeepInfra, Liquid, Ollama, OpenAi, VercelDecisions,
    )

    init {
        check(all.map { it.id }.toSet().size == all.size) { "Duplicate preset id" }
    }

    fun byId(id: String?): ProviderPreset? = id?.let { wanted -> all.firstOrNull { it.id == wanted } }

    /** The preset whose endpoint is exactly [endpoint] in [protocol], if any. */
    fun matching(protocol: DecisionProtocol, endpoint: String): ProviderPreset? =
        all.firstOrNull { it.protocol == protocol && it.endpoint == endpoint.trim() }
}
