package com.clearchain.app.domain.usecase.auth

import android.util.Log
import com.clearchain.app.data.local.database.ClearChainDatabase
import com.clearchain.app.data.remote.signalr.SignalRService
import com.clearchain.app.domain.repository.AuthRepository
import com.clearchain.app.domain.usecase.fcm.UnregisterFCMTokenUseCase
import javax.inject.Inject

class LogoutUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val unregisterFCMTokenUseCase: UnregisterFCMTokenUseCase,
    private val signalRService: SignalRService,
    private val database: ClearChainDatabase
) {
    suspend operator fun invoke(): Result<Unit> {
        // Order matters: the unregister endpoint is authenticated, so it has to go out while the
        // session is still valid. Doing it after logout would silently 401 and leave this device
        // subscribed to the account being left.
        unregisterFCMTokenUseCase()

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
