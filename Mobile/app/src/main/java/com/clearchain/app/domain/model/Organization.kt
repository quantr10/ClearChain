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
    val hours: String? = null,
    val profilePictureUrl: String? = null,
    val createdAt: String,
    // ═══ NEW FIELDS (Part 1) ═══
    val latitude: Double? = null,
    val longitude: Double? = null,
    val contactPerson: String? = null,
    val pickupInstructions: String? = null,
    val description: String? = null
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

    fun getMissingFields(): List<String> {
        val missing = mutableListOf<String>()
        if (phone.isBlank()) missing.add("Phone")
        if (address.isBlank()) missing.add("Address")
        if (location.isBlank()) missing.add("City/Location")
        if (hours.isNullOrBlank()) missing.add("Operating Hours")
        if ((type == OrganizationType.NGO || type == OrganizationType.GROCERY) && contactPerson.isNullOrBlank()) {
            missing.add("Contact Person")
        }
        return missing
    }
}

@Serializable
enum class OrganizationType { GROCERY, NGO, ADMIN }

@Serializable
enum class VerificationStatus { PENDING, APPROVED, REJECTED }