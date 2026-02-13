package com.clearchain.app.domain.model

import android.annotation.SuppressLint
import kotlinx.serialization.Serializable

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class PickupRequest(
    val id: String,
    val listingId: String,
    val ngoId: String,
    val ngoName: String,
    val groceryId: String,
    val groceryName: String,
    val status: PickupRequestStatus,
    val requestedQuantity: Int,
    val pickupDate: String,
    val pickupTime: String,
    val notes: String? = null,
    val listingTitle: String,
    val listingCategory: String,
    val createdAt: String
)

@Serializable
enum class PickupRequestStatus {
    PENDING,
    APPROVED,
    REJECTED,
    COMPLETED,
    CANCELLED
}

fun PickupRequestStatus.displayName(): String {
    return when (this) {
        PickupRequestStatus.PENDING -> "Pending"
        PickupRequestStatus.APPROVED -> "Approved"
        PickupRequestStatus.REJECTED -> "Rejected"
        PickupRequestStatus.COMPLETED -> "Completed"
        PickupRequestStatus.CANCELLED -> "Cancelled"
    }
}