package com.clearchain.app.presentation.auth.register

enum class PasswordStrength { NONE, WEAK, MEDIUM, STRONG }

data class RegisterState(
    val name: String = "",
    val type: String = "grocery",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",

    // Errors
    val nameError: String? = null,
    val emailError: String? = null,
    val passwordError: String? = null,
    val confirmPasswordError: String? = null,

    // UI state
    val isLoading: Boolean = false,

    val passwordStrength: PasswordStrength = PasswordStrength.NONE,

    val emailAlreadyExists: Boolean = false,

    val tosAccepted: Boolean = false,
    val tosError: Boolean = false
)
