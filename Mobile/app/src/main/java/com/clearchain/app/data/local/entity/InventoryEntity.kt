package com.clearchain.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.clearchain.app.domain.model.InventoryItem
import com.clearchain.app.domain.model.InventoryStatus

@Entity(tableName = "inventory")
data class InventoryEntity(
    @PrimaryKey val id: String,
    val productName: String,
    val category: String,
    val quantity: Double,
    val unit: String,
    val expiryDate: String,
    val status: String,
    val receivedAt: String,
    val distributedAt: String? = null,
    val pickupRequestId: String? = null,
    val notes: String? = null,
    val photoUrl: String? = null,
    val beneficiaryCount: Int? = null,
    val isManuallyAdded: Boolean = false,
    val sourcePickupRequestId: String? = null,
    val cachedAt: Long = System.currentTimeMillis()
)

fun InventoryEntity.toDomain(): InventoryItem = InventoryItem(
    id = id,
    productName = productName,
    category = category,
    quantity = quantity,
    unit = unit,
    expiryDate = expiryDate,
    status = runCatching { InventoryStatus.valueOf(status.uppercase()) }.getOrDefault(InventoryStatus.ACTIVE),
    receivedAt = receivedAt,
    distributedAt = distributedAt,
    pickupRequestId = pickupRequestId,
    photoUrl = photoUrl
)

fun InventoryItem.toEntity(): InventoryEntity = InventoryEntity(
    id = id,
    productName = productName,
    category = category,
    quantity = quantity,
    unit = unit,
    expiryDate = expiryDate,
    status = status.name.lowercase(),
    receivedAt = receivedAt,
    distributedAt = distributedAt,
    pickupRequestId = pickupRequestId,
    photoUrl = photoUrl
)
