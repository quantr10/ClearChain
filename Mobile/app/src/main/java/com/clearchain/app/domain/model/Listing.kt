package com.clearchain.app.domain.model

import android.annotation.SuppressLint
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
    val createdAt: String
)

@Serializable
enum class FoodCategory {
    FRUITS,
    VEGETABLES,
    DAIRY,
    BAKERY,
    MEAT,
    SEAFOOD,
    PACKAGED,
    BEVERAGES,
    OTHER
}

@Serializable
enum class ListingStatus {
    AVAILABLE,
    RESERVED,
    COMPLETED,
    EXPIRED
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