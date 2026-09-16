package com.clearchain.app.domain.usecase.auth

import android.util.Log
import com.clearchain.app.data.remote.signalr.SignalRService
import com.clearchain.app.domain.model.AuthTokens
import com.clearchain.app.domain.model.Organization
import com.clearchain.app.domain.repository.AuthRepository
import com.clearchain.app.domain.usecase.fcm.RegisterFCMTokenUseCase
import javax.inject.Inject

class VerifyEmailUseCase @Inject constructor(
    private val repository: AuthRepository,
    private val registerFCMTokenUseCase: RegisterFCMTokenUseCase,
    private val signalRService: SignalRService
) {
    suspend operator fun invoke(email: String, code: String): Result<Pair<Organization, AuthTokens>> {
        if (code.isBlank()) return Result.failure(Exception("Verification code is required"))
        if (code.length != 6) return Result.failure(Exception("Enter the 6-digit code from your email"))

        val result = repository.verifyEmail(email, code)

        // Verification is where a new account's session actually begins, so it needs the same
        // push registration and real-time handshake login does — otherwise a user who never
        // signs in again stays unreachable.
        if (result.isSuccess) {
            registerFCMTokenUseCase()
                .onFailure { Log.w(TAG, "Push registration failed after verification: ${it.message}") }

            runCatching { signalRService.reconnect() }
                .onFailure { Log.w(TAG, "Real-time connect failed after verification: ${it.message}") }
        }

        return result
    }

    private companion object {
        const val TAG = "VerifyEmailUseCase"
    }
}
