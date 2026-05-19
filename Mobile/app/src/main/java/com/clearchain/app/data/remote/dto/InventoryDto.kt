package com.clearchain.app.data.remote.dto

import android.annotation.SuppressLint
import com.clearchain.app.domain.model.InventoryItem
import com.clearchain.app.domain.model.InventoryStatus
import kotlinx.serialization.Serializable

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class InventoryListResponse(
    val message: String,
    val data: List<InventoryItemData>
)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class InventoryItemResponse(
    val message: String,
    val data: InventoryItemData
)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class InventoryItemData(
    val id: String,
    val ngoId: String,
    val productName: String,
    val category: String,
    val quantity: Double,
    val unit: String,
    val expiryDate: String,
    val status: String,
    val receivedAt: String,
    val distributedAt: String? = null,
    val pickupRequestId: String? = null,
    val photoUrl: String? = null
)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class UpdateInventoryItemRequest(
    val productName: String,
    val category: String,
    val quantity: Double,
    val unit: String,
    val expiryDate: String
)

// Extension function to convert DTO to Domain
fun InventoryItemData.toDomain(): InventoryItem {
    return InventoryItem(
        id = id,
        productName = productName,
        category = category,
        quantity = quantity,
        unit = unit,
        expiryDate = expiryDate,
        status = when (status.lowercase()) {
            "active" -> InventoryStatus.ACTIVE
            "distributed" -> InventoryStatus.DISTRIBUTED
            "expired" -> InventoryStatus.EXPIRED
            else -> InventoryStatus.ACTIVE
        },
        receivedAt = receivedAt,
        distributedAt = distributedAt,
        pickupRequestId = pickupRequestId,
        photoUrl = photoUrl
    )
}