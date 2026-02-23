package com.clearchain.app.data.remote.dto

import android.annotation.SuppressLint
import com.clearchain.app.domain.model.ListingGroup
import kotlinx.serialization.Serializable

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class ListingGroupData(
    val id: String,
    val originalListingId: String,
    val groceryId: String,
    val productName: String,
    val category: String,
    val unit: String,
    val originalQuantity: Int,
    val totalReserved: Int,
    val totalAvailable: Int,
    val totalCompleted: Int,
    val isFullyConsumed: Boolean,
    val createdAt: String
)

fun ListingGroupData.toDomain(): ListingGroup {
    return ListingGroup(
        id = id,
        originalListingId = originalListingId,
        groceryId = groceryId,
        productName = productName,
        category = category,
        unit = unit,
        originalQuantity = originalQuantity,
        totalReserved = totalReserved,
        totalAvailable = totalAvailable,
        totalCompleted = totalCompleted,
        isFullyConsumed = isFullyConsumed,
        createdAt = createdAt
    )
}