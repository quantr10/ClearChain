package com.clearchain.app.domain.model

data class AdminStats(
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