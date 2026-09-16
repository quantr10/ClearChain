package com.clearchain.app.domain.usecase.fcm

import android.util.Log
import com.clearchain.app.data.local.database.ClearChainDatabase
import com.clearchain.app.data.local.entity.FCMTokenEntity
import com.clearchain.app.data.remote.api.AuthApi
import com.clearchain.app.data.remote.dto.RegisterFCMTokenRequest
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * Publishes this device's FCM token to the server — the one step that makes push notifications
 * possible at all.
 *
 * Run it on every launch and after every sign-in. A token obtained while signed out can't be
 * attributed to anyone, so it's held locally and sent as soon as a session exists; and
 * re-sending an unchanged token is what refreshes its server-side timestamp, keeping an active
 * device out of the stale-token sweep.
 */
class RegisterFCMTokenUseCase @Inject constructor(
    private val authApi: AuthApi,
    private val database: ClearChainDatabase
) {
    suspend operator fun invoke(): Result<Unit> {
        return try {
            val token = FirebaseMessaging.getInstance().token.await()
            register(token)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Could not obtain FCM token: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Registers a token Firebase handed us directly, skipping the fetch. Used by
     * [com.clearchain.app.data.remote.firebase.FCMService] when the token is rotated.
     */
    suspend fun register(token: String): Result<Unit> {
        return try {
            database.fcmTokenDao().saveToken(FCMTokenEntity(token = token))

            if (database.authTokenDao().getTokens() == null) {
                // Nothing to attach the token to yet. It's saved locally, and the next sign-in
                // re-runs this use case.
                Log.d(TAG, "⚠️ Signed out — token cached locally, will register after login")
                return Result.success(Unit)
            }

            authApi.registerFCMToken(RegisterFCMTokenRequest(fcmToken = token))
            Log.d(TAG, "🚀 Device registered for push")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Registration failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    private companion object {
        const val TAG = "RegisterFCMToken"
    }
}
