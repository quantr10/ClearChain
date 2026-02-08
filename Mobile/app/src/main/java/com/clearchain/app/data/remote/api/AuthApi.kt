package com.clearchain.app.data.remote.api

import com.clearchain.app.data.remote.dto.*
import retrofit2.http.*

interface AuthApi {

    @POST("auth/register")
    suspend fun register(
        @Body request: RegisterRequest
    ): AuthResponse

    @POST("auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): AuthResponse

    @POST("auth/refresh")
    suspend fun refreshToken(
        @Body request: RefreshTokenRequest
    ): AuthResponse

    @POST("auth/logout")
    suspend fun logout(
        @Body request: RefreshTokenRequest
    ): Unit

    @GET("auth/me")
    suspend fun getCurrentUser(): AuthResponse

    companion object {
        const val BASE_URL = "http://10.0.2.2:5000/api/"
    }
}