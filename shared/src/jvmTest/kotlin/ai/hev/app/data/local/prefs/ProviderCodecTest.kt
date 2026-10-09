package ai.hev.app.data.local.prefs

import ai.hev.app.domain.provider.DecisionProtocol
import ai.hev.app.domain.provider.ProviderConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProviderCodecTest {

    /** Exactly what version 1.0.0 wrote before providers had a protocol. */
    private val unversionedJson = """
        [
          {"id":"a","name":"TypeSafe","baseUrl":"https://api.typesafe.ai/v1/systemone","apiKey":"sk-1","model":"jev-latest"},
          {"id":"b","name":"Mine","baseUrl":"https://example.com/v1/systemone","apiKey":"","model":"m"}
        ]
    """.trimIndent()

    /** What versions with presets wrote: a preset id and models URL per provider. */
    private val presetJson = """
        [
          {"id":"a","name":"Ollama","baseUrl":"http://localhost:11434/v1/systemone","apiKey":"","model":"clef",
           "protocol":"system_one","presetId":"ollama","modelsUrl":"/api/tags"},
          {"id":"b","name":"OpenAI","baseUrl":"https://api.openai.com/v1/decisions","apiKey":"sk-2","model":"gpt-6-luna",
           "protocol":"openai_decisions","presetId":"gone"}
        ]
    """.trimIndent()

    @Test
    fun `rows without a protocol read as system one and keep every field`() {
        assertEquals(
            listOf(
                ProviderConfig("a", "TypeSafe", DecisionProtocol.SystemOne, "https://api.typesafe.ai/v1/systemone", "sk-1", "jev-latest"),
                ProviderConfig("b", "Mine", DecisionProtocol.SystemOne, "https://example.com/v1/systemone", "", "m"),
            ),
            ProviderCodec.decode(unversionedJson),
        )
    }

    @Test
    fun `rows with preset fields keep the provider and drop the preset`() {
        assertEquals(
            listOf(
                ProviderConfig("a", "Ollama", DecisionProtocol.SystemOne, "http://localhost:11434/v1/systemone", "", "clef"),
                ProviderConfig("b", "OpenAI", DecisionProtocol.OpenAiDecisions, "https://api.openai.com/v1/decisions", "sk-2", "gpt-6-luna"),
            ),
            ProviderCodec.decode(presetJson),
        )
    }

    @Test
    fun `round trips every protocol`() {
        val providers = DecisionProtocol.entries.mapIndexed { i, protocol ->
            ProviderConfig("$i", "P$i", protocol, "https://gateway.example/v1/$i", "k$i", "m$i")
        }

        assertEquals(providers, ProviderCodec.decode(ProviderCodec.encode(providers)))
    }

    @Test
    fun `keeps the legacy endpoint key and writes no preset`() {
        val encoded = ProviderCodec.encode(
            listOf(ProviderConfig("1", "T", DecisionProtocol.SystemOne, "https://api.typesafe.ai/v1/systemone", "", "jev-latest")),
        )

        assertTrue(encoded.contains("\"baseUrl\":\"https://api.typesafe.ai/v1/systemone\""))
        assertTrue(encoded.contains("\"protocol\":\"system_one\""))
        assertFalse(encoded.contains("preset"))
    }

    @Test
    fun `unreadable data yields no providers`() {
        assertEquals(emptyList<ProviderConfig>(), ProviderCodec.decode("not json"))
        assertEquals(emptyList<ProviderConfig>(), ProviderCodec.decode("""[{"id":"a"}]"""))
    }
}
