package com.clearchain.app.presentation.profile

sealed class ProfileEvent {
    object LoadProfile : ProfileEvent()
    object ClearError : ProfileEvent()

    data class ChangePassword(val currentPassword: String, val newPassword: String) : ProfileEvent()

    // Edit mode
    object StartEdit : ProfileEvent()
    object CancelEdit : ProfileEvent()
    data class EditNameChanged(val name: String) : ProfileEvent()
    data class EditPhoneChanged(val phone: String) : ProfileEvent()
    data class EditAddressChanged(val address: String) : ProfileEvent()
    data class EditLocationChanged(val location: String) : ProfileEvent()
    data class EditOpenTimeChanged(val time: String) : ProfileEvent()
    data class EditCloseTimeChanged(val time: String) : ProfileEvent()

    // ═══ NEW events (Part 1) ═══
    data class EditContactPersonChanged(val contactPerson: String) : ProfileEvent()
    data class EditPickupInstructionsChanged(val instructions: String) : ProfileEvent()
    data class EditDescriptionChanged(val description: String) : ProfileEvent()

    object SaveProfile : ProfileEvent()
}