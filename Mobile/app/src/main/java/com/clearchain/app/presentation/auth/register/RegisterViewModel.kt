package com.clearchain.app.presentation.auth.register

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clearchain.app.R
import com.clearchain.app.data.remote.api.AuthApi
import com.clearchain.app.domain.usecase.auth.RegisterUseCase
import com.clearchain.app.presentation.navigation.Screen
import com.clearchain.app.util.UiEvent
import com.clearchain.app.util.ValidationUtils
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class RegisterViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val registerUseCase: RegisterUseCase,
    private val authApi: AuthApi
) : ViewModel() {

    private val _state = MutableStateFlow(RegisterState())
    val state: StateFlow<RegisterState> = _state.asStateFlow()

    private val _uiEvent = Channel<UiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    private var emailCheckJob: Job? = null

    fun onEvent(event: RegisterEvent) {
        when (event) {
            is RegisterEvent.NameChanged ->
                _state.update { it.copy(name = event.name, nameError = null) }
            is RegisterEvent.TypeChanged ->
                _state.update { it.copy(type = event.type) }
            is RegisterEvent.EmailChanged -> {
                _state.update { it.copy(email = event.email, emailError = null, emailAvailable = null) }
                scheduleEmailCheck(event.email)
            }
            is RegisterEvent.PasswordChanged -> {
                _state.update {
                    it.copy(
                        password = event.password,
                        passwordError = null,
                        passwordStrength = evaluatePasswordStrength(event.password)
                    )
                }
            }
            is RegisterEvent.ConfirmPasswordChanged ->
                _state.update { it.copy(confirmPassword = event.confirmPassword, confirmPasswordError = null) }
            RegisterEvent.ToggleTos ->
                _state.update { it.copy(tosAccepted = !it.tosAccepted, tosError = false) }
            RegisterEvent.Register -> register()
            RegisterEvent.NavigateToLogin ->
                viewModelScope.launch { _uiEvent.send(UiEvent.NavigateUp) }
            RegisterEvent.ClearError ->
                _state.update { it.copy(error = null) }
        }
    }

    private fun scheduleEmailCheck(email: String) {
        emailCheckJob?.cancel()
        if (email.isBlank() || !ValidationUtils.isValidEmail(email)) return

        emailCheckJob = viewModelScope.launch {
            delay(600)  // 600ms debounce
            _state.update { it.copy(isCheckingEmail = true) }
            try {
                val response = authApi.checkEmail(email)
                _state.update { it.copy(isCheckingEmail = false, emailAvailable = response.available) }
            } catch (e: Exception) {
                _state.update { it.copy(isCheckingEmail = false, emailAvailable = null) }
            }
        }
    }

    private fun evaluatePasswordStrength(password: String): PasswordStrength {
        if (password.isEmpty()) return PasswordStrength.NONE
        var score = 0
        if (password.length >= 8) score++
        if (password.any { it.isUpperCase() }) score++
        if (password.any { it.isDigit() }) score++
        if (password.any { !it.isLetterOrDigit() }) score++
        return when {
            score <= 1 -> PasswordStrength.WEAK
            score == 2 -> PasswordStrength.MEDIUM
            else -> PasswordStrength.STRONG
        }
    }

    private fun register() {
        if (!validateInputs()) return
        val s = _state.value

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            val fcmToken = try {
                FirebaseMessaging.getInstance().token.await()
            } catch (e: Exception) {
                Log.e("RegisterViewModel", "Failed to get FCM token", e); null
            }

            val result = registerUseCase(
                name = s.name, type = s.type, email = s.email,
                password = s.password, fcmToken = fcmToken
            )

            result.fold(
                onSuccess = { email ->
                    _state.update { it.copy(isLoading = false) }
                    _uiEvent.send(UiEvent.Navigate(Screen.EmailVerification.createRoute(email)))
                },
                onFailure = { error ->
                    val raw = error.message ?: ""
                    val (emailErr, passwordErr) = when {
                        raw.contains("409") || raw.contains("Conflict", ignoreCase = true)
                            || raw.contains("already", ignoreCase = true)
                            -> context.getString(R.string.error_email_taken) to null
                        raw.contains("500") || raw.contains("502") || raw.contains("503") ->
                            null to context.getString(R.string.error_server)
                        raw.contains("Unable to resolve host", ignoreCase = true)
                            || raw.contains("timeout", ignoreCase = true)
                            || raw.contains("connect", ignoreCase = true) ->
                            null to context.getString(R.string.error_no_internet)
                        else ->
                            null to raw.ifBlank { context.getString(R.string.error_registration_failed) }
                    }
                    _state.update {
                        it.copy(
                            isLoading     = false,
                            error         = null,
                            emailError    = emailErr,
                            passwordError = passwordErr
                        )
                    }
                }
            )
        }
    }

    private fun validateInputs(): Boolean {
        val s = _state.value
        var valid = true
        if (s.name.isBlank()) {
            _state.update { it.copy(nameError = context.getString(R.string.error_name_required)) }; valid = false
        } else if (s.name.length < 3) {
            _state.update { it.copy(nameError = context.getString(R.string.error_name_min_length)) }; valid = false
        }
        if (s.email.isBlank()) {
            _state.update { it.copy(emailError = context.getString(R.string.error_email_required)) }; valid = false
        } else if (!ValidationUtils.isValidEmail(s.email)) {
            _state.update { it.copy(emailError = context.getString(R.string.error_email_invalid_format)) }; valid = false
        } else if (s.emailAvailable == false) {
            _state.update { it.copy(emailError = context.getString(R.string.error_email_already_registered)) }; valid = false
        }
        if (s.password.isBlank()) {
            _state.update { it.copy(passwordError = context.getString(R.string.error_password_required)) }; valid = false
        } else if (!ValidationUtils.isValidPassword(s.password)) {
            _state.update { it.copy(passwordError = context.getString(R.string.error_password_complexity)) }; valid = false
        }
        if (s.confirmPassword.isBlank()) {
            _state.update { it.copy(confirmPasswordError = context.getString(R.string.error_confirm_password_required)) }; valid = false
        } else if (s.password != s.confirmPassword) {
            _state.update { it.copy(confirmPasswordError = context.getString(R.string.error_passwords_dont_match)) }; valid = false
        }
        if (!s.tosAccepted) {
            _state.update { it.copy(tosError = true) }; valid = false
        }
        return valid
    }
}
