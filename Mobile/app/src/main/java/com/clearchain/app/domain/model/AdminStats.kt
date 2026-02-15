package com.clearchain.app.domain.model

data class AdminStats(
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