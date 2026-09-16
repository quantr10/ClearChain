package com.clearchain.app.data.remote.dto

import android.annotation.SuppressLint
import com.clearchain.app.domain.model.AuthTokens
import com.clearchain.app.domain.model.Organization
import com.clearchain.app.domain.model.OrganizationType
import com.clearchain.app.domain.model.VerificationStatus
import kotlinx.serialization.Serializable

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class LoginRequest(
    val email: String,
    val password: String
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
    val data: MeData
)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class MeData(
    val user: OrganizationDto
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
    // ── Onboarding verification document ─────────────────────────────────────
    val documentUrl: String? = null
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
data class VerifyEmailRequest(val email: String, val code: String)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class ResendVerificationRequest(val email: String)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class ApiResponse<T>(val message: String? = null, val data: T? = null)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class EmailAvailabilityResponse(val available: Boolean, val message: String? = null)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class DeleteAccountRequest(val password: String)

// ── toDomain() includes new fields ───────────────────────────────────────────
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
        verificationNotes = verificationNotes,
        hours = hours, profilePictureUrl = profilePictureUrl, createdAt = createdAt,
        latitude = latitude, longitude = longitude,
        state = state, zipCode = zipCode,
        contactPerson = contactPerson, pickupInstructions = pickupInstructions,
        description = description,
        documentUrl = documentUrl
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
