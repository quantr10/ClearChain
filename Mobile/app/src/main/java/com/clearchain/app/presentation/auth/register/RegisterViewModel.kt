package com.clearchain.app.presentation.auth.register

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clearchain.app.domain.usecase.auth.RegisterUseCase
import com.clearchain.app.presentation.navigation.Screen
import com.clearchain.app.util.UiEvent
import com.clearchain.app.util.ValidationUtils
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val registerUseCase: RegisterUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(RegisterState())
    val state: StateFlow<RegisterState> = _state.asStateFlow()

    private val _uiEvent = Channel<UiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    fun onEvent(event: RegisterEvent) {
        when (event) {
            is RegisterEvent.NameChanged ->
                _state.update { it.copy(name = event.name, nameError = null) }
            is RegisterEvent.TypeChanged ->
                _state.update { it.copy(type = event.type) }
            is RegisterEvent.EmailChanged ->
                _state.update { it.copy(email = event.email, emailError = null) }
            is RegisterEvent.PasswordChanged ->
                _state.update { it.copy(password = event.password, passwordError = null) }
            is RegisterEvent.ConfirmPasswordChanged ->
                _state.update { it.copy(confirmPassword = event.confirmPassword, confirmPasswordError = null) }
            RegisterEvent.Register -> register()
            RegisterEvent.NavigateToLogin ->
                viewModelScope.launch { _uiEvent.send(UiEvent.NavigateUp) }
            RegisterEvent.ClearError ->
                _state.update { it.copy(error = null) }
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
                onSuccess = { (user, _) ->
                    _state.update { it.copy(isLoading = false) }

                    // ═══ NEW: Always go to Onboarding after register (Part 1) ═══
                    // Admin skips onboarding (isProfileComplete returns true)
                    val route = if (user.type.name.lowercase() == "admin") {
                        Screen.AdminDashboard.route
                    } else {
                        Screen.Onboarding.route
                    }

                    _uiEvent.send(UiEvent.Navigate(route))
                    _uiEvent.send(UiEvent.ShowSnackbar("Welcome! Let's complete your profile."))
                },
                onFailure = { error ->
                    _state.update { it.copy(isLoading = false, error = error.message ?: "Registration failed") }
                    _uiEvent.send(UiEvent.ShowSnackbar(error.message ?: "Registration failed"))
                }
            )
        }
    }

    private fun validateInputs(): Boolean {
        val s = _state.value
        var valid = true
        if (s.name.isBlank()) {
            _state.update { it.copy(nameError = "Name is required") }; valid = false
        } else if (s.name.length < 3) {
            _state.update { it.copy(nameError = "Name must be at least 3 characters") }; valid = false
        }
        if (s.email.isBlank()) {
            _state.update { it.copy(emailError = "Email is required") }; valid = false
        } else if (!ValidationUtils.isValidEmail(s.email)) {
            _state.update { it.copy(emailError = "Invalid email format") }; valid = false
        }
        if (s.password.isBlank()) {
            _state.update { it.copy(passwordError = "Password is required") }; valid = false
        } else if (!ValidationUtils.isValidPassword(s.password)) {
            _state.update { it.copy(passwordError = "Password must be 8+ chars with uppercase, lowercase, and number") }; valid = false
        }
        if (s.confirmPassword.isBlank()) {
            _state.update { it.copy(confirmPasswordError = "Please confirm your password") }; valid = false
        } else if (s.password != s.confirmPassword) {
            _state.update { it.copy(confirmPasswordError = "Passwords do not match") }; valid = false
        }
        return valid
    }
}