package ai.hev.app.domain.provider

import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProviderPresetsTest {

    @Test
    fun `endpoints are valid once the workspace is filled in`() {
        ProviderPresets.all.forEach { preset ->
            val templated = preset.endpoint.contains("{WorkspaceId}")
            assertEquals(preset.id, !templated, Endpoints.isValid(preset.endpoint))
            assertTrue(preset.id, Endpoints.isValid(preset.endpoint.replace("{WorkspaceId}", "ws-1")))
        }
    }

    @Test
    fun `endpoint validation`() {
        assertTrue(Endpoints.isValid(" http://192.168.1.5:11434/v1/systemone "))
        assertFalse(Endpoints.isValid(""))
        assertFalse(Endpoints.isValid("api.typesafe.ai/v1/systemone"))
        assertFalse(Endpoints.isValid("ftp://host/x"))
        assertFalse(Endpoints.isValid("https:///v1"))
    }

    @Test
    fun `models urls resolve against the endpoint`() {
        assertEquals(
            "http://localhost:11434/api/tags",
            ProviderPresets.Ollama.endpoint.toHttpUrl().resolve(ProviderPresets.Ollama.modelsUrl!!).toString(),
        )
        assertEquals(
            "https://openrouter.ai/api/v1/models?output_modalities=decisions",
            ProviderPresets.OpenRouter.endpoint.toHttpUrl().resolve(ProviderPresets.OpenRouter.modelsUrl!!).toString(),
        )
    }

    @Test
    fun `only local ollama works without a key`() {
        assertEquals(listOf(ProviderPresets.Ollama), ProviderPresets.all.filterNot { it.apiKeyRequired })
    }

    @Test
    fun `openai protocol presets`() {
        assertEquals(
            listOf(ProviderPresets.OpenAi, ProviderPresets.VercelDecisions),
            ProviderPresets.all.filter { it.protocol == DecisionProtocol.OpenAiDecisions },
        )
    }

    @Test
    fun `new provider copies the preset`() {
        val provider = ProviderPresets.Perplexity.newProvider("id")

        assertEquals("Perplexity", provider.name)
        assertEquals("https://api.perplexity.ai/v1/decisions", provider.endpoint)
        assertEquals("pplx-decider-v1.1-27b", provider.model)
        assertEquals(ProviderPresets.Perplexity, provider.preset)
        assertTrue(provider.requiresApiKey)
    }

    @Test
    fun `custom providers do not require a key`() {
        val custom = ProviderConfig("id", "n", DecisionProtocol.SystemOne, "https://x", "", "m")
        assertNull(custom.preset)
        assertFalse(custom.requiresApiKey)
    }

    @Test
    fun `matching is exact`() {
        assertEquals(
            ProviderPresets.TypeSafe,
            ProviderPresets.matching(DecisionProtocol.SystemOne, " https://api.typesafe.ai/v1/systemone "),
        )
        assertNull(ProviderPresets.matching(DecisionProtocol.OpenAiDecisions, ProviderPresets.TypeSafe.endpoint))
        assertNull(ProviderPresets.matching(DecisionProtocol.SystemOne, "https://api.typesafe.ai/v1/systemone/"))
    }
}
