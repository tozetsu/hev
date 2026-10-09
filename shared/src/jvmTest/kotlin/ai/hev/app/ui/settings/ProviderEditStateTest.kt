package ai.hev.app.ui.settings

import ai.hev.app.domain.provider.DecisionProtocol
import ai.hev.app.domain.provider.ProviderConfig
import ai.hev.app.domain.provider.ProviderIssue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProviderEditStateTest {
    private val blank = ProviderEditState(id = "id", isNew = true)
    private val filled = blank.copy(name = "Mine", endpoint = "https://x.example/v1/systemone", model = "m")

    @Test
    fun `a new provider starts empty on system one`() {
        assertEquals(DecisionProtocol.SystemOne, blank.protocol)
        assertEquals(listOf("", "", "", ""), listOf(blank.name, blank.endpoint, blank.apiKey, blank.model))
    }

    @Test
    fun `validation order`() {
        assertEquals(ProviderIssue.MissingName, blank.validate())
        assertEquals(ProviderIssue.InvalidEndpoint, filled.copy(endpoint = "https://{WorkspaceId}.example/v1").validate())
        assertEquals(ProviderIssue.MissingModel, filled.copy(model = " ").validate())
        assertNull(filled.validate())
    }

    @Test
    fun `the key is optional`() {
        assertNull(filled.copy(apiKey = "").validate())
    }

    @Test
    fun `saved config is trimmed`() {
        val config = filled.copy(
            name = " Mine ",
            protocol = DecisionProtocol.OpenAiDecisions,
            endpoint = " https://x.example/v1/decisions ",
            apiKey = " k ",
            model = " m ",
        ).toConfig()

        assertEquals(ProviderConfig("id", "Mine", DecisionProtocol.OpenAiDecisions, "https://x.example/v1/decisions", "k", "m"), config)
    }

    @Test
    fun `editing starts from the saved provider`() {
        val config = ProviderConfig("p", "OpenAI", DecisionProtocol.OpenAiDecisions, "https://api.openai.com/v1/decisions", "sk", "gpt-6-luna")
        val state = ProviderEditState.of(config)

        assertEquals(false, state.isNew)
        assertEquals(config, state.toConfig())
    }
}
