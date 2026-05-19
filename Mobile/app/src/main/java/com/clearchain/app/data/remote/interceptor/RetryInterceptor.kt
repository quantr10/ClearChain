package com.clearchain.app.data.remote.interceptor

import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

/**
 * Retries requests on 429 (rate-limited) and 5xx (server error) responses
 * with exponential backoff: 1s → 2s → 4s (up to [maxRetries] attempts).
 *
 * The Retry-After header is respected when present on 429 responses.
 */
class RetryInterceptor @Inject constructor() : Interceptor {

    private val maxRetries = 3

    override fun intercept(chain: Interceptor.Chain): Response {
        var attempt = 0
        var response = chain.proceed(chain.request())

        while (shouldRetry(response) && attempt < maxRetries) {
            val waitMs = retryDelayMs(response, attempt)
            response.close()
            Thread.sleep(waitMs)
            attempt++
            response = chain.proceed(chain.request())
        }

        return response
    }

    private fun shouldRetry(response: Response): Boolean =
        response.code == 429 || response.code in 500..599

    private fun retryDelayMs(response: Response, attempt: Int): Long {
        // Honour Retry-After header (in seconds) when provided
        val retryAfter = response.header("Retry-After")?.toLongOrNull()
        if (retryAfter != null) return (retryAfter * 1000).coerceAtMost(30_000)

        // Exponential backoff: 1000ms, 2000ms, 4000ms
        return (1000L shl attempt).coerceAtMost(30_000)
    }
}
