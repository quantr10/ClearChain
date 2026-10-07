package com.clearchain.app.util

import android.content.Context
import com.clearchain.app.R
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

    /**
     * A user-facing message when the failure is the system's fault (rate limit, server
     * error, no connection) rather than a problem with what the user typed — or null when
     * it is the latter. Forms use it to decide between a snackbar (system) and an error
     * under the field (input).
     */
    fun systemMessage(context: Context, raw: String): String? = when {
        raw.contains("429") || raw.contains("Too Many", ignoreCase = true) ->
            context.getString(R.string.error_too_many_attempts)
        raw.contains("500") || raw.contains("502") || raw.contains("503") ->
            context.getString(R.string.error_server)
        raw.contains("Unable to resolve host", ignoreCase = true) ||
            raw.contains("timeout", ignoreCase = true) ||
            raw.contains("connect", ignoreCase = true) ->
            context.getString(R.string.error_no_internet)
        else -> null
    }
}
