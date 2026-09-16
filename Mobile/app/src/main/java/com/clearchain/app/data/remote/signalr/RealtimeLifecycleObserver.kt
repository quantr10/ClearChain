package com.clearchain.app.data.remote.signalr

import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Owns the lifetime of the app's SignalR connection, binding it to the process being in the
 * foreground rather than to any one screen.
 *
 * This exists because screen-scoped ownership cannot work: the service is a singleton shared by
 * every view model, so whichever screen closed first took real-time down for all the rest. The
 * process lifecycle is the only scope that matches what the connection actually is — one
 * connection, alive while the user is looking at the app.
 *
 * Backgrounding disconnects on purpose. A socket held open behind a locked screen drains
 * battery for events the user cannot see, and anything that happens while away arrives as a
 * push notification and is waiting in the inbox on return.
 */
@Singleton
class RealtimeLifecycleObserver @Inject constructor(
    private val signalRService: SignalRService
) : DefaultLifecycleObserver {

    private companion object {
        const val TAG = "RealtimeLifecycle"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Call once from Application.onCreate. */
    fun start() {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override fun onStart(owner: LifecycleOwner) {
        Log.d(TAG, "App foregrounded — bringing SignalR up")
        scope.launch {
            runCatching { signalRService.connect() }
                .onFailure { Log.e(TAG, "Failed to connect on foreground", it) }
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        Log.d(TAG, "App backgrounded — releasing SignalR")
        scope.launch {
            runCatching { signalRService.disconnect() }
                .onFailure { Log.e(TAG, "Failed to disconnect on background", it) }
        }
    }
}
