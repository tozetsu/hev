package ai.hev.app.data.remote

import ai.hev.app.domain.decision.DecisionError
import ai.hev.app.testing.Fixtures
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
    fun `resolves models urls`() {
        val endpoint = "http://192.168.1.5:11434/v1/systemone"
        assertEquals("http://192.168.1.5:11434/api/tags", ModelCatalog.resolve("/api/tags", endpoint))
        assertEquals("https://x.example/v1/models", ModelCatalog.resolve(" https://x.example/v1/models ", endpoint))
        assertNull(ModelCatalog.resolve("", endpoint))
        assertNull(ModelCatalog.resolve("/api/tags", "not a url"))
    }

    @Test
    fun `fetches with auth from the resolved url`() = runTest {
        val server = MockWebServer()
        server.enqueue(MockResponse().setBody(Fixtures.read("ollama/tags.json")))
        server.start()
        try {
            val models = ModelCatalog().fetch("/api/tags", server.url("/v1/systemone").toString(), "k")

            assertEquals(listOf("gemma4"), models)
            val request = server.takeRequest()
            assertEquals("GET", request.method)
            assertEquals("/api/tags", request.path)
            assertEquals("Bearer k", request.getHeader("Authorization"))
        } finally {
            server.shutdown()
        }
    }
}
