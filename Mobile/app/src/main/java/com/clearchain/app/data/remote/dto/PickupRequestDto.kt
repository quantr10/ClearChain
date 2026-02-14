package com.clearchain.app.data.remote.dto

import android.annotation.SuppressLint
import com.clearchain.app.domain.model.PickupRequest
import com.clearchain.app.domain.model.PickupRequestStatus
import kotlinx.serialization.Serializable

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class CreatePickupRequestRequest(
    val listingId: String,
    val requestedQuantity: Int,
    val pickupDate: String,
    val pickupTime: String,
    val notes: String? = null
)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class UpdatePickupRequestStatusRequest(
    val status: String
)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class PickupRequestResponse(
    val message: String,
    val data: PickupRequestData
)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class PickupRequestsResponse(
    val message: String,
    val data: List<PickupRequestData>
)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class PickupRequestData(
    val id: String,
    val listingId: String,
    val ngoId: String,
    val ngoName: String,
    val groceryId: String,
    val groceryName: String,
    val status: String,
    val requestedQuantity: Int,
    val pickupDate: String,
    val pickupTime: String,
    val notes: String?,
    val listingTitle: String,
    val listingCategory: String,
    val createdAt: String
)

// Extension function
fun PickupRequestData.toDomain(): PickupRequest {
    return PickupRequest(
        id = id,
        listingId = listingId,
        ngoId = ngoId,
        ngoName = ngoName,
        groceryId = groceryId,
        groceryName = groceryName,
        status = when (status.lowercase()) {
            "pending" -> PickupRequestStatus.PENDING
            "approved" -> PickupRequestStatus.APPROVED
            "rejected" -> PickupRequestStatus.REJECTED
            "completed" -> PickupRequestStatus.COMPLETED
            "cancelled" -> PickupRequestStatus.CANCELLED
            else -> PickupRequestStatus.PENDING
        },
        requestedQuantity = requestedQuantity,
        pickupDate = pickupDate,
        pickupTime = pickupTime,
        notes = notes,
        listingTitle = listingTitle,
        listingCategory = listingCategory,
        createdAt = createdAt
    )
}