package ai.hev.app.testing

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals

/** Structural JSON comparison: key order and whitespace do not matter. */
fun assertJsonEquals(expected: String, actual: String) {
    assertEquals(Json.parseToJsonElement(expected), Json.parseToJsonElement(actual))
}
