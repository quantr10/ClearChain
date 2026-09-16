package com.clearchain.app.domain.usecase.fcm

import android.util.Log
import com.clearchain.app.data.local.database.ClearChainDatabase
import com.clearchain.app.data.remote.api.AuthApi
import com.clearchain.app.data.remote.dto.RegisterFCMTokenRequest
import javax.inject.Inject

/**
 * Detaches this device from the signed-in account at logout.
 *
 * Must run *before* the auth tokens are cleared — the endpoint is authenticated, and it is the
 * account being left that we need to identify. Skipping it leaves the server pushing the
 * previous user's notifications to a device someone else may now be signed into.
 */
class UnregisterFCMTokenUseCase @Inject constructor(
    private val authApi: AuthApi,
    private val database: ClearChainDatabase
) {
    suspend operator fun invoke(): Result<Unit> {
        return try {
            val token = database.fcmTokenDao().getToken()
            if (token == null) {
                Log.d(TAG, "No device token on file — nothing to unregister")
                return Result.success(Unit)
            }

            authApi.unregisterFCMToken(RegisterFCMTokenRequest(fcmToken = token))
            Log.d(TAG, "🔕 Device unregistered from push")
            Result.success(Unit)
        } catch (e: Exception) {
            // Never fatal: logout has to complete even if the server is unreachable. The token
            // is pruned server-side once FCM reports it unregistered, or by the weekly sweep.
            Log.w(TAG, "Could not unregister device: ${e.message}")
            Result.failure(e)
        }
    }

    private companion object {
        const val TAG = "UnregisterFCMToken"
    }
}
