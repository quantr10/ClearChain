package com.clearchain.app.domain.model

import android.annotation.SuppressLint
import androidx.annotation.StringRes
import com.clearchain.app.R
import kotlinx.serialization.Serializable

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class PickupRequest(
    val id: String,
    val listingId: String,
    val ngoId: String,
    val ngoName: String,
    // Not persisted in the Room cache: offline the party card falls back to the
    // initial anyway, since the image itself needs the network.
    val ngoProfilePictureUrl: String? = null,
    val groceryId: String,
    val groceryName: String,
    val groceryProfilePictureUrl: String? = null,
    val status: PickupRequestStatus,
    val requestedQuantity: Int,
    val pickupDate: String,
    val pickupTime: String,
    val notes: String? = null,
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
    val items: List<PickupRequestItem> = emptyList()
)

@Serializable
data class PickupRequestItem(
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

@Serializable
enum class PickupRequestStatus(@StringRes val labelResId: Int) {
    PENDING(R.string.status_pending),
    APPROVED(R.string.status_approved),
    READY(R.string.status_ready),
    COMPLETED(R.string.status_completed),
    CANCELLED(R.string.status_cancelled),
    REJECTED(R.string.status_rejected)
}

fun PickupRequestStatus.displayName(): String {
    return when (this) {
        PickupRequestStatus.PENDING -> "Pending"
        PickupRequestStatus.APPROVED -> "Approved"
        PickupRequestStatus.READY -> "Ready"
        PickupRequestStatus.COMPLETED -> "Completed"
        PickupRequestStatus.CANCELLED -> "Cancelled"
        PickupRequestStatus.REJECTED -> "Rejected"
    }
}
