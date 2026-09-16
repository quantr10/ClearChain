package com.clearchain.app.domain.usecase.auth

import com.clearchain.app.domain.model.Organization
import com.clearchain.app.domain.repository.AuthRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class GetCurrentUserUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(): Flow<Organization?> {
        return repository.getCurrentUser()
    }
}
