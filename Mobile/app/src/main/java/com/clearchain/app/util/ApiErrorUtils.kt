package com.clearchain.app.util

import org.json.JSONObject
import retrofit2.HttpException

/**
 * Reads the human-readable reason out of a failed API call.
 *
 * Every error the API returns carries a top-level "message", whether it comes from
 * ApiResponse.ErrorResponse, the error middleware, or an inline anonymous object.
 * Without this the UI falls back to Retrofit's own text ("HTTP 400 Bad Request"),
 * which hides the reason the server actually gave.
 */
object ApiErrorUtils {

    /** The server's message, or null when the failure carries none. */
    fun serverMessage(throwable: Throwable): String? = runCatching {
        val body = (throwable as? HttpException)?.response()?.errorBody()?.string()
        JSONObject(body ?: "").optString("message").takeIf { it.isNotBlank() }
    }.getOrNull()

    /** The server's message, falling back to [fallback] when there is none. */
    fun messageOr(throwable: Throwable, fallback: String): String =
        serverMessage(throwable) ?: throwable.message ?: fallback
}
