package ai.hev.app.data.local.prefs

import ai.hev.app.domain.provider.DecisionProtocol
import ai.hev.app.domain.provider.ProviderConfig
import ai.hev.app.domain.provider.ProviderPresets
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProviderCodecTest {

    /** Exactly what version 1.0.0 wrote before providers had a protocol. */
    private val legacyJson = """
        [
          {"id":"a","name":"TypeSafe","baseUrl":"https://api.typesafe.ai/v1/systemone","apiKey":"sk-1","model":"jev-latest"},
          {"id":"b","name":"Mine","baseUrl":"https://example.com/v1/systemone","apiKey":"","model":"m"}
        ]
    """.trimIndent()

    @Test
    fun `legacy rows read as system one and keep every field`() {
        val (typesafe, custom) = ProviderCodec.decode(legacyJson)

        assertEquals(
            ProviderConfig(
                id = "a",
                name = "TypeSafe",
                protocol = DecisionProtocol.SystemOne,
                endpoint = "https://api.typesafe.ai/v1/systemone",
                apiKey = "sk-1",
                model = "jev-latest",
                presetId = ProviderPresets.TypeSafe.id,
                modelsUrl = ProviderPresets.TypeSafe.modelsUrl,
            ),
            typesafe,
        )
        assertEquals(
            ProviderConfig("b", "Mine", DecisionProtocol.SystemOne, "https://example.com/v1/systemone", "", "m"),
            custom,
        )
    }

    @Test
    fun `round trips every protocol`() {
        val providers = listOf(
            ProviderPresets.OpenAi.newProvider("1").copy(apiKey = "k"),
            ProviderPresets.Ollama.newProvider("2"),
            ProviderConfig(
                id = "3",
                name = "Custom",
                protocol = DecisionProtocol.OpenAiDecisions,
                endpoint = "https://gateway.example/v1/decisions",
                apiKey = "k3",
                model = "x",
                modelsUrl = "https://gateway.example/v1/models",
            ),
        )

        assertEquals(providers, ProviderCodec.decode(ProviderCodec.encode(providers)))
    }

    @Test
    fun `keeps the legacy endpoint key`() {
        val encoded = ProviderCodec.encode(listOf(ProviderPresets.TypeSafe.newProvider("1")))

        assertTrue(encoded.contains("\"baseUrl\":\"https://api.typesafe.ai/v1/systemone\""))
        assertTrue(encoded.contains("\"protocol\":\"system_one\""))
    }

    @Test
    fun `unknown preset becomes custom`() {
        val raw = """[{"id":"a","name":"n","baseUrl":"https://x/y","apiKey":"","model":"m",
            "protocol":"openai_decisions","presetId":"gone"}]"""

        val provider = ProviderCodec.decode(raw).single()

        assertEquals(null, provider.presetId)
        assertEquals(DecisionProtocol.OpenAiDecisions, provider.protocol)
    }

    @Test
    fun `unreadable data yields no providers`() {
        assertEquals(emptyList<ProviderConfig>(), ProviderCodec.decode("not json"))
        assertEquals(emptyList<ProviderConfig>(), ProviderCodec.decode("""[{"id":"a"}]"""))
    }
}
