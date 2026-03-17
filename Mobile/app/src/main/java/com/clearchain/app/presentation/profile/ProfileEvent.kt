package com.clearchain.app.presentation.profile

sealed class ProfileEvent {
    object LoadProfile : ProfileEvent()
    object ClearError : ProfileEvent()

    // Change password event
    data class ChangePassword(
        val currentPassword: String,
        val newPassword: String
    ) : ProfileEvent()
    
    // ✅ NEW: Profile edit events
    object StartEdit : ProfileEvent()
    object CancelEdit : ProfileEvent()
    data class EditNameChanged(val name: String) : ProfileEvent()
    data class EditPhoneChanged(val phone: String) : ProfileEvent()
    data class EditAddressChanged(val address: String) : ProfileEvent()
    data class EditLocationChanged(val location: String) : ProfileEvent()
    data class EditHoursChanged(val hours: String) : ProfileEvent()
    object SaveProfile : ProfileEvent()
}