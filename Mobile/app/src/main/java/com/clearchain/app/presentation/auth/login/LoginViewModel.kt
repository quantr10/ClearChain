package com.clearchain.app.presentation.auth.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clearchain.app.domain.usecase.auth.LoginUseCase
import com.clearchain.app.presentation.navigation.Screen
import com.clearchain.app.util.UiEvent
import com.clearchain.app.util.ValidationUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(LoginState())
    val state: StateFlow<LoginState> = _state.asStateFlow()

    private val _uiEvent = Channel<UiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

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
                _state.update { it.copy(error = null) }
        }
    }

    private fun login() {
        if (!validateInputs()) return
        val currentState = _state.value

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            val result = loginUseCase(
                email = currentState.email,
                password = currentState.password
            )

            result.fold(
                onSuccess = { (user, _) ->
                    _state.update { it.copy(isLoading = false) }

                    // ═══ NEW: isProfileComplete() check (Part 1) ═══
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
                    _uiEvent.send(UiEvent.ShowSnackbar("Welcome back, ${user.name}!"))
                },
                onFailure = { error ->
                    _state.update { it.copy(isLoading = false, error = error.message ?: "Login failed") }
                    _uiEvent.send(UiEvent.ShowSnackbar(error.message ?: "Login failed"))
                }
            )
        }
    }

    private fun validateInputs(): Boolean {
        val s = _state.value
        var valid = true
        if (s.email.isBlank()) {
            _state.update { it.copy(emailError = "Email is required") }; valid = false
        } else if (!ValidationUtils.isValidEmail(s.email)) {
            _state.update { it.copy(emailError = "Invalid email format") }; valid = false
        }
        if (s.password.isBlank()) {
            _state.update { it.copy(passwordError = "Password is required") }; valid = false
        }
        return valid
    }
}