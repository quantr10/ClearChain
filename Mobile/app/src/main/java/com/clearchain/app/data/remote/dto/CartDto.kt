package com.clearchain.app.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class CartResponse(
    val message: String,
    val data: List<CartGroupData> = emptyList()
)

@Serializable
data class CartGroupData(
    val groceryId: String,
    val groceryName: String,
    val groceryProfilePictureUrl: String? = null,
    val items: List<CartItemData> = emptyList(),
    val earliestExpiryDate: String? = null,
    val pickupTimeStart: String? = null,
    val pickupTimeEnd: String? = null,
    val canCheckout: Boolean = false
)

@Serializable
data class CartItemData(
    val id: String,
    val listingId: String,
    val groceryId: String,
    val groceryName: String,
    val groceryProfilePictureUrl: String? = null,
    val title: String,
    val category: String,
    val unit: String,
    val requestedQuantity: Int,
    val maxQuantity: Double,
    val expiryDate: String? = null,
    val imageUrl: String? = null,
    val pickupTimeStart: String? = null,
    val pickupTimeEnd: String? = null,
    val status: String,
    val isValid: Boolean,
    val invalidReason: String? = null
)

@Serializable
data class AddCartItemRequest(
    val listingId: String,
    val quantity: Int = 1
)

@Serializable
data class UpdateCartItemRequest(
    val quantity: Int
)

@Serializable
data class CheckoutCartGroupRequest(
    val groceryId: String,
    val pickupDate: String,
    val pickupTime: String,
    val notes: String? = null,
    val requiresRefrigeration: Boolean = false,
    val isFragile: Boolean = false,
    val isHeavy: Boolean = false
)
