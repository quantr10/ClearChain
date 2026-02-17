package com.clearchain.app.domain.usecase.auth

import com.clearchain.app.domain.repository.AuthRepository
import javax.inject.Inject

class ChangePasswordUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(
        currentPassword: String,
        newPassword: String
    ): Result<Unit> {
        if (currentPassword.isBlank()) {
            return Result.failure(Exception("Current password is required"))
        }
        if (newPassword.isBlank()) {
            return Result.failure(Exception("New password is required"))
        }
        if (newPassword.length < 8) {
            return Result.failure(Exception("New password must be at least 8 characters"))
        }
        if (!newPassword.any { it.isUpperCase() } ||
            !newPassword.any { it.isLowerCase() } ||
            !newPassword.any { it.isDigit() }) {
            return Result.failure(Exception("Password must contain uppercase, lowercase, and number"))
        }
        if (currentPassword == newPassword) {
            return Result.failure(Exception("New password must be different from current password"))
        }

        return authRepository.changePassword(currentPassword, newPassword)
    }
}