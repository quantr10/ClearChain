package com.clearchain.app.domain.usecase.auth

import android.util.Log
import com.clearchain.app.data.remote.signalr.SignalRService
import com.clearchain.app.domain.model.AuthTokens
import com.clearchain.app.domain.model.Organization
import com.clearchain.app.domain.repository.AuthRepository
import com.clearchain.app.domain.usecase.fcm.RegisterFCMTokenUseCase
import javax.inject.Inject

class LoginUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val registerFCMTokenUseCase: RegisterFCMTokenUseCase,
    private val signalRService: SignalRService
) {
    suspend operator fun invoke(
        email: String,
        password: String
    ): Result<Pair<Organization, AuthTokens>> {
        if (email.isBlank()) {
            return Result.failure(Exception("Email cannot be empty"))
        }

        if (password.isBlank()) {
            return Result.failure(Exception("Password cannot be empty"))
        }

        if (!isValidEmail(email)) {
            return Result.failure(Exception("Invalid email format"))
        }

        val result = authRepository.login(email.trim(), password)

        if (result.isSuccess) {
            // Ask Firebase for the token rather than reading the local cache. The cache is only
            // written by FCMService.onNewToken, which does not fire when Firebase hands back a
            // token it already had — after a reinstall that left Room empty, reading the cache
            // here found nothing and the device was never registered for push.
            registerFCMTokenUseCase()
                .onFailure { Log.w(TAG, "Push registration failed after login: ${it.message}") }

            // The previous session's connection carries the previous account's token, so it has
            // to be rebuilt before this user can receive anything.
            runCatching { signalRService.reconnect() }
                .onFailure { Log.w(TAG, "Real-time reconnect failed after login: ${it.message}") }
        }

        return result
    }

    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private companion object {
        const val TAG = "LoginUseCase"
    }
}
