package com.clearchain.app.presentation.auth.register

sealed class RegisterEvent {
    data class NameChanged(val name: String) : RegisterEvent()
    data class TypeChanged(val type: String) : RegisterEvent()
    data class EmailChanged(val email: String) : RegisterEvent()
    data class PasswordChanged(val password: String) : RegisterEvent()
    data class ConfirmPasswordChanged(val confirmPassword: String) : RegisterEvent()
    data class PhoneChanged(val phone: String) : RegisterEvent()
    data class AddressChanged(val address: String) : RegisterEvent()
    data class LocationChanged(val location: String) : RegisterEvent()
    data class HoursChanged(val hours: String) : RegisterEvent()

    object Register : RegisterEvent()
    object NavigateToLogin : RegisterEvent()
    object ClearError : RegisterEvent()
}