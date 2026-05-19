package com.clearchain.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.clearchain.app.data.local.entity.InventoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InventoryDao {

    @Query("SELECT * FROM inventory WHERE status = 'active' ORDER BY expiryDate ASC")
    fun observeActive(): Flow<List<InventoryEntity>>

    @Query("SELECT * FROM inventory ORDER BY receivedAt DESC")
    fun observeAll(): Flow<List<InventoryEntity>>

    @Query("SELECT * FROM inventory WHERE id = :id")
    suspend fun getById(id: String): InventoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<InventoryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: InventoryEntity)

    @Query("UPDATE inventory SET status = :status, distributedAt = :distributedAt, beneficiaryCount = :beneficiaryCount WHERE id = :id")
    suspend fun markDistributed(id: String, status: String, distributedAt: String, beneficiaryCount: Int?)

    @Query("DELETE FROM inventory")
    suspend fun clearAll()

    @Query("SELECT cachedAt FROM inventory ORDER BY cachedAt DESC LIMIT 1")
    suspend fun getLatestCacheTime(): Long?

    @Query("SELECT * FROM inventory WHERE expiryDate <= :isoDate AND status = 'active' ORDER BY expiryDate ASC")
    suspend fun getExpiringSoon(isoDate: String): List<InventoryEntity>

    @Query("SELECT category, COUNT(*) as count FROM inventory WHERE status = 'active' GROUP BY category")
    suspend fun getCategoryBreakdown(): List<CategoryCount>
}

data class CategoryCount(val category: String, val count: Int)
