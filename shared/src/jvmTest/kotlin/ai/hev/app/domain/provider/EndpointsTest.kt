package ai.hev.app.domain.provider

import ai.hev.app.testing.Vendors
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EndpointsTest {

    @Test
    fun `documented endpoints are valid`() {
        Vendors.all.forEach { assertTrue(it.id, Endpoints.isValid(it.endpoint)) }
    }

    @Test
    fun `endpoint validation`() {
        assertTrue(Endpoints.isValid(" http://192.168.1.5:11434/v1/systemone "))
        assertFalse(Endpoints.isValid(""))
        assertFalse(Endpoints.isValid("api.typesafe.ai/v1/systemone"))
        assertFalse(Endpoints.isValid("ftp://host/x"))
        assertFalse(Endpoints.isValid("https:///v1"))
        assertFalse(Endpoints.isValid("https://{WorkspaceId}.cn-beijing.maas.aliyuncs.com/v1/systemone"))
    }

    @Test
    fun `rejects characters outside rfc 3986`() {
        assertFalse(Endpoints.isValid("https://api.example/v1/{model}"))
        assertFalse(Endpoints.isValid("https://api example/v1"))
        assertFalse(Endpoints.isValid("https://-bad.example/v1"))
    }

    /** Expected values are what OkHttp's HttpUrl.resolve, a browser-grade resolver, returns. */
    @Test
    fun `resolves references like a browser`() {
        val cases = mapOf(
            ("http://192.168.1.5:11434/v1/systemone" to "/api/tags") to "http://192.168.1.5:11434/api/tags",
            ("http://192.168.1.5:11434/v1/systemone" to "models") to "http://192.168.1.5:11434/v1/models",
            ("http://192.168.1.5:11434/v1/systemone" to "../models") to "http://192.168.1.5:11434/models",
            ("http://192.168.1.5:11434/v1/systemone" to "?q=2") to "http://192.168.1.5:11434/v1/systemone?q=2",
            ("http://192.168.1.5:11434/v1/systemone" to "//b.example/m") to "http://b.example/m",
            ("https://a.example/x/y/decide?k=1#f" to "./m?x=1") to "https://a.example/x/y/m?x=1",
            ("https://a.example/x/y/decide?k=1#f" to "#frag") to "https://a.example/x/y/decide?k=1#frag",
            ("https://a.example/x/y/decide?k=1#f" to "../models") to "https://a.example/x/models",
            ("https://a.example" to "?q=2") to "https://a.example/?q=2",
            ("https://a.example" to "../models") to "https://a.example/models",
            ("https://a.example" to "#frag") to "https://a.example/#frag",
            ("https://a.example/v1/" to "./m?x=1") to "https://a.example/v1/m?x=1",
            ("https://a.example/v1/" to "https://c.example/z") to "https://c.example/z",
        )
        cases.forEach { (input, expected) ->
            assertEquals("$input", expected, Endpoints.resolve(input.first, input.second))
        }
    }

    @Test
    fun `unusable bases and non-http targets resolve to nothing`() {
        assertNull(Endpoints.resolve("not a url", "/api/tags"))
        assertNull(Endpoints.resolve("https://a.example/v1", "ftp://b.example/models"))
    }

    @Test
    fun `authority is the lowercased host and explicit port`() {
        assertEquals(Endpoints.Authority("api.typesafe.ai", null), Endpoints.authority(" https://API.TypeSafe.ai/v1/systemone "))
        assertEquals(Endpoints.Authority("localhost", 11434), Endpoints.authority("http://u:p@localhost:11434/v1"))
        assertEquals(Endpoints.Authority("[::1]", 8080), Endpoints.authority("http://[::1]:8080"))
        assertEquals(Endpoints.Authority("[::1]", null), Endpoints.authority("http://[::1]/v1"))
        assertEquals(Endpoints.Authority("example.com", null), Endpoints.authority("https://example.com.:/v1"))
        assertNull(Endpoints.authority("not a url"))
    }
}
