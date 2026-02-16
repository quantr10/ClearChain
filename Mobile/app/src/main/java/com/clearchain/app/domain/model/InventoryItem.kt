package com.clearchain.app.domain.model

data class InventoryItem(
    val id: String,
    val productName: String,
    val category: String,
    val quantity: Double,
    val unit: String,
    val expiryDate: String,
    val status: InventoryStatus,
    val receivedAt: String,
    val distributedAt: String?
)

enum class InventoryStatus {
    ACTIVE,
    DISTRIBUTED,
    EXPIRED
}