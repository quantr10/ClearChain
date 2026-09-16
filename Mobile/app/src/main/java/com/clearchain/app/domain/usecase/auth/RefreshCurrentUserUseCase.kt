package com.clearchain.app.domain.usecase.auth

import com.clearchain.app.domain.model.Organization
import com.clearchain.app.domain.repository.AuthRepository
import javax.inject.Inject

/**
 * Re-fetches the signed-in organization from the server (GET /auth/me) and updates
 * the local cache. Used to pick up an admin verification decision without forcing
 * the user to log out and back in.
 */
class RefreshCurrentUserUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(): Result<Organization> = authRepository.refreshCurrentUser()
}
