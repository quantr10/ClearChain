package com.clearchain.app.data.local.dao

import androidx.room.*
import com.clearchain.app.data.local.entity.AuthTokenEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AuthTokenDao {

    @Query("SELECT * FROM auth_tokens WHERE id = 1")
    suspend fun getTokens(): AuthTokenEntity?

    @Query("SELECT * FROM auth_tokens WHERE id = 1")
    fun getTokensFlow(): Flow<AuthTokenEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveTokens(tokens: AuthTokenEntity)

    @Query("DELETE FROM auth_tokens")
    suspend fun clearTokens()
}