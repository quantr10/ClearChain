package com.clearchain.app.domain.model

data class AdminStats(
    // Organization stats
    val totalOrganizations: Int,
    val totalGroceries: Int,
    val totalNgos: Int,

    // Listing stats
    val activeListings: Int,
    val reservedListings: Int,
    val expiredListings: Int,

    // Pickup request stats
    val totalPickupRequests: Int,
    val pendingRequests: Int,
    val approvedRequests: Int,
    val readyRequests: Int,
    val completedRequests: Int,
    val cancelledRequests: Int
)
