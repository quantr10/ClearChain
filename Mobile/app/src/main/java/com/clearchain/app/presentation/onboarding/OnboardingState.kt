package com.clearchain.app.presentation.onboarding

import com.clearchain.app.domain.model.OrganizationType

data class OnboardingState(
    val currentStep: Int = 1,           // 1, 2, or 3
    val totalSteps: Int = 3,
    val userType: OrganizationType = OrganizationType.GROCERY,
    val userName: String = "",

    // Step 1 fields
    val phone: String = "",
    val description: String = "",
    val contactPerson: String = "",     // NGO only
    val phoneError: String? = null,
    val contactPersonError: String? = null,

    // Step 2 fields
    val address: String = "",
    val city: String = "",              // maps to Organization.location
    val hours: String = "",
    val pickupInstructions: String = "", // Grocery only
    val addressError: String? = null,
    val cityError: String? = null,

    // General
    val isSaving: Boolean = false,
    val error: String? = null
)