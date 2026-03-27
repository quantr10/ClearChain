package com.clearchain.app.data.remote.dto

import android.annotation.SuppressLint
import com.clearchain.app.domain.model.Organization
import com.clearchain.app.domain.model.OrganizationType
import com.clearchain.app.domain.model.VerificationStatus
import com.clearchain.app.domain.model.AuthTokens
import kotlinx.serialization.Serializable

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class LoginRequest(
    val email: String,
    val password: String,
    val deviceToken: String? = null
)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class RegisterRequest(
    val name: String,
    val type: String,
    val email: String,
    val password: String,
    val fcmToken: String? = null
)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class AuthResponse(
    val message: String,
    val data: AuthData
)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class MeResponse(
    val data: AuthData
)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class AuthData(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String,
    val expiresIn: Int,
    val user: OrganizationDto
)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class OrganizationDto(
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
    val createdAt: String,
    // ═══ NEW FIELDS (Part 1) ═══
    val latitude: Double? = null,
    val longitude: Double? = null,
    val contactPerson: String? = null,
    val pickupInstructions: String? = null,
    val description: String? = null
)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class RefreshTokenRequest(val refreshToken: String)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class ChangePasswordRequest(val currentPassword: String, val newPassword: String)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class MessageResponse(val message: String)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class RegisterFCMTokenRequest(val fcmToken: String)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class ApiResponse<T>(val message: String? = null, val data: T? = null)

// ═══ UPDATED toDomain() includes new fields (Part 1) ═══
fun OrganizationDto.toDomain(): Organization {
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
        hours = hours, profilePictureUrl = profilePictureUrl, createdAt = createdAt,
        latitude = latitude, longitude = longitude,
        contactPerson = contactPerson, pickupInstructions = pickupInstructions,
        description = description
    )
}

fun AuthData.toDomain(): Pair<Organization, AuthTokens> {
    val organization = user.toDomain()
    val tokens = AuthTokens(
        accessToken = accessToken, refreshToken = refreshToken,
        expiresIn = expiresIn, tokenType = tokenType
    )
    return Pair(organization, tokens)
}