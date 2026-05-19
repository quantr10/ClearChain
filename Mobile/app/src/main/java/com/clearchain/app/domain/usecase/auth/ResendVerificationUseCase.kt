package com.clearchain.app.domain.usecase.auth

import com.clearchain.app.domain.repository.AuthRepository
import javax.inject.Inject

class ResendVerificationUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(email: String): Result<Unit> {
        return repository.resendVerification(email)
    }
}
