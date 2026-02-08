package com.clearchain.app.domain.usecase.auth

import com.clearchain.app.domain.model.Organization
import com.clearchain.app.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetCurrentUserUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    operator fun invoke(): Flow<Organization?> {
        return authRepository.getStoredUser()
    }

    suspend fun refresh(): Result<Organization> {
        return authRepository.getCurrentUser()
    }
}