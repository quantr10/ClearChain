package com.clearchain.app.data.remote.dto

import android.annotation.SuppressLint
import kotlinx.serialization.Serializable

// NGO stats: inStock, activeRequests, distributed, availableFood, totalCompleted
// Grocery stats: activeListings, pendingRequests, completed, foodSaved, totalListings
@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class DashboardStatsData(
    // NGO
    val inStock: Int = 0,
    val activeRequests: Int = 0,
    val distributed: Int = 0,
    val availableFood: Int = 0,
    val totalCompleted: Int = 0,
    // Grocery
    val activeListings: Int = 0,
    val pendingRequests: Int = 0,
    val completed: Int = 0,
    val foodSaved: Int = 0,
    val totalListings: Int = 0
)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class DashboardStatsResponse(
    val data: DashboardStatsData
)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class ActivityItemData(
    val id: String,
    val type: String,
    val title: String,
    val subtitle: String,
    val timestamp: String,
    val relatedId: String? = null
)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class ActivityResponse(
    val data: List<ActivityItemData>
)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class TodaySummaryData(
    // Grocery
    val expiringToday: Int = 0,
    val pickupsToday: Int = 0,
    val clearedToday: Int = 0,
    val listingsCreatedToday: Int = 0,
    val requestsCreatedToday: Int = 0,
    // NGO
    val distributedToday: Int = 0,
    val upcomingPickups: List<UpcomingPickupData> = emptyList()
)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class UpcomingPickupData(
    val id: String,
    val listingTitle: String,
    val groceryName: String = "",
    val ngoName: String = "",
    val pickupTime: String,
    val status: String
)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class TodaySummaryResponse(val data: TodaySummaryData)

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

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class AvatarUploadData(val url: String)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class AvatarUploadResponse(
    val message: String = "",
    val data: AvatarUploadData? = null
)