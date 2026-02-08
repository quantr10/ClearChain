package com.clearchain.app.data.remote.interceptor

import com.clearchain.app.data.local.dao.AuthTokenDao
import com.clearchain.app.data.local.entity.AuthTokenEntity
import com.clearchain.app.data.remote.api.AuthApi
import com.clearchain.app.data.remote.dto.RefreshTokenRequest
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject

class TokenAuthenticator @Inject constructor(
    private val authApi: AuthApi,
    private val tokenDao: AuthTokenDao
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        // If already tried to refresh, don't retry
        if (response.request.header("Authorization") != null &&
            response.priorResponse?.code == 401) {
            return null
        }

        val currentToken = runBlocking {
            tokenDao.getTokens()
        } ?: return null

        // Try to refresh token
        return runBlocking {
            try {
                val refreshResponse = authApi.refreshToken(
                    RefreshTokenRequest(currentToken.refreshToken)
                )

                // Save new tokens
                val newTokens = AuthTokenEntity(
                    accessToken = refreshResponse.data.accessToken,
                    refreshToken = refreshResponse.data.refreshToken,
                    expiresIn = refreshResponse.data.expiresIn,
                    tokenType = refreshResponse.data.tokenType
                )
                tokenDao.saveTokens(newTokens)

                // Retry request with new token
                response.request.newBuilder()
                    .header("Authorization", "Bearer ${newTokens.accessToken}")
                    .build()

            } catch (e: Exception) {
                // Refresh failed, clear tokens
                tokenDao.clearTokens()
                null
            }
        }
    }
}