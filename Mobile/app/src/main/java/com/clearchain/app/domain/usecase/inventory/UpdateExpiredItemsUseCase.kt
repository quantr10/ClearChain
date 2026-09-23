package com.clearchain.app.domain.usecase.inventory

import com.clearchain.app.domain.repository.InventoryRepository
import javax.inject.Inject

class UpdateExpiredItemsUseCase @Inject constructor(
    private val inventoryRepository: InventoryRepository
) {
    suspend operator fun invoke(): Result<Unit> =
        inventoryRepository.updateExpiredItems().map { }
}
