package com.clearchain.app.presentation.auth.register

data class RegisterState(
    val name: String = "",
    val type: String = "grocery", // grocery or ngo
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val phone: String = "",
    val address: String = "",
    val location: String = "",
    val hours: String = "",

    // Errors
    val nameError: String? = null,
    val emailError: String? = null,
    val passwordError: String? = null,
    val confirmPasswordError: String? = null,
    val phoneError: String? = null,
    val addressError: String? = null,
    val locationError: String? = null,

    // UI State
    val isLoading: Boolean = false,
    val error: String? = null
)