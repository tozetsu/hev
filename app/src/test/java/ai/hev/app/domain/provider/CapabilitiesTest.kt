package ai.hev.app.domain.provider

import ai.hev.app.domain.decision.DecisionDraft
import ai.hev.app.domain.decision.DecisionItems
import ai.hev.app.domain.decision.DecisionKind
import ai.hev.app.domain.decision.DraftIssue
import ai.hev.app.domain.decision.ModelCapabilities
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CapabilitiesTest {

    private fun capabilities(preset: ProviderPreset) = preset.newProvider("id").capabilities

    @Test
    fun `documented vendor limits`() {
        assertEquals(2..52, capabilities(ProviderPresets.DeepInfra).choiceOptions)
        assertEquals(2..10, capabilities(ProviderPresets.DeepInfra).scoreLevels)
        assertEquals(2..26, capabilities(ProviderPresets.Ollama).choiceOptions)
        assertEquals(2..26, capabilities(ProviderPresets.Ollama).scoreLevels)
        assertEquals(2..255, capabilities(ProviderPresets.AlibabaBeijing).scoreLevels)
        assertEquals(2..255, capabilities(ProviderPresets.AlibabaSingapore).scoreLevels)
    }

    @Test
    fun `typesafe and openai limits match the lenient defaults`() {
        assertEquals(ModelCapabilities.Lenient, capabilities(ProviderPresets.TypeSafe))
        assertEquals(ModelCapabilities.Lenient, capabilities(ProviderPresets.OpenAi))
        assertEquals(ModelCapabilities(DecisionKind.entries.toSet(), 2..255, 2..10), ModelCapabilities.Lenient)
    }

    @Test
    fun `custom providers are lenient`() {
        val custom = ProviderConfig("id", "n", DecisionProtocol.OpenAiDecisions, "https://x", "", "m")
        assertEquals(ModelCapabilities.Lenient, custom.capabilities)
    }

    @Test
    fun `every preset supports every kind`() {
        ProviderPresets.all.forEach { preset ->
            assertTrue(preset.id, DecisionKind.entries.all(preset.capabilities::supports))
        }
    }

    @Test
    fun `drafts are checked against the vendor limits`() {
        val labels = (1..53).map { "Option $it" }
        val draft = DecisionDraft(DecisionKind.Choice, "Which?", "", DecisionItems.choiceOptions(labels))

        assertEquals(DraftIssue.ItemCount(DecisionKind.Choice, 2..52), draft.validate(capabilities(ProviderPresets.DeepInfra)))
        assertNull(draft.validate(capabilities(ProviderPresets.TypeSafe)))
    }
}
