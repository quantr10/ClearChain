package com.clearchain.app.presentation.auth.login

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clearchain.app.R
import com.clearchain.app.data.local.AuthPreferenceStore
import com.clearchain.app.domain.usecase.auth.LoginUseCase
import com.clearchain.app.presentation.navigation.Screen
import com.clearchain.app.util.UiEvent
import com.clearchain.app.util.ValidationUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val loginUseCase: LoginUseCase,
    private val authPreferenceStore: AuthPreferenceStore
) : ViewModel() {

    private val _state = MutableStateFlow(LoginState())
    val state: StateFlow<LoginState> = _state.asStateFlow()

    private val _uiEvent = Channel<UiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    init {
        // Load saved email and remember me preference
        viewModelScope.launch {
            combine(
                authPreferenceStore.rememberMe,
                authPreferenceStore.savedEmail
            ) { rememberMe, savedEmail -> rememberMe to savedEmail }
                .first()
                .let { (rememberMe, savedEmail) ->
                    _state.update { it.copy(rememberMe = rememberMe, email = savedEmail) }
                }
        }
    }

    fun onEvent(event: LoginEvent) {
        when (event) {
            is LoginEvent.EmailChanged ->
                _state.update { it.copy(email = event.email, emailError = null) }
            is LoginEvent.PasswordChanged ->
                _state.update { it.copy(password = event.password, passwordError = null) }
            LoginEvent.Login -> login()
            LoginEvent.NavigateToRegister ->
                viewModelScope.launch { _uiEvent.send(UiEvent.Navigate("register")) }
            LoginEvent.ClearError ->
                _state.update { it.copy(error = null, isLockedOut = false) }
            LoginEvent.ToggleRememberMe ->
                _state.update { it.copy(rememberMe = !it.rememberMe) }
        }
    }

    private fun login() {
        if (!validateInputs()) return
        val currentState = _state.value

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null, isLockedOut = false) }

            val result = loginUseCase(
                email = currentState.email,
                password = currentState.password
            )

            result.fold(
                onSuccess = { (user, _) ->
                    // Save or clear remember-me preference
                    authPreferenceStore.saveRememberMe(
                        enabled = currentState.rememberMe,
                        email = if (currentState.rememberMe) currentState.email else ""
                    )
                    _state.update { it.copy(isLoading = false) }

                    val route = if (!user.isProfileComplete()) {
                        Screen.Onboarding.route
                    } else {
                        when (user.type.name.lowercase()) {
                            "grocery" -> "grocery_dashboard"
                            "ngo" -> "ngo_dashboard"
                            "admin" -> "admin_dashboard"
                            else -> "login"
                        }
                    }

                    _uiEvent.send(UiEvent.Navigate(route))
                    _uiEvent.send(UiEvent.ShowSnackbar(context.getString(R.string.snack_welcome_back, user.name)))
                },
                onFailure = { error ->
                    val message = error.message ?: context.getString(R.string.error_login_failed)
                    // Detect lockout: "Account locked. Try again in X minutes."
                    val lockoutMinutes = parseLockoutMinutes(message)
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = message,
                            isLockedOut = lockoutMinutes > 0,
                            lockoutMinutes = lockoutMinutes
                        )
                    }
                }
            )
        }
    }

    private fun parseLockoutMinutes(message: String): Int {
        // Backend format: "Account locked. Try again in X minutes."
        val regex = Regex("""(\d+)\s+minute""")
        return regex.find(message)?.groupValues?.get(1)?.toIntOrNull() ?: 0
    }

    private fun validateInputs(): Boolean {
        val s = _state.value
        var valid = true
        if (s.email.isBlank()) {
            _state.update { it.copy(emailError = context.getString(R.string.error_email_required)) }; valid = false
        } else if (!ValidationUtils.isValidEmail(s.email)) {
            _state.update { it.copy(emailError = context.getString(R.string.error_email_invalid_format)) }; valid = false
        }
        if (s.password.isBlank()) {
            _state.update { it.copy(passwordError = context.getString(R.string.error_password_required)) }; valid = false
        }
        return valid
    }
}
