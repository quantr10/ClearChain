package com.clearchain.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.clearchain.app.domain.model.Organization
import com.clearchain.app.domain.model.OrganizationType
import com.clearchain.app.domain.model.VerificationStatus

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val name: String,
    val type: String,
    val email: String,
    val phone: String,
    val address: String,
    val location: String,
    val verified: Boolean,
    val verificationStatus: String,
    val verificationNotes: String? = null,
    val hours: String? = null,
    val profilePictureUrl: String? = null,
    val createdAt: String,
    // ═══ NEW FIELDS (Part 1) ═══
    val latitude: Double? = null,
    val longitude: Double? = null,
    val state: String? = null,
    val zipCode: String? = null,
    val contactPerson: String? = null,
    val pickupInstructions: String? = null,
    val description: String? = null,
    // ═══ Onboarding verification document ═══
    val documentUrl: String? = null,
    // Deprecated: the app only supports a single verification document. Column kept
    // (always null) so no Room migration is needed; do not read or write it.
    val documentUrl2: String? = null
)

// CANONICAL mapping functions — used everywhere, no duplicates
fun UserEntity.toDomain(): Organization {
    return Organization(
        id = id, name = name,
        type = when (type.lowercase()) {
            "grocery" -> OrganizationType.GROCERY
            "ngo" -> OrganizationType.NGO
            "admin" -> OrganizationType.ADMIN
            else -> OrganizationType.GROCERY
        },
        email = email, phone = phone, address = address, location = location,
        verified = verified,
        verificationStatus = when (verificationStatus.lowercase()) {
            "approved" -> VerificationStatus.APPROVED
            "rejected" -> VerificationStatus.REJECTED
            else -> VerificationStatus.PENDING
        },
        verificationNotes = verificationNotes,
        hours = hours, profilePictureUrl = profilePictureUrl, createdAt = createdAt,
        latitude = latitude, longitude = longitude,
        state = state, zipCode = zipCode,
        contactPerson = contactPerson, pickupInstructions = pickupInstructions,
        description = description,
        documentUrl = documentUrl
    )
}

fun Organization.toEntity(): UserEntity {
    return UserEntity(
        id = id, name = name, type = type.name.lowercase(),
        email = email, phone = phone, address = address, location = location,
        verified = verified, verificationStatus = verificationStatus.name.lowercase(),
        verificationNotes = verificationNotes,
        hours = hours, profilePictureUrl = profilePictureUrl, createdAt = createdAt,
        latitude = latitude, longitude = longitude,
        state = state, zipCode = zipCode,
        contactPerson = contactPerson, pickupInstructions = pickupInstructions,
        description = description,
        documentUrl = documentUrl
    )
}
