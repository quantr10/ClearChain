package com.clearchain.app

import android.app.Application
import android.util.Log
import com.clearchain.app.data.remote.signalr.RealtimeLifecycleObserver
import com.clearchain.app.domain.usecase.fcm.RegisterFCMTokenUseCase
import com.google.firebase.FirebaseApp
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@HiltAndroidApp
class ClearChainApplication : Application() {

    @Inject
    lateinit var realtimeLifecycleObserver: RealtimeLifecycleObserver

    @Inject
    lateinit var registerFCMTokenUseCase: RegisterFCMTokenUseCase

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()

        Log.d(TAG, "📱 Application starting...")

        initializeFirebase()

        // Binds the SignalR connection to the process being in the foreground. Nothing else in
        // the app may connect or disconnect — see RealtimeLifecycleObserver.
        realtimeLifecycleObserver.start()

        // Re-registers this device on every launch. The token itself rarely changes, but the
        // server only learns about it through this call, and a re-register also refreshes the
        // token's UpdatedAt so an active device isn't swept by the stale-token job.
        applicationScope.launch {
            registerFCMTokenUseCase()
                .onFailure { Log.w(TAG, "FCM registration skipped: ${it.message}") }
        }
    }

    private fun initializeFirebase() {
        try {
            FirebaseApp.initializeApp(this)
            Log.d(TAG, "🔥 Firebase initialized (project: ${FirebaseApp.getInstance().options.projectId})")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Firebase initialization failed", e)
        }
    }

    private companion object {
        const val TAG = "ClearChainApp"
    }
}
