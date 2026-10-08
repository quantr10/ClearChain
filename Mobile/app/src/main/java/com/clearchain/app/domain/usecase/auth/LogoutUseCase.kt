package com.clearchain.app.domain.usecase.auth

import android.util.Log
import com.clearchain.app.data.local.database.ClearChainDatabase
import com.clearchain.app.data.remote.signalr.SignalRService
import com.clearchain.app.domain.repository.AuthRepository
import javax.inject.Inject

class LogoutUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val signalRService: SignalRService,
    private val database: ClearChainDatabase
) {
    suspend operator fun invoke(): Result<Unit> {
        // The logout call sends this device's FCM token too, and the server drops it along with
        // the session, so the next account signed in here doesn't inherit our push.
        val result = authRepository.logout()

        runCatching { signalRService.disconnect() }
            .onFailure { Log.w(TAG, "Failed to close real-time connection: ${it.message}") }

        if (result.isSuccess) {
            try {
                database.fcmTokenDao().clearToken()
                database.notificationDao().clearAll()
                Log.d(TAG, "🔔 Cleared device token and notification inbox")
            } catch (e: Exception) {
                // Never fail a logout over local cleanup — the session is already gone.
                Log.e(TAG, "Failed to clear local push state: ${e.message}")
            }
        }

        return result
    }

    private companion object {
        const val TAG = "LogoutUseCase"
    }
}
