package com.clearchain.app.domain.usecase.auth

import com.clearchain.app.domain.model.AuthTokens
import com.clearchain.app.domain.model.Organization
import com.clearchain.app.domain.repository.AuthRepository
import javax.inject.Inject

class LoginUseCase @Inject constructor(
    private val authRepository: AuthRepository
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

        return authRepository.login(email.trim(), password)
    }

    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
}