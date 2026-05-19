package com.clearchain.app.domain.model

data class OrgStats(
    // NGO fields
    val inStock: Int = 0,
    val activeRequests: Int = 0,
    val distributed: Int = 0,
    val availableFood: Int = 0,
    val totalCompleted: Int = 0,
    // Grocery fields
    val activeListings: Int = 0,
    val pendingRequests: Int = 0,
    val completed: Int = 0,
    val foodSaved: Int = 0,
    val totalListings: Int = 0
)

data class ActivityItem(
    val id: String,
    /** One of: listing_created | pickup_request | pickup_approved |
     *  pickup_ready | pickup_completed | pickup_cancelled | inventory_received */
    val type: String,
    val title: String,
    val subtitle: String,
    val timestamp: String,
    val relatedId: String?
)
