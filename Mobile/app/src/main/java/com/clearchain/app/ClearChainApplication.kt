package com.clearchain.app

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@HiltAndroidApp
class ClearChainApplication : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()

        Log.d("ClearChainApp", "📱 Application starting...")

        // ✅ CRITICAL: Initialize Firebase first
        initializeFirebase()

        // ✅ Then request FCM token
        requestFCMToken()
    }

    private fun initializeFirebase() {
        try {
            // Initialize Firebase (reads google-services.json)
            FirebaseApp.initializeApp(this)
            Log.d("ClearChainApp", "🔥 Firebase initialized successfully")

            // Verify Firebase app
            val firebaseApp = FirebaseApp.getInstance()
            Log.d("ClearChainApp", "🔥 Firebase project: ${firebaseApp.options.projectId}")
            Log.d("ClearChainApp", "🔥 Firebase app name: ${firebaseApp.name}")
        } catch (e: Exception) {
            Log.e("ClearChainApp", "❌ Firebase initialization failed", e)
            Log.e("ClearChainApp", "❌ Error: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun requestFCMToken() {
        applicationScope.launch {
            try {
                Log.d("ClearChainApp", "🔔 Requesting FCM token...")

                // Force token generation
                val token = FirebaseMessaging.getInstance().token.await()

                Log.d("ClearChainApp", "✅ FCM Token obtained!")
                Log.d("ClearChainApp", "🔔 Token (first 30 chars): ${token.take(30)}...")
                // Token will be saved by FCMService.onNewToken()
            } catch (e: Exception) {
                Log.e("ClearChainApp", "❌ Failed to get FCM token", e)
                Log.e("ClearChainApp", "❌ Error type: ${e.javaClass.simpleName}")
                Log.e("ClearChainApp", "❌ Error message: ${e.message}")
                e.printStackTrace()
            }
        }
    }
}