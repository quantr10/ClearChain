package com.clearchain.app.domain.usecase.auth

import com.clearchain.app.domain.repository.AuthRepository
import javax.inject.Inject

class IsLoggedInUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(): Boolean {
        return authRepository.isLoggedIn()
    }
}