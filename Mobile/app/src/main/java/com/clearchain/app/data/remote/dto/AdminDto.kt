package com.clearchain.app.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class OrganizationListResponse(
    val message: String,
    val data: List<OrganizationData>
)

@Serializable
data class OrganizationResponse(
    val message: String,
    val data: OrganizationData
)

@Serializable
data class OrganizationData(
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

@Serializable
data class AdminStatsResponse(
    val message: String,
    val data: AdminStatsData
)

@Serializable
data class AdminStatsData(
    val totalOrganizations: Int,
    val totalGroceries: Int,
    val totalNgos: Int,
    val verifiedOrganizations: Int,
    val unverifiedOrganizations: Int,
    
    val totalListings: Int,
    val activeListings: Int,
    val reservedListings: Int,
    
    val totalPickupRequests: Int,
    val pendingRequests: Int,
    val approvedRequests: Int,
    val readyRequests: Int,        // ✅ ADD
    val rejectedRequests: Int,     // ✅ ADD
    val completedRequests: Int,
    val cancelledRequests: Int,    // ✅ ADD
    
    val totalFoodSaved: Int
)