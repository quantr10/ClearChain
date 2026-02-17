package com.clearchain.app.presentation.profile

sealed class ProfileEvent {
    object LoadProfile : ProfileEvent()
    object ClearError : ProfileEvent()

    // ✅ ADD
    data class ChangePassword(
        val currentPassword: String,
        val newPassword: String
    ) : ProfileEvent()
}