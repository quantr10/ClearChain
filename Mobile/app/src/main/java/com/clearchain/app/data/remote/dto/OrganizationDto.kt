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
    /** Pickups completed in the last 7 days, counted by the API off the hand-over date. */
    val completedThisWeek: Int = 0,
    // Grocery
    val activeListings: Int = 0,
    val pendingRequests: Int = 0,
    val completed: Int = 0,
    val foodSaved: Int = 0,
    val totalListings: Int = 0,
    // Weighed and converted by the API (QuantityUnits) so every screen quotes the same
    // impact for the same food. Never recompute these on the client.
    val mealsEstimate: Int = 0,
    val co2EstimateKg: Int = 0,
    val requestStatus: RequestStatusCounts = RequestStatusCounts(),
    /** NGO only. */
    val inventoryStatus: InventoryStatusCounts = InventoryStatusCounts(),
    /** Grocery only. */
    val listingStatus: ListingStatusCounts = ListingStatusCounts()
)

/** What an NGO is holding: on the shelf, handed on, or spoiled before it could be. */
@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class InventoryStatusCounts(
    val active: Int = 0,
    val distributed: Int = 0,
    val expired: Int = 0
) {
    val total: Int get() = active + distributed + expired
}

/**
 * A store's listings as they stand. Collected listings are absent by design - the row is
 * deleted on pickup - so these are the ones that have not moved.
 */
@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class ListingStatusCounts(
    val open: Int = 0,
    val reserved: Int = 0,
    val expired: Int = 0,
    val archived: Int = 0
) {
    val total: Int get() = open + reserved + expired + archived
}

/** Where this organization's pickup requests stand; the six buckets add up to the total. */
@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class RequestStatusCounts(
    val pending: Int = 0,
    val approved: Int = 0,
    val ready: Int = 0,
    val completed: Int = 0,
    val cancelled: Int = 0,
    val rejected: Int = 0
) {
    val total: Int get() = pending + approved + ready + completed + cancelled + rejected
}

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
    val email: String? = null,
    val phone: String?,
    val address: String?,
    val location: String?,
    val state: String?,
    val zipCode: String?,
    val hours: String?,
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

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class DocumentUploadData(val url: String)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class DocumentUploadResponse(
    val message: String = "",
    val data: DocumentUploadData? = null
)
