package com.clearchain.app.domain.model

data class SavedListing(
    val savedId: String,
    val savedAt: String,
    val listingId: String,
    val listingTitle: String,
    val category: String,
    val quantity: Int,
    val unit: String,
    val expiryDate: String?,
    val status: String,
    val imageUrl: String?,
    val groceryName: String,
    val location: String
)
