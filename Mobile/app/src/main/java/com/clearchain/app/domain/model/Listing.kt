package com.clearchain.app.domain.model

import android.annotation.SuppressLint
import androidx.annotation.StringRes
import com.clearchain.app.R
import kotlinx.serialization.Serializable

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class Listing(
    val id: String,
    val groceryId: String,
    val groceryName: String,
    val title: String,
    val description: String,
    val category: FoodCategory,
    val quantity: Int,
    val unit: String,
    val expiryDate: String,
    val pickupTimeStart: String,
    val pickupTimeEnd: String,
    val status: ListingStatus,
    val imageUrl: String? = null,
    val location: String,
    val createdAt: String,
    
    // ListingGroup tracking fields
    val groupId: String? = null,
    val splitReason: String = "new_listing",
    val relatedRequestId: String? = null,
    val splitIndex: Int = 0,
    
    // OPTIONAL: Group summary for UI context
    val groupSummary: ListingGroupSummary? = null,

    // ═══ NEW (Part 2): Distance from NGO location ═══
    val distanceKm: Double? = null,

    // ═══ Analytics + Archive ═══
    val viewCount: Int = 0,
    val requestCount: Int = 0,
    val isArchived: Boolean = false,
    val archivedAt: String? = null,
    val imageUrls: List<String> = emptyList(),

    // Grocery store coordinates for map pins
    val groceryLatitude: Double? = null,
    val groceryLongitude: Double? = null,

    // Operating hours — live from grocery's profile (not stored on listing)
    val groceryHours: String? = null
)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class ListingGroupSummary(
    val groupId: String,
    val originalQuantity: Int,
    val totalReserved: Int,
    val totalAvailable: Int,
    val childListingsCount: Int
)

@Serializable
enum class FoodCategory(@StringRes val labelResId: Int) {
    FRUITS(R.string.category_fruits),
    VEGETABLES(R.string.category_vegetables),
    DAIRY(R.string.category_dairy),
    BAKERY(R.string.category_bakery),
    MEAT(R.string.category_meat),
    SEAFOOD(R.string.category_seafood),
    PACKAGED(R.string.category_packaged),
    BEVERAGES(R.string.category_beverages),
    OTHER(R.string.category_other)
}

@Serializable
enum class ListingStatus(@StringRes val labelResId: Int) {
    AVAILABLE(R.string.status_listing_available),
    RESERVED(R.string.status_listing_reserved),
    COMPLETED(R.string.status_listing_completed),
    EXPIRED(R.string.status_listing_expired)
}

fun FoodCategory.displayName(): String {
    return when (this) {
        FoodCategory.FRUITS -> "Fruits"
        FoodCategory.VEGETABLES -> "Vegetables"
        FoodCategory.DAIRY -> "Dairy"
        FoodCategory.BAKERY -> "Bakery"
        FoodCategory.MEAT -> "Meat"
        FoodCategory.SEAFOOD -> "Seafood"
        FoodCategory.PACKAGED -> "Packaged Foods"
        FoodCategory.BEVERAGES -> "Beverages"
        FoodCategory.OTHER -> "Other"
    }
}

fun ListingStatus.displayName(): String {
    return when (this) {
        ListingStatus.AVAILABLE -> "Available"
        ListingStatus.RESERVED -> "Reserved"
        ListingStatus.COMPLETED -> "Completed"
        ListingStatus.EXPIRED -> "Expired"
    }
}