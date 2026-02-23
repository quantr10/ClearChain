package com.clearchain.app.domain.model

import android.annotation.SuppressLint
import kotlinx.serialization.Serializable

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class ListingGroup(
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