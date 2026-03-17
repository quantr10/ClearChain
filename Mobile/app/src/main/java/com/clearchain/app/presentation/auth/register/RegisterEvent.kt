package com.clearchain.app.presentation.auth.register

sealed class RegisterEvent {
    data class NameChanged(val name: String) : RegisterEvent()
    data class TypeChanged(val type: String) : RegisterEvent()  // ✅ KEEP
    data class EmailChanged(val email: String) : RegisterEvent()
    data class PasswordChanged(val password: String) : RegisterEvent()
    data class ConfirmPasswordChanged(val confirmPassword: String) : RegisterEvent()
    
    // ✅ REMOVED: PhoneChanged, AddressChanged, LocationChanged, HoursChanged

    object Register : RegisterEvent()
    object NavigateToLogin : RegisterEvent()
    object ClearError : RegisterEvent()
}