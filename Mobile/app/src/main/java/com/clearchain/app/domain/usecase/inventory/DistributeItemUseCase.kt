package com.clearchain.app.domain.usecase.inventory

import com.clearchain.app.domain.model.InventoryItem
import com.clearchain.app.domain.repository.InventoryRepository
import javax.inject.Inject

class DistributeItemUseCase @Inject constructor(
    private val inventoryRepository: InventoryRepository
) {
    suspend operator fun invoke(itemId: String): Result<InventoryItem> =
        inventoryRepository.distributeItem(itemId)
}
