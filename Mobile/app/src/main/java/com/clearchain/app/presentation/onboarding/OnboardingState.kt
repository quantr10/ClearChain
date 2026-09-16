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
    val state: String = "",
    val zipCode: String = "",
    val openTime: String = "",
    val closeTime: String = "",
    val pickupInstructions: String = "",
    val addressError: String? = null,
    val cityError: String? = null,
    val stateError: String? = null,
    val zipCodeError: String? = null,
    val openTimeError: String? = null,
    val closeTimeError: String? = null,

    // Document upload (verification) — required, at least one document
    val verificationDocumentUri: Uri? = null,
    val verificationDocumentName: String? = null,
    val isUploadingDocument: Boolean = false,
    val documentUploadError: String? = null,
    val uploadedDocumentUrl: String? = null,

    // General
    val isSaving: Boolean = false,
    val error: String? = null,
    val addressLat: Double? = null,
    val addressLng: Double? = null
) {
    val canContinueStep1: Boolean
        get() = phone.isNotBlank() &&
            (userType !in listOf(OrganizationType.NGO, OrganizationType.GROCERY) || contactPerson.isNotBlank())

    val canContinueStep2: Boolean
        get() = address.isNotBlank() &&
            city.isNotBlank() &&
            state.isNotBlank() &&
            zipCode.isNotBlank() &&
            openTime.isNotBlank() &&
            closeTime.isNotBlank() &&
            uploadedDocumentUrl != null &&
            !isUploadingDocument
}
