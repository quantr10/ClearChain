package com.clearchain.app.data.remote.interceptor

import android.util.Log
import com.clearchain.app.data.local.SessionManager
import com.clearchain.app.data.local.dao.AuthTokenDao
import com.clearchain.app.data.local.dao.UserDao
import com.clearchain.app.data.local.entity.AuthTokenEntity
import com.clearchain.app.data.remote.dto.AuthData
import com.clearchain.app.data.remote.dto.AuthResponse
import com.clearchain.app.data.remote.dto.RefreshTokenRequest
import com.clearchain.app.util.Constants
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Transparently refreshes an expired access token on a 401 and retries the original request
 * once. Access tokens expire after JWT_EXPIRY_MINUTES (60 min server-side) and nothing else in
 * the app renewed them automatically before this — AuthInterceptor just attaches whatever
 * token is cached, and RetryInterceptor only retries 429/5xx. Any request made after being
 * idle for over an hour used to fail outright with a raw 401.
 *
 * Uses its own bare OkHttpClient for the refresh call itself, separate from the app's main
 * client this Authenticator is attached to — calling /auth/refresh through the main client
 * would recursively trigger authenticate() again on that call's own failure.
 */
class TokenAuthenticator @Inject constructor(
    private val authTokenDao: AuthTokenDao,
    private val userDao: UserDao,
    private val json: Json,
    private val sessionManager: SessionManager
) : Authenticator {

    private val refreshClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    override fun authenticate(route: Route?, response: Response): Request? {
        // Never try to "refresh" a failed call to the refresh endpoint itself, and give up
        // after one retry so a token that's still bad after refreshing can't loop forever.
        if (response.request.url.encodedPath.contains("auth/refresh")) return null
        if (responseCount(response) >= 2) return null

        // Serialize concurrent 401s (e.g. several requests in flight at once) so only one of
        // them actually calls /auth/refresh; the rest just pick up the token it produced.
        synchronized(this) {
            val current = runBlocking { authTokenDao.getTokens() } ?: return null
            val failedAccessToken = response.request.header("Authorization")?.removePrefix("Bearer ")

            // Another thread already refreshed while this one was waiting for the lock.
            if (failedAccessToken != null && current.accessToken != failedAccessToken) {
                return response.request.newBuilder()
                    .header("Authorization", "Bearer ${current.accessToken}")
                    .build()
            }

            val refreshed = refreshTokensSync(current.refreshToken)
            if (refreshed == null) {
                // Refresh token is dead too — clear the local session instead of retrying
                // forever, and tell the UI layer so it can bounce back to Login instead of
                // leaving the user stuck wherever this 401 happened to fire.
                //
                // Clearing the cached org too (not just the tokens) matters: without it,
                // GetCurrentUserUseCase would keep returning the stale cached profile after
                // this point, which could make Splash/Login route straight into a Dashboard
                // using data with no valid token behind it — the very next API call there
                // would 401 again, but with no refresh token left to even attempt recovery.
                runBlocking {
                    authTokenDao.clearTokens()
                    userDao.clearUsers()
                }
                sessionManager.notifySessionExpired()
                return null
            }

            runBlocking {
                authTokenDao.saveTokens(
                    AuthTokenEntity(
                        accessToken = refreshed.accessToken,
                        refreshToken = refreshed.refreshToken,
                        expiresIn = refreshed.expiresIn,
                        tokenType = refreshed.tokenType
                    )
                )
            }

            return response.request.newBuilder()
                .header("Authorization", "Bearer ${refreshed.accessToken}")
                .build()
        }
    }

    private fun refreshTokensSync(refreshToken: String): AuthData? {
        return try {
            val bodyJson = json.encodeToString(RefreshTokenRequest(refreshToken))
            val request = Request.Builder()
                .url("${Constants.BASE_URL}auth/refresh")
                .post(bodyJson.toRequestBody("application/json".toMediaType()))
                .build()

            refreshClient.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) return null
                val raw = resp.body?.string() ?: return null
                json.decodeFromString<AuthResponse>(raw).data
            }
        } catch (e: Exception) {
            Log.e(TAG, "Token refresh failed", e)
            null
        }
    }

    /** How many times this logical request has already been attempted (1 = first failure). */
    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }

    private companion object {
        const val TAG = "TokenAuthenticator"
    }
}
