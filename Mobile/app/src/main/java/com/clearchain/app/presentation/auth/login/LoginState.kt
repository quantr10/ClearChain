package com.clearchain.app.presentation.auth.login

data class LoginState(
    val email: String = "",
    val password: String = "",
    val emailError: String? = null,
    val passwordError: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val rememberMe: Boolean = false,
    val isLockedOut: Boolean = false,
    val lockoutMinutes: Int = 0,
    val systemError: String? = null
)
