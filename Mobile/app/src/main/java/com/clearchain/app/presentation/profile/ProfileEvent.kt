package com.clearchain.app.presentation.profile

import android.net.Uri

sealed class ProfileEvent {
    object LoadProfile  : ProfileEvent()
    object LoadStats    : ProfileEvent()
    object LoadActivity : ProfileEvent()
    object Refresh      : ProfileEvent()
    object ClearError   : ProfileEvent()

    data class ChangePassword(val currentPassword: String, val newPassword: String) : ProfileEvent()
    data class DeleteAccount(val password: String) : ProfileEvent()

    // ── Edit mode ──────────────────────────────────────────────────────────
    object StartEdit  : ProfileEvent()
    object CancelEdit : ProfileEvent()
    object SaveProfile : ProfileEvent()

    data class EditNameChanged(val name: String)                       : ProfileEvent()
    data class EditPhoneChanged(val phone: String)                     : ProfileEvent()
    data class EditAddressChanged(val address: String)                 : ProfileEvent()
    data class EditLocationChanged(val location: String)               : ProfileEvent()
    data class EditOpenTimeChanged(val time: String)                   : ProfileEvent()
    data class EditCloseTimeChanged(val time: String)                  : ProfileEvent()
    data class EditContactPersonChanged(val contactPerson: String)     : ProfileEvent()
    data class EditPickupInstructionsChanged(val instructions: String) : ProfileEvent()
    data class EditDescriptionChanged(val description: String)         : ProfileEvent()
    data class EditLocationCoordsChanged(val lat: Double, val lng: Double) : ProfileEvent()

    data class AvatarSelected(val uri: Uri) : ProfileEvent()
}
