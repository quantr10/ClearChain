package com.clearchain.app.domain.usecase.inventory

import com.clearchain.app.data.remote.api.InventoryApi
import javax.inject.Inject

class UpdateExpiredItemsUseCase @Inject constructor(
    private val inventoryApi: InventoryApi
) {
    suspend operator fun invoke(): Result<Unit> {
        return try {
            inventoryApi.updateExpiredItems()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}