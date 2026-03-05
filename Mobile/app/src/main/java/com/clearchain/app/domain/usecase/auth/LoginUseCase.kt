package com.clearchain.app.domain.usecase.auth

import android.util.Log
import com.clearchain.app.data.local.database.ClearChainDatabase
import com.clearchain.app.data.remote.api.AuthApi
import com.clearchain.app.data.remote.dto.RegisterFCMTokenRequest
import com.clearchain.app.domain.model.AuthTokens
import com.clearchain.app.domain.model.Organization
import com.clearchain.app.domain.repository.AuthRepository
import javax.inject.Inject

class LoginUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val database: ClearChainDatabase,  // ✅ ADD
    private val authApi: AuthApi  // ✅ ADD
) {
    suspend operator fun invoke(
        email: String,
        password: String
    ): Result<Pair<Organization, AuthTokens>> {
        // Validate inputs
        if (email.isBlank()) {
            return Result.failure(Exception("Email cannot be empty"))
        }

        if (password.isBlank()) {
            return Result.failure(Exception("Password cannot be empty"))
        }

        if (!isValidEmail(email)) {
            return Result.failure(Exception("Invalid email format"))
        }

        // Login
        val result = authRepository.login(email.trim(), password)
        
        // ✅ ADD: Register FCM token after successful login
        if (result.isSuccess) {
            try {
                val fcmToken = database.fcmTokenDao().getToken()
                if (fcmToken != null) {
                    authApi.registerFCMToken(RegisterFCMTokenRequest(fcmToken))
                    Log.d("LoginUseCase", "🔔 FCM token registered with backend")
                } else {
                    Log.d("LoginUseCase", "⚠️ No FCM token found in database")
                }
            } catch (e: Exception) {
                Log.e("LoginUseCase", "Failed to register FCM token: ${e.message}")
                // Don't fail login if FCM registration fails
            }
        }

        return result
    }

    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
}