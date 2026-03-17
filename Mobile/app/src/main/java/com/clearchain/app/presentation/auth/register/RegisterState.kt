package com.clearchain.app.presentation.auth.register

data class RegisterState(
    val name: String = "",
    val type: String = "grocery", // ✅ KEEP: grocery or ngo
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    
    // ✅ REMOVED: phone, address, location, hours

    // Errors
    val nameError: String? = null,
    val emailError: String? = null,
    val passwordError: String? = null,
    val confirmPasswordError: String? = null,
    // ✅ REMOVED: phoneError, addressError, locationError

    // UI State
    val isLoading: Boolean = false,
    val error: String? = null
)