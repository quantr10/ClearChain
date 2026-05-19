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

    // UI State
    val isLoading: Boolean = false,
    val error: String? = null,

    // Password strength meter
    val passwordStrength: PasswordStrength = PasswordStrength.NONE,

    // Email availability check
    val isCheckingEmail: Boolean = false,
    val emailAvailable: Boolean? = null,  // null = not checked yet

    // Terms of service
    val tosAccepted: Boolean = false,
    val tosError: Boolean = false
)
