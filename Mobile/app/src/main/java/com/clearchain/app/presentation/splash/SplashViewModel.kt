package com.clearchain.app.presentation.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clearchain.app.domain.model.Organization
import com.clearchain.app.domain.usecase.auth.GetCurrentUserUseCase
import com.clearchain.app.domain.usecase.auth.RefreshCurrentUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val refreshCurrentUserUseCase: RefreshCurrentUserUseCase
) : ViewModel() {

    private val _isLoggedIn = MutableStateFlow<Boolean?>(null)
    val isLoggedIn: StateFlow<Boolean?> = _isLoggedIn.asStateFlow()

    init {
        checkLoginStatus()
    }

    private fun checkLoginStatus() {
        viewModelScope.launch {
            getCurrentUserUseCase()
                .map { it != null }
                .collect { loggedIn ->
                    _isLoggedIn.value = loggedIn
                }
        }
    }

    // ✅ ADD: Method to get current user
    suspend fun getCurrentUser(): Organization? {
        // Best-effort: pull the latest verification status from the server so an
        // admin approval/rejection is reflected without a full re-login. Falls back
        // to the cached user if offline.
        return refreshCurrentUserUseCase().getOrNull() ?: getCurrentUserUseCase().first()
    }
}