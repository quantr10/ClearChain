package com.clearchain.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.clearchain.app.domain.model.Organization
import com.clearchain.app.domain.model.OrganizationType
import com.clearchain.app.domain.model.VerificationStatus

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val type: String,
    val email: String,
    val phone: String,
    val address: String,
    val location: String,
    val verified: Boolean,
    val verificationStatus: String,
    val hours: String? = null,
    val profilePictureUrl: String? = null,
    val createdAt: String
)

// Extension functions
fun UserEntity.toDomain(): Organization {
    return Organization(
        id = id,
        name = name,
        type = when (type.lowercase()) {
            "grocery" -> OrganizationType.GROCERY
            "ngo" -> OrganizationType.NGO
            "admin" -> OrganizationType.ADMIN
            else -> OrganizationType.GROCERY
        },
        email = email,
        phone = phone,
        address = address,
        location = location,
        verified = verified,
        verificationStatus = when (verificationStatus.lowercase()) {
            "approved" -> VerificationStatus.APPROVED
            "rejected" -> VerificationStatus.REJECTED
            else -> VerificationStatus.PENDING
        },
        hours = hours,
        profilePictureUrl = profilePictureUrl,
        createdAt = createdAt
    )
}

fun Organization.toEntity(): UserEntity {
    return UserEntity(
        id = id,
        name = name,
        type = type.name.lowercase(),
        email = email,
        phone = phone,
        address = address,
        location = location,
        verified = verified,
        verificationStatus = verificationStatus.name.lowercase(),
        hours = hours,
        profilePictureUrl = profilePictureUrl,
        createdAt = createdAt
    )
}