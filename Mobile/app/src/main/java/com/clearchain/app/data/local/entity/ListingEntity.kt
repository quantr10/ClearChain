package com.clearchain.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.clearchain.app.domain.model.FoodCategory
import com.clearchain.app.domain.model.Listing
import com.clearchain.app.domain.model.ListingStatus

@Entity(tableName = "listings")
data class ListingEntity(
    @PrimaryKey val id: String,
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
    val groupId: String? = null,
    val splitReason: String = "new_listing",
    val relatedRequestId: String? = null,
    val splitIndex: Int = 0,
    val distanceKm: Double? = null,
    val cachedAt: Long = System.currentTimeMillis()
)

fun ListingEntity.toDomain(): Listing = Listing(
    id = id,
    groceryId = groceryId,
    groceryName = groceryName,
    title = title,
    description = description,
    category = runCatching { FoodCategory.valueOf(category.uppercase()) }.getOrDefault(FoodCategory.OTHER),
    quantity = quantity,
    unit = unit,
    expiryDate = expiryDate,
    pickupTimeStart = pickupTimeStart,
    pickupTimeEnd = pickupTimeEnd,
    status = runCatching { ListingStatus.valueOf(status.uppercase()) }.getOrDefault(ListingStatus.AVAILABLE),
    imageUrl = imageUrl,
    location = location,
    createdAt = createdAt,
    groupId = groupId,
    splitReason = splitReason,
    relatedRequestId = relatedRequestId,
    splitIndex = splitIndex,
    distanceKm = distanceKm
)

fun Listing.toEntity(): ListingEntity = ListingEntity(
    id = id,
    groceryId = groceryId,
    groceryName = groceryName,
    title = title,
    description = description,
    category = category.name.lowercase(),
    quantity = quantity,
    unit = unit,
    expiryDate = expiryDate,
    pickupTimeStart = pickupTimeStart,
    pickupTimeEnd = pickupTimeEnd,
    status = status.name.lowercase(),
    imageUrl = imageUrl,
    location = location,
    createdAt = createdAt,
    groupId = groupId,
    splitReason = splitReason,
    relatedRequestId = relatedRequestId,
    splitIndex = splitIndex,
    distanceKm = distanceKm
)
