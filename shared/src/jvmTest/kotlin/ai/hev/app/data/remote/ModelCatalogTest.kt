package ai.hev.app.data.remote

import ai.hev.app.domain.decision.DecisionError
import ai.hev.app.testing.Fixtures
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ModelCatalogTest {

    /** TypeSafe documents the fields of `GET /v1/models` but publishes no example body. */
    @Test
    fun `reads typesafe model names`() {
        val raw = """
            {"models": [
              {"name": "jev-latest", "description": "Most recent stable release", "release_date": "2026-01-01"},
              {"name": "jev-preview", "description": "Most recent release", "release_date": "2026-01-01"}
            ]}
        """.trimIndent()

        assertEquals(listOf("jev-latest", "jev-preview"), ModelCatalog.parse(raw))
    }

    @Test
    fun `reads ollama tags`() {
        assertEquals(listOf("gemma4"), ModelCatalog.parse(Fixtures.read("ollama/tags.json")))
    }

    @Test
    fun `prefers ids over display names`() {
        assertEquals(listOf("openai/gpt-4"), ModelCatalog.parse(Fixtures.read("openrouter/models.json")))
    }

    @Test
    fun `skips unusable entries and duplicates`() {
        val raw = """{"data": [{"id": "a"}, {"id": ""}, "b", {"name": "c"}, {"id": "a"}, {"object": "model"}]}"""
        assertEquals(listOf("a", "c"), ModelCatalog.parse(raw))
    }

    @Test
    fun `other bodies are malformed`() {
        listOf("""{"object": "list"}""", "[]", "<html></html>").forEach { raw ->
            assertThrows(raw, DecisionError.MalformedResponse::class.java) { ModelCatalog.parse(raw) }
        }
    }

    @Test
    fun `models are listed next to the endpoint, then at ollama tags`() {
        assertEquals(
            listOf("https://api.typesafe.ai/v1/models", "https://api.typesafe.ai/api/tags"),
            ModelCatalog.locations(" https://api.typesafe.ai/v1/systemone "),
        )
        assertEquals(
            listOf("http://192.168.1.5:11434/v1/models", "http://192.168.1.5:11434/api/tags"),
            ModelCatalog.locations("http://192.168.1.5:11434/v1/systemone?x=1"),
        )
        assertEquals(emptyList<String>(), ModelCatalog.locations("not a url"))
    }

    @Test
    fun `fetches with auth from the first location that answers`() = runTest {
        val server = MockWebServer()
        server.enqueue(MockResponse(code = 404))
        server.enqueue(MockResponse(body = Fixtures.read("ollama/tags.json")))
        server.start()
        try {
            val models = ModelCatalog().fetch(server.url("/v1/systemone").toString(), "k")

            assertEquals(listOf("gemma4"), models)
            assertEquals("/v1/models", server.takeRequest().target)
            val tags = server.takeRequest()
            assertEquals("GET", tags.method)
            assertEquals("/api/tags", tags.target)
            assertEquals("Bearer k", tags.headers["Authorization"])
        } finally {
            server.close()
        }
    }

    @Test
    fun `no model list means none`() = runTest {
        val server = MockWebServer()
        repeat(2) { server.enqueue(MockResponse(body = "<html></html>")) }
        server.start()
        try {
            assertEquals(emptyList<String>(), ModelCatalog().fetch(server.url("/v1/systemone").toString(), ""))
            assertEquals(2, server.requestCount)
        } finally {
            server.close()
        }
        assertEquals(emptyList<String>(), ModelCatalog().fetch("", ""))
    }
}
