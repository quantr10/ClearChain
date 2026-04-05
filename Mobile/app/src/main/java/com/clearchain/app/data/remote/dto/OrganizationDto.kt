package com.clearchain.app.data.remote.dto

import android.annotation.SuppressLint
import kotlinx.serialization.Serializable

// NGO stats: inStock, activeRequests, distributed, availableFood
// Grocery stats: activeListings, pendingRequests, completed, foodSaved
@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class DashboardStatsData(
    val inStock: Int = 0,
    val activeRequests: Int = 0,
    val distributed: Int = 0,
    val availableFood: Int = 0,
    val activeListings: Int = 0,
    val pendingRequests: Int = 0,
    val completed: Int = 0,
    val foodSaved: Int = 0
)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class DashboardStatsResponse(
    val data: DashboardStatsData
)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class UpdateProfileRequest(
    val name: String,
    val phone: String?,
    val address: String?,
    val location: String?,
    val hours: String?,
    // ═══ NEW FIELDS (Part 1) ═══
    val latitude: Double? = null,
    val longitude: Double? = null,
    val contactPerson: String? = null,
    val pickupInstructions: String? = null,
    val description: String? = null
)