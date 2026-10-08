package com.clearchain.app.presentation.auth.login

sealed class LoginEvent {
    data class EmailChanged(val email: String) : LoginEvent()
    data class PasswordChanged(val password: String) : LoginEvent()
    object Login : LoginEvent()
    object ToggleRememberMe : LoginEvent()
    data class PasswordResetCompleted(val email: String) : LoginEvent()
}
