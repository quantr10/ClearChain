package com.clearchain.app.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class SavedListingItem(
    val savedId: String,
    val savedAt: String,
    val listing: SavedListingDetail? = null
)

@Serializable
data class SavedListingDetail(
    val id: String,
    val title: String,
    val category: String,
    val quantity: Int,
    val unit: String,
    val expiryDate: String? = null,
    val status: String,
    val imageUrl: String? = null,
    val groceryName: String,
    val location: String,
    val createdAt: String
)

@Serializable
data class SavedListingsResponse(
    val message: String = "",
    val data: List<SavedListingItem> = emptyList(),
    val total: Int = 0,
    val page: Int = 1,
    val pageSize: Int = 20,
    val totalPages: Int = 1
)

@Serializable
data class SavedListingIdsResponse(
    val data: List<String> = emptyList()
)

@Serializable
data class SavedListingToggleResponse(
    val message: String = "",
    val saved: Boolean = false
)
