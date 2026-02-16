package com.clearchain.app.domain.usecase.inventory

import com.clearchain.app.data.remote.api.InventoryApi
import com.clearchain.app.data.remote.dto.toDomain
import com.clearchain.app.domain.model.InventoryItem
import javax.inject.Inject

class DistributeItemUseCase @Inject constructor(
    private val inventoryApi: InventoryApi
) {
    suspend operator fun invoke(itemId: String): Result<InventoryItem> {
        return try {
            val response = inventoryApi.distributeItem(itemId)
            Result.success(response.data.toDomain())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}