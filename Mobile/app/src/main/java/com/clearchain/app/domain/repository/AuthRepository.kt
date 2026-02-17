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
        phone: String,
        address: String,
        location: String,
        hours: String? = null
    ): Result<Pair<Organization, AuthTokens>>

    suspend fun login(
        email: String,
        password: String
    ): Result<Pair<Organization, AuthTokens>>

    suspend fun logout(): Result<Unit>

    suspend fun refreshToken(): Result<AuthTokens>

    suspend fun getCurrentUser(): Result<Organization>

    fun getStoredUser(): Flow<Organization?>

    fun getStoredTokens(): Flow<AuthTokens?>

    suspend fun isLoggedIn(): Boolean

    suspend fun clearUserData()

    suspend fun changePassword(currentPassword: String, newPassword: String): Result<Unit>
}