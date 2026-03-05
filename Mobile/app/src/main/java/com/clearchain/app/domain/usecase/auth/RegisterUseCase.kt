package com.clearchain.app.domain.usecase.auth

import android.util.Log
import com.clearchain.app.data.local.database.ClearChainDatabase
import com.clearchain.app.data.remote.api.AuthApi
import com.clearchain.app.data.remote.dto.RegisterFCMTokenRequest
import com.clearchain.app.domain.model.AuthTokens
import com.clearchain.app.domain.model.Organization
import com.clearchain.app.domain.repository.AuthRepository
import javax.inject.Inject

class RegisterUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val database: ClearChainDatabase,  // ✅ ADD
    private val authApi: AuthApi  // ✅ ADD
) {
    suspend operator fun invoke(
        name: String,
        type: String,
        email: String,
        password: String,
        phone: String,
        address: String,
        location: String,
        hours: String? = null
    ): Result<Pair<Organization, AuthTokens>> {

        // Validate inputs
        if (name.isBlank()) {
            return Result.failure(Exception("Name cannot be empty"))
        }

        if (name.length < 3) {
            return Result.failure(Exception("Name must be at least 3 characters"))
        }

        if (email.isBlank()) {
            return Result.failure(Exception("Email cannot be empty"))
        }

        if (!isValidEmail(email)) {
            return Result.failure(Exception("Invalid email format"))
        }

        if (password.isBlank()) {
            return Result.failure(Exception("Password cannot be empty"))
        }

        if (password.length < 8) {
            return Result.failure(Exception("Password must be at least 8 characters"))
        }

        if (!isValidPassword(password)) {
            return Result.failure(Exception("Password must contain uppercase, lowercase, and number"))
        }

        if (phone.isBlank()) {
            return Result.failure(Exception("Phone cannot be empty"))
        }

        if (address.isBlank()) {
            return Result.failure(Exception("Address cannot be empty"))
        }

        if (location.isBlank()) {
            return Result.failure(Exception("Location cannot be empty"))
        }

        if (type.lowercase() !in listOf("grocery", "ngo")) {
            return Result.failure(Exception("Type must be 'grocery' or 'ngo'"))
        }

        // Register
        val result = authRepository.register(
            name = name.trim(),
            type = type.lowercase(),
            email = email.trim(),
            password = password,
            phone = phone.trim(),
            address = address.trim(),
            location = location.trim(),
            hours = hours?.trim()
        )

        // ✅ ADD: Register FCM token after successful registration
        if (result.isSuccess) {
            try {
                val fcmToken = database.fcmTokenDao().getToken()
                if (fcmToken != null) {
                    authApi.registerFCMToken(RegisterFCMTokenRequest(fcmToken))
                    Log.d("RegisterUseCase", "🔔 FCM token registered with backend")
                } else {
                    Log.d("RegisterUseCase", "⚠️ No FCM token found in database")
                }
            } catch (e: Exception) {
                Log.e("RegisterUseCase", "Failed to register FCM token: ${e.message}")
                // Don't fail registration if FCM registration fails
            }
        }

        return result
    }

    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun isValidPassword(password: String): Boolean {
        val hasUppercase = password.any { it.isUpperCase() }
        val hasLowercase = password.any { it.isLowerCase() }
        val hasDigit = password.any { it.isDigit() }
        return hasUppercase && hasLowercase && hasDigit
    }
}