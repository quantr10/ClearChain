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
    val createdAt: String
)

@Serializable
enum class OrganizationType {
    GROCERY,
    NGO,
    ADMIN
}

@Serializable
enum class VerificationStatus {
    PENDING,
    APPROVED,
    REJECTED
}