package com.clearchain.app.domain.model

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
