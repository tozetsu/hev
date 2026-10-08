package ai.hev.app.data.remote.http

import ai.hev.app.domain.decision.DecisionError
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * JSON over HTTP with bearer auth. URLs are used exactly as configured.
 * Non-success statuses become [DecisionError]s; transient ones are retried once.
 */
class HttpTransport(
    private val client: OkHttpClient = defaultClient(),
    private val retryPolicy: RetryPolicy = RetryPolicy(),
    private val io: CoroutineDispatcher = Dispatchers.IO,
) {
    suspend fun post(url: String, apiKey: String, json: String): String =
        send(request(url, apiKey).post(json.toRequestBody(JSON_MEDIA_TYPE)).build())

    suspend fun get(url: String, apiKey: String): String =
        send(request(url, apiKey).get().build())

    private suspend fun send(request: Request): String = try {
        execute(request)
    } catch (e: DecisionError.Transient) {
        delay(retryPolicy.delayFor(e))
        execute(request)
    }

    private suspend fun execute(request: Request): String = withContext(io) {
        try {
            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    throw DecisionError.forStatus(
                        status = response.code,
                        detail = ErrorBody.detail(body, fallback = response.message),
                        retryAfterSeconds = RetryPolicy.parseRetryAfter(response.header("Retry-After")),
                    )
                }
                body
            }
        } catch (e: IOException) {
            throw DecisionError.Network(e)
        }
    }

    private fun request(url: String, apiKey: String): Request.Builder = Request.Builder()
        .url(url.trim())
        .header("Accept", "application/json")
        .apply { if (apiKey.isNotBlank()) header("Authorization", "Bearer ${apiKey.trim()}") }

    companion object {
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }
}
