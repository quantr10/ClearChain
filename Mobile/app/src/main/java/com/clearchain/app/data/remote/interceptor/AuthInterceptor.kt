package com.clearchain.app.data.remote.interceptor

import com.clearchain.app.data.local.dao.AuthTokenDao
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class AuthInterceptor @Inject constructor(
    private val tokenDao: AuthTokenDao
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        // Get token from database
        val token = runBlocking {
            tokenDao.getTokens()?.accessToken
        }

        // If token exists, add Authorization header
        val newRequest = if (token != null) {
            request.newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        } else {
            request
        }

        return chain.proceed(newRequest)
    }
}