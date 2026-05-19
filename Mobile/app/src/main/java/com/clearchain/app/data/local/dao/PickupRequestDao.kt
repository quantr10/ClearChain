package com.clearchain.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.clearchain.app.data.local.entity.PickupRequestEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PickupRequestDao {

    @Query("SELECT * FROM pickup_requests WHERE groceryId = :groceryId ORDER BY createdAt DESC")
    fun observeByGrocery(groceryId: String): Flow<List<PickupRequestEntity>>

    @Query("SELECT * FROM pickup_requests WHERE ngoId = :ngoId ORDER BY createdAt DESC")
    fun observeByNgo(ngoId: String): Flow<List<PickupRequestEntity>>

    @Query("SELECT * FROM pickup_requests WHERE id = :id")
    suspend fun getById(id: String): PickupRequestEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(requests: List<PickupRequestEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(request: PickupRequestEntity)

    @Query("UPDATE pickup_requests SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: String)

    @Query("DELETE FROM pickup_requests WHERE groceryId = :groceryId")
    suspend fun deleteByGrocery(groceryId: String)

    @Query("DELETE FROM pickup_requests WHERE ngoId = :ngoId")
    suspend fun deleteByNgo(ngoId: String)

    @Query("DELETE FROM pickup_requests")
    suspend fun clearAll()

    @Query("SELECT cachedAt FROM pickup_requests ORDER BY cachedAt DESC LIMIT 1")
    suspend fun getLatestCacheTime(): Long?

    @Query("SELECT * FROM pickup_requests WHERE ngoId = :ngoId AND status IN ('approved', 'ready') ORDER BY pickupDate ASC")
    suspend fun getUpcomingForNgo(ngoId: String): List<PickupRequestEntity>
}
