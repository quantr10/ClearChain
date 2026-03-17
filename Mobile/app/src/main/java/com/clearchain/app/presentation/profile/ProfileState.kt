package com.clearchain.app.presentation.profile

import com.clearchain.app.domain.model.Organization

data class ProfileState(
    val user: Organization? = null,
    val isLoading: Boolean = false,
    val isChangingPassword: Boolean = false,
    val error: String? = null,
    
    // ✅ NEW: Edit mode fields
    val isEditing: Boolean = false,
    val editName: String = "",
    val editPhone: String = "",
    val editAddress: String = "",
    val editLocation: String = "",
    val editHours: String = "",
    
    // ✅ NEW: Edit errors
    val editNameError: String? = null,
    val editPhoneError: String? = null,
    val editAddressError: String? = null,
    val editLocationError: String? = null,
    
    val isSavingProfile: Boolean = false
)