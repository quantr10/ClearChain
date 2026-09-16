package com.clearchain.app.domain.model

import android.annotation.SuppressLint
import kotlinx.serialization.Serializable

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class Organization(
    val id: String,
    val name: String,
    val type: OrganizationType,
    val email: String,
    val phone: String,
    val address: String,
    val location: String,
    val verified: Boolean,
    val verificationStatus: VerificationStatus,
    val verificationNotes: String? = null,
    val hours: String? = null,
    val profilePictureUrl: String? = null,
    val createdAt: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val state: String? = null,
    val zipCode: String? = null,
    val contactPerson: String? = null,
    val pickupInstructions: String? = null,
    val description: String? = null,
    // ── Document fields ──────────────────────────────────────────────────────
    val documentUrl: String? = null,
    val documentMimeType: String? = null
) {
    fun isProfileComplete(): Boolean {
        val commonComplete = phone.isNotBlank() &&
            address.isNotBlank() &&
            location.isNotBlank() &&
            !hours.isNullOrBlank()

        return when (type) {
            OrganizationType.NGO -> commonComplete && !contactPerson.isNullOrBlank()
            OrganizationType.GROCERY -> commonComplete && !contactPerson.isNullOrBlank()
            OrganizationType.ADMIN -> true
        }
    }

    /**
     * True when the org has finished onboarding but an admin has not approved it yet
     * (pending or rejected). Such orgs are held on the PendingReview screen and blocked
     * from creating listings / pickup requests. Admins are never gated.
     */
    fun requiresVerificationGate(): Boolean =
        type != OrganizationType.ADMIN && verificationStatus != VerificationStatus.APPROVED

}

@Serializable
enum class OrganizationType { GROCERY, NGO, ADMIN }

@Serializable
enum class VerificationStatus { PENDING, APPROVED, REJECTED }
