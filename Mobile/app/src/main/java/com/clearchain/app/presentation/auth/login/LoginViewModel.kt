package com.clearchain.app.presentation.auth.login

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clearchain.app.R
import com.clearchain.app.data.local.AuthPreferenceStore
import com.clearchain.app.domain.usecase.auth.LoginUseCase
import com.clearchain.app.presentation.navigation.Screen
import com.clearchain.app.util.ApiErrorUtils
import com.clearchain.app.util.UiEvent
import com.clearchain.app.util.ValidationUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

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
            LoginEvent.ToggleRememberMe ->
                _state.update { it.copy(rememberMe = !it.rememberMe) }
            is LoginEvent.PasswordResetCompleted -> {
                // Reset also clears any lockout server-side, so drop ours to match.
                _state.update {
                    it.copy(
                        email = event.email,
                        password = "",
                        emailError = null,
                        passwordError = null,
                        isLockedOut = false
                    )
                }
                viewModelScope.launch {
                    _uiEvent.send(UiEvent.ShowSnackbar(context.getString(R.string.snack_password_reset_done)))
                }
            }
        }
    }

    private fun login() {
        if (!validateInputs()) return
        val currentState = _state.value

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, isLockedOut = false) }

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

                    val route = when {
                        !user.isProfileComplete() -> Screen.Onboarding.route
                        user.requiresVerificationGate() -> Screen.PendingReview.route
                        else -> when (user.type.name.lowercase()) {
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
                    val raw = error.message ?: ""
                    val lockoutMinutes = parseLockoutMinutes(raw)
                    if (lockoutMinutes > 0) {
                        _state.update {
                            it.copy(isLoading = false, isLockedOut = true, lockoutMinutes = lockoutMinutes)
                        }
                    } else {
                        // Route each error to the appropriate field instead of a banner
                        val systemMsg = ApiErrorUtils.systemMessage(context, raw)

                        val (emailErr, passwordErr) = when {
                            systemMsg != null -> null to null // shown as a snackbar below
                            raw.contains("401") || raw.contains("Unauthorized", ignoreCase = true) ->
                                "" to context.getString(R.string.error_wrong_credentials)
                            raw.contains("404") || raw.contains("Not Found", ignoreCase = true) ->
                                context.getString(R.string.error_account_not_found) to null
                            else ->
                                null to raw.ifBlank { context.getString(R.string.error_login_failed) }
                        }

                        _state.update {
                            it.copy(
                                isLoading = false,
                                emailError = emailErr,
                                passwordError = passwordErr
                            )
                        }

                        if (systemMsg != null) {
                            _uiEvent.send(UiEvent.ShowSnackbar(systemMsg))
                        }
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
            _state.update { it.copy(emailError = context.getString(R.string.error_email_required)) }
            valid = false
        } else if (!ValidationUtils.isValidEmail(s.email)) {
            _state.update { it.copy(emailError = context.getString(R.string.error_email_invalid_format)) }
            valid = false
        }
        if (s.password.isBlank()) {
            _state.update { it.copy(passwordError = context.getString(R.string.error_password_required)) }
            valid = false
        }
        return valid
    }
}
