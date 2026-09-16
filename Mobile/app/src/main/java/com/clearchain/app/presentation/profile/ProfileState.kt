package com.clearchain.app.presentation.profile

import com.clearchain.app.domain.model.ActivityItem
import com.clearchain.app.domain.model.OrgStats
import com.clearchain.app.domain.model.Organization

data class ProfileState(
    val user: Organization? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val averageRating: Double = 0.0,
    val reviewCount: Int = 0,

    // ── Stats ────────────────────────────────────────────────────────────────
    val stats: OrgStats? = null,
    val isLoadingStats: Boolean = false,

    // ── Activity feed ────────────────────────────────────────────────────────
    val activity: List<ActivityItem> = emptyList(),
    val isLoadingActivity: Boolean = false,
    val activityError: String? = null,

    // ── Password change ──────────────────────────────────────────────────────
    val isChangingPassword: Boolean = false,

    // ── Edit mode ────────────────────────────────────────────────────────────
    val isEditing: Boolean = false,
    val editName: String = "",
    val editEmail: String = "",
    val editPhone: String = "",
    val editAddress: String = "",
    val editLocation: String = "",
    val editState: String = "",
    val editZipCode: String = "",
    val editOpenTime: String = "",
    val editCloseTime: String = "",
    val editContactPerson: String = "",
    val editPickupInstructions: String = "",
    val editDescription: String = "",
    val editLat: Double? = null,
    val editLng: Double? = null,

    // ── Edit validation errors ───────────────────────────────────────────────
    val editNameError: String? = null,
    val editEmailError: String? = null,
    val editPhoneError: String? = null,
    val editAddressError: String? = null,
    val editLocationError: String? = null,
    val editStateError: String? = null,
    val editZipCodeError: String? = null,
    val editOpenTimeError: String? = null,
    val editCloseTimeError: String? = null,
    val editContactPersonError: String? = null,

    val isSavingProfile: Boolean = false,

    // Delete account
    val isDeletingAccount: Boolean = false,

    // Avatar upload
    val isUploadingAvatar: Boolean = false,
    val avatarUploadError: String? = null
)
