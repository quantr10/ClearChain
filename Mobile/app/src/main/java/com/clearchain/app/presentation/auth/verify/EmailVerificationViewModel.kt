package com.clearchain.app.presentation.auth.verify

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clearchain.app.R
import com.clearchain.app.domain.model.OrganizationType
import com.clearchain.app.domain.usecase.auth.ResendVerificationUseCase
import com.clearchain.app.domain.usecase.auth.VerifyEmailUseCase
import com.clearchain.app.presentation.navigation.Screen
import com.clearchain.app.util.UiEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.net.URLDecoder
import javax.inject.Inject

@HiltViewModel
class EmailVerificationViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle,
    private val verifyEmailUseCase: VerifyEmailUseCase,
    private val resendVerificationUseCase: ResendVerificationUseCase
) : ViewModel() {

    private val email: String = URLDecoder.decode(
        savedStateHandle.get<String>("email") ?: "", "UTF-8"
    )

    private val _state = MutableStateFlow(EmailVerificationState(email = email))
    val state: StateFlow<EmailVerificationState> = _state.asStateFlow()

    private val _uiEvent = Channel<UiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    private var cooldownJob: Job? = null

    fun onEvent(event: EmailVerificationEvent) {
        when (event) {
            is EmailVerificationEvent.CodeChanged -> {
                val digits = event.code.filter { it.isDigit() }.take(6)
                _state.update { it.copy(code = digits, codeError = null) }
                if (digits.length == 6) verify()
            }
            EmailVerificationEvent.Verify -> verify()
            EmailVerificationEvent.ResendCode -> resend()
            EmailVerificationEvent.ClearError -> _state.update { it.copy(error = null) }
        }
    }

    private fun verify() {
        val s = _state.value
        if (s.code.length != 6) {
            _state.update { it.copy(codeError = context.getString(R.string.error_enter_6_digit_code)) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val result = verifyEmailUseCase(s.email, s.code)
            result.fold(
                onSuccess = { (user, _) ->
                    _state.update { it.copy(isLoading = false) }
                    val route = when (user.type) {
                        OrganizationType.ADMIN -> Screen.AdminDashboard.route
                        else -> Screen.Onboarding.route
                    }
                    _uiEvent.send(UiEvent.Navigate(route))
                    _uiEvent.send(UiEvent.ShowSnackbar(context.getString(R.string.snack_email_verified)))
                },
                onFailure = { error ->
                    _state.update { it.copy(isLoading = false, error = error.message ?: context.getString(R.string.error_verification_failed)) }
                }
            )
        }
    }

    private fun resend() {
        if (_state.value.resendCooldownSeconds > 0) return
        viewModelScope.launch {
            _state.update { it.copy(isResending = true, error = null) }
            val result = resendVerificationUseCase(email)
            _state.update { it.copy(isResending = false) }
            result.fold(
                onSuccess = {
                    _uiEvent.send(UiEvent.ShowSnackbar(context.getString(R.string.snack_code_resent, email)))
                    startCooldown(60)
                },
                onFailure = { error ->
                    _state.update { it.copy(error = error.message ?: context.getString(R.string.error_resend_code_failed)) }
                }
            )
        }
    }

    private fun startCooldown(seconds: Int) {
        cooldownJob?.cancel()
        _state.update { it.copy(resendCooldownSeconds = seconds) }
        cooldownJob = viewModelScope.launch {
            repeat(seconds) {
                delay(1_000)
                _state.update { it.copy(resendCooldownSeconds = it.resendCooldownSeconds - 1) }
            }
        }
    }
}
