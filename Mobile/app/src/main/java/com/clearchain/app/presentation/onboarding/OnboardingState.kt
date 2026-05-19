package com.clearchain.app.presentation.onboarding

import android.net.Uri
import com.clearchain.app.domain.model.OrganizationType

data class OnboardingState(
    val currentStep: Int = 1,
    val totalSteps: Int = 3,
    val userType: OrganizationType = OrganizationType.GROCERY,
    val userName: String = "",

    // Step 1 fields
    val phone: String = "",
    val description: String = "",
    val contactPerson: String = "",
    val phoneError: String? = null,
    val contactPersonError: String? = null,

    // Step 2 fields
    val address: String = "",
    val city: String = "",
    val openTime: String = "",
    val closeTime: String = "",
    val pickupInstructions: String = "",
    val addressError: String? = null,
    val cityError: String? = null,

    // Document upload (verification)
    val verificationDocumentUri: Uri? = null,
    val verificationDocumentName: String? = null,
    val isUploadingDocument: Boolean = false,
    val documentUploadError: String? = null,

    // Draft
    val hasSavedDraft: Boolean = false,
    val showDraftRecoveryDialog: Boolean = false,

    // General
    val isSaving: Boolean = false,
    val error: String? = null,
    val addressLat: Double? = null,
    val addressLng: Double? = null,
)
