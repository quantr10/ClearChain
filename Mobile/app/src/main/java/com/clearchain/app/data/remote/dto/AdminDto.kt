package com.clearchain.app.data.remote.dto

import android.annotation.SuppressLint
import com.clearchain.app.domain.model.AdminStats
import kotlinx.serialization.Serializable

// ─── Organization DTOs ─────────────────────────────────────────────────────────

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class AdminOrganizationDto(
    val id: String,
    val name: String,
    val email: String,
    val type: String,
    val phone: String,
    val address: String,
    val location: String,
    val verified: Boolean,
    val verificationStatus: String,
    val createdAt: String
)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class OrganizationListResponse(
    val message: String,
    val data: List<AdminOrganizationDto>
)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class OrganizationResponse(
    val message: String,
    val data: AdminOrganizationDto
)

// ─── Stats DTO ────────────────────────────────────────────────────────────────

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class AdminStatsDto(
    // Organization stats
    val totalOrganizations: Int,
    val totalGroceries: Int,
    val totalNgos: Int,
    val verifiedOrganizations: Int,
    val unverifiedOrganizations: Int,

    // Listing stats
    val totalListings: Int,
    val activeListings: Int,
    val reservedListings: Int,

    // Pickup request stats
    val totalPickupRequests: Int,
    val pendingRequests: Int,
    val approvedRequests: Int,
    val readyRequests: Int,
    val rejectedRequests: Int,
    val completedRequests: Int,
    val cancelledRequests: Int,

    // Food saved
    val totalFoodSaved: Double
)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class AdminStatsResponse(
    val message: String,
    val data: AdminStatsDto
)

// ─── Extension: DTO → Domain ──────────────────────────────────────────────────

fun AdminStatsDto.toDomain(): AdminStats {
    return AdminStats(
        totalOrganizations    = totalOrganizations,
        totalGroceries        = totalGroceries,
        totalNgos             = totalNgos,
        verifiedOrganizations = verifiedOrganizations,
        unverifiedOrganizations = unverifiedOrganizations,
        totalListings         = totalListings,
        activeListings        = activeListings,
        reservedListings      = reservedListings,
        totalPickupRequests   = totalPickupRequests,
        pendingRequests       = pendingRequests,
        approvedRequests      = approvedRequests,
        readyRequests         = readyRequests,
        rejectedRequests      = rejectedRequests,
        completedRequests     = completedRequests,
        cancelledRequests     = cancelledRequests,
        totalFoodSaved        = totalFoodSaved
    )
}