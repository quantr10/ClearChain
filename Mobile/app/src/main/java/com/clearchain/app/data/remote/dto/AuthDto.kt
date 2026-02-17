package com.clearchain.app.data.remote.dto

import android.annotation.SuppressLint
import com.clearchain.app.domain.model.Organization
import com.clearchain.app.domain.model.AuthTokens
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

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
    val phone: String,
    val address: String,
    val location: String,
    val hours: String? = null
)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class AuthResponse(
    val message: String,
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
    val createdAt: String
)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class RefreshTokenRequest(
    val refreshToken: String
)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class ChangePasswordRequest(
    val currentPassword: String,
    val newPassword: String
)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class MessageResponse(
    val message: String
)

// Extension functions to convert DTOs to Domain models
fun OrganizationDto.toDomain(): Organization {
    return Organization(
        id = id,
        name = name,
        type = when (type.lowercase()) {
            "grocery" -> com.clearchain.app.domain.model.OrganizationType.GROCERY
            "ngo" -> com.clearchain.app.domain.model.OrganizationType.NGO
            "admin" -> com.clearchain.app.domain.model.OrganizationType.ADMIN
            else -> com.clearchain.app.domain.model.OrganizationType.GROCERY
        },
        email = email,
        phone = phone,
        address = address,
        location = location,
        verified = verified,
        verificationStatus = when (verificationStatus.lowercase()) {
            "approved" -> com.clearchain.app.domain.model.VerificationStatus.APPROVED
            "rejected" -> com.clearchain.app.domain.model.VerificationStatus.REJECTED
            else -> com.clearchain.app.domain.model.VerificationStatus.PENDING
        },
        hours = hours,
        profilePictureUrl = profilePictureUrl,
        createdAt = createdAt
    )
}

fun AuthData.toDomain(): Pair<Organization, AuthTokens> {
    val organization = user.toDomain()
    val tokens = AuthTokens(
        accessToken = accessToken,
        refreshToken = refreshToken,
        expiresIn = expiresIn,
        tokenType = tokenType
    )
    return Pair(organization, tokens)
}