package com.clearchain.app.data.local.dao

import androidx.room.*
import com.clearchain.app.data.local.entity.FCMTokenEntity

@Dao
interface FCMTokenDao {
    @Query("SELECT token FROM fcm_tokens WHERE id = 1")
    suspend fun getToken(): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveToken(token: FCMTokenEntity)

    @Query("DELETE FROM fcm_tokens")
    suspend fun clearToken()
}