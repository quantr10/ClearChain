package com.clearchain.app.domain.usecase.auth

import com.clearchain.app.domain.repository.AuthRepository
import javax.inject.Inject

class ResetPasswordUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(email: String, code: String, newPassword: String): Result<Unit> {
        return repository.resetPassword(email.trim(), code, newPassword)
    }
}
