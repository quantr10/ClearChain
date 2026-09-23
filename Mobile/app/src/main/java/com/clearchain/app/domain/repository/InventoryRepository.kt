package com.clearchain.app.domain.repository

import com.clearchain.app.domain.model.InventoryItem

interface InventoryRepository {

    suspend fun getMyInventory(status: String? = null): Result<List<InventoryItem>>

    suspend fun getInventoryItemById(id: String): Result<InventoryItem>

    suspend fun distributeItem(id: String): Result<InventoryItem>

    /** Marks the NGO's own expired-but-still-active items as expired server-side. Returns
     * how many were updated. */
    suspend fun updateExpiredItems(): Result<Int>
}
