package com.clearchain.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.clearchain.app.data.local.entity.ListingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ListingDao {

    @Query("SELECT * FROM listings WHERE status = 'available' ORDER BY expiryDate ASC")
    fun observeAvailableListings(): Flow<List<ListingEntity>>

    @Query("SELECT * FROM listings WHERE groceryId = :groceryId ORDER BY createdAt DESC")
    fun observeListingsByGrocery(groceryId: String): Flow<List<ListingEntity>>

    @Query("SELECT * FROM listings WHERE id = :id")
    suspend fun getById(id: String): ListingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(listings: List<ListingEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(listing: ListingEntity)

    @Query("DELETE FROM listings WHERE groceryId = :groceryId")
    suspend fun deleteByGrocery(groceryId: String)

    @Query("DELETE FROM listings WHERE status = 'available'")
    suspend fun deleteAllAvailable()

    @Query("DELETE FROM listings")
    suspend fun clearAll()

    @Query("SELECT cachedAt FROM listings ORDER BY cachedAt DESC LIMIT 1")
    suspend fun getLatestCacheTime(): Long?

    @Query("SELECT * FROM listings WHERE expiryDate <= :isoDate AND status = 'available' ORDER BY expiryDate ASC")
    suspend fun getExpiringSoon(isoDate: String): List<ListingEntity>
}
