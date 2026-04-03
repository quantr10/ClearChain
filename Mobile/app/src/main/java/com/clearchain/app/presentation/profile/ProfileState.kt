package com.clearchain.app.presentation.profile

import com.clearchain.app.domain.model.Organization

data class ProfileState(
    val user: Organization? = null,
    val isLoading: Boolean = false,
    val isChangingPassword: Boolean = false,
    val error: String? = null,

    // Edit mode
    val isEditing: Boolean = false,
    val editName: String = "",
    val editPhone: String = "",
    val editAddress: String = "",
    val editLocation: String = "",
    val editOpenTime: String = "",
    val editCloseTime: String = "",

    // ═══ NEW edit fields (Part 1) ═══
    val editContactPerson: String = "",
    val editPickupInstructions: String = "",
    val editDescription: String = "",
    val editLat: Double? = null,
    val editLng: Double? = null,
    
    // Edit errors
    val editNameError: String? = null,
    val editPhoneError: String? = null,
    val editAddressError: String? = null,
    val editLocationError: String? = null,
    val editContactPersonError: String? = null,

    val isSavingProfile: Boolean = false
)