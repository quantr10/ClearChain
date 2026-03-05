package com.clearchain.app.domain.usecase.fcm

import android.util.Log
import com.clearchain.app.data.local.database.ClearChainDatabase
import com.clearchain.app.data.local.entity.FCMTokenEntity
import com.clearchain.app.data.remote.api.AuthApi
import com.clearchain.app.data.remote.dto.RegisterFCMTokenRequest
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class RegisterFCMTokenUseCase @Inject constructor(
    private val authApi: AuthApi,
    private val database: ClearChainDatabase
) {
    suspend operator fun invoke(): Result<Unit> {
        return try {
            // Get FCM token from Firebase
            val token = FirebaseMessaging.getInstance().token.await()
            Log.d("RegisterFCMToken", "📱 FCM Token obtained: ${token.take(20)}...")
            
            // Save locally
            database.fcmTokenDao().saveToken(
                FCMTokenEntity(token = token)
            )
            Log.d("RegisterFCMToken", "✅ Token saved to local database")
            
            // Check if user is logged in
            val authToken = database.authTokenDao().getTokens()
            if (authToken == null) {
                Log.d("RegisterFCMToken", "⚠️ User not logged in, skipping backend registration")
                return Result.success(Unit)
            }
            
            // Send to backend
            authApi.registerFCMToken(RegisterFCMTokenRequest(fcmToken = token))
            Log.d("RegisterFCMToken", "🚀 Token registered with backend")
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("RegisterFCMToken", "❌ Error: ${e.message}", e)
            Result.failure(e)
        }
    }
}