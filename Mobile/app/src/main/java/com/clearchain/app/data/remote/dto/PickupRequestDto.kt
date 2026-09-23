package com.clearchain.app.data.remote.dto

import android.annotation.SuppressLint
import com.clearchain.app.domain.model.PickupRequest
import com.clearchain.app.domain.model.PickupRequestItem
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
    val requiresRefrigeration: Boolean = false,
    val isFragile: Boolean = false,
    val isHeavy: Boolean = false
)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class BulkActionRequest(val ids: List<String>)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class BulkRejectRequest(val ids: List<String>, val reason: String? = null)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class BulkActionResultItem(val id: String, val success: Boolean, val message: String)

// The backend returns { message, results: [{id, success, message}] } — this used to declare
// top-level succeeded/failed counts that don't exist in that shape, so they silently defaulted
// to 0 and every bulk approve/reject reported "0 requests" regardless of the real outcome.
@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class BulkActionResponse(val message: String, val results: List<BulkActionResultItem> = emptyList())

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
    val ngoProfilePictureUrl: String? = null,
    val groceryId: String,
    val groceryName: String,
    val groceryProfilePictureUrl: String? = null,
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
    val requiresRefrigeration: Boolean = false,
    val isFragile: Boolean = false,
    val isHeavy: Boolean = false,
    val listingDescription: String? = null,
    val groceryLocation: String? = null,
    val distanceKm: Double? = null,
    val items: List<PickupRequestItemData> = emptyList()
)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class PickupRequestItemData(
    val id: String,
    val listingGroupId: String? = null,
    val originalListingId: String? = null,
    val reservedListingId: String? = null,
    val requestedQuantity: Int,
    val listingTitle: String,
    val listingCategory: String,
    val listingExpiryDate: String? = null,
    val listingUnit: String = "",
    val listingPhotoUrl: String? = null
)

// Extension function
fun PickupRequestData.toDomain(): PickupRequest {
    return PickupRequest(
        id = id,
        listingId = listingId,
        ngoId = ngoId,
        ngoName = ngoName,
        ngoProfilePictureUrl = ngoProfilePictureUrl,
        groceryId = groceryId,
        groceryName = groceryName,
        groceryProfilePictureUrl = groceryProfilePictureUrl,
        status = when (status.lowercase()) {
            "pending" -> PickupRequestStatus.PENDING
            "approved" -> PickupRequestStatus.APPROVED
            "ready" -> PickupRequestStatus.READY
            "completed" -> PickupRequestStatus.COMPLETED
            "cancelled" -> PickupRequestStatus.CANCELLED
            "rejected" -> PickupRequestStatus.REJECTED
            else -> PickupRequestStatus.PENDING
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
        requiresRefrigeration = requiresRefrigeration,
        isFragile = isFragile,
        isHeavy = isHeavy,
        listingDescription = listingDescription,
        groceryLocation = groceryLocation,
        distanceKm = distanceKm,
        items = items.map {
            PickupRequestItem(
                id = it.id,
                listingGroupId = it.listingGroupId,
                originalListingId = it.originalListingId,
                reservedListingId = it.reservedListingId,
                requestedQuantity = it.requestedQuantity,
                listingTitle = it.listingTitle,
                listingCategory = it.listingCategory,
                listingExpiryDate = it.listingExpiryDate,
                listingUnit = it.listingUnit,
                listingPhotoUrl = it.listingPhotoUrl
            )
        }
    )
}
