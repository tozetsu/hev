package ai.hev.app.domain.provider

import ai.hev.app.domain.decision.DecisionDraft
import ai.hev.app.domain.decision.DecisionItems
import ai.hev.app.domain.decision.DecisionKind
import ai.hev.app.domain.decision.DraftIssue
import ai.hev.app.domain.decision.ModelCapabilities
import ai.hev.app.testing.Vendor
import ai.hev.app.testing.Vendors
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CapabilitiesTest {

    private fun capabilities(endpoint: String) =
        ProviderConfig("id", "n", DecisionProtocol.SystemOne, endpoint, "", "m").capabilities

    private fun capabilities(vendor: Vendor) = capabilities(vendor.endpoint)

    @Test
    fun `documented vendor limits`() {
        assertEquals(2..52, capabilities(Vendors.DeepInfra).choiceOptions)
        assertEquals(2..10, capabilities(Vendors.DeepInfra).scoreLevels)
        assertEquals(2..26, capabilities(Vendors.Ollama).choiceOptions)
        assertEquals(2..26, capabilities(Vendors.Ollama).scoreLevels)
        assertEquals(2..255, capabilities(Vendors.AlibabaBeijing).scoreLevels)
        assertEquals(2..255, capabilities(Vendors.AlibabaSingapore).scoreLevels)
    }

    @Test
    fun `vendors without documented limits are lenient`() {
        listOf(Vendors.TypeSafe, Vendors.Perplexity, Vendors.OpenRouter, Vendors.Liquid, Vendors.OpenAi).forEach {
            assertEquals(it.id, ModelCapabilities.Lenient, capabilities(it))
        }
        assertEquals(ModelCapabilities(DecisionKind.entries.toSet(), 2..255, 2..10), ModelCapabilities.Lenient)
    }

    @Test
    fun `limits follow the host, not the path or model`() {
        assertEquals(2..52, capabilities("https://API.DeepInfra.com./other/path").choiceOptions)
        assertEquals(2..52, capabilities("https://user@api.deepinfra.com:443/v1/decisions").choiceOptions)
        assertEquals(ModelCapabilities.Lenient, capabilities("https://notdeepinfra.com/v1/decisions"))
        assertEquals(ModelCapabilities.Lenient, capabilities("https://deepinfra.com.example/v1/decisions"))
        assertEquals(ModelCapabilities.Lenient, capabilities("https://gateway.example/deepinfra.com/v1"))
    }

    @Test
    fun `ollama is recognised by its port on any host`() {
        assertEquals(2..26, capabilities("http://192.168.1.5:11434/v1/systemone").choiceOptions)
        assertEquals(2..26, capabilities("http://[::1]:11434/v1/systemone").choiceOptions)
        assertEquals(ModelCapabilities.Lenient, capabilities("http://localhost:8080/v1/systemone"))
        assertEquals(ModelCapabilities.Lenient, capabilities("http://[::1]/v1/systemone"))
    }

    @Test
    fun `unusable endpoints are lenient`() {
        assertEquals(ModelCapabilities.Lenient, capabilities(""))
        assertEquals(ModelCapabilities.Lenient, capabilities("https://{WorkspaceId}.cn-beijing.maas.aliyuncs.com/v1"))
    }

    @Test
    fun `every vendor supports every kind`() {
        Vendors.all.forEach { vendor ->
            assertTrue(vendor.id, DecisionKind.entries.all(capabilities(vendor)::supports))
        }
    }

    @Test
    fun `drafts are checked against the vendor limits`() {
        val labels = (1..53).map { "Option $it" }
        val draft = DecisionDraft(DecisionKind.Choice, "Which?", "", DecisionItems.choiceOptions(labels))

        assertEquals(DraftIssue.ItemCount(DecisionKind.Choice, 2..52), draft.validate(capabilities(Vendors.DeepInfra)))
        assertNull(draft.validate(capabilities(Vendors.TypeSafe)))
    }
}
