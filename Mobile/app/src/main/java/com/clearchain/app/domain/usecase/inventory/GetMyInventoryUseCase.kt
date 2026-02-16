package com.clearchain.app.domain.usecase.inventory

import com.clearchain.app.data.remote.api.InventoryApi
import com.clearchain.app.data.remote.dto.toDomain
import com.clearchain.app.domain.model.InventoryItem
import javax.inject.Inject

class GetMyInventoryUseCase @Inject constructor(
    private val inventoryApi: InventoryApi
) {
    suspend operator fun invoke(status: String? = null): Result<List<InventoryItem>> {
        return try {
            val response = inventoryApi.getMyInventory(status)
            Result.success(response.data.map { it.toDomain() })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}