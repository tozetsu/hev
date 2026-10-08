package ai.hev.app.ui.settings

import ai.hev.app.R
import ai.hev.app.domain.provider.DecisionProtocol
import ai.hev.app.domain.provider.ProviderPresets
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProviderEditStateTest {
    private val typesafe = ProviderEditState.of(ProviderPresets.TypeSafe.newProvider("id"), isNew = true)

    @Test
    fun `preset prefills every field and follows the name`() {
        val openAi = typesafe.copy(apiKey = "sk").withPreset(ProviderPresets.OpenAi)

        assertEquals("OpenAI", openAi.name)
        assertEquals(DecisionProtocol.OpenAiDecisions, openAi.protocol)
        assertEquals("https://api.openai.com/v1/decisions", openAi.endpoint)
        assertEquals("gpt-6-luna", openAi.model)
        assertEquals("", openAi.modelsUrl)
        assertEquals("sk", openAi.apiKey)
    }

    @Test
    fun `a custom name survives switching presets`() {
        val state = typesafe.copy(name = "Work").withPreset(ProviderPresets.Ollama)
        assertEquals("Work", state.name)
    }

    @Test
    fun `custom keeps the values to edit`() {
        val custom = typesafe.withPreset(ProviderPresets.OpenRouter).withPreset(null)

        assertEquals(true, custom.isCustom)
        assertEquals(ProviderPresets.OpenRouter.endpoint, custom.endpoint)
        assertEquals(ProviderPresets.OpenRouter.modelsUrl, custom.modelsUrl)
        assertNull(custom.toConfig().presetId)
    }

    @Test
    fun `validation order`() {
        assertEquals(R.string.error_name_required, typesafe.copy(name = " ").validate())
        assertEquals(R.string.error_endpoint_invalid, typesafe.withPreset(ProviderPresets.AlibabaBeijing).validate())
        assertEquals(R.string.error_api_key, typesafe.validate())
        assertEquals(R.string.error_model_required, typesafe.copy(apiKey = "k", model = "").validate())
        assertNull(typesafe.copy(apiKey = "k").validate())
    }

    @Test
    fun `key is optional for ollama and custom providers`() {
        assertNull(typesafe.withPreset(ProviderPresets.Ollama).validate())
        assertNull(typesafe.withPreset(null).validate())
    }

    @Test
    fun `saved config is trimmed and drops an empty models url`() {
        val config = typesafe.withPreset(null).copy(
            name = " Mine ",
            endpoint = " https://x.example/v1/systemone ",
            apiKey = " k ",
            modelsUrl = " ",
        ).toConfig()

        assertEquals("Mine", config.name)
        assertEquals("https://x.example/v1/systemone", config.endpoint)
        assertEquals("k", config.apiKey)
        assertNull(config.modelsUrl)
    }

    @Test
    fun `model options put fetched models first and keep suggestions`() {
        val state = typesafe.copy(fetchedModels = listOf("jev-1.13.0", "jev-latest"))

        assertEquals(listOf("jev-1.13.0", "jev-latest", "jev-preview"), state.modelOptions)
        assertEquals(ProviderPresets.Ollama.models, state.withPreset(ProviderPresets.Ollama).modelOptions)
        assertEquals(emptyList<String>(), state.withPreset(null).copy(fetchedModels = emptyList()).modelOptions)
    }
}
