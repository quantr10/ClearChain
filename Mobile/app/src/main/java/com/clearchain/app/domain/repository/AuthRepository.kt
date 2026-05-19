package com.clearchain.app.domain.repository

import com.clearchain.app.domain.model.AuthTokens
import com.clearchain.app.domain.model.Organization
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    suspend fun register(
        name: String,
        type: String,
        email: String,
        password: String,
        fcmToken: String? = null
    ): Result<String> // returns email for verification screen

    suspend fun verifyEmail(email: String, code: String): Result<Pair<Organization, AuthTokens>>
    suspend fun resendVerification(email: String): Result<Unit>

    suspend fun login(email: String, password: String): Result<Pair<Organization, AuthTokens>>
    suspend fun logout(): Result<Unit>
    suspend fun refreshToken(refreshToken: String): Result<Pair<Organization, AuthTokens>>
    suspend fun changePassword(currentPassword: String, newPassword: String): Result<Unit>
    suspend fun getCurrentUser(): Flow<Organization?>
    suspend fun isLoggedIn(): Flow<Boolean>
}