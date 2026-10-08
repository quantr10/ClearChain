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

    /** Emails a reset code; succeeds whether or not the address has an account. */
    suspend fun requestPasswordReset(email: String): Result<Unit>
    suspend fun resetPassword(email: String, code: String, newPassword: String): Result<Unit>

    suspend fun login(email: String, password: String): Result<Pair<Organization, AuthTokens>>
    suspend fun logout(): Result<Unit>
    suspend fun changePassword(currentPassword: String, newPassword: String): Result<Unit>
    suspend fun getCurrentUser(): Flow<Organization?>

    /** Re-fetch the signed-in org from GET /auth/me and refresh the local cache. */
    suspend fun refreshCurrentUser(): Result<Organization>
}
