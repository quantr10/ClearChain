package com.clearchain.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.clearchain.app.domain.model.PickupRequest
import com.clearchain.app.domain.model.PickupRequestStatus

@Entity(tableName = "pickup_requests")
data class PickupRequestEntity(
    @PrimaryKey val id: String,
    val listingId: String,
    val ngoId: String,
    val ngoName: String,
    val groceryId: String,
    val groceryName: String,
    val status: String,
    val requestedQuantity: Int,
    val pickupDate: String,
    val pickupTime: String,
    val notes: String? = null,
    val listingTitle: String,
    val listingCategory: String,
    val createdAt: String,
    val proofPhotoUrl: String? = null,
    val cancellationReason: String? = null,
    val vehicleType: String? = null,
    val licensePlate: String? = null,
    val requiresRefrigeration: Boolean = false,
    val isFragile: Boolean = false,
    val isHeavy: Boolean = false,
    val cachedAt: Long = System.currentTimeMillis()
)

fun PickupRequestEntity.toDomain(): PickupRequest = PickupRequest(
    id = id,
    listingId = listingId,
    ngoId = ngoId,
    ngoName = ngoName,
    groceryId = groceryId,
    groceryName = groceryName,
    status = runCatching { PickupRequestStatus.valueOf(status.uppercase()) }.getOrDefault(PickupRequestStatus.PENDING),
    requestedQuantity = requestedQuantity,
    pickupDate = pickupDate,
    pickupTime = pickupTime,
    notes = notes,
    listingTitle = listingTitle,
    listingCategory = listingCategory,
    createdAt = createdAt,
    proofPhotoUrl = proofPhotoUrl,
    vehicleType = vehicleType,
    requiresRefrigeration = requiresRefrigeration,
    isFragile = isFragile,
    isHeavy = isHeavy
)

fun PickupRequest.toEntity(): PickupRequestEntity = PickupRequestEntity(
    id = id,
    listingId = listingId,
    ngoId = ngoId,
    ngoName = ngoName,
    groceryId = groceryId,
    groceryName = groceryName,
    status = status.name.lowercase(),
    requestedQuantity = requestedQuantity,
    pickupDate = pickupDate,
    pickupTime = pickupTime,
    notes = notes,
    listingTitle = listingTitle,
    listingCategory = listingCategory,
    createdAt = createdAt,
    proofPhotoUrl = proofPhotoUrl,
    vehicleType = vehicleType,
    requiresRefrigeration = requiresRefrigeration,
    isFragile = isFragile,
    isHeavy = isHeavy
)
