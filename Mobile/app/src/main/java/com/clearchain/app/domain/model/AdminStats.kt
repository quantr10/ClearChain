package com.clearchain.app.domain.model

data class AdminStats(
    // Organization stats
    val totalOrganizations: Int,
    val totalGroceries: Int,
    val totalNgos: Int,
    /** Of [totalGroceries] and [totalNgos], those still awaiting verification. */
    val pendingGroceries: Int = 0,
    val pendingNgos: Int = 0,

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
