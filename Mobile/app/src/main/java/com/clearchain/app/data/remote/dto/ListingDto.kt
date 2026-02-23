package com.clearchain.app.data.remote.dto

import android.annotation.SuppressLint
import com.clearchain.app.domain.model.*
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
    val createdAt: String,
    
    // NEW: ListingGroup tracking
    val groupId: String? = null,
    val splitReason: String = "new_listing",
    val relatedRequestId: String? = null,
    val splitIndex: Int = 0,
    val groupSummary: ListingGroupSummaryDto? = null
)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class ListingGroupSummaryDto(
    val groupId: String,
    val originalQuantity: Int,
    val totalReserved: Int,
    val totalAvailable: Int,
    val childListingsCount: Int
)

// data/remote/dto/ListingDto.kt - Add this:

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class UpdateListingQuantityRequest(
    val newQuantity: Int
)

// Updated mapping function
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
        createdAt = createdAt,
        
        // NEW: Map group fields
        groupId = groupId,
        splitReason = splitReason,
        relatedRequestId = relatedRequestId,
        splitIndex = splitIndex,
        groupSummary = groupSummary?.let {
            ListingGroupSummary(
                groupId = it.groupId,
                originalQuantity = it.originalQuantity,
                totalReserved = it.totalReserved,
                totalAvailable = it.totalAvailable,
                childListingsCount = it.childListingsCount
            )
        }
    )
}