package com.clearchain.app.domain.usecase.auth

import com.clearchain.app.domain.model.AuthTokens
import com.clearchain.app.domain.model.Organization
import com.clearchain.app.domain.repository.AuthRepository
import com.clearchain.app.util.ValidationUtils
import javax.inject.Inject

class RegisterUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    // ✅ UPDATED: Removed phone, address, location, hours parameters
    suspend operator fun invoke(
        name: String,
        type: String,
        email: String,
        password: String,
        fcmToken: String? = null
    ): Result<Pair<Organization, AuthTokens>> {
        
        // Validate inputs
        if (name.isBlank()) {
            return Result.failure(Exception("Name is required"))
        }
        
        if (name.length < 3) {
            return Result.failure(Exception("Name must be at least 3 characters"))
        }
        
        if (email.isBlank()) {
            return Result.failure(Exception("Email is required"))
        }
        
        if (!ValidationUtils.isValidEmail(email)) {
            return Result.failure(Exception("Invalid email format"))
        }
        
        if (password.isBlank()) {
            return Result.failure(Exception("Password is required"))
        }
        
        if (!ValidationUtils.isValidPassword(password)) {
            return Result.failure(Exception("Password must be at least 8 characters with uppercase, lowercase, and number"))
        }
        
        if (type !in listOf("grocery", "ngo", "admin")) {
            return Result.failure(Exception("Invalid organization type"))
        }
        
        // Call repository
        return repository.register(
            name = name,
            type = type,
            email = email,
            password = password,
            fcmToken = fcmToken
        )
    }
}