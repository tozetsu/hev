package ai.hev.app.data.remote.http

import ai.hev.app.domain.decision.DecisionError
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.content.TextContent
import io.ktor.http.isSuccess
import io.ktor.http.withCharset
import io.ktor.utils.io.charsets.Charsets
import kotlinx.coroutines.delay
import kotlinx.io.IOException
import kotlin.time.Duration.Companion.seconds

/**
 * JSON over HTTP with bearer auth. URLs are used exactly as configured.
 * Non-success statuses become [DecisionError]s; transient ones are retried once.
 */
class HttpTransport(
    private val client: HttpClient = defaultClient(),
    private val retryPolicy: RetryPolicy = RetryPolicy(),
) {
    suspend fun post(url: String, apiKey: String, json: String): String = send(url, apiKey) {
        method = HttpMethod.Post
        setBody(TextContent(json, JSON_UTF8))
    }

    suspend fun get(url: String, apiKey: String): String = send(url, apiKey) {
        method = HttpMethod.Get
    }

    private suspend fun send(url: String, apiKey: String, configure: HttpRequestBuilder.() -> Unit): String = try {
        execute(url, apiKey, configure)
    } catch (e: DecisionError.Transient) {
        delay(retryPolicy.delayFor(e))
        execute(url, apiKey, configure)
    }

    private suspend fun execute(url: String, apiKey: String, configure: HttpRequestBuilder.() -> Unit): String {
        // The response body is read in full before request() returns, so I/O errors surface here.
        val response = try {
            client.request(url.trim()) {
                header(HttpHeaders.Accept, "application/json")
                if (apiKey.isNotBlank()) bearerAuth(apiKey.trim())
                configure()
            }
        } catch (e: IOException) {
            throw DecisionError.Network(e)
        }
        val body = response.bodyAsText()
        if (!response.status.isSuccess()) {
            throw DecisionError.forStatus(
                status = response.status.value,
                detail = ErrorBody.detail(body, fallback = response.status.description),
                retryAfterSeconds = RetryPolicy.parseRetryAfter(response.headers[HttpHeaders.RetryAfter]),
            )
        }
        return body
    }

    companion object {
        private val JSON_UTF8 = ContentType.Application.Json.withCharset(Charsets.UTF_8)

        /** The engine is whichever one the platform ships (OkHttp on Android and desktop). */
        private fun defaultClient(): HttpClient = HttpClient {
            install(HttpTimeout) {
                connectTimeoutMillis = 30.seconds.inWholeMilliseconds
                socketTimeoutMillis = 90.seconds.inWholeMilliseconds
            }
        }
    }
}
