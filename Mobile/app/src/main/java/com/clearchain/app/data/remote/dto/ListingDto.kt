package com.clearchain.app.data.remote.dto

import android.annotation.SuppressLint
import com.clearchain.app.domain.model.FoodCategory
import com.clearchain.app.domain.model.Listing
import com.clearchain.app.domain.model.ListingStatus
import kotlinx.serialization.Serializable

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class CreateListingRequest(
    val title: String,
    val description: String,
    val category: String,
    val quantity: Int,
    val unit: String,
    val expiryDate: String,
    val pickupTimeStart: String,
    val pickupTimeEnd: String,
    val imageUrl: String? = null
)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class ListingResponse(
    val message: String,
    val data: ListingData
)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class ListingsResponse(
    val message: String,
    val data: List<ListingData>
)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class ListingData(
    val id: String,
    val groceryId: String,
    val groceryName: String,
    val title: String,
    val description: String,
    val category: String,
    val quantity: Int,
    val unit: String,
    val expiryDate: String,
    val pickupTimeStart: String,
    val pickupTimeEnd: String,
    val status: String,
    val imageUrl: String? = null,
    val location: String,
    val createdAt: String
)

// Extension function to convert DTO to Domain model
// Extension function to convert DTO to Domain model
fun ListingData.toDomain(): Listing {
    return Listing(
        id = id,
        groceryId = groceryId,
        groceryName = groceryName,
        title = title,
        description = description,
        category = when (category.uppercase()) {
            "FRUITS" -> FoodCategory.FRUITS
            "VEGETABLES" -> FoodCategory.VEGETABLES
            "DAIRY" -> FoodCategory.DAIRY
            "BAKERY" -> FoodCategory.BAKERY
            "MEAT" -> FoodCategory.MEAT
            "SEAFOOD" -> FoodCategory.SEAFOOD
            "PACKAGED" -> FoodCategory.PACKAGED
            "BEVERAGES" -> FoodCategory.BEVERAGES
            else -> FoodCategory.OTHER
        },
        quantity = quantity,
        unit = unit,
        expiryDate = expiryDate,
        pickupTimeStart = pickupTimeStart,
        pickupTimeEnd = pickupTimeEnd,
        status = when (status.lowercase()) {
            "open" -> ListingStatus.AVAILABLE
            "reserved" -> ListingStatus.RESERVED
            "completed" -> ListingStatus.COMPLETED
            "expired" -> ListingStatus.EXPIRED
            else -> ListingStatus.AVAILABLE
        },
        imageUrl = imageUrl,
        location = location,
        createdAt = createdAt
    )
}