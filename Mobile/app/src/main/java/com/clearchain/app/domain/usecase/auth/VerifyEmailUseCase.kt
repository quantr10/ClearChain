package com.clearchain.app.domain.usecase.auth

import com.clearchain.app.domain.model.AuthTokens
import com.clearchain.app.domain.model.Organization
import com.clearchain.app.domain.repository.AuthRepository
import javax.inject.Inject

class VerifyEmailUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(email: String, code: String): Result<Pair<Organization, AuthTokens>> {
        if (code.isBlank()) return Result.failure(Exception("Verification code is required"))
        if (code.length != 6) return Result.failure(Exception("Enter the 6-digit code from your email"))
        return repository.verifyEmail(email, code)
    }
}
