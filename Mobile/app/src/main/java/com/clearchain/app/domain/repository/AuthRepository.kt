package com.clearchain.app.domain.repository

import com.clearchain.app.domain.model.AuthTokens
import com.clearchain.app.domain.model.Organization
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    // ✅ UPDATED: Simplified signature
    suspend fun register(
        name: String,
        type: String,
        email: String,
        password: String,
        fcmToken: String? = null
    ): Result<Pair<Organization, AuthTokens>>
    
    suspend fun login(email: String, password: String): Result<Pair<Organization, AuthTokens>>
    suspend fun logout(): Result<Unit>
    suspend fun refreshToken(refreshToken: String): Result<Pair<Organization, AuthTokens>>
    suspend fun changePassword(currentPassword: String, newPassword: String): Result<Unit>
    suspend fun getCurrentUser(): Flow<Organization?>
    suspend fun isLoggedIn(): Flow<Boolean>
}