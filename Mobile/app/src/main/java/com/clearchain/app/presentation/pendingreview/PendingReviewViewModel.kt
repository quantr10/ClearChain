package com.clearchain.app.presentation.pendingreview

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clearchain.app.R
import com.clearchain.app.domain.model.Organization
import com.clearchain.app.domain.model.OrganizationType
import com.clearchain.app.domain.model.VerificationStatus
import com.clearchain.app.domain.usecase.auth.GetCurrentUserUseCase
import com.clearchain.app.domain.usecase.auth.LogoutUseCase
import com.clearchain.app.domain.usecase.auth.RefreshCurrentUserUseCase
import com.clearchain.app.util.UiEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PendingReviewState(
    val user: Organization? = null,
    val isRefreshing: Boolean = false
)

@HiltViewModel
class PendingReviewViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val refreshCurrentUserUseCase: RefreshCurrentUserUseCase,
    private val logoutUseCase: LogoutUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(PendingReviewState())
    val state: StateFlow<PendingReviewState> = _state.asStateFlow()

    private val _uiEvent = Channel<UiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    init {
        viewModelScope.launch {
            getCurrentUserUseCase().collect { user -> _state.update { it.copy(user = user) } }
        }
        refresh(silent = true)
    }

    fun refresh(silent: Boolean = false) {
        viewModelScope.launch {
            if (!silent) _state.update { it.copy(isRefreshing = true) }
            refreshCurrentUserUseCase().fold(
                onSuccess = { user ->
                    _state.update { it.copy(isRefreshing = false, user = user) }
                    if (!user.requiresVerificationGate()) {
                        _uiEvent.send(UiEvent.ShowSnackbar(context.getString(R.string.pending_review_approved_snack)))
                        _uiEvent.send(UiEvent.Navigate(dashboardRoute(user.type)))
                    } else if (!silent) {
                        val msg = when (user.verificationStatus) {
                            VerificationStatus.REJECTED -> context.getString(R.string.pending_review_still_rejected)
                            else -> context.getString(R.string.pending_review_still_pending)
                        }
                        _uiEvent.send(UiEvent.ShowSnackbar(msg))
                    }
                },
                onFailure = { e ->
                    _state.update { it.copy(isRefreshing = false) }
                    if (!silent) {
                        _uiEvent.send(UiEvent.ShowSnackbar(e.message ?: context.getString(R.string.pending_review_refresh_failed)))
                    }
                }
            )
        }
    }

    fun logout() {
        viewModelScope.launch {
            logoutUseCase()
            _uiEvent.send(UiEvent.Navigate("login"))
        }
    }

    private fun dashboardRoute(type: OrganizationType): String = when (type) {
        OrganizationType.NGO -> "ngo_dashboard"
        OrganizationType.GROCERY -> "grocery_dashboard"
        OrganizationType.ADMIN -> "admin_dashboard"
    }
}
