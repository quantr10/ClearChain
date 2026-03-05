package com.clearchain.app.domain.usecase.auth

import android.util.Log
import com.clearchain.app.data.local.database.ClearChainDatabase
import com.clearchain.app.domain.repository.AuthRepository
import javax.inject.Inject

class LogoutUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val database: ClearChainDatabase  // ✅ ADD
) {
    suspend operator fun invoke(): Result<Unit> {
        val result = authRepository.logout()
        
        // ✅ ADD: Clear FCM token from local database on logout
        if (result.isSuccess) {
            try {
                database.fcmTokenDao().clearToken()
                Log.d("LogoutUseCase", "🔔 FCM token cleared from local database")
            } catch (e: Exception) {
                Log.e("LogoutUseCase", "Failed to clear FCM token: ${e.message}")
                // Don't fail logout if token clearing fails
            }
        }
        
        return result
    }
}