package ai.hev.app.domain.provider

import java.net.URI

object Endpoints {
    /** An absolute http(s) URL with a host; unfilled `{placeholders}` are rejected. */
    fun isValid(url: String): Boolean {
        val uri = runCatching { URI(url.trim()) }.getOrNull() ?: return false
        return uri.scheme?.lowercase() in setOf("http", "https") && !uri.host.isNullOrEmpty()
    }
}
