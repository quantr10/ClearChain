package com.clearchain.app.data.repository

import com.clearchain.app.data.local.dao.InventoryDao
import com.clearchain.app.data.local.entity.toDomain
import com.clearchain.app.data.local.entity.toEntity
import com.clearchain.app.data.remote.api.InventoryApi
import com.clearchain.app.data.remote.dto.toDomain
import com.clearchain.app.domain.model.InventoryItem
import com.clearchain.app.domain.repository.InventoryRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/**
 * Was missing entirely — every inventory read/write went straight to [InventoryApi] with no
 * offline fallback, unlike Listings and PickupRequests, even though [InventoryDao] already
 * existed fully wired for it (just never called by anything). Follows the same pattern as
 * [ListingRepositoryImpl]/[PickupRequestRepositoryImpl]: write-through the cache on success,
 * fall back to it on a network failure.
 */
class InventoryRepositoryImpl @Inject constructor(
    private val inventoryApi: InventoryApi,
    private val inventoryDao: InventoryDao
) : InventoryRepository {

    override suspend fun getMyInventory(status: String?): Result<List<InventoryItem>> {
        return try {
            val response = inventoryApi.getMyInventory(status)
            val domain = response.data.map { it.toDomain() }
            inventoryDao.upsertAll(domain.map { it.toEntity() })
            Result.success(domain)
        } catch (e: Exception) {
            val cached = inventoryDao.observeAll().first().map { it.toDomain() }
                .let { items -> if (status != null) items.filter { it.status.name.equals(status, ignoreCase = true) } else items }
            if (cached.isNotEmpty()) {
                Result.success(cached)
            } else {
                Result.failure(e)
            }
        }
    }

    override suspend fun getInventoryItemById(id: String): Result<InventoryItem> {
        return try {
            val response = inventoryApi.getInventoryItemById(id)
            val domain = response.data.toDomain()
            inventoryDao.upsert(domain.toEntity())
            Result.success(domain)
        } catch (e: Exception) {
            val cached = inventoryDao.getById(id)
            if (cached != null) {
                Result.success(cached.toDomain())
            } else {
                Result.failure(e)
            }
        }
    }

    override suspend fun distributeItem(id: String): Result<InventoryItem> {
        return try {
            val response = inventoryApi.distributeItem(id)
            val domain = response.data.toDomain()
            inventoryDao.upsert(domain.toEntity())
            Result.success(domain)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateExpiredItems(): Result<Int> {
        return try {
            Result.success(inventoryApi.updateExpiredItems().count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
