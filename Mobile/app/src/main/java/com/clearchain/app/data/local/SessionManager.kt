package com.clearchain.app.data.local

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * App-wide signal for "the session just died and there's nothing left to retry" — emitted by
 * [com.clearchain.app.data.remote.interceptor.TokenAuthenticator] when a token refresh itself
 * fails (refresh token expired/revoked too), from a background OkHttp thread with no UI access
 * of its own. MainActivity collects [sessionExpired] and bounces the user back to Login.
 *
 * Must be a true singleton: the instance TokenAuthenticator emits on has to be the same one
 * the UI layer collects from.
 */
@Singleton
class SessionManager @Inject constructor() {

    private val _sessionExpired = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val sessionExpired: SharedFlow<Unit> = _sessionExpired.asSharedFlow()

    fun notifySessionExpired() {
        _sessionExpired.tryEmit(Unit)
    }
}
