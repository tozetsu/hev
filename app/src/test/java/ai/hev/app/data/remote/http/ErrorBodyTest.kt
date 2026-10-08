package ai.hev.app.data.remote.http

import ai.hev.app.testing.Fixtures
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** One case per error body shape seen in provider documentation. */
class ErrorBodyTest {

    @Test
    fun `openai style error object`() {
        assertEquals(
            "Image input isn't supported on AI Gateway's Decisions API yet. Send text input.",
            ErrorBody.message(Fixtures.read("vercel/decisions_error.json")),
        )
    }

    @Test
    fun `perplexity error object`() {
        assertEquals(
            "Noul question must have criteria or instructions",
            ErrorBody.message(Fixtures.read("perplexity/error_noul.json")),
        )
        assertEquals(
            true,
            ErrorBody.message(Fixtures.read("perplexity/error_invalid_model.json"))?.startsWith("Invalid model"),
        )
    }

    @Test
    fun `ollama error string`() {
        assertEquals(
            "request body must not exceed 64 KiB without images",
            ErrorBody.message(Fixtures.read("ollama/error_too_large.json")),
        )
    }

    @Test
    fun `typesafe message with error type`() {
        assertEquals(
            "questions.refund.type: expected one of 'noul', 'choice', 'score'",
            ErrorBody.message(Fixtures.read("vercel/typesafe_error.json")),
        )
    }

    /** Shaped after DeepInfra's published OpenAPI schema (HTTPValidationError). */
    @Test
    fun `fastapi validation detail list`() {
        assertEquals(
            "body.questions.decision.criteria: Dictionary should have at most 52 items after validation, not 60",
            ErrorBody.message(Fixtures.read("deepinfra/error_validation.json")),
        )
    }

    @Test
    fun `fastapi detail string`() {
        assertEquals("unknown model", ErrorBody.message("""{"detail": "unknown model"}"""))
    }

    @Test
    fun `unrecognised bodies fall back to the raw text`() {
        assertNull(ErrorBody.message("<html>504 Gateway Timeout</html>"))
        assertEquals("<html>504 Gateway Timeout</html>", ErrorBody.detail("<html>504 Gateway Timeout</html>\n", "x"))
        assertEquals("Not Found", ErrorBody.detail("", "Not Found"))
        assertNull(ErrorBody.message("""{"error": {"code": 500}}"""))
    }
}
