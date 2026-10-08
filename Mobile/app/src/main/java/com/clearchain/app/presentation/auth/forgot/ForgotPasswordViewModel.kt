package com.clearchain.app.presentation.auth.forgot

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clearchain.app.R
import com.clearchain.app.domain.usecase.auth.RequestPasswordResetUseCase
import com.clearchain.app.domain.usecase.auth.ResetPasswordUseCase
import com.clearchain.app.util.ApiErrorUtils
import com.clearchain.app.util.UiEvent
import com.clearchain.app.util.ValidationUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.net.URLDecoder
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@HiltViewModel
class ForgotPasswordViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle,
    private val requestPasswordResetUseCase: RequestPasswordResetUseCase,
    private val resetPasswordUseCase: ResetPasswordUseCase
) : ViewModel() {

    // Pre-filled with whatever was typed on the login screen.
    private val initialEmail: String = URLDecoder.decode(
        savedStateHandle.get<String>("email") ?: "",
        "UTF-8"
    )

    private val _state = MutableStateFlow(ForgotPasswordState(email = initialEmail))
    val state: StateFlow<ForgotPasswordState> = _state.asStateFlow()

    private val _uiEvent = Channel<UiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    private var cooldownJob: Job? = null

    fun onEvent(event: ForgotPasswordEvent) {
        when (event) {
            is ForgotPasswordEvent.EmailChanged ->
                _state.update { it.copy(email = event.email, emailError = null) }
            ForgotPasswordEvent.SendCode -> sendCode()
            is ForgotPasswordEvent.CodeChanged -> {
                val digits = event.code.filter { it.isDigit() }.take(6)
                _state.update { it.copy(code = digits, codeError = null) }
            }
            is ForgotPasswordEvent.NewPasswordChanged ->
                _state.update { it.copy(newPassword = event.password, newPasswordError = null) }
            is ForgotPasswordEvent.ConfirmPasswordChanged ->
                _state.update { it.copy(confirmPassword = event.password, confirmPasswordError = null) }
            ForgotPasswordEvent.ResetPassword -> resetPassword()
            ForgotPasswordEvent.ResendCode -> if (_state.value.resendCooldownSeconds == 0) sendCode()
            ForgotPasswordEvent.ChangeEmail -> {
                cooldownJob?.cancel()
                _state.update { ForgotPasswordState(email = it.email) }
            }
        }
    }

    private fun sendCode() {
        val email = _state.value.email.trim()
        val emailError = when {
            email.isBlank() -> context.getString(R.string.error_email_required)
            !ValidationUtils.isValidEmail(email) -> context.getString(R.string.error_email_invalid_format)
            else -> null
        }
        if (emailError != null) {
            _state.update { it.copy(emailError = emailError) }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isSending = true, email = email) }
            val result = requestPasswordResetUseCase(email)
            _state.update { it.copy(isSending = false) }
            result.fold(
                onSuccess = {
                    _state.update { it.copy(codeSent = true) }
                    _uiEvent.send(UiEvent.ShowSnackbar(context.getString(R.string.snack_reset_code_sent, email)))
                    startCooldown(60)
                },
                onFailure = { error ->
                    val msg = ApiErrorUtils.serverMessage(error)
                        ?: ApiErrorUtils.systemMessage(context, error.message.orEmpty())
                        ?: context.getString(R.string.error_reset_code_failed)
                    _uiEvent.send(UiEvent.ShowSnackbar(msg))
                }
            )
        }
    }

    private fun resetPassword() {
        if (!validateResetInputs()) return
        val s = _state.value

        viewModelScope.launch {
            _state.update { it.copy(isResetting = true) }
            val result = resetPasswordUseCase(s.email, s.code, s.newPassword)
            result.fold(
                onSuccess = {
                    _state.update { it.copy(isResetting = false) }
                    // The screen hands the email back to login, which shows the confirmation.
                    _uiEvent.send(UiEvent.NavigateUp)
                },
                onFailure = { error ->
                    // A system failure is a snackbar; anything the server rejected is about
                    // the code (wrong, expired, too many tries) and belongs under that field.
                    val systemMsg = ApiErrorUtils.systemMessage(context, error.message.orEmpty())
                    if (systemMsg != null) {
                        _state.update { it.copy(isResetting = false) }
                        _uiEvent.send(UiEvent.ShowSnackbar(systemMsg))
                    } else {
                        val msg = ApiErrorUtils.serverMessage(error)
                            ?: context.getString(R.string.error_reset_password_failed)
                        _state.update { it.copy(isResetting = false, codeError = msg) }
                    }
                }
            )
        }
    }

    private fun validateResetInputs(): Boolean {
        val s = _state.value
        val codeError = if (s.code.length != 6) context.getString(R.string.error_enter_6_digit_code) else null
        val newPasswordError = when {
            s.newPassword.isBlank() -> context.getString(R.string.error_password_required)
            !ValidationUtils.isValidPassword(s.newPassword) -> context.getString(R.string.error_password_complexity)
            else -> null
        }
        val confirmPasswordError = when {
            s.confirmPassword.isBlank() -> context.getString(R.string.error_confirm_password_required)
            s.confirmPassword != s.newPassword -> context.getString(R.string.error_passwords_dont_match)
            else -> null
        }
        _state.update {
            it.copy(
                codeError = codeError,
                newPasswordError = newPasswordError,
                confirmPasswordError = confirmPasswordError
            )
        }
        return codeError == null && newPasswordError == null && confirmPasswordError == null
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
