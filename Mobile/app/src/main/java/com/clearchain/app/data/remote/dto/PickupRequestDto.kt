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
    val notes: String? = null,
    val vehicleType: String? = null,
    val requiresRefrigeration: Boolean = false,
    val isFragile: Boolean = false,
    val isHeavy: Boolean = false
)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class UpdatePickupRequestStatusRequest(
    val status: String
)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class BulkActionRequest(val ids: List<String>)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class BulkRejectRequest(val ids: List<String>, val reason: String? = null)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class BulkActionResponse(val message: String, val succeeded: Int = 0, val failed: Int = 0)

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
    val data: List<PickupRequestData>,
    val total: Int = 0,
    val page: Int = 1,
    val pageSize: Int = 20,
    val totalPages: Int = 1
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
    val listingExpiryDate: String? = null,
    val listingUnit: String = "",
    val createdAt: String,
    val proofPhotoUrl: String? = null,
    val markedReadyAt: String? = null,
    val markedPickedUpAt: String? = null,
    val confirmedReceivedAt: String? = null,
    val vehicleType: String? = null,
    val requiresRefrigeration: Boolean = false,
    val isFragile: Boolean = false,
    val isHeavy: Boolean = false,
    val listingDescription: String? = null
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
            "pending"   -> PickupRequestStatus.PENDING
            "approved"  -> PickupRequestStatus.APPROVED
            "ready"     -> PickupRequestStatus.READY
            "completed" -> PickupRequestStatus.COMPLETED
            "cancelled" -> PickupRequestStatus.CANCELLED
            "rejected"  -> PickupRequestStatus.REJECTED
            else        -> PickupRequestStatus.PENDING
        },
        requestedQuantity = requestedQuantity,
        pickupDate = pickupDate,
        pickupTime = pickupTime,
        notes = notes,
        listingTitle = listingTitle,
        listingCategory = listingCategory,
        listingExpiryDate = listingExpiryDate,
        listingUnit = listingUnit,
        createdAt = createdAt,
        proofPhotoUrl = proofPhotoUrl,
        markedReadyAt = markedReadyAt,
        markedPickedUpAt = markedPickedUpAt,
        confirmedReceivedAt = confirmedReceivedAt,
        vehicleType = vehicleType,
        requiresRefrigeration = requiresRefrigeration,
        isFragile = isFragile,
        isHeavy = isHeavy,
        listingDescription = listingDescription
    )
}